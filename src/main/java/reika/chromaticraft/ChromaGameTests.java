package reika.chromaticraft;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;

import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.data.ChromaTestStructureProvider;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.registry.CrystalElement;

import java.util.List;
import java.util.function.Consumer;

/**
 * In-world game tests for the progression core — runnable headless via
 * {@code gradlew :ChromatiCraft:runGameTest} or interactively with {@code /test runall} (the
 * {@code chromaticraft} namespace is enabled in build.gradle). They exercise the runtime path the
 * datagen smoke-test can't: granting a stage to a real player and querying it back (storage +
 * recursive-parent propagation + prerequisite gating + the colour→ALLCOLORS integration).
 *
 * <p>The {@link DirectInstance} codec workaround is copied from RotaryCraft's {@code RotaryGameTests}:
 * NeoForge 26.x's {@code RegisterGameTestsEvent} only registers {@link GameTestInstance}s, and the
 * vanilla function-backed instance resolves its body through the {@code TEST_FUNCTION} registry
 * (frozen before mod construction), so the body is carried directly instead.
 */
public final class ChromaGameTests {

	private ChromaGameTests() {}

	/** Codec types for our in-code {@link GameTestInstance}s; this registry is network-synced. */
	public static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_INSTANCE_TYPES =
			DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, ChromatiCraft.MODID);

	static {
		TEST_INSTANCE_TYPES.register("direct", () -> DirectInstance.CODEC);
	}

	public static void onRegisterGameTests(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "default"),
				new TestEnvironmentDefinition.AllOf(List.of()));

		register(event, env, "progression_grant_and_query", ChromaGameTests::grantAndQuery);
		register(event, env, "progression_recursive_parents", ChromaGameTests::recursiveParents);
		register(event, env, "progression_prereq_gating", ChromaGameTests::prereqGating);
		register(event, env, "progression_color_discovery", ChromaGameTests::colorDiscovery);
	}

	/** Grant a stage directly and read it back; an unrelated stage stays ungranted. */
	private static void grantAndQuery(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		ProgressionManager.instance.setPlayerStage(p, ProgressStage.CRYSTALS, true, false, false);
		helper.assertTrue(ProgressStage.CRYSTALS.isPlayerAtStage(p), "CRYSTALS should be granted");
		helper.assertTrue(!ProgressStage.PYLON.isPlayerAtStage(p), "PYLON should NOT be granted");
		helper.succeed();
	}

	/** Granting a deep stage force-sets its whole recursive-parent chain. */
	private static void recursiveParents(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		// RUNEUSE ← {ALLCOLORS ← PYLON, CASTING ← CRYSTALS}
		ProgressionManager.instance.setPlayerStage(p, ProgressStage.RUNEUSE, true, false, false);
		helper.assertTrue(ProgressStage.RUNEUSE.isPlayerAtStage(p), "RUNEUSE granted");
		helper.assertTrue(ProgressStage.ALLCOLORS.isPlayerAtStage(p), "ALLCOLORS (parent) granted recursively");
		helper.assertTrue(ProgressStage.PYLON.isPlayerAtStage(p), "PYLON (ancestor) granted recursively");
		helper.assertTrue(ProgressStage.CASTING.isPlayerAtStage(p), "CASTING (parent) granted recursively");
		helper.assertTrue(ProgressStage.CRYSTALS.isPlayerAtStage(p), "CRYSTALS (ancestor) granted recursively");
		helper.succeed();
	}

	/** A stage cannot be stepped to without its prerequisites, and can once they are present. */
	private static void prereqGating(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(!ProgressionManager.instance.canStepPlayerTo(p, ProgressStage.RUNEUSE),
				"RUNEUSE should be blocked with no prerequisites");
		ProgressionManager.instance.setPlayerStage(p, ProgressStage.ALLCOLORS, true, false, false);
		ProgressionManager.instance.setPlayerStage(p, ProgressStage.CASTING, true, false, false);
		helper.assertTrue(ProgressionManager.instance.canStepPlayerTo(p, ProgressStage.RUNEUSE),
				"RUNEUSE should be reachable once ALLCOLORS + CASTING are held");
		helper.succeed();
	}

	/** Discovering all 16 colours (with the PYLON prereq held) auto-grants ALLCOLORS. */
	private static void colorDiscovery(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		ProgressionManager.instance.setPlayerStage(p, ProgressStage.PYLON, true, false, false);
		for (CrystalElement e : CrystalElement.elements)
			ProgressionManager.instance.setPlayerDiscoveredColor(p, e, true, false);
		helper.assertTrue(ProgressionManager.instance.hasPlayerDiscoveredColor(p, CrystalElement.WHITE),
				"discovered colour should be stored");
		helper.assertTrue(ProgressStage.ALLCOLORS.isPlayerAtStage(p),
				"ALLCOLORS should be auto-granted after all 16 colours (PYLON prereq held)");
		helper.succeed();
	}

	private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env,
			String name, Consumer<GameTestHelper> body) {
		Identifier id = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, name);
		TestData<Holder<TestEnvironmentDefinition<?>>> data =
				new TestData<>(env, ChromaTestStructureProvider.ARENA, 20, 0, true, Rotation.NONE);
		event.registerTest(id, new DirectInstance(data, body));
	}

	/** A {@link GameTestInstance} carrying its body as a plain {@link Consumer} (see class javadoc). */
	private static final class DirectInstance extends GameTestInstance {

		static final MapCodec<DirectInstance> CODEC =
				TestData.CODEC.xmap(data -> new DirectInstance(data, h -> {}), inst -> inst.info);

		private final TestData<Holder<TestEnvironmentDefinition<?>>> info;
		private final Consumer<GameTestHelper> body;

		DirectInstance(TestData<Holder<TestEnvironmentDefinition<?>>> info, Consumer<GameTestHelper> body) {
			super(info);
			this.info = info;
			this.body = body;
		}

		@Override
		public void run(GameTestHelper helper) {
			body.accept(helper);
		}

		@Override
		public MapCodec<? extends GameTestInstance> codec() {
			return CODEC;
		}

		@Override
		protected MutableComponent typeDescription() {
			return Component.literal("chromaticraft direct test");
		}
	}
}
