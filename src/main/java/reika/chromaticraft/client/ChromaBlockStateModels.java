package reika.chromaticraft.client;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.render.model.CaveCrystalModel;
import reika.chromaticraft.render.model.CliffDirtModel;

/** Registers ChromatiCraft's code-driven, non-block-entity chunk models. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ChromaBlockStateModels {
	private ChromaBlockStateModels() {}
	@SubscribeEvent
	public static void register(RegisterBlockStateModels event) {
		event.registerModel(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "cave_crystal"), CaveCrystalModel.Unbaked.CODEC);
		event.registerModel(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "pylon_structure"),
				reika.chromaticraft.render.model.PylonStructureModel.Unbaked.CODEC);
		event.registerModel(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "cliff_dirt"), CliffDirtModel.Unbaked.CODEC);
		event.registerModel(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "encrusted_crystal"),
				reika.chromaticraft.render.model.EncrustedCrystalModel.Unbaked.CODEC);
		event.registerModel(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "tiered_ore"),
				reika.chromaticraft.render.model.TieredOreModel.Unbaked.CODEC);
	}
}