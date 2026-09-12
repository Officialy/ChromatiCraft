package reika.chromaticraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import reika.chromaticraft.client.ClientPayloadHandlers;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.PylonAttackOverlay;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;

public final class ChromaNetwork {

	private ChromaNetwork() {}

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(ChromatiCraft.MODID).versioned("1");
		registrar.playToClient(RelayConnection.TYPE, RelayConnection.CODEC,
				(payload, context) -> context.enqueueWork(() -> ClientPayloadHandlers.relayConnection(
						payload.path(), element(payload.color()))));
		registrar.playToClient(ProximaLayoutSeed.TYPE, ProximaLayoutSeed.CODEC,
				ChromaNetwork::handleProximaLayoutSeed);
		registrar.playToClient(MonumentRitualState.TYPE, MonumentRitualState.CODEC,
				ChromaNetwork::handleMonumentRitualState);
		registrar.playToClient(AttackBeam.TYPE, AttackBeam.CODEC, ChromaNetwork::handleAttackBeam);
		registrar.playToClient(Discharge.TYPE, Discharge.CODEC, ChromaNetwork::handleDischarge);
		registrar.playToClient(AttackReceive.TYPE, AttackReceive.CODEC, ChromaNetwork::handleAttackReceive);
		registrar.playToClient(JarRejection.TYPE, JarRejection.CODEC, ChromaNetwork::handleJarRejection);
		registrar.playToClient(PowerCrystalDestroy.TYPE, PowerCrystalDestroy.CODEC, ChromaNetwork::handlePowerCrystalDestroy);
		registrar.playToClient(PylonCrystalBreak.TYPE, PylonCrystalBreak.CODEC, ChromaNetwork::handlePylonCrystalBreak);
		registrar.playToClient(RepeaterConnections.TYPE, RepeaterConnections.CODEC, ChromaNetwork::handleRepeaterConnections);
		registrar.playToClient(RepeaterSurgeBurst.TYPE, RepeaterSurgeBurst.CODEC,
				ChromaNetwork::handleRepeaterSurgeBurst);
		registrar.playToClient(ProgressionNote.TYPE, ProgressionNote.CODEC, ChromaNetwork::handleProgressionNote);
		registrar.playToServer(SelectResearchFragment.TYPE, SelectResearchFragment.CODEC,
				ChromaNetwork::handleSelectResearchFragment);
		registrar.playToServer(SelectFragmentChoice.TYPE, SelectFragmentChoice.CODEC,
				ChromaNetwork::handleSelectFragmentChoice);
		registrar.playToServer(RecoverResearchPage.TYPE, RecoverResearchPage.CODEC,
				ChromaNetwork::handleRecoverResearchPage);
		registrar.playToServer(TransferLexiconPage.TYPE, TransferLexiconPage.CODEC,
				ChromaNetwork::handleTransferLexiconPage);
		registrar.playToServer(ScrollLexiconPages.TYPE, ScrollLexiconPages.CODEC,
				ChromaNetwork::handleScrollLexiconPages);
		registrar.playToServer(UpdateLexiconNotes.TYPE, UpdateLexiconNotes.CODEC,
				ChromaNetwork::handleUpdateLexiconNotes);
		registrar.playToServer(RequestGuideCastingRecipes.TYPE, RequestGuideCastingRecipes.CODEC,
				ChromaNetwork::handleRequestGuideCastingRecipes);
		registrar.playToClient(GuideCastingRecipes.TYPE, GuideCastingRecipes.CODEC,
				ChromaNetwork::handleGuideCastingRecipes);
		registrar.playToServer(RequestGuideCraftingRecipes.TYPE, RequestGuideCraftingRecipes.CODEC,
				ChromaNetwork::handleRequestGuideCraftingRecipes);
		registrar.playToClient(GuideCraftingRecipes.TYPE, GuideCraftingRecipes.CODEC,
				ChromaNetwork::handleGuideCraftingRecipes);
		registrar.playToClient(DataNodeScan.TYPE, DataNodeScan.CODEC, ChromaNetwork::handleDataNodeScan);
		registrar.playToClient(TowerLocations.TYPE, TowerLocations.CODEC, ChromaNetwork::handleTowerLocations);
		registrar.playToClient(LoreNote.TYPE, LoreNote.CODEC, ChromaNetwork::handleLoreNote);
		registrar.playToClient(Inscription.TYPE, Inscription.CODEC, ChromaNetwork::handleInscription);
		registrar.playToClient(OpenLorePuzzle.TYPE, OpenLorePuzzle.CODEC, ChromaNetwork::handleOpenLorePuzzle);
		registrar.playToServer(LorePuzzleMove.TYPE, LorePuzzleMove.CODEC, ChromaNetwork::handleLorePuzzleMove);
		registrar.playToServer(SetHeatLampTemperature.TYPE, SetHeatLampTemperature.CODEC,
				ChromaNetwork::handleSetHeatLampTemperature);
		registrar.playToServer(ToggleCrystalCharger.TYPE, ToggleCrystalCharger.CODEC,
				ChromaNetwork::handleToggleCrystalCharger);
	}

	public static void sendAttack(ServerLevel level, BlockPos source, LivingEntity target, CrystalElement color, float size) {
		BlockPos end = BlockPos.containing(target.getX(), target.getY() + target.getBbHeight() * 0.65, target.getZ());
		PacketDistributor.sendToPlayersNear(level, null, source.getX() + 0.5, source.getY() + 0.5,
				source.getZ() + 0.5, 128, new AttackBeam(source, end, color.ordinal()));
		if (target instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new AttackReceive(color.ordinal()));
	}

	public static void sendPowerCrystalDestroy(ServerLevel level, BlockPos crystal) {
		PacketDistributor.sendToPlayersNear(level, null, crystal.getX() + 0.5, crystal.getY() + 0.5,
				crystal.getZ() + 0.5, 32, new PowerCrystalDestroy(crystal));
	}

	public static void sendPylonCrystalBreak(ServerLevel level, BlockPos pylon, CrystalElement color) {
		PacketDistributor.sendToPlayersNear(level, null, pylon.getX() + 0.5, pylon.getY() + 0.5,
				pylon.getZ() + 0.5, 64, new PylonCrystalBreak(pylon, color.ordinal()));
	}
	public static void sendRepeaterConnections(ServerLevel level, BlockPos repeater) {
		PacketDistributor.sendToPlayersNear(level, null, repeater.getX() + 0.5, repeater.getY() + 0.5,
				repeater.getZ() + 0.5, 128, new RepeaterConnections(repeater));
	}
	public static void sendRepeaterSurgeBurst(ServerLevel level, BlockPos repeater, CrystalElement color) {
		PacketDistributor.sendToPlayersNear(level, null, repeater.getX() + 0.5, repeater.getY() + 0.5,
				repeater.getZ() + 0.5, 64, new RepeaterSurgeBurst(repeater, color.ordinal()));
	}
	public static void sendJarRejection(ServerLevel level, BlockPos pylon, CrystalElement color) {
		PacketDistributor.sendToPlayersNear(level, null, pylon.getX() + 0.5, pylon.getY() + 0.5,
				pylon.getZ() + 0.5, 128, new JarRejection(pylon, color.ordinal()));
	}

	/**
	 * The seed Proxima's layout was built from.
	 *
	 * <p>The sky rivers are wholly derived from it, and there are tens of thousands of points across the
	 * dimension, so sending the seed and letting the client run the same generator is enormously cheaper
	 * than streaming the geometry — which is what V33a does, in scheduled batches, precisely because a
	 * 1.7.10 client had no way to recompute it. Upstream already ships a seed to clients for the same
	 * reason elsewhere: {@code StructureCalculator.assignSeed} sends one so a client can recompute the
	 * colour-to-structure map.
	 */
	/**
	 * Starts or stops the client's copy of the monument ritual's effects.
	 *
	 * <p>One packet rather than V33a's separate start, end, complete and reset messages: the client half
	 * runs its own copy of the same score, so all the server has to say is where the monument is and
	 * whether the ceremony is on. Its four messages exist because upstream's single class held both
	 * halves and had to be told about each transition; here there is nothing to transition.
	 */
	public record MonumentRitualState(BlockPos pos, boolean running, boolean inProxima)
			implements CustomPacketPayload {
		public static final Type<MonumentRitualState> TYPE = createType("monument_ritual_state");
		public static final StreamCodec<ByteBuf, MonumentRitualState> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, MonumentRitualState::pos,
				ByteBufCodecs.BOOL, MonumentRitualState::running,
				ByteBufCodecs.BOOL, MonumentRitualState::inProxima, MonumentRitualState::new);
		@Override public Type<MonumentRitualState> type() { return TYPE; }
	}

	public static void sendMonumentRitualState(ServerLevel level, BlockPos pos, boolean running,
			boolean inProxima) {
		// Everyone near enough to hear it: the tracks are unattenuated, so the audible radius is the
		// ceremony's real reach rather than the particles'.
		PacketDistributor.sendToPlayersNear(level, null, pos.getX(), pos.getY(), pos.getZ(), 256,
				new MonumentRitualState(pos, running, inProxima));
	}

	private static void handleMonumentRitualState(MonumentRitualState payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.monumentRitualState(
				payload.pos(), payload.running(), payload.inProxima()));
	}

	public record ProximaLayoutSeed(long seed) implements CustomPacketPayload {
		public static final Type<ProximaLayoutSeed> TYPE = createType("proxima_layout_seed");
		public static final StreamCodec<ByteBuf, ProximaLayoutSeed> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_LONG, ProximaLayoutSeed::seed, ProximaLayoutSeed::new);
		@Override public Type<ProximaLayoutSeed> type() { return TYPE; }
	}

	/** Tells the player's client which seed to build its own copy of the sky rivers from. */
	public static void sendProximaLayoutSeed(ServerPlayer player, long seed) {
		PacketDistributor.sendToPlayer(player, new ProximaLayoutSeed(seed));
	}

	private static void handleProximaLayoutSeed(ProximaLayoutSeed payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.proximaLayoutSeed(payload.seed()));
	}

	public record AttackBeam(BlockPos source, BlockPos target, int color) implements CustomPacketPayload {
		public static final Type<AttackBeam> TYPE = createType("pylon_attack");
		public static final StreamCodec<ByteBuf, AttackBeam> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, AttackBeam::source, BlockPos.STREAM_CODEC, AttackBeam::target,
				ByteBufCodecs.VAR_INT, AttackBeam::color, AttackBeam::new);
		@Override public Type<AttackBeam> type() { return TYPE; }
	}

	public record Discharge(BlockPos source, int targetId, int color, float size) implements CustomPacketPayload {
		public static final Type<Discharge> TYPE = createType("pylon_discharge");
		public static final StreamCodec<ByteBuf, Discharge> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Discharge::source, ByteBufCodecs.VAR_INT, Discharge::targetId,
				ByteBufCodecs.VAR_INT, Discharge::color, ByteBufCodecs.FLOAT, Discharge::size, Discharge::new);
		@Override public Type<Discharge> type() { return TYPE; }
	}

	public record AttackReceive(int color) implements CustomPacketPayload {
		public static final Type<AttackReceive> TYPE = createType("pylon_attack_receive");
		public static final StreamCodec<ByteBuf, AttackReceive> CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_INT, AttackReceive::color, AttackReceive::new);
		@Override public Type<AttackReceive> type() { return TYPE; }
	}

	/**
	 * V33a fires this whenever a player gains a progression stage; the client answers with
	 * ChromaSounds.GAINPROGRESS. Without it a granted stage is completely silent, so there is no way
	 * to tell a working trigger from a broken one.
	 */
	public record ProgressionNote(boolean researchLevel, int ordinal) implements CustomPacketPayload {
		public static final Type<ProgressionNote> TYPE = createType("progression_note");
		public static final StreamCodec<ByteBuf, ProgressionNote> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, ProgressionNote::researchLevel,
				ByteBufCodecs.VAR_INT, ProgressionNote::ordinal, ProgressionNote::new);
		@Override public Type<ProgressionNote> type() { return TYPE; }
	}

	public record SelectResearchFragment(String pageId) implements CustomPacketPayload {
		public static final Type<SelectResearchFragment> TYPE = createType("select_research_fragment");
		public static final StreamCodec<ByteBuf, SelectResearchFragment> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, SelectResearchFragment::pageId, SelectResearchFragment::new);
		@Override public Type<SelectResearchFragment> type() { return TYPE; }
	}

	public record RecoverResearchPage(String pageId) implements CustomPacketPayload {
		public static final Type<RecoverResearchPage> TYPE = createType("recover_research_page");
		public static final StreamCodec<ByteBuf, RecoverResearchPage> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, RecoverResearchPage::pageId, RecoverResearchPage::new);
		@Override public Type<RecoverResearchPage> type() { return TYPE; }
	}

	public record TransferLexiconPage(String pageId, boolean intoBook) implements CustomPacketPayload {
		public static final Type<TransferLexiconPage> TYPE = createType("transfer_lexicon_page");
		public static final StreamCodec<ByteBuf, TransferLexiconPage> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, TransferLexiconPage::pageId,
				ByteBufCodecs.BOOL, TransferLexiconPage::intoBook, TransferLexiconPage::new);
		@Override public Type<TransferLexiconPage> type() { return TYPE; }
	}

	/** V33a BOOKNOTESRESET + BOOKNOTE combined into one atomic, bounded 26.2 update. */
	public record UpdateLexiconNotes(java.util.List<String> notes) implements CustomPacketPayload {
		public static final Type<UpdateLexiconNotes> TYPE = createType("update_lexicon_notes");
		public UpdateLexiconNotes {
			java.util.ArrayList<String> bounded = new java.util.ArrayList<>();
			for (int i = 0; i < Math.min(notes.size(), 256); i++) {
				String note = notes.get(i);
				bounded.add(note.length() <= 1024 ? note : note.substring(0, 1024));
			}
			notes = java.util.List.copyOf(bounded);
		}
		public static final StreamCodec<RegistryFriendlyByteBuf, UpdateLexiconNotes> CODEC = StreamCodec.of(
				(buffer, payload) -> {
					buffer.writeVarInt(payload.notes.size());
					for (String note : payload.notes)
						buffer.writeUtf(note, 4096);
				}, buffer -> {
					int count = buffer.readVarInt();
					if (count < 0 || count > 256)
						throw new IllegalArgumentException("Invalid lexicon note count " + count);
					java.util.ArrayList<String> notes = new java.util.ArrayList<>(count);
					for (int i = 0; i < count; i++)
						notes.add(buffer.readUtf(4096));
					return new UpdateLexiconNotes(java.util.List.copyOf(notes));
				});
		@Override public Type<UpdateLexiconNotes> type() { return TYPE; }
	}

	/** Client asks only for the recipe output currently being viewed; no whole recipe registry sync. */
	/**
	 * The guide book has to show a grid recipe whether or not the player has unlocked it. The recipe
	 * book cannot answer that -- it only holds what the server has awarded -- and the client has no
	 * other copy, since {@code ClientRecipeContainer} carries only item sets and stonecutter recipes.
	 * So the page asks, exactly as the casting page does.
	 */
	public record RequestGuideCraftingRecipes(String itemId) implements CustomPacketPayload {
		public static final Type<RequestGuideCraftingRecipes> TYPE = createType("request_guide_crafting_recipes");
		public static final StreamCodec<ByteBuf, RequestGuideCraftingRecipes> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, RequestGuideCraftingRecipes::itemId, RequestGuideCraftingRecipes::new);
		@Override public Type<RequestGuideCraftingRecipes> type() { return TYPE; }
	}

	/** Vanilla's own display record, so the page renders the same data the recipe book would. */
	public record GuideCraftingRecipes(String itemId,
			java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplayEntry> recipes)
			implements CustomPacketPayload {
		public static final Type<GuideCraftingRecipes> TYPE = createType("guide_crafting_recipes");
		public static final StreamCodec<RegistryFriendlyByteBuf, GuideCraftingRecipes> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, GuideCraftingRecipes::itemId,
				net.minecraft.world.item.crafting.display.RecipeDisplayEntry.STREAM_CODEC
						.apply(ByteBufCodecs.list()), GuideCraftingRecipes::recipes,
				GuideCraftingRecipes::new);
		@Override public Type<GuideCraftingRecipes> type() { return TYPE; }
	}

	public record RequestGuideCastingRecipes(String itemId) implements CustomPacketPayload {
		public static final Type<RequestGuideCastingRecipes> TYPE = createType("request_guide_casting_recipes");
		public static final StreamCodec<ByteBuf, RequestGuideCastingRecipes> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, RequestGuideCastingRecipes::itemId, RequestGuideCastingRecipes::new);
		@Override public Type<RequestGuideCastingRecipes> type() { return TYPE; }
	}

	/** Exact registered casting recipes needed by one open lexicon page. */
	public record GuideCastingRecipes(String itemId, java.util.List<CastingTableRecipe> recipes)
			implements CustomPacketPayload {
		public static final Type<GuideCastingRecipes> TYPE = createType("guide_casting_recipes");
		public static final StreamCodec<RegistryFriendlyByteBuf, GuideCastingRecipes> CODEC = StreamCodec.of(
				(buffer, payload) -> {
					buffer.writeUtf(payload.itemId);
					buffer.writeVarInt(payload.recipes.size());
					for (CastingTableRecipe recipe : payload.recipes)
						CastingTableRecipe.STREAM_CODEC.encode(buffer, recipe);
				},
				buffer -> {
					String itemId = buffer.readUtf();
					int count = buffer.readVarInt();
					java.util.ArrayList<CastingTableRecipe> recipes = new java.util.ArrayList<>(count);
					for (int i = 0; i < count; i++)
						recipes.add(CastingTableRecipe.STREAM_CODEC.decode(buffer));
					return new GuideCastingRecipes(itemId, java.util.List.copyOf(recipes));
				});
		@Override public Type<GuideCastingRecipes> type() { return TYPE; }
	}

	/** V33a DATASCAN: starts the local 360-seed ring, sky column and layered completion chord. */
	public record DataNodeScan(BlockPos source) implements CustomPacketPayload {
		public static final Type<DataNodeScan> TYPE = createType("data_node_scan");
		public static final StreamCodec<ByteBuf, DataNodeScan> CODEC =
				StreamCodec.composite(BlockPos.STREAM_CODEC, DataNodeScan::source, DataNodeScan::new);
		@Override public Type<DataNodeScan> type() { return TYPE; }
	}

	/** The thirteen fixed lore-tower roots; needed by the original inter-tower seed directions. */
	public record TowerLocations(int[] coordinates) implements CustomPacketPayload {
		public static final Type<TowerLocations> TYPE = createType("tower_locations");
		public static final StreamCodec<ByteBuf, TowerLocations> CODEC = StreamCodec.of(
				(buffer, payload) -> {
					buffer.writeInt(payload.coordinates.length);
					for (int value : payload.coordinates) buffer.writeInt(value);
				},
				buffer -> {
					int count = buffer.readInt();
					if (count < 0 || count > reika.chromaticraft.magic.lore.Towers.towerList.length * 2)
						throw new IllegalArgumentException("Invalid lore-tower coordinate count " + count);
					int[] values = new int[count];
					for (int i = 0; i < count; i++) values[i] = buffer.readInt();
					return new TowerLocations(values);
				});
		@Override public Type<TowerLocations> type() { return TYPE; }
	}

	/** V33a LORENOTE, emitted after the source's 50-tick post-scan delay. */
	public record LoreNote(int tower, long seed, int scannedMask) implements CustomPacketPayload {
		public static final Type<LoreNote> TYPE = createType("lore_note");
		public static final StreamCodec<RegistryFriendlyByteBuf, LoreNote> CODEC = StreamCodec.of(
				(buffer, payload) -> {
					buffer.writeVarInt(payload.tower);
					buffer.writeLong(payload.seed);
					buffer.writeVarInt(payload.scannedMask);
				}, buffer -> new LoreNote(buffer.readVarInt(), buffer.readLong(), buffer.readVarInt()));
		@Override public Type<LoreNote> type() { return TYPE; }
	}

	public record Inscription(BlockPos source, int recipe) implements CustomPacketPayload {
		public static final Type<Inscription> TYPE = createType("inscription");
		public static final StreamCodec<ByteBuf, Inscription> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Inscription::source, ByteBufCodecs.VAR_INT, Inscription::recipe,
				Inscription::new);
		@Override public Type<Inscription> type() { return TYPE; }
	}

	public record OpenLorePuzzle(long seed, int scannedMask, boolean complete, int[] moves) implements CustomPacketPayload {
		public static final Type<OpenLorePuzzle> TYPE = createType("open_lore_puzzle");
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenLorePuzzle> CODEC = StreamCodec.of(
				(buffer, payload) -> {
					buffer.writeLong(payload.seed);
					buffer.writeVarInt(payload.scannedMask);
					buffer.writeBoolean(payload.complete);
					buffer.writeVarInt(payload.moves.length);
					for (int move : payload.moves) buffer.writeVarInt(move);
				},
				buffer -> {
					long seed = buffer.readLong();
					int mask = buffer.readVarInt();
					boolean complete = buffer.readBoolean();
					int count = Mth.clamp(buffer.readVarInt(), 0, 32768);
					int[] moves = new int[count];
					for (int i = 0; i < count; i++) moves[i] = buffer.readVarInt();
					return new OpenLorePuzzle(seed, mask, complete, moves);
				});
		@Override public Type<OpenLorePuzzle> type() { return TYPE; }
	}

	public record LorePuzzleMove(int q, int r, int s) implements CustomPacketPayload {
		public static final Type<LorePuzzleMove> TYPE = createType("lore_puzzle_move");
		public static final StreamCodec<ByteBuf, LorePuzzleMove> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, LorePuzzleMove::q, ByteBufCodecs.VAR_INT, LorePuzzleMove::r,
				ByteBufCodecs.VAR_INT, LorePuzzleMove::s, LorePuzzleMove::new);
		@Override public Type<LorePuzzleMove> type() { return TYPE; }
	}

	public record JarRejection(BlockPos source, int color) implements CustomPacketPayload {
		public static final Type<JarRejection> TYPE = createType("pylon_jar_rejection");
		public static final StreamCodec<ByteBuf, JarRejection> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, JarRejection::source, ByteBufCodecs.VAR_INT, JarRejection::color,
				JarRejection::new);
		@Override public Type<JarRejection> type() { return TYPE; }
	}

	public record PowerCrystalDestroy(BlockPos source) implements CustomPacketPayload {
		public static final Type<PowerCrystalDestroy> TYPE = createType("power_crystal_destroy");
		public static final StreamCodec<ByteBuf, PowerCrystalDestroy> CODEC =
				StreamCodec.composite(BlockPos.STREAM_CODEC, PowerCrystalDestroy::source, PowerCrystalDestroy::new);
		@Override public Type<PowerCrystalDestroy> type() { return TYPE; }
	}

	public record PylonCrystalBreak(BlockPos source, int color) implements CustomPacketPayload {
		public static final Type<PylonCrystalBreak> TYPE = createType("pylon_crystal_break");
		public static final StreamCodec<ByteBuf, PylonCrystalBreak> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, PylonCrystalBreak::source, ByteBufCodecs.VAR_INT,
				PylonCrystalBreak::color, PylonCrystalBreak::new);
		@Override public Type<PylonCrystalBreak> type() { return TYPE; }
	}
	public record RepeaterConnections(BlockPos source) implements CustomPacketPayload {
		public static final Type<RepeaterConnections> TYPE = createType("repeater_connections");
		public static final StreamCodec<ByteBuf, RepeaterConnections> CODEC =
				StreamCodec.composite(BlockPos.STREAM_CODEC, RepeaterConnections::source, RepeaterConnections::new);
		@Override public Type<RepeaterConnections> type() { return TYPE; }
	}
	public record SelectFragmentChoice(int index) implements CustomPacketPayload {
		public static final Type<SelectFragmentChoice> TYPE = createType("select_fragment_choice");
		public static final StreamCodec<ByteBuf, SelectFragmentChoice> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, SelectFragmentChoice::index, SelectFragmentChoice::new);
		@Override public Type<SelectFragmentChoice> type() { return TYPE; }
	}
	public record ScrollLexiconPages(int direction) implements CustomPacketPayload {
		public static final Type<ScrollLexiconPages> TYPE = createType("scroll_lexicon_pages");
		public static final StreamCodec<ByteBuf, ScrollLexiconPages> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, ScrollLexiconPages::direction, ScrollLexiconPages::new);
		@Override public Type<ScrollLexiconPages> type() { return TYPE; }
	}
	public record RepeaterSurgeBurst(BlockPos source, int color) implements CustomPacketPayload {
		public static final Type<RepeaterSurgeBurst> TYPE = createType("repeater_surge_burst");
		public static final StreamCodec<ByteBuf, RepeaterSurgeBurst> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, RepeaterSurgeBurst::source,
				ByteBufCodecs.VAR_INT, RepeaterSurgeBurst::color, RepeaterSurgeBurst::new);
		@Override public Type<RepeaterSurgeBurst> type() { return TYPE; }
	}
	public record SetHeatLampTemperature(BlockPos source, int temperature) implements CustomPacketPayload {
		public static final Type<SetHeatLampTemperature> TYPE = createType("set_heat_lamp_temperature");
		public static final StreamCodec<ByteBuf, SetHeatLampTemperature> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, SetHeatLampTemperature::source, ByteBufCodecs.VAR_INT,
				SetHeatLampTemperature::temperature, SetHeatLampTemperature::new);
		@Override public Type<SetHeatLampTemperature> type() { return TYPE; }
	}
	public record ToggleCrystalCharger(BlockPos source, int color) implements CustomPacketPayload {
		public static final Type<ToggleCrystalCharger> TYPE = createType("toggle_crystal_charger");
		public static final StreamCodec<ByteBuf, ToggleCrystalCharger> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, ToggleCrystalCharger::source, ByteBufCodecs.VAR_INT,
				ToggleCrystalCharger::color, ToggleCrystalCharger::new);
		@Override public Type<ToggleCrystalCharger> type() { return TYPE; }
	}
	public record RelayConnection(java.util.List<BlockPos> path, int color) implements CustomPacketPayload {
		public static final Type<RelayConnection> TYPE = createType("relay_connection");
		public static final StreamCodec<ByteBuf, RelayConnection> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(64)), RelayConnection::path,
				ByteBufCodecs.VAR_INT, RelayConnection::color, RelayConnection::new);

		public RelayConnection {
			path = path.stream().map(BlockPos::immutable).toList();
			if (path.size() < 2 || path.size() > 64 || color < 0 || color >= CrystalElement.elements.length)
				throw new IllegalArgumentException("Invalid lumen relay path");
		}

		@Override public Type<RelayConnection> type() { return TYPE; }
	}

	public static void sendRelayConnection(ServerLevel level, java.util.List<BlockPos> path,
			CrystalElement color) {
		RelayConnection payload = new RelayConnection(path, color.ordinal());
		for (ServerPlayer player : level.players()) {
			for (BlockPos point : payload.path()) {
				if (player.distanceToSqr(point.getX() + 0.5, point.getY() + 0.5, point.getZ() + 0.5) < 4096) {
					PacketDistributor.sendToPlayer(player, payload);
					break;
				}
			}
		}
	}

	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createType(String path) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path));
	}

	// Every body below is a one-line hand-off. The lambdas must not touch Minecraft here: they are
	// compiled into synthetic methods of THIS class, which the dedicated server loads and verifies
	// during mod construction, and a ClientLevel/LocalPlayer descriptor in any of them fails the load.
	private static void handleAttackBeam(AttackBeam payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.attackBeam(
				payload.source, payload.target, element(payload.color)));
	}
	private static void handleDischarge(Discharge payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.discharge(
				payload.source, payload.targetId, element(payload.color)));
	}
	private static void handleProgressionNote(ProgressionNote payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.progressionNote(
				payload.researchLevel(), payload.ordinal()));
	}
	private static void handleSelectResearchFragment(SelectResearchFragment payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player))
				return;
			var page = reika.chromaticraft.magic.progression.LexiconCatalog.byId(payload.pageId());
			var stack = player.getMainHandItem();
			var data = reika.chromaticraft.magic.progression.ResearchFragmentData.read(stack);
			if (stack.is(reika.chromaticraft.registry.ChromaItems.INFO_FRAGMENT.get()) && data.blank()
					&& !data.random() && page != null
					&& reika.chromaticraft.magic.progression.PlayerResearch.nextResearch(player).contains(page))
				data.withPage(page).writeTo(stack);
		});
	}
	private static void handleRecoverResearchPage(RecoverResearchPage payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player))
				return;
			var page = reika.chromaticraft.magic.progression.LexiconCatalog.byId(payload.pageId());
			var book = player.getMainHandItem();
			if (book.is(reika.chromaticraft.registry.ChromaItems.LEXICON.get()) && page != null
					&& reika.chromaticraft.magic.progression.PlayerResearch.hasFragment(player, page))
				reika.chromaticraft.item.ItemChromaBook.recoverFragment(player, book, page);
		});
	}
	private static void handleTransferLexiconPage(TransferLexiconPage payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player))
				return;
			var page = reika.chromaticraft.magic.progression.LexiconCatalog.byId(payload.pageId());
			var book = player.getMainHandItem();
			if (!book.is(reika.chromaticraft.registry.ChromaItems.LEXICON.get()) || page == null || !page.obtainable())
				return;
			if (payload.intoBook()) {
				for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
					var stack = player.getInventory().getItem(slot);
					if (stack.is(reika.chromaticraft.registry.ChromaItems.INFO_FRAGMENT.get())
							&& reika.chromaticraft.magic.progression.ResearchFragmentData.read(stack).page() == page
							&& reika.chromaticraft.item.ItemChromaBook.addPage(book, page)) {
						stack.shrink(1);
						return;
					}
				}
			}
			else {
				var data = reika.chromaticraft.magic.progression.LexiconData.read(book);
				if (!data.creative() && data.pages().contains(page.id())) {
					data.withoutPage(page.id()).writeTo(book);
					player.getInventory().placeItemBackInInventory(reika.chromaticraft.item.ItemInfoFragment.forPage(page));
				}
			}
		});
	}
	private static void handleSelectFragmentChoice(SelectFragmentChoice payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player
					&& player.containerMenu instanceof reika.chromaticraft.container.MenuFragmentSelection menu)
				menu.select(payload.index());
		});
	}
	private static void handleScrollLexiconPages(ScrollLexiconPages payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player
					&& player.containerMenu instanceof reika.chromaticraft.container.MenuLexiconPages menu)
				menu.scroll(payload.direction());
		});
	}
	private static void handleSetHeatLampTemperature(SetHeatLampTemperature payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)
					|| !(player.containerMenu instanceof reika.chromaticraft.container.MenuHeatLamp menu)
					|| !menu.lamp().getBlockPos().equals(payload.source())
					|| player.distanceToSqr(payload.source().getX() + 0.5, payload.source().getY() + 0.5,
						payload.source().getZ() + 0.5) > 64)
				return;
			menu.lamp().setTemperature(payload.temperature());
		});
	}
	private static void handleToggleCrystalCharger(ToggleCrystalCharger payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)
					|| !(player.containerMenu instanceof reika.chromaticraft.container.MenuCrystalCharger menu)
					|| !menu.charger().getBlockPos().equals(payload.source())
					|| payload.color() < 0 || payload.color() >= CrystalElement.elements.length
					|| player.distanceToSqr(payload.source().getX() + 0.5, payload.source().getY() + 0.5,
							payload.source().getZ() + 0.5) > 64)
				return;
			menu.charger().toggle(CrystalElement.elements[payload.color()]);
		});
	}
	private static void handleUpdateLexiconNotes(UpdateLexiconNotes payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player))
				return;
			var book = player.getMainHandItem();
			if (!book.is(reika.chromaticraft.registry.ChromaItems.LEXICON.get()))
				return;
			java.util.ArrayList<String> notes = new java.util.ArrayList<>();
			int characters = 0;
			for (String raw : payload.notes()) {
				String note = raw.strip();
				if (note.isBlank())
					continue;
				characters += note.length();
				if (notes.size() >= 256 || characters > 32768)
					break;
				notes.add(note);
			}
			var data = reika.chromaticraft.magic.progression.LexiconData.read(book);
			data.withNotes(notes).writeTo(book);
		});
	}
	private static void handleRequestGuideCastingRecipes(RequestGuideCastingRecipes payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player))
				return;
			Identifier id = Identifier.tryParse(payload.itemId());
			if (id == null || !BuiltInRegistries.ITEM.containsKey(id))
				return;
			var item = BuiltInRegistries.ITEM.getValue(id);
			java.util.List<CastingTableRecipe> recipes = guideCastingRecipes(
					player.level().getServer().getRecipeManager(), item);
			PacketDistributor.sendToPlayer(player, new GuideCastingRecipes(payload.itemId(), recipes));
		});
	}

	/**
	 * Builds the narrow authoritative snapshot used by the lexicon. Keeping this independent of a
	 * connection makes the dedicated-server selection contract directly verifiable by GameTest.
	 */
	public static java.util.List<CastingTableRecipe> guideCastingRecipes(
			net.minecraft.world.item.crafting.RecipeManager manager, net.minecraft.world.item.Item item) {
		java.util.ArrayList<CastingTableRecipe> recipes = new java.util.ArrayList<>();
		for (var holder : manager.getRecipes()) {
			if (holder.value() instanceof CastingTableRecipe recipe && recipe.output().is(item))
				recipes.add(recipe);
		}
		recipes.sort(java.util.Comparator.comparingInt(recipe -> recipe.tier().ordinal()));
		return java.util.List.copyOf(recipes);
	}
	private static void handleRequestGuideCraftingRecipes(RequestGuideCraftingRecipes payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player))
				return;
			Identifier id = Identifier.tryParse(payload.itemId());
			if (id == null || !BuiltInRegistries.ITEM.containsKey(id))
				return;
			PacketDistributor.sendToPlayer(player, new GuideCraftingRecipes(payload.itemId(),
					guideCraftingRecipes(player.level(), BuiltInRegistries.ITEM.getValue(id))));
		});
	}

	/**
	 * Every grid recipe producing the given item, as the displays vanilla would put in a recipe book.
	 * Independent of a connection so the selection is directly testable.
	 */
	public static java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplayEntry> guideCraftingRecipes(
			net.minecraft.world.level.Level level, net.minecraft.world.item.Item item) {
		net.minecraft.world.item.crafting.RecipeManager manager = level.getServer().getRecipeManager();
		java.util.ArrayList<net.minecraft.world.item.crafting.display.RecipeDisplayEntry> out = new java.util.ArrayList<>();
		for (var holder : manager.getRecipes()) {
			if (!(holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe))
				continue;
			manager.listDisplaysForRecipe(holder.id(), entry -> {
				boolean grid = entry.display() instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay
						|| entry.display() instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
				if (grid && !out.contains(entry))
					out.add(entry);
			});
		}
		// listDisplaysForRecipe cannot filter by result, so narrow here against the item the page is
		// about. Resolution needs a context, and the server's registry access is the right one.
		net.minecraft.util.context.ContextMap ctx =
				net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level);
		out.removeIf(entry -> {
			for (net.minecraft.world.item.ItemStack result : entry.resultItems(ctx))
				if (result.is(item))
					return false;
			return true;
		});
		return java.util.List.copyOf(out);
	}

	private static void handleGuideCraftingRecipes(GuideCraftingRecipes payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.guideCraftingRecipes(payload.itemId(), payload.recipes()));
	}

	private static void handleGuideCastingRecipes(GuideCastingRecipes payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.guideCastingRecipes(payload.itemId(), payload.recipes()));
	}
	private static void handleDataNodeScan(DataNodeScan payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.dataNodeScan(payload.source()));
	}
	private static void handleTowerLocations(TowerLocations payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.towerLocations(payload.coordinates()));
	}
	private static void handleLoreNote(LoreNote payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.loreNote(
				payload.tower(), payload.seed(), payload.scannedMask()));
	}
	private static void handleInscription(Inscription payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.inscription(payload.source(), payload.recipe()));
	}
	private static void handleOpenLorePuzzle(OpenLorePuzzle payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.openLorePuzzle(
				payload.seed, payload.scannedMask, payload.complete, payload.moves));
	}
	private static void handleLorePuzzleMove(LorePuzzleMove payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player)) return;
			if (payload.q < -7 || payload.q > 7 || payload.r < -7 || payload.r > 7
					|| payload.s < -7 || payload.s > 7 || payload.q + payload.r + payload.s != 0) return;
			var state = reika.chromaticraft.magic.lore.LoreManager.instance.move(
					player, payload.q, payload.r, payload.s);
			if (player.connection != null && player.connection.hasChannel(OpenLorePuzzle.TYPE))
				PacketDistributor.sendToPlayer(player, openPuzzlePayload(state));
		});
	}

	public static void sendDataNodeScan(ServerLevel level, BlockPos source) {
		double rangeSquared = 128D * 128D;
		for (ServerPlayer player : level.players()) {
			if (player.connection != null && player.connection.hasChannel(DataNodeScan.TYPE)
					&& player.distanceToSqr(source.getX() + 0.5, source.getY() + 4.5,
							source.getZ() + 0.5) <= rangeSquared)
				PacketDistributor.sendToPlayer(player, new DataNodeScan(source));
		}
	}

	public static void sendTowerLocations(ServerPlayer player) {
		if (player.connection == null || !player.connection.hasChannel(TowerLocations.TYPE))
			return;
		ServerLevel world = player.level().getServer().overworld();
		if (!reika.chromaticraft.magic.lore.Towers.initialized(world))
			reika.chromaticraft.magic.lore.Towers.loadPositions(world, 64 * 16 * 2);
		int[] coordinates = new int[reika.chromaticraft.magic.lore.Towers.towerList.length * 2];
		for (int i = 0; i < reika.chromaticraft.magic.lore.Towers.towerList.length; i++) {
			var root = reika.chromaticraft.magic.lore.Towers.towerList[i].getRootPosition();
			coordinates[i * 2] = root.x();
			coordinates[i * 2 + 1] = root.z();
		}
		PacketDistributor.sendToPlayer(player, new TowerLocations(coordinates));
	}

	public static void sendLoreNote(ServerPlayer player, reika.chromaticraft.magic.lore.Towers tower) {
		if (player.connection != null && player.connection.hasChannel(LoreNote.TYPE)) {
			var state = reika.chromaticraft.magic.lore.LoreManager.instance.state(player);
			PacketDistributor.sendToPlayer(player, new LoreNote(
					tower.ordinal(), state.seed(), state.scannedMask()));
		}
	}

	public static void sendInscription(ServerLevel level, BlockPos source, int recipe) {
		double rangeSquared = 128D * 128D;
		for (ServerPlayer player : level.players()) {
			if (player.connection != null && player.connection.hasChannel(Inscription.TYPE)
							&& player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(source)) <= rangeSquared)
				PacketDistributor.sendToPlayer(player, new Inscription(source, recipe));
		}
	}

	public static void openLorePuzzle(ServerPlayer player) {
		if (player.connection != null && player.connection.hasChannel(OpenLorePuzzle.TYPE))
			PacketDistributor.sendToPlayer(player, openPuzzlePayload(
					reika.chromaticraft.magic.lore.LoreManager.instance.state(player)));
	}

	private static OpenLorePuzzle openPuzzlePayload(
			reika.chromaticraft.magic.lore.LoreManager.PuzzleState state) {
		return new OpenLorePuzzle(state.seed(), state.scannedMask(), state.complete(),
				state.moves().stream().mapToInt(Integer::intValue).toArray());
	}

	/** Ticked from the client so the progress-sound cooldown drains. */
	public static void tickProgressSoundCooldown() {
		ClientPayloadHandlers.tickProgressSoundCooldown();
	}

	/** Server-side sender; kept here beside its payload. */
	public static void sendProgressionNote(net.minecraft.server.level.ServerPlayer player, int stageOrdinal) {
		// GameTest mock players (and any connection that never negotiated our channel) throw on send,
		// so check before distributing rather than letting progression grants blow up.
		if (player.connection == null || !player.connection.hasChannel(ProgressionNote.TYPE))
			return;
		PacketDistributor.sendToPlayer(player, new ProgressionNote(false, stageOrdinal));
	}

	/** The same V33a overlay channel also carried research-tier milestones. */
	public static void sendResearchLevelNote(net.minecraft.server.level.ServerPlayer player, int levelOrdinal) {
		if (player.connection == null || !player.connection.hasChannel(ProgressionNote.TYPE))
			return;
		PacketDistributor.sendToPlayer(player, new ProgressionNote(true, levelOrdinal));
	}

	private static void handleAttackReceive(AttackReceive payload, IPayloadContext context) {
		context.enqueueWork(() -> PylonAttackOverlay.trigger(element(payload.color)));
	}
	private static void handleJarRejection(JarRejection payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.jarRejection(payload.source, element(payload.color)));
	}
	private static void handlePowerCrystalDestroy(PowerCrystalDestroy payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.powerCrystalDestroy(payload.source));
	}
	private static void handleRepeaterConnections(RepeaterConnections payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.repeaterConnections(payload.source));
	}
	private static void handleRepeaterSurgeBurst(RepeaterSurgeBurst payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.repeaterSurgeBurst(
				payload.source, element(payload.color)));
	}
	private static void handlePylonCrystalBreak(PylonCrystalBreak payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.pylonCrystalBreak(payload.source, element(payload.color)));
	}
	private static CrystalElement element(int ordinal) {
		CrystalElement[] elements = CrystalElement.elements;
		return elements[Mth.clamp(ordinal, 0, elements.length - 1)];
	}
}
