package reika.chromaticraft.data;

import java.lang.reflect.Field;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.color.item.Constant;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;

import net.minecraft.world.item.Item;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.block.BlockChromaDoor;
import reika.chromaticraft.block.BlockTrapFloor;
import reika.chromaticraft.block.dimension.structure.shiftmaze.BlockShiftLock;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightPanel;
import reika.chromaticraft.block.dimension.structure.lightpanel.BlockLightSwitch;
import reika.chromaticraft.block.dimension.structure.locks.BlockColoredLock;
import reika.chromaticraft.world.dimension.structure.lightpanel.LightType;
import reika.chromaticraft.block.decoration.BlockMetaAlloyLamp;
import reika.chromaticraft.block.BlockCrystallineStone;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaTieredPlants;
import reika.chromaticraft.registry.ChromaDecoFlowers;
import reika.chromaticraft.registry.ChromaShieldTypes;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ChromaClusterItems;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.registry.StorageCrystalTier;
import reika.chromaticraft.render.item.ItemStandItemRenderer;
import reika.chromaticraft.render.item.CrystalChargerItemRenderer;
import reika.chromaticraft.render.item.ItemAuraInfuserRenderer;

/**
 * ChromatiCraft block/item model provider (port-in-progress). Simple full-cube blocks use
 * {@link BlockModelGenerators#createTrivialCube}; multi-variant blocks (e.g. the crystal COLOR
 * property) are emitted through the low-level blockstate/model sinks (accessed reflectively, as
 * ReactorModelProvider does) since the high-level helpers don't cover an arbitrary int property.
 */
public class ChromaModelProvider extends ModelProvider {

	public ChromaModelProvider(PackOutput output) {
		super(output, ChromatiCraft.MODID);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
		blockModels.createTrivialCube(ChromaBlocks.STORAGE.get());
		blockModels.createTrivialCube(ChromaBlocks.DISPLAY_POINT.get());

		Consumer<BlockModelDefinitionGenerator> blockStateOut;
		ItemModelOutput itemModelOut;
		BiConsumer<Identifier, ModelInstance> modelOut;
		try {
			Field bsf = BlockModelGenerators.class.getDeclaredField("blockStateOutput");
			bsf.setAccessible(true);
			blockStateOut = (Consumer<BlockModelDefinitionGenerator>) bsf.get(blockModels);
			Field imf = BlockModelGenerators.class.getDeclaredField("itemModelOutput");
			imf.setAccessible(true);
			itemModelOut = (ItemModelOutput) imf.get(blockModels);
			Field mof = BlockModelGenerators.class.getDeclaredField("modelOutput");
			mof.setAccessible(true);
			modelOut = (BiConsumer<Identifier, ModelInstance>) mof.get(blockModels);
		}
		catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to access BlockModelGenerators sinks — vanilla shape changed?", e);
		}
		Identifier powerCrystalModel = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/power_crystal"),
				TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal/chroma"))),

				modelOut);
		// Top face uses the animated chroma mud texture (128-frame strip, see mud.png.mcmeta); the
		// bottom and sides use plain dirt, matching pristine BlockChromaMud#getIcon(side, meta)
		// (side == 1 -> mud icon, everything else -> Blocks.dirt.blockIcon).
		Identifier mudModel = ModelTemplates.CUBE_BOTTOM_TOP.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/chroma_mud"),
				new TextureMapping()
						.put(TextureSlot.TOP, new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/mud")))
						.put(TextureSlot.BOTTOM, new Material(Identifier.fromNamespaceAndPath("minecraft", "block/dirt")))
						.put(TextureSlot.SIDE, new Material(Identifier.fromNamespaceAndPath("minecraft", "block/dirt"))),
				modelOut);
		MultiVariant mudVariant = new MultiVariant(WeightedList.of(new Variant(mudModel)));
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.MUD.get(), mudVariant));
		itemModelOut.accept(ChromaBlocks.MUD.get().asItem(), ItemModelUtils.plainModel(mudModel));
		// LiquidBlock renders through its registered FluidModel, but still requires a blockstate entry.
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.CHROMA.get(), mudVariant));
		// As above: the in-world power crystal is the shared crystal mesh with every arm and its
		// inert texture swap, so its blockstate is hand-authored and must not be generated over.
		itemModelOut.accept(ChromaBlocks.POWER_CRYSTAL.get().asItem(), ItemModelUtils.plainModel(powerCrystalModel));
		pylonModel(blockStateOut, itemModelOut, modelOut);
		networkTileModel(ChromaBlocks.REPEATER.get(), "crystal_repeater", "block/icons/repeater", blockStateOut, itemModelOut, modelOut);
		networkTileModel(ChromaBlocks.SKYPEATER.get(), "skypeater", "block/icons/repeater", blockStateOut, itemModelOut, modelOut);
		networkTileModel(ChromaBlocks.CREATIVEPYLON.get(), "creative_pylon", "block/crystal/chroma", blockStateOut, itemModelOut, modelOut);
		networkTileModel(ChromaBlocks.COMPOUND.get(), "compound_repeater", "block/icons/multirepeater", blockStateOut, itemModelOut, modelOut);
		networkTileModel(ChromaBlocks.PYLON_LINK.get(), "pylon_link", "block/tile/pylonlink_side", blockStateOut, itemModelOut, modelOut);
		itemStandModel(blockStateOut, itemModelOut, modelOut);
		castingTableModel(blockStateOut, itemModelOut, modelOut);
		crystalChargerModel(blockStateOut, itemModelOut, modelOut);
		itemAuraInfuserModel(blockStateOut, itemModelOut, modelOut);
		playerAuraInfuserModel(blockStateOut, itemModelOut, modelOut);
		portalRiftModels(blockStateOut, itemModelOut, modelOut);
		networkTileModel(ChromaBlocks.FOCUS_CRYSTAL.get(), "focus_crystal", "block/crystal/chroma", blockStateOut, itemModelOut, modelOut);
		dataNodeModel(blockStateOut, itemModelOut, modelOut);
		structureControllerModel(blockStateOut, modelOut);
		dimensionCoreModel(blockStateOut, itemModelOut, modelOut);
		voidRiftModels(blockStateOut, itemModelOut, modelOut);
		auraPointModel(blockStateOut, itemModelOut, modelOut);
		fireJetModel(blockStateOut, itemModelOut, modelOut);
		chromaDoorModel(blockStateOut, itemModelOut, modelOut);
		heatLampModels(blockStateOut, itemModelOut, modelOut);
		metaAlloyModel(blockStateOut, itemModelOut, modelOut);
		tieredOreItems(itemModelOut, modelOut);


		// The crystal-coloured blocks: the COLOR property (0..15 = CrystalElement) maps to a per-colour
		// cube model using the dedicated block/crystal/crystal_<colour> textures. (The renderBase base+arm
		// geometry of the lamp/super crystals is deferred; a coloured cube is the placeholder.)
		caveCrystalItems(itemModelOut, modelOut);
		// V33a ChromaItems.TOOL — flat item icon from items_tool.png sprite 32.
		Item manipulator = ChromaItems.MANIPULATOR.get();
		itemModelOut.accept(manipulator, ItemModelUtils.plainModel(ModelTemplates.FLAT_ITEM.create(
				ModelLocationUtils.getModelLocation(manipulator), TextureMapping.layer0(manipulator), modelOut)));
		for (Item item : List.of(ChromaItems.LEXICON.get(), ChromaItems.DATA_CRYSTAL.get())) {
			Identifier model = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item),
					TextureMapping.layer0(item), modelOut);
			itemModelOut.accept(item, ItemModelUtils.plainModel(model));
		}
		Item doorKey = ChromaItems.DOOR_KEY.get();
		Identifier doorKeyModel = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(doorKey),
				TextureMapping.layer0(doorKey), modelOut);
		itemModelOut.accept(doorKey, ItemModelUtils.plainModel(doorKeyModel));
		// All seven V33a storage capacities used sprite 2 of the same tool sheet; tier is conveyed by
		// its Nula/Aru name and capacity, not by an invented recolour.
		Identifier storageTexture = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "item/storage_crystal");
		for (StorageCrystalTier tier : StorageCrystalTier.list) {
			Item item = ChromaItems.STORAGE_CRYSTALS.get(tier).get();
			Identifier model = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item),
					new TextureMapping().put(TextureSlot.LAYER0, new Material(storageTexture)), modelOut);
			itemModelOut.accept(item, ItemModelUtils.plainModel(model));
		}
		Item speedUpgrade = ChromaItems.SPEED_UPGRADE.get();
		itemModelOut.accept(speedUpgrade, ItemModelUtils.plainModel(ModelTemplates.FLAT_ITEM.create(
				ModelLocationUtils.getModelLocation(speedUpgrade), TextureMapping.layer0(speedUpgrade), modelOut)));
		for (Item item : List.of(ChromaItems.GLOW_CAVE_DUST.get(), ChromaItems.UNKNOWN_ARTEFACT_FRAGMENT.get())) {
			itemModelOut.accept(item, ItemModelUtils.plainModel(ModelTemplates.FLAT_ITEM.create(
					ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item), modelOut)));
		}
		Item fragment = ChromaItems.INFO_FRAGMENT.get();
		Identifier fragmentModel = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(fragment),
				TextureMapping.layer0(fragment), modelOut);
		itemModelOut.accept(fragment, new reika.chromaticraft.render.item.InfoFragmentItemModel.Unbaked(
				ItemModelUtils.plainModel(fragmentModel)));
		// V33a draws lamps and potion crystals with the cave crystal's spikes plus a base plinth.
		// Lamps query double-stone-slab for UP (modern smooth stone); potion/super crystals return
		// obsidian for every face. They are intentionally different source materials.
		basedCrystalBlocks(ChromaBlocks.CRYSTAL_LAMPS, "crystal_lamp",
				Identifier.fromNamespaceAndPath("minecraft", "block/smooth_stone"),
				blockStateOut, itemModelOut, modelOut);
		basedCrystalBlocks(ChromaBlocks.SUPER_CRYSTALS, "super_crystal",
				Identifier.fromNamespaceAndPath("minecraft", "block/obsidian"),
				blockStateOut, itemModelOut, modelOut);
		// Every former CRAFTING metadata is a distinct 26.2 item. Models are generated from the
		// matching item/<registry_name> texture, keeping JSON in datagen rather than runtime maps.
		for (ChromaCraftingItems crafting : ChromaCraftingItems.list) {
			Item item = ChromaItems.CRAFTING.get(crafting).get();
			Identifier model = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item),
					TextureMapping.layer0(item), modelOut);
			itemModelOut.accept(item, ItemModelUtils.plainModel(model));
		}
		for (ChromaClusterItems cluster : ChromaClusterItems.list) {
			Item item = ChromaItems.CLUSTERS.get(cluster).get();
			Identifier model = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item),
					TextureMapping.layer0(item), modelOut);
			itemModelOut.accept(item, ItemModelUtils.plainModel(model));
		}
		for (ChromaTieredItems tiered : ChromaTieredItems.list) {
			Item item = ChromaItems.TIERED.get(tiered).get();
			Identifier model = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item),
					TextureMapping.layer0(item), modelOut);
			itemModelOut.accept(item, ItemModelUtils.plainModel(model));
		}
		for (CrystalElement element : CrystalElement.elements) {
			Item shard = ChromaItems.SHARDS.get(element).get();
			Identifier model = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(shard),
					TextureMapping.layer0(shard), modelOut);
			itemModelOut.accept(shard, ItemModelUtils.plainModel(model));
			Item boosted = ChromaItems.BOOSTED_SHARDS.get(element).get();
			Identifier boostedModel = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(boosted), TextureMapping.layer0(boosted), modelOut);
			itemModelOut.accept(boosted, ItemModelUtils.plainModel(boostedModel));
		}

		for (CrystalElement element : CrystalElement.elements) {
			Item berry = ChromaItems.BERRIES.get(element).get();
			Identifier berryModel = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(berry), TextureMapping.layer0(berry), modelOut);
			itemModelOut.accept(berry, ItemModelUtils.plainModel(berryModel));
			Item stone = ChromaItems.ELEMENTAL_STONES.get(element).get();
			Identifier stoneModel = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(stone), TextureMapping.layer0(stone), modelOut);
			itemModelOut.accept(stone, ItemModelUtils.plainModel(stoneModel));
		}
		Item bucket = ChromaItems.CHROMA_BUCKET.get();
		Identifier bucketModel = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(bucket), TextureMapping.layer0(bucket), modelOut);
		itemModelOut.accept(bucket, ItemModelUtils.plainModel(bucketModel));



		lootChestModel(blockStateOut, itemModelOut, modelOut);
		dummyAuxModel(blockStateOut, modelOut);
		shieldingBlocks(blockStateOut, itemModelOut, modelOut);
		dimensionDecoBlocks(blockStateOut, itemModelOut, modelOut);
		glowTreeBlocks(blockStateOut, itemModelOut, modelOut);
		trapFloorModel(blockStateOut, itemModelOut, modelOut);
		shiftLockModel(blockStateOut, itemModelOut, modelOut);
		hoverModel(blockStateOut, itemModelOut, modelOut);
		lightPanelModel(blockStateOut, itemModelOut, modelOut);
		lightSwitchModel(blockStateOut, itemModelOut, modelOut);
		colorLockModel(blockStateOut, itemModelOut, modelOut);
		lockKeyModel(blockStateOut, itemModelOut, modelOut);
		musicTriggerModel(blockStateOut, itemModelOut, modelOut);
		biomeReplayModel(blockStateOut, modelOut);
		warpNodeModel(blockStateOut, modelOut);
		unknownArtefactBlock(blockStateOut, itemModelOut, modelOut);
		decoFlowerBlocks(blockStateOut, itemModelOut, modelOut);
		caveIndicatorBlock(blockStateOut, itemModelOut, modelOut);
		tieredPlantBlocks(blockStateOut, itemModelOut, modelOut);
		pylonStructureBlock(blockStateOut, itemModelOut, modelOut);
		runeBlock(blockStateOut, itemModelOut, modelOut);
		encrustedBlock(blockStateOut, itemModelOut, modelOut);
		dyeTreeBlocks(blockStateOut, itemModelOut, modelOut);
		cliffBlocks(blockStateOut, itemModelOut, modelOut);
		luminousPlants(blockStateOut, itemModelOut, modelOut);
	}

	/**
	 * Tiered ores use a viewer-dependent custom blockstate model and therefore have no ordinary
	 * {@code block/<id>} model for the automatic BlockItem to inherit. Give each inventory form a
	 * concrete cube using the source underlay instead of leaving three missing-model references.
	 * The animated emissive overlay remains an in-world second pass, as in V33a.
	 */
	private static void tieredOreItems(ItemModelOutput itemModelOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		tieredOreItem(ChromaBlocks.ENERGIZED_ROCK.get(), "chromaticraft:block/ore/tier_0_underlay",
				"chromaticraft:block/ore/tier_0_overlay", itemModelOut, modelOut);
		tieredOreItem(ChromaBlocks.ELEMENTAL_STONES.get(), "chromaticraft:block/ore/tier_1_underlay",
				"chromaticraft:block/ore/tier_1_overlay", itemModelOut, modelOut);
		// The old tier_9_underlay baked 1.7.10 netherrack into the ore. Modern netherrack is the
		// backing now; the unchanged animated overlay retains the exact firestone cutout positions.
		tieredOreItem(ChromaBlocks.FIRESTONE.get(), "minecraft:block/netherrack",
				"chromaticraft:block/ore/tier_9_overlay", itemModelOut, modelOut);
	}

	private static void tieredOreItem(Block block, String underlay, String overlay, ItemModelOutput itemModelOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		Identifier underlayId = Identifier.parse(underlay);
		Identifier overlayId = Identifier.parse(overlay);
		Identifier model = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
						"block/" + BuiltInRegistries.BLOCK.getKey(block).getPath() + "_item"),
				TextureMapping.cube(new Material(underlayId)), modelOut);
		itemModelOut.accept(block.asItem(), ItemModelUtils.specialModel(model,
				new reika.chromaticraft.render.item.TieredOreItemRenderer.Unbaked(underlayId, overlayId)));
	}

	private static void metaAlloyModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.META_ALLOY_LAMP.get();
		Identifier leaves = ModelTemplates.CROSS.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/meta_alloy_leaves"),
				new TextureMapping().put(TextureSlot.CROSS, new Material(Identifier.fromNamespaceAndPath(
						ChromatiCraft.MODID, "block/metaleaf"))), modelOut);
		Identifier pod = ModelTemplates.CUBE_BOTTOM_TOP.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/meta_alloy_pod"),
				new TextureMapping()
						.put(TextureSlot.TOP, new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/metaalloy")))
						.put(TextureSlot.BOTTOM, new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/metaalloy")))
						.put(TextureSlot.SIDE, new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/metaalloy_side"))),
				modelOut);
		MultiVariant leafVariant = new MultiVariant(WeightedList.of(new Variant(leaves)));
		MultiVariant podVariant = new MultiVariant(WeightedList.of(new Variant(pod)));
		blockStateOut.accept(MultiPartGenerator.multiPart(block).with(leafVariant)
				.with(new ConditionBuilder().term(BlockMetaAlloyLamp.POD, true), podVariant));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(pod));
	}

	/**
	 * Crystal rune: each concrete colour block uses its corresponding complete animated rune sheet, yielding a per-colour
	 * cube using the real {@code block/runes/real/tile<i>_0} texture, plus a matching item model for
	 * each of the 16 colour BlockItems.
	 */
	private static void runeBlock(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		for (CrystalElement element : CrystalElement.elements) {
			int i = element.ordinal();
			Block block = ChromaBlocks.rune(element).get();
			Identifier model = ModelTemplates.CUBE_ALL.create(
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal_rune_" + element.getEnglishName()),
					TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(
							ChromatiCraft.MODID, "block/runes/real/tile" + i + "_0"))), modelOut);
			MultiVariant variant = new MultiVariant(WeightedList.of(new Variant(model)));
			blockStateOut.accept(MultiVariantGenerator.dispatch(block, variant));
			itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
		}
	}
	/**
	 * V33a face selection plus its full-bright animated second render pass.
	 *
	 * <p>Emits one whole-cube model per variant. That serves the item icon for every one, and the
	 * in-world blockstate for the twelve with no neighbour rule; beams, corners and resonance rings
	 * instead build their faces in {@link reika.chromaticraft.render.model.PylonStructureModel},
	 * which needs per-face selection that a static model cannot express. Each variant ships a
	 * hand-authored blockstate under its own registry name, because the custom model type cannot come
	 * out of {@code MultiVariantGenerator}.
	 */
	/**
	 * V33a TieredPlantRenderer draws each tiered plant twice: pass 0 is drawCrossedSquares with the
	 * plant's "backing" sprite at ordinary block brightness, and pass 1 repeats the same cross with
	 * the "overlay" sprite forced to brightness 240. That is reproduced here as two crossed-plane
	 * element pairs, the second full-bright and offset a hair along its own normal so it does not
	 * z-fight the first.
	 */
	/**
	 * V33a CaveIndicatorRenderer: an ordinary stone-textured block with its own top sprite, plus one
	 * extra top-face quad inset 0.1 blocks (1.6 pixels) carrying the inner sprite -- the active one
	 * at brightness 240, the inactive one lit normally.
	 */
	/** V33a renders these as ordinary crossed squares; only Aura Ivy takes the biome grass tint. */
	/**
	 * V33a draws the artefact through its own ISBRH, but the shape it draws is a plain box sunk to
	 * 0.75 of a block with the single {@code ua} sprite on every face, so a slab-height cube_all
	 * reproduces it. The sprite keeps its original animation metadata.
	 */
	/**
	 * V33a BlockWarpNode returns render type -1: the block itself draws nothing at all and its whole
	 * appearance comes from RenderWarpNode. Particle-only, exactly as the pylon is, and no item model
	 * because the block has no BlockItem.
	 */
	/**
	 * One cube_all per material. The reinforced flag changes behaviour, not appearance -- V33a keys
	 * its icon off {@code meta % 8} alone -- so both states share a model.
	 */
	/** V33a's loot chest is entirely model-rendered; the baked block model supplies particles only. */
	private static void lootChestModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.LOOT_CHEST.get();
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, ChromaShieldTypes.STONE.texture()));
		Identifier model = ModelTemplates.PARTICLE_ONLY.create(block,
				new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(block,
				new MultiVariant(WeightedList.of(new Variant(model)))));
		itemModelOut.accept(block.asItem(), ItemModelUtils.specialModel(model,
				new reika.chromaticraft.render.item.LootChestItemRenderer.Unbaked()));
	}

	/** The dummy aux draws as structure stone or nothing at all, decided per tile by its RENDER flag. */
	private static void dummyAuxModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, ChromaShieldTypes.STONE.texture()));
		Identifier model = ModelTemplates.PARTICLE_ONLY.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/dummy_aux"),
				new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.DUMMY_AUX.get(),
				new MultiVariant(WeightedList.of(new Variant(model)))));
	}

	private static void shieldingBlocks(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		for (ChromaShieldTypes type : ChromaShieldTypes.list) {
			Block block = ChromaBlocks.shielding(type).get();
			Material texture = new Material(Identifier.fromNamespaceAndPath(
					ChromatiCraft.MODID, type.texture()));
			Identifier model = ModelTemplates.CUBE_ALL.create(block, TextureMapping.cube(texture), modelOut);
			blockStateOut.accept(MultiVariantGenerator.dispatch(block,
					new MultiVariant(WeightedList.of(new Variant(model)))));
			itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
		}
	}

	/**
	 * Proxima decoration, as plain cubes of {@link ProximaDecoTypes#modelTexture()}. Floatstone is
	 * complete: its two layers are complementary and both drawn in the solid pass, so one composited
	 * texture is exactly what upstream draws. The other five compositing variants still show only their
	 * first layer, because theirs need either a real translucent second pass or a per-position random
	 * layer, and that is the dynamic-model effort.
	 */
	private static void dimensionDecoBlocks(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		for (reika.chromaticraft.registry.ProximaDecoTypes type
				: reika.chromaticraft.registry.ProximaDecoTypes.list) {
			Block block = ChromaBlocks.deco(type).get();
			Material texture = new Material(Identifier.fromNamespaceAndPath(
					ChromatiCraft.MODID, type.modelTexture()));
			// Crystal Leaves are tinted per position, and a tint only reaches a face that carries a
			// tintindex. Vanilla's leaves template is the one that does; a plain cube would silently
			// ignore the tint source and leave the foliage flat red.
			Identifier model = (type.isHueShifted() ? ModelTemplates.LEAVES : ModelTemplates.CUBE_ALL)
					.create(block, TextureMapping.cube(texture), modelOut);
			// Miasma is three oversized double-sided sheets rather than a cube, so its blockstate is
			// hand-authored and names MiasmaModel. Emitting one here would win the resource merge and
			// put the fog back in a box. Its flat model is still generated: that is what the item in a
			// hand or an inventory draws, which is what upstream's item icon is too.
			if (type != reika.chromaticraft.registry.ProximaDecoTypes.MIASMA)
				blockStateOut.accept(MultiVariantGenerator.dispatch(block,
						new MultiVariant(WeightedList.of(new Variant(model)))));
			itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
		}
	}

	/**
	 * Proxima's glowing trees. The log draws upstream's own pass-0 base — vanilla oak log, which is
	 * literally what {@code BlockLightedLog.getIcon} returns — because its glow is a second-pass
	 * overlay and that pass is the deferred rendering effort. The sapling has no overlay and uses its
	 * own real art. The canopy is not here: V33a's GLOWLEAF is already registered as
	 * {@code glowing_leaves}, since the Glowing Cliffs biome places the same block.
	 */
	private static void glowTreeBlocks(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block log = ChromaBlocks.GLOW_LOG.get();
		Identifier logModel = ModelTemplates.CUBE_COLUMN.create(log, new TextureMapping()
				.put(TextureSlot.SIDE, new Material(Identifier.withDefaultNamespace("block/oak_log")))
				.put(TextureSlot.END, new Material(Identifier.withDefaultNamespace("block/oak_log_top"))),
				modelOut);
		// Vanilla's own pillar dispatch: unrotated on Y, X-rotated on Z, and both on X.
		blockStateOut.accept(MultiVariantGenerator.dispatch(log,
				new MultiVariant(WeightedList.of(new Variant(logModel))))
				.with(net.minecraft.client.data.models.blockstates.PropertyDispatch.modify(
						net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)
						.select(net.minecraft.core.Direction.Axis.Y,
								net.minecraft.client.data.models.BlockModelGenerators.NOP)
						.select(net.minecraft.core.Direction.Axis.Z,
								net.minecraft.client.data.models.BlockModelGenerators.X_ROT_90)
						.select(net.minecraft.core.Direction.Axis.X,
								net.minecraft.client.data.models.BlockModelGenerators.X_ROT_90.then(
										net.minecraft.client.data.models.BlockModelGenerators.Y_ROT_90))));
		itemModelOut.accept(log.asItem(), ItemModelUtils.plainModel(logModel));

		Block sapling = ChromaBlocks.GLOW_SAPLING.get();
		Identifier saplingModel = ModelTemplates.CROSS.create(sapling, TextureMapping.cross(
				new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/dimgen/sapling"))),
				modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(sapling,
				new MultiVariant(WeightedList.of(new Variant(saplingModel)))));
		itemModelOut.accept(sapling.asItem(), ItemModelUtils.plainModel(
				ModelTemplates.FLAT_ITEM.create(net.minecraft.client.data.models.model.ModelLocationUtils
						.getModelLocation(sapling.asItem()), TextureMapping.layer0(
						new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/dimgen/sapling"))),
						modelOut)));
	}

	private static void warpNodeModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/icons/roundflare"));
		Identifier model = ModelTemplates.PARTICLE_ONLY.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/warp_node"),
				new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.WARP_NODE.get(),
				new MultiVariant(WeightedList.of(new Variant(model)))));
	}

	private static void unknownArtefactBlock(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.UNKNOWN_ARTEFACT.get();
		Identifier id = ModelLocationUtils.getModelLocation(block);
		modelOut.accept(id, () -> {
			JsonObject root = new JsonObject();
			root.addProperty("parent", "minecraft:block/block");
			JsonObject textures = new JsonObject();
			textures.addProperty("all", "chromaticraft:block/unknown_artefact");
			textures.addProperty("particle", "chromaticraft:block/unknown_artefact");
			root.add("textures", textures);
			JsonObject faces = new JsonObject();
			for (Direction face : Direction.values())
				faces.add(face.getSerializedName(), modelFace("#all", face.getSerializedName()));
			JsonArray elements = new JsonArray();
			JsonObject element = new JsonObject();
			JsonArray from = new JsonArray();
			from.add(0F); from.add(0F); from.add(0F);
			JsonArray to = new JsonArray();
			to.add(16F); to.add(12F); to.add(16F);
			element.add("from", from);
			element.add("to", to);
			element.add("faces", faces);
			elements.add(element);
			root.add("elements", elements);
			return root;
		});
		blockStateOut.accept(MultiVariantGenerator.dispatch(block,
				new MultiVariant(WeightedList.of(new Variant(id)))));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(id));
	}

	private static void decoFlowerBlocks(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		for (ChromaDecoFlowers flower : ChromaDecoFlowers.list) {
			Block block = ChromaBlocks.decoFlower(flower).get();
			Material texture = new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					flower.texture()));
			Identifier model = (flower.isBiomeColored() ? ModelTemplates.TINTED_CROSS : ModelTemplates.CROSS)
					.create(block, TextureMapping.cross(texture), modelOut);
			blockStateOut.accept(MultiVariantGenerator.dispatch(block,
					new MultiVariant(WeightedList.of(new Variant(model)))));
			// A flat sprite, not the cross model. Reusing the block model puts two intersecting planes
			// in the slot, which at the inventory's viewing angle reads as a pair of slivers -- vanilla
			// gives every one of its own flowers an item/generated model for exactly this reason.
			Identifier itemModel = ModelTemplates.FLAT_ITEM.create(
					ModelLocationUtils.getModelLocation(block.asItem()),
					new TextureMapping().put(TextureSlot.LAYER0, texture), modelOut);
			itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(itemModel));
		}
	}

	private static void caveIndicatorBlock(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.CAVE_INDICATOR.get();
		Identifier base = ModelLocationUtils.getModelLocation(block);
		Identifier active = base.withSuffix("_active");
		modelOut.accept(base, () -> caveIndicatorModel("chromaticraft:block/caveindicator_inner_inactive", false));
		modelOut.accept(active, () -> caveIndicatorModel("chromaticraft:block/caveindicator_inner", true));
		blockStateOut.accept(MultiVariantGenerator.dispatch(block)
				.with(net.minecraft.client.data.models.blockstates.PropertyDispatch
						.initial(reika.chromaticraft.block.worldgen26.BlockCaveIndicator.ACTIVE)
						.select(false, new MultiVariant(WeightedList.of(new Variant(base))))
						.select(true, new MultiVariant(WeightedList.of(new Variant(active))))));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(base));
	}

	private static JsonObject caveIndicatorModel(String inner, boolean emissive) {
		JsonObject root = new JsonObject();
		root.addProperty("parent", "minecraft:block/block");
		JsonObject textures = new JsonObject();
		textures.addProperty("side", "minecraft:block/stone");
		textures.addProperty("top", "chromaticraft:block/caveindicator_top");
		textures.addProperty("inner", inner);
		textures.addProperty("particle", "minecraft:block/stone");
		root.add("textures", textures);
		JsonObject faces = new JsonObject();
		for (Direction face : Direction.values()) {
			String name = face.getSerializedName();
			faces.add(name, modelFace(face == Direction.UP ? "#top" : "#side", name));
		}
		JsonArray elements = new JsonArray();
		elements.add(modelElement(0, 16, faces, false));
		// The inner quad: a flat element whose up face sits 1.6 pixels below the top.
		JsonObject innerFaces = new JsonObject();
		JsonObject up = new JsonObject();
		up.addProperty("texture", "#inner");
		up.add("uv", uvFull());
		innerFaces.add("up", up);
		JsonObject element = new JsonObject();
		JsonArray from = new JsonArray();
		from.add(0F); from.add(14.4F); from.add(0F);
		JsonArray to = new JsonArray();
		to.add(16F); to.add(14.4F); to.add(16F);
		element.add("from", from);
		element.add("to", to);
		element.add("faces", innerFaces);
		if (emissive) {
			element.addProperty("shade", false);
			element.addProperty("light_emission", 15);
		}
		elements.add(element);
		root.add("elements", elements);
		return root;
	}

	private static void tieredPlantBlocks(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		for (ChromaTieredPlants plant : ChromaTieredPlants.list) {
			Block block = ChromaBlocks.tieredPlant(plant).get();
			Identifier id = ModelLocationUtils.getModelLocation(block);
			String back = ChromatiCraft.MODID + ":" + plant.backTexture();
			String front = ChromatiCraft.MODID + ":" + plant.frontTexture();
			modelOut.accept(id, () -> layeredCross(back, front));
			blockStateOut.accept(MultiVariantGenerator.dispatch(block,
					new MultiVariant(WeightedList.of(new Variant(id)))));
			// Flat sprite for the slot, as with the deco flowers. The front layer is the one that
			// carries the plant's shape, so that is the sprite the item shows.
			Identifier itemModel = ModelTemplates.FLAT_ITEM.create(
					ModelLocationUtils.getModelLocation(block.asItem()),
					new TextureMapping().put(TextureSlot.LAYER0, new Material(
							Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, plant.frontTexture()), true)),
					modelOut);
			itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(itemModel));
		}
	}

	/** Two vanilla-style crossed planes, the second emissive; see {@link #tieredPlantBlocks}. */
	private static JsonObject layeredCross(String back, String front) {
		JsonObject root = new JsonObject();
		root.addProperty("parent", "minecraft:block/block");
		root.addProperty("ambientocclusion", false);
		JsonObject textures = new JsonObject();
		textures.addProperty("back", back);
		textures.addProperty("front", front);
		textures.addProperty("particle", front);
		root.add("textures", textures);
		JsonArray elements = new JsonArray();
		// Pass 0: the backing cross, ordinary lighting.
		elements.add(crossPlane("#back", 45, 8F, false));
		elements.add(crossPlane("#back", -45, 8F, false));
		// Pass 1: the same cross in the overlay sprite at V33a's fixed brightness 240.
		elements.add(crossPlane("#front", 45, 8.01F, true));
		elements.add(crossPlane("#front", -45, 8.01F, true));
		root.add("elements", elements);
		return root;
	}

	private static JsonObject crossPlane(String texture, float angle, float depth, boolean emissive) {
		JsonObject element = new JsonObject();
		JsonArray from = new JsonArray();
		from.add(0.8F); from.add(0F); from.add(depth);
		JsonArray to = new JsonArray();
		to.add(15.2F); to.add(16F); to.add(depth);
		element.add("from", from);
		element.add("to", to);
		JsonObject rotation = new JsonObject();
		JsonArray origin = new JsonArray();
		origin.add(8F); origin.add(8F); origin.add(8F);
		rotation.add("origin", origin);
		rotation.addProperty("axis", "y");
		rotation.addProperty("angle", angle);
		rotation.addProperty("rescale", true);
		element.add("rotation", rotation);
		JsonObject faces = new JsonObject();
		// Explicit UVs for the same reason as layeredCube: these elements are not cube-shaped, and a
		// derived uv on an inflated element aborts the bake.
		for (String name : new String[] {"north", "south"}) {
			JsonObject face = new JsonObject();
			face.addProperty("texture", texture);
			face.add("uv", uvFull());
			faces.add(name, face);
		}
		element.add("faces", faces);
		if (emissive) {
			element.addProperty("shade", false);
			element.addProperty("light_emission", 15);
		}
		return element;
	}

	private static void pylonStructureBlock(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		for (StoneTypes type : StoneTypes.list) {
			Block block = ChromaBlocks.crystallineStone(type).get();
			// Model id follows the block's registry name now that each variant is its own block.
			Identifier id = ModelLocationUtils.getModelLocation(block);
			Identifier model = createPylonStoneModel(id, type, 0, modelOut);
			itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
		}
	}

	/**
	 * @param neighbourIndex V33a {@code getIconIndex}: 0 = no same-type neighbour, 1 = Z, 2 = X.
	 */
	private static Identifier createPylonStoneModel(Identifier id, BlockCrystallineStone.StoneTypes type,
			int neighbourIndex, BiConsumer<Identifier, ModelInstance> modelOut) {
		String[] base = new String[Direction.values().length];
		String[] glow = new String[Direction.values().length];
		for (Direction face : Direction.values()) {
			int i = face.ordinal();
			String p = ChromatiCraft.MODID + ":block/pylon/block_";
			if (type.isBeam()) {
				if (face.getAxis() == Direction.Axis.Y)
					// V33a: X neighbour -> block_1-3, Z neighbour -> block_1-2, otherwise block_1.
					base[i] = p + (neighbourIndex == 2 ? "1-3" : neighbourIndex == 1 ? "1-2" : "1");
				else {
					base[i] = p + type.ordinal();
					if (type == BlockCrystallineStone.StoneTypes.GLOWBEAM) glow[i] = p + "4-4";
				}
			}
			else if (type.isColumn()) {
				base[i] = p + (face.getAxis() == Direction.Axis.Y ? BlockCrystallineStone.StoneTypes.COLUMN.ordinal() : type.ordinal());
				if (type == BlockCrystallineStone.StoneTypes.GLOWCOL && face.getAxis() != Direction.Axis.Y) glow[i] = p + "3-2";
			}
			else if (type == BlockCrystallineStone.StoneTypes.RESORING) {
				// CHROMA-PORT: V33a's resonance ring has its own multi-step neighbour rule in
				// getIconIndex (vertical pairs, then the flag/flag2/flag3 scan). Only the beam rule
				// is ported so far, so the ring keeps its non-adjacent artwork.
				boolean axial = false;
				base[i] = p + (axial ? "15" : "15-2");
				glow[i] = p + (axial ? "15-3" : "15-4");
			}
			else {
				base[i] = p + type.ordinal();
				if (type.glows()) glow[i] = p + type.ordinal() + "-2";
			}
		}
		// V33a getIcon(int s, int meta): "if (s < 2 && meta < 6) return icons[0][idx]" -- for the
		// first six types the up/down faces are the plain smooth stone, not the type's own artwork.
		// That is what gives a lone beam (and the column, energy stabilizer and pylon focus) flat
		// ends instead of the running-beam texture bleeding onto its top and bottom.
		if (type.ordinal() < 6) {
			for (Direction face : Direction.values()) {
				if (face.getAxis() != Direction.Axis.Y) continue;
				base[face.ordinal()] = ChromatiCraft.MODID + ":block/pylon/block_"
						+ BlockCrystallineStone.StoneTypes.SMOOTH.ordinal();
				glow[face.ordinal()] = null;
			}
		}

		modelOut.accept(id, () -> layeredCube(base, glow));
		return id;
	}

	private static JsonObject layeredCube(String[] base, String[] glow) {
		JsonObject root = new JsonObject();
		// Inherit the vanilla block display transforms. Without a parent these models carry no
		// "display" section at all, so the item form is drawn with an identity transform and looks
		// far too small in inventories and in hand; the local elements/textures still win.
		root.addProperty("parent", "minecraft:block/block");
		root.addProperty("ambientocclusion", false);
		JsonObject textures = new JsonObject();
		JsonObject baseFaces = new JsonObject();
		JsonObject glowFaces = new JsonObject();
		boolean hasGlow = false;
		for (Direction face : Direction.values()) {
			String name = face.getSerializedName();
			textures.addProperty("base_" + name, base[face.ordinal()]);
			baseFaces.add(name, modelFace("#base_" + name, name));
			if (glow[face.ordinal()] != null) {
				textures.addProperty("glow_" + name, glow[face.ordinal()]);
				// Explicit full-sprite UVs are load-bearing, not cosmetic. The glow element is
				// inflated past the block to beat z-fighting, and when a face has no "uv" the
				// bakery derives one from the element bounds -- here [-1,-1,17,17], which
				// FaceBakery.computeMaterialTransparency then hands to
				// SpriteContents.computeTransparency and it throws "Cannot compute translucency
				// out of bounds". That aborted the whole model bake, so every glowing
				// crystalline-stone type rendered as missing-texture in world *and* inventory.
				JsonObject glowFace = modelFace("#glow_" + name, name);
				glowFace.add("uv", uvFull());
				glowFaces.add(name, glowFace);
				hasGlow = true;
			}
		}
		textures.addProperty("particle", base[Direction.NORTH.ordinal()]);
		root.add("textures", textures);
		JsonArray elements = new JsonArray();
		elements.add(modelElement(0, 16, baseFaces, false));
		if (hasGlow) elements.add(modelElement(-0.002F, 16.002F, glowFaces, true));
		root.add("elements", elements);
		return root;
	}

	/** 0..16 across the whole sprite; see the glow-face comment in layeredCube. */
	private static JsonArray uvFull() {
		JsonArray uv = new JsonArray();
		uv.add(0F); uv.add(0F); uv.add(16F); uv.add(16F);
		return uv;
	}

	private static JsonObject modelElement(float from, float to, JsonObject faces, boolean emissive) {
		JsonObject element = new JsonObject();
		element.add("from", vector(from));
		element.add("to", vector(to));
		element.add("faces", faces);
		if (emissive) {
			element.addProperty("shade", false);
			element.addProperty("light_emission", 15);
		}
		return element;
	}

	/** Full cube whose top face carries tintindex 0, the way vanilla's grass block does. */
	private static JsonObject cliffGrassModel(String top, String side, String bottom) {
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", side);
		textures.addProperty("top", top);
		textures.addProperty("side", side);
		textures.addProperty("bottom", bottom);
		JsonObject faces = new JsonObject();
		faces.add("down", modelFace("#bottom", "down"));
		JsonObject up = modelFace("#top", "up");
		up.addProperty("tintindex", 0);
		faces.add("up", up);
		for (String dir : new String[] {"north", "south", "east", "west"})
			faces.add(dir, modelFace("#side", dir));
		JsonArray elements = new JsonArray();
		elements.add(modelElement(0, 16, faces, false));
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	private static JsonObject modelFace(String texture, String cullFace) {
		JsonObject face = new JsonObject();
		face.addProperty("texture", texture);
		face.addProperty("cullface", cullFace);
		return face;
	}

	private static JsonArray vector(float value) {
		JsonArray array = new JsonArray();
		array.add(value);
		array.add(value);
		array.add(value);
		return array;
	}

	private static void encrustedBlock(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Identifier texture = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal/encrusted");
		Identifier model = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/encrusted_crystal"),
				TextureMapping.cube(new Material(texture, true)), modelOut);
		for (CrystalElement element : CrystalElement.elements) {
			Block block = ChromaBlocks.encrustedCrystal(element).get();
			// Deliberately no blockstate: the crust is built per block entity by
			// EncrustedCrystalModel, whose hand-authored blockstate this cube_all stub would
			// silently win the resource merge against. Only the inventory model is generated.
			itemModelOut.accept(block.asItem(), ItemModelUtils.specialModel(model,
					new reika.chromaticraft.render.item.EncrustedCrystalItemRenderer.Unbaked(texture, element)));
		}
	}

	private static void trapFloorModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.TRAP_FLOOR.get();
		PropertyDispatch.C1<MultiVariant, BlockTrapFloor.Disguise> dispatch = PropertyDispatch.initial(BlockTrapFloor.DISGUISE);
		Identifier own = null;
		for (BlockTrapFloor.Disguise disguise : BlockTrapFloor.Disguise.values()) {
			String texture = switch (disguise) {
				case SELF -> "chromaticraft:block/basic/trapfloor";
				case STONE_BRICKS -> "minecraft:block/stone_bricks";
				case OAK_PLANKS -> "minecraft:block/oak_planks";
				case STRUCTURE_STONE -> "chromaticraft:block/shield/stone";
			};
			Identifier model = ModelTemplates.CUBE_ALL.create(
					ModelLocationUtils.getModelLocation(block).withSuffix("_" + disguise.getSerializedName()),
					TextureMapping.cube(new Material(Identifier.parse(texture))), modelOut);
			dispatch.select(disguise, variant(model));
			if (disguise == BlockTrapFloor.Disguise.SELF) own = model;
		}
		blockStateOut.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(own));
	}

	private static void shiftLockModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.SHIFT_LOCK.get();
		PropertyDispatch.C1<MultiVariant, BlockShiftLock.Passability> dispatch = PropertyDispatch.initial(BlockShiftLock.PASSABILITY);
		Identifier item = null;
		for (BlockShiftLock.Passability passability : BlockShiftLock.Passability.values()) {
			Identifier model = ModelLocationUtils.getModelLocation(block)
					.withSuffix("_" + passability.getSerializedName());
			modelOut.accept(model, () -> shiftLockJson(passability));
			dispatch.select(passability, variant(model));
			if (passability == BlockShiftLock.Passability.CLOSED) item = model;
		}
		blockStateOut.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(item));
	}

	private static JsonObject shiftLockJson(BlockShiftLock.Passability passability) {
		JsonObject root = new JsonObject();
		root.addProperty("parent", "minecraft:block/block");
		JsonObject textures = new JsonObject();
		textures.addProperty("lock", "chromaticraft:block/dimstruct/shiftlock-"
				+ (passability.useOpenTexture() ? "open" : "closed"));
		textures.addProperty("shield", "chromaticraft:block/shield/stone");
		textures.addProperty("particle", "chromaticraft:block/shield/stone");
		root.add("textures", textures);
		JsonObject faces = new JsonObject();
		for (Direction face : Direction.values())
			faces.add(face.getSerializedName(), modelFace(passability.isDisguised(face) ? "#shield" : "#lock",
					face.getSerializedName()));
		JsonObject element = new JsonObject();
		JsonArray from = new JsonArray(); from.add(0); from.add(0); from.add(0);
		JsonArray to = new JsonArray(); to.add(16); to.add(16); to.add(16);
		element.add("from", from); element.add("to", to); element.add("faces", faces);
		JsonArray elements = new JsonArray(); elements.add(element); root.add("elements", elements);
		return root;
	}

	private static void hoverModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.HOVER.get();
		Identifier model = ModelLocationUtils.getModelLocation(block);
		modelOut.accept(model, () -> {
			JsonObject root = new JsonObject();
			root.addProperty("parent", "minecraft:block/block");
			JsonObject textures = new JsonObject();
			textures.addProperty("all", "chromaticraft:block/basic/hover");
			textures.addProperty("particle", "chromaticraft:block/basic/hover");
			root.add("textures", textures);
			JsonObject faces = new JsonObject();
			for (Direction direction : Direction.values()) {
				JsonObject face = modelFace("#all", direction.getSerializedName());
				face.addProperty("tintindex", 0);
				faces.add(direction.getSerializedName(), face);
			}
			JsonObject element = new JsonObject();
			JsonArray from = new JsonArray(); from.add(0); from.add(0); from.add(0);
			JsonArray to = new JsonArray(); to.add(16); to.add(16); to.add(16);
			element.add("from", from); element.add("to", to); element.add("faces", faces);
			JsonArray elements = new JsonArray(); elements.add(element); root.add("elements", elements);
			return root;
		});
		blockStateOut.accept(MultiVariantGenerator.dispatch(block, variant(model)));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
	}

	private static void lightPanelModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.LIGHT_PANEL.get();
		PropertyDispatch.C2<MultiVariant, LightType, Boolean> dispatch =
				PropertyDispatch.initial(BlockLightPanel.TYPE, BlockLightPanel.ACTIVE);
		Identifier item = null;
		for (LightType type : LightType.values()) {
			for (boolean active : List.of(false, true)) {
				Identifier model = ModelLocationUtils.getModelLocation(block).withSuffix("_"
						+ type.getSerializedName() + "_" + (active ? "on" : "off"));
				String side = "chromaticraft:block/dimstruct/lightpanel_" + switch (type) {
					case TARGET -> "green";
					case BLOCK -> "red";
					case CANCEL -> "blue";
				} + "_" + (active ? 1 : 0);
				modelOut.accept(model, () -> lightPanelJson(side));
				dispatch.select(type, active, variant(model));
				if (type == LightType.TARGET && !active) item = model;
			}
		}
		blockStateOut.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(item));
	}

	private static JsonObject lightPanelJson(String side) {
		JsonObject root = new JsonObject();
		root.addProperty("parent", "minecraft:block/cube_column");
		JsonObject textures = new JsonObject();
		textures.addProperty("end", "chromaticraft:block/dimstruct/lightpanel");
		textures.addProperty("side", side);
		textures.addProperty("particle", "chromaticraft:block/dimstruct/lightpanel");
		root.add("textures", textures);
		return root;
	}

	private static void lightSwitchModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.PANEL_SWITCH.get();
		PropertyDispatch.C1<MultiVariant, Boolean> dispatch = PropertyDispatch.initial(BlockLightSwitch.UP);
		Identifier item = null;
		for (boolean up : List.of(false, true)) {
			Identifier model = ModelLocationUtils.getModelLocation(block).withSuffix(up ? "_on" : "_off");
			modelOut.accept(model, () -> lightPanelJson("chromaticraft:block/dimstruct/lightpanel_switch_"
					+ (up ? "on" : "off")));
			dispatch.select(up, variant(model));
			if (!up) item = model;
		}
		blockStateOut.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(item));
	}

	private static void colorLockModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.COLOR_LOCK.get();
		PropertyDispatch.C2<MultiVariant, Boolean, Boolean> dispatch =
				PropertyDispatch.initial(BlockColoredLock.OPEN, BlockColoredLock.GATE);
		Identifier closed = ModelLocationUtils.getModelLocation(block).withSuffix("_closed");
		Identifier openModel = ModelLocationUtils.getModelLocation(block).withSuffix("_open");
		ModelTemplates.CUBE_ALL.create(closed, TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/dimstruct/colorlock_0"))), modelOut);
		ModelTemplates.CUBE_ALL.create(openModel, TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/dimstruct/colorlock_1"))), modelOut);
		for (boolean open : List.of(false, true)) for (boolean gate : List.of(false, true)) {
			Identifier model = open ? openModel : closed;
			dispatch.select(open, gate, variant(model));
		}
		blockStateOut.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(closed));
	}

	private static void lockKeyModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.LOCK_KEY.get();
		Identifier model = ModelTemplates.CUBE_ALL.create(ModelLocationUtils.getModelLocation(block),
				TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(
						ChromatiCraft.MODID, "block/dimstruct/key"))), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(block, variant(model)));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
	}

	private static void musicTriggerModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.MUSIC_TRIGGER.get();
		Identifier model = ModelTemplates.CUBE_COLUMN.create(ModelLocationUtils.getModelLocation(block),
				TextureMapping.column(new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
						"block/dimstruct/musictrigger_side")),
						new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
								"block/dimstruct/musictrigger"))), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(block, variant(model)));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
	}

	private static void biomeReplayModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.BIOME_REPLAY.get();
		Identifier model = ModelTemplates.CUBE_COLUMN.create(ModelLocationUtils.getModelLocation(block),
				TextureMapping.column(new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
						"block/dimstruct/dimdata_side")),
						new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
								"block/dimstruct/dimdata"))), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(block, variant(model)));
	}

	private static void itemStandModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Material texture = new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/tile/table_side"));
		Identifier blockModel = ModelTemplates.PARTICLE_ONLY.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/casting_item_stand"),
				new TextureMapping().put(TextureSlot.PARTICLE, texture), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.ITEM_STAND.get(),
				new MultiVariant(WeightedList.of(new Variant(blockModel)))));
		// Inventory rendering cannot invoke a block-entity renderer; retain a visible textured model.
		Identifier itemModel = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/casting_item_stand_inventory"),
				TextureMapping.cube(texture), modelOut);
		itemModelOut.accept(ChromaBlocks.ITEM_STAND.get().asItem(),
				ItemModelUtils.specialModel(itemModel, new ItemStandItemRenderer.Unbaked()));
	}

	private static void castingTableModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		TextureMapping textures = new TextureMapping()
				.put(TextureSlot.SIDE, new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/tile/table_side")))
				.put(TextureSlot.TOP, new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/tile/table_top")))
				.put(TextureSlot.BOTTOM, new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/tile/table_bottom")));
		Identifier model = ModelTemplates.CUBE_BOTTOM_TOP.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/casting_table"), textures, modelOut);
		MultiVariant variant = new MultiVariant(WeightedList.of(new Variant(model)));
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.CASTING_TABLE.get(), variant));
		itemModelOut.accept(ChromaBlocks.CASTING_TABLE.get().asItem(), ItemModelUtils.plainModel(model));
	}
	/** V33a BlockCrystalPylon metadata 0 is transparent; the BER supplies all visible core geometry. */
	private static void pylonModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/icons/roundflare"));
		Identifier worldModel = ModelTemplates.PARTICLE_ONLY.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/pylon"),
				new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.PYLON.get(),
				new MultiVariant(WeightedList.of(new Variant(worldModel)))));
		Identifier itemModel = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/pylon_inventory"),
				TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(
						ChromatiCraft.MODID, "block/crystal/chroma"))), modelOut);
		itemModelOut.accept(ChromaBlocks.PYLON.get().asItem(), ItemModelUtils.plainModel(itemModel));
	}
	/** The BER owns the world mesh; the inventory keeps a stable shield-textured representation. */
	private static void dataNodeModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/shield/moss"));
		Identifier worldModel = ModelTemplates.PARTICLE_ONLY.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/data_node"),
				new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.DATA_NODE.get(),
				new MultiVariant(WeightedList.of(new Variant(worldModel)))));
		Identifier itemModel = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/data_node_inventory"),
				TextureMapping.cube(particle), modelOut);
		itemModelOut.accept(ChromaBlocks.DATA_NODE.get().asItem(), ItemModelUtils.plainModel(itemModel));
	}
	private static void networkTileModel(Block block, String name, String texture,
			Consumer<BlockModelDefinitionGenerator> blockStateOut, ItemModelOutput itemModelOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		Identifier model = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/" + name),
				TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, texture))),
				modelOut);
		MultiVariant variant = new MultiVariant(WeightedList.of(new Variant(model)));
		blockStateOut.accept(MultiVariantGenerator.dispatch(block, variant));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
	}

	private static void caveCrystalItems(ItemModelOutput itemModelOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		for (CrystalElement element : CrystalElement.elements) {
			Block block = ChromaBlocks.caveCrystal(element).get();
			// V33a drew the real spike geometry in the inventory, not a flat cube; the special
			// renderer shares CaveCrystalGeometry with the in-world block model.
			Identifier base = ModelTemplates.CUBE_ALL.create(
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/cave_crystal_item_" + element.getEnglishName()),
					TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal/crystal_outline"), true)),
					modelOut);
			itemModelOut.accept(block.asItem(), ItemModelUtils.specialModel(base,
					new reika.chromaticraft.render.item.CaveCrystalItemRenderer.Unbaked(
							Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal/crystal_outline"),
							element, java.util.Optional.empty())));
		}
	}
	/**
	 * Crystal blocks that draw the V33a base plinth: the block side is the shared dynamic model with
	 * a {@code base_texture}, and the item side the matching special renderer.
	 *
	 * <p>The old renderer asks {@code getBaseBlock(..., UP)} for every base face. Crystal lamps return
	 * double stone slab for that query; super/potion crystals return obsidian.
	 */
	private static void basedCrystalBlocks(List<? extends net.neoforged.neoforge.registries.DeferredBlock<? extends Block>> blocks,
			String name, Identifier baseTexture,
			Consumer<BlockModelDefinitionGenerator> blockStateOut, ItemModelOutput itemModelOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		Identifier outline = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal/crystal_outline");
		for (CrystalElement element : CrystalElement.elements) {
			Block block = blocks.get(element.ordinal()).get();
			// The in-world model is hand-authored per colour (see the cave crystal blockstates); the
			// item icon is the same geometry through the special renderer.
			Identifier base = ModelTemplates.CUBE_ALL.create(
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/" + name + "_item_" + element.getEnglishName()),
					TextureMapping.cube(new Material(outline, true)), modelOut);
			itemModelOut.accept(block.asItem(), ItemModelUtils.specialModel(base,
					new reika.chromaticraft.render.item.CaveCrystalItemRenderer.Unbaked(
							outline, element, java.util.Optional.of(baseTexture))));
		}
	}

	private static void crystalColourBlocks(List<? extends net.neoforged.neoforge.registries.DeferredBlock<? extends Block>> blocks,
			String name, Consumer<BlockModelDefinitionGenerator> blockStateOut, ItemModelOutput itemModelOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		for (CrystalElement element : CrystalElement.elements) {
			String colour = element.getEnglishName();
			Block block = blocks.get(element.ordinal()).get();
			Identifier model = ModelTemplates.CUBE_ALL.create(
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/" + name + "_" + colour),
					TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(
							ChromatiCraft.MODID, "block/crystal/crystal_" + colour))), modelOut);
			MultiVariant variant = new MultiVariant(WeightedList.of(new Variant(model)));
			blockStateOut.accept(MultiVariantGenerator.dispatch(block, variant));
			itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
		}
	}
	@Override
	protected Stream<? extends Holder<Block>> getKnownBlocks() {
		// Cave crystal and cliff dirt ship hand-authored blockstates whose variant values are custom
		// dynamic models (CaveCrystalModel / CliffDirtModel); exclude only those custom-model blocks
		// from the generated-blockstate completeness check. Cliff dirt is excluded by identity, not
		// by class: its three BlockCliffStone siblings (stone/grass/farmland) are datagen'd normally.
		return BuiltInRegistries.BLOCK.listElements()
				.filter(h -> h.getKey().identifier().getNamespace().equals(ChromatiCraft.MODID))
				.filter(h -> !(h.value() instanceof reika.chromaticraft.block.crystal.BlockCaveCrystal))
				.filter(h -> h.value() != ChromaBlocks.CLIFF_DIRT.get())
				// Lamps and potion crystals now ship hand-authored blockstates pointing at the
				// shared dynamic crystal model, exactly as the cave crystals do.
				.filter(h -> !(h.value() instanceof reika.chromaticraft.block.crystal.BlockCrystalLamp))
				.filter(h -> !(h.value() instanceof reika.chromaticraft.block.crystal.BlockSuperCrystal))
				// Every crystalline-stone variant ships a hand-authored blockstate: the neighbour-scanned
				// ones need the custom model type, which MultiVariantGenerator cannot emit.
				.filter(h -> !(h.value() instanceof BlockCrystallineStone))
				// Tiered ores ship a hand-authored blockstate pointing at TieredOreModel: the model has
				// to pick between the real ore and its host-stone disguise per viewer.
				.filter(h -> !(h.value() instanceof reika.chromaticraft.block.worldgen26.BlockTieredOre))
				// Encrusted crystals build their crust from live block-entity growth, and the power
				// crystal reuses the cave-crystal mesh with every arm plus its inert texture swap;
				// both ship hand-authored blockstates naming those custom model types.
				.filter(h -> !(h.value() instanceof reika.chromaticraft.block.BlockEncrustedCrystal))
				.filter(h -> h.value() != ChromaBlocks.POWER_CRYSTAL.get())
				// Miasma is not a cube: it is three oversized double-sided sheets, so it ships a
				// hand-authored blockstate naming MiasmaModel. A generated cube_all here would win the
				// resource merge and put the fog back in a box.
				.filter(h -> h.value() != ChromaBlocks.deco(
						reika.chromaticraft.registry.ProximaDecoTypes.MIASMA).get());
	}

	private static void dyeTreeBlocks(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Material leavesTexture = new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/dye/leaves"));
		Material saplingTexture = new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/dye/sapling"));
		for (CrystalElement element : CrystalElement.elements) {
			Block leaves = ChromaBlocks.dyeLeaves(element).get();
			Identifier leavesModel = ModelTemplates.LEAVES.create(leaves, TextureMapping.cube(leavesTexture), modelOut);
			blockStateOut.accept(MultiVariantGenerator.dispatch(leaves,
					new MultiVariant(WeightedList.of(new Variant(leavesModel)))));
			itemModelOut.accept(leaves.asItem(), ItemModelUtils.tintedModel(leavesModel, new Constant(element.getColor())));

			Block sapling = ChromaBlocks.dyeSapling(element).get();
			Identifier saplingModel = ModelTemplates.TINTED_CROSS.create(sapling, TextureMapping.cross(saplingTexture), modelOut);
			blockStateOut.accept(MultiVariantGenerator.dispatch(sapling,
					new MultiVariant(WeightedList.of(new Variant(saplingModel)))));
			itemModelOut.accept(sapling.asItem(), ItemModelUtils.tintedModel(saplingModel, new Constant(element.getColor())));
		}

		Block glowingLeaves = ChromaBlocks.GLOWING_LEAVES.get();
		Identifier glowingModel = ModelLocationUtils.getModelLocation(glowingLeaves);
		modelOut.accept(glowingModel, ChromaModelProvider::glowingLeafModel);
        blockStateOut.accept(MultiVariantGenerator.dispatch(glowingLeaves,
                new MultiVariant(WeightedList.of(new Variant(glowingModel)))));
		itemModelOut.accept(glowingLeaves.asItem(), ItemModelUtils.tintedModel(glowingModel,
				new Constant(net.minecraft.world.level.FoliageColor.FOLIAGE_DEFAULT)));

        Block rainbowLeaves = ChromaBlocks.RAINBOW_LEAVES.get();
		Identifier rainbowLeavesModel = ModelTemplates.LEAVES.create(rainbowLeaves, TextureMapping.cube(leavesTexture), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(rainbowLeaves,
				new MultiVariant(WeightedList.of(new Variant(rainbowLeavesModel)))));
		itemModelOut.accept(rainbowLeaves.asItem(), ItemModelUtils.tintedModel(rainbowLeavesModel, new Constant(0xFFFF00FF)));

		Block rainbowSapling = ChromaBlocks.RAINBOW_SAPLING.get();
		Material rainbowSaplingTexture = new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/dye/rainbowsapling"));
		Identifier rainbowSaplingModel = ModelTemplates.CROSS.create(rainbowSapling, TextureMapping.cross(rainbowSaplingTexture), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(rainbowSapling,
				new MultiVariant(WeightedList.of(new Variant(rainbowSaplingModel)))));
		itemModelOut.accept(rainbowSapling.asItem(), ItemModelUtils.plainModel(rainbowSaplingModel));
	}
	private static void crystalChargerModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/crystal/chroma"));
		Identifier worldModel = ModelTemplates.PARTICLE_ONLY.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal_charger"),
				new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.CRYSTAL_CHARGER.get(),
				new MultiVariant(WeightedList.of(new Variant(worldModel)))));
		Identifier itemBase = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal_charger_inventory"),
				TextureMapping.cube(particle), modelOut);
		itemModelOut.accept(ChromaBlocks.CRYSTAL_CHARGER.get().asItem(),
				ItemModelUtils.specialModel(itemBase, new CrystalChargerItemRenderer.Unbaked()));
	}

	private static void itemAuraInfuserModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/crystal/chroma"));
		Identifier worldModel = ModelTemplates.PARTICLE_ONLY.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/item_aura_infuser"),
				new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.ITEM_INFUSER.get(),
				new MultiVariant(WeightedList.of(new Variant(worldModel)))));
		Identifier itemBase = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/item_aura_infuser_inventory"),
				TextureMapping.cube(particle), modelOut);
		itemModelOut.accept(ChromaBlocks.ITEM_INFUSER.get().asItem(),
				ItemModelUtils.specialModel(itemBase, new ItemAuraInfuserRenderer.Unbaked()));
	}

	private static void playerAuraInfuserModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/crystal/chroma"));
		Identifier worldModel = ModelTemplates.PARTICLE_ONLY.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/player_aura_infuser"),
				new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.PLAYER_INFUSER.get(),
				new MultiVariant(WeightedList.of(new Variant(worldModel)))));
		Identifier itemBase = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/player_aura_infuser_inventory"),
				TextureMapping.cube(particle), modelOut);
		itemModelOut.accept(ChromaBlocks.PLAYER_INFUSER.get().asItem(),
				ItemModelUtils.specialModel(itemBase, new ItemAuraInfuserRenderer.Unbaked()));
	}

	/**
	 * V33a draws the Portal Rift entirely through {@code RenderCrystalPortal} ({@code getRenderType()}
	 * is -1), so both rift identities only need a particle-carrying definition in world.
	 *
	 * <p>CHROMA-PORT: upstream's {@code PortalItemRenderer} draws the held/inventory form as an unlit
	 * cube textured with vanilla {@code textures/entity/end_portal.png}. That texture is not on the
	 * block atlas in 26.2, so it needs a special item renderer rather than a model; until that lands
	 * the item shares the world definition. Do not substitute another sprite here — there is no V33a
	 * block texture for this block to borrow.
	 */
	private static void portalRiftModels(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/crystal/chroma"));
		for (Block portal : new Block[] { ChromaBlocks.PORTAL.get(), ChromaBlocks.RETURN_PORTAL.get() }) {
			Identifier model = ModelTemplates.PARTICLE_ONLY.create(
					ModelLocationUtils.getModelLocation(portal),
					new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
			blockStateOut.accept(MultiVariantGenerator.dispatch(portal,
					new MultiVariant(WeightedList.of(new Variant(model)))));
			itemModelOut.accept(portal.asItem(), ItemModelUtils.plainModel(model));
		}
	}

	/**
	 * The Dimension Core is drawn entirely by its block entity renderer, like the controller, so the
	 * world model exists only to name a particle sprite. It does carry an item model, because a core is
	 * an item a player carries out to the monument and plants.
	 */
	/**
	 * V33a {@code BlockVoidRift.getIcon}: {@code dimgen/voidrift} on the top face and Stone Shielding's
	 * own icon on every other, which is what makes a rift read as a seam in the fissure floor rather
	 * than as a block sitting on it. The colour never reaches the model — upstream's live renderer is a
	 * plain cube and its coloured aura pass is commented out.
	 */
	private static void voidRiftModels(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Material top = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/dimgen/voidrift"));
		Material side = new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
				reika.chromaticraft.registry.ChromaShieldTypes.STONE.texture()));
		for (CrystalElement element : CrystalElement.elements) {
			Block block = ChromaBlocks.voidRift(element).get();
			Identifier model = ModelTemplates.CUBE_BOTTOM_TOP.create(block,
					new TextureMapping().put(TextureSlot.TOP, top).put(TextureSlot.BOTTOM, side)
							.put(TextureSlot.SIDE, side),
					modelOut);
			blockStateOut.accept(MultiVariantGenerator.dispatch(block,
					new MultiVariant(WeightedList.of(new Variant(model)))));
			itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
		}
	}

	private static void dimensionCoreModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		// blurflare is one of Reika's own glow sprites and the closest thing she ships to what
		// RenderDimensionCore draws: a soft round light with no shape of its own, which is exactly what
		// takes an element's colour well. roundflare, the obvious-looking name, is a 256x46080 sprite
		// strip that has no business in the block atlas.
		Material texture = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/icons/blurflare"));
		for (CrystalElement element : CrystalElement.elements) {
			Block block = ChromaBlocks.dimensionCore(element).get();
			// Vanilla's leaves template for the same reason Crystal Leaves use it: it is the cube that
			// carries a tintindex, and without one the core's colour never reaches the model.
			Identifier model = ModelTemplates.LEAVES.create(block, TextureMapping.cube(texture), modelOut);
			blockStateOut.accept(MultiVariantGenerator.dispatch(block,
					new MultiVariant(WeightedList.of(new Variant(model)))));
			// Each identity is one fixed colour now, so the item tint is a plain Constant rather than
			// anything that has to read the stack.
			itemModelOut.accept(block.asItem(), ItemModelUtils.tintedModel(model,
					new net.minecraft.client.color.item.Constant(0xFF000000 | element.getColor())));
		}
	}

	/** The Aura Point is drawn by its block entity, so its world model only names a particle sprite. */
	private static void auraPointModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.AURA_POINT.get();
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/icons/roundflare"));
		Identifier worldModel = ModelTemplates.PARTICLE_ONLY.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/aura_point"),
				new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(block,
				new MultiVariant(WeightedList.of(new Variant(worldModel)))));
		Identifier itemModel = ModelTemplates.FLAT_ITEM.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "item/aura_point"),
				TextureMapping.layer0(particle), modelOut);
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(itemModel));
	}

	/**
	 * The Fire Jet. V33a composites an underlay and an overlay from a {@code dimgen2} sheet that does
	 * not exist in its repository; what does exist is the item icon {@code dimgen/aurajet}, which is
	 * this block's own art and is what it wears here. The absent pair is recorded in PORTING.md.
	 */
	private static void fireJetModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block block = ChromaBlocks.FIRE_JET.get();
		Material texture = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/dimgen/aurajet"));
		Identifier model = ModelTemplates.CUBE_ALL.create(block, TextureMapping.cube(texture), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(block,
				new MultiVariant(WeightedList.of(new Variant(model)))));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
	}

	/** V33a renders the controller entirely through its dynamic structure-script renderer. */
	private static void structureControllerModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		Material particle = new Material(Identifier.fromNamespaceAndPath(
				ChromatiCraft.MODID, "block/icons/roundflare"));
		Identifier worldModel = ModelTemplates.PARTICLE_ONLY.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/structure_controller"),
				new TextureMapping().put(TextureSlot.PARTICLE, particle), modelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.STRUCTURE_CONTROLLER.get(),
				new MultiVariant(WeightedList.of(new Variant(worldModel)))));
	}

	/**
	 * V33a's state-sized normal renderer: a four-pixel core with an arm to every connected door or
	 * sturdy neighbour. Separate multipart pieces preserve that geometry in the modern baked model
	 * pipeline, while the open flag swaps the complete animated texture exactly as metadata bit zero did.
	 */
	private static void chromaDoorModel(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		String closed = ChromatiCraft.MODID + ":block/basic/door_closed";
		String open = ChromatiCraft.MODID + ":block/basic/door_open";
		MultiPartGenerator multipart = MultiPartGenerator.multiPart(ChromaBlocks.CHROMA_DOOR.get());
		for (boolean isOpen : new boolean[] {false, true}) {
			String texture = isOpen ? open : closed;
			String suffix = isOpen ? "open" : "closed";
			Identifier core = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/chroma_door_core_" + suffix);
			modelOut.accept(core, () -> cuboidModel(texture, 6, 6, 6, 10, 10, 10));
			multipart.with(new ConditionBuilder().term(BlockChromaDoor.OPEN, isOpen), variant(core));
			for (Direction direction : Direction.values()) {
				float minX = 6, minY = 6, minZ = 6, maxX = 10, maxY = 10, maxZ = 10;
				switch (direction) {
					case UP -> { minY = 10; maxY = 16; }
					case DOWN -> { minY = 0; maxY = 6; }
					case NORTH -> { minZ = 0; maxZ = 6; }
					case SOUTH -> { minZ = 10; maxZ = 16; }
					case EAST -> { minX = 10; maxX = 16; }
					case WEST -> { minX = 0; maxX = 6; }
				}
				Identifier arm = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
						"block/chroma_door_" + direction.getName() + "_" + suffix);
				float x1 = minX, y1 = minY, z1 = minZ, x2 = maxX, y2 = maxY, z2 = maxZ;
				modelOut.accept(arm, () -> cuboidModel(texture, x1, y1, z1, x2, y2, z2));
				multipart.with(new ConditionBuilder().term(BlockChromaDoor.OPEN, isOpen)
						.term(doorConnection(direction), true), variant(arm));
			}
		}
		blockStateOut.accept(multipart);
		Identifier itemModel = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/chroma_door_item"),
				TextureMapping.cube(new Material(Identifier.parse(closed))), modelOut);
		itemModelOut.accept(ChromaBlocks.CHROMA_DOOR.get().asItem(), ItemModelUtils.plainModel(itemModel));
	}

	/** V33a BlockAttachableMini bounds, with hot/cold now represented by distinct registry blocks. */
	private static void heatLampModels(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		heatLampModel(ChromaBlocks.HEAT_LAMP.get(), "block/ore/tier_6_geode", blockStateOut, itemModelOut, modelOut);
		heatLampModel(ChromaBlocks.COLD_LAMP.get(), "block/coldlamp", blockStateOut, itemModelOut, modelOut);
	}

	private static void heatLampModel(Block block, String texturePath,
			Consumer<BlockModelDefinitionGenerator> blockStateOut, ItemModelOutput itemModelOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		String texture = ChromatiCraft.MODID + ":" + texturePath;
		java.util.EnumMap<Direction, MultiVariant> variants = new java.util.EnumMap<>(Direction.class);
		Identifier[] itemModel = new Identifier[1];
		for (Direction direction : Direction.values()) {
			float x1 = 4, y1 = 4, z1 = 4, x2 = 12, y2 = 12, z2 = 12;
			switch (direction) {
				case DOWN -> { y1 = 12; y2 = 16; }
				case UP -> { y1 = 0; y2 = 4; }
				case NORTH -> { z1 = 12; z2 = 16; }
				case SOUTH -> { z1 = 0; z2 = 4; }
				case WEST -> { x1 = 12; x2 = 16; }
				case EAST -> { x1 = 0; x2 = 4; }
			}
			Identifier model = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
					"block/" + BuiltInRegistries.BLOCK.getKey(block).getPath() + "_" + direction.getName());
			float minX = x1, minY = y1, minZ = z1, maxX = x2, maxY = y2, maxZ = z2;
			modelOut.accept(model, () -> heatLampCuboidModel(
					texture, minX, minY, minZ, maxX, maxY, maxZ));
			variants.put(direction, variant(model));
			if (direction == Direction.EAST) itemModel[0] = model;
		}
		blockStateOut.accept(MultiVariantGenerator.dispatch(block).with(
				PropertyDispatch.initial(reika.chromaticraft.block.BlockHeatLamp.FACING)
						.select(Direction.DOWN, variants.get(Direction.DOWN))
						.select(Direction.UP, variants.get(Direction.UP))
						.select(Direction.NORTH, variants.get(Direction.NORTH))
						.select(Direction.SOUTH, variants.get(Direction.SOUTH))
						.select(Direction.WEST, variants.get(Direction.WEST))
						.select(Direction.EAST, variants.get(Direction.EAST))));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(itemModel[0]));
	}

	private static net.minecraft.world.level.block.state.properties.BooleanProperty doorConnection(Direction direction) {
		return switch (direction) {
			case UP -> BlockChromaDoor.UP;
			case DOWN -> BlockChromaDoor.DOWN;
			case NORTH -> BlockChromaDoor.NORTH;
			case SOUTH -> BlockChromaDoor.SOUTH;
			case EAST -> BlockChromaDoor.EAST;
			case WEST -> BlockChromaDoor.WEST;
		};
	}

	private static MultiVariant variant(Identifier model) {
		return new MultiVariant(WeightedList.of(new Variant(model)));
	}

	private static JsonObject cuboidModel(String texture, float x1, float y1, float z1,
			float x2, float y2, float z2) {
		JsonObject root = new JsonObject();
		JsonObject textures = new JsonObject();
		textures.addProperty("all", texture);
		textures.addProperty("particle", texture);
		root.add("textures", textures);
		JsonObject element = new JsonObject();
		element.add("from", modelVector(x1, y1, z1));
		element.add("to", modelVector(x2, y2, z2));
		JsonObject faces = new JsonObject();
		for (Direction direction : Direction.values()) {
			JsonObject face = new JsonObject();
			face.addProperty("texture", "#all");
			faces.add(direction.getName(), face);
		}
		element.add("faces", faces);
		JsonArray elements = new JsonArray();
		elements.add(element);
		root.add("elements", elements);
		return root;
	}

	private static JsonObject heatLampCuboidModel(String texture, float x1, float y1, float z1,
			float x2, float y2, float z2) {
		JsonObject root = cuboidModel(texture, x1, y1, z1, x2, y2, z2);
		// V33a's inventory block renderer applied the ordinary block-item camera transform after
		// selecting the east-facing BlockAttachableMini bounds. In 26.2 those transforms come from
		// this parent; without it the hot and cold lamps render in raw block space and appear to have
		// the wrong orientation in inventory/hand contexts.
		root.addProperty("parent", "minecraft:block/block");
		return root;
	}

	/** V33a GlowTreeRenderer: biome-tinted vanilla leaves, then the animated full-bright overlay. */
	private static JsonObject glowingLeafModel() {
		String[] base = new String[Direction.values().length];
		String[] glow = new String[Direction.values().length];
		java.util.Arrays.fill(base, "minecraft:block/oak_leaves");
		java.util.Arrays.fill(glow, ChromatiCraft.MODID + ":block/dimgen/glowleaf-light");
		JsonObject model = layeredCube(base, glow);
		JsonArray elements = model.getAsJsonArray("elements");
		JsonObject baseElement = elements.get(0).getAsJsonObject();
		for (var face : baseElement.getAsJsonObject("faces").entrySet())
			face.getValue().getAsJsonObject().addProperty("tintindex", 0);
		return model;
	}
	private static void cliffBlocks(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		Block stone = ChromaBlocks.CLIFF_STONE.get();
		Identifier stoneModel = ModelTemplates.CUBE_ALL.create(stone,
				TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/cliffstone/stone_base"))), modelOut);
		registerSimple(stone, stoneModel, blockStateOut, itemModelOut);

		Block dirt = ChromaBlocks.CLIFF_DIRT.get();
		Identifier dirtModel = ModelTemplates.CUBE_ALL.create(dirt,
				TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/cliffstone/dirt_base"))), modelOut);
		// The in-world blockstate is hand-authored at
		// assets/chromaticraft/blockstates/cliff_dirt.json using the "chromaticraft:cliff_dirt"
		// dynamic model (CliffDirtModel) so it can swap the bottom/side textures based on the
		// neighbour below, matching pristine BlockCliffStone#getIcon(IBlockAccess,...). Only the
		// item icon (a plain dirt_base cube) is datagen'd here.
		itemModelOut.accept(dirt.asItem(), ItemModelUtils.plainModel(dirtModel));

		// grass_top_base.png is greyscale, exactly like vanilla's grass_block_top: it is meant to be
		// biome-tinted. CUBE_BOTTOM_TOP carries no tintindex, so the raw grey showed through as a
		// white top. Hand-build the cube with tintindex 0 on the up face, and see ChromaBlockColors
		// for the matching tint source.
		Block grass = ChromaBlocks.CLIFF_GRASS.get();
		Identifier grassModel = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/cliff_grass_block");
		modelOut.accept(grassModel, () -> cliffGrassModel(
				ChromatiCraft.MODID + ":block/cliffstone/grass_top_base",
				ChromatiCraft.MODID + ":block/cliffstone/grass_base",
				ChromatiCraft.MODID + ":block/cliffstone/dirt_base"));
		registerSimple(grass, grassModel, blockStateOut, itemModelOut);

		Block farmland = ChromaBlocks.CLIFF_FARMLAND.get();
		TextureMapping farmlandTextures = new TextureMapping()
				.put(TextureSlot.BOTTOM, new Material(Identifier.withDefaultNamespace("block/dirt")))
				.put(TextureSlot.TOP, new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/cliffstone/farmland_top")))
				.put(TextureSlot.SIDE, new Material(Identifier.withDefaultNamespace("block/dirt")));
		Identifier farmlandModel = ModelTemplates.CUBE_BOTTOM_TOP.create(farmland, farmlandTextures, modelOut);
		registerSimple(farmland, farmlandModel, blockStateOut, itemModelOut);
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.LUMA.get(),
				new MultiVariant(WeightedList.of(new Variant(stoneModel)))));
		// Fluid blocks draw through the registered FluidModel, never the blockstate variant; both
		// LUMA and ENDER only need a definition present so the loader does not log a missing model.
		blockStateOut.accept(MultiVariantGenerator.dispatch(ChromaBlocks.ENDER.get(),
				new MultiVariant(WeightedList.of(new Variant(stoneModel)))));
	}

	/** V33a crop-type geometry: four upright, non-diagonal planes inset from each block edge. */
	private static void luminousPlants(Consumer<BlockModelDefinitionGenerator> blockStateOut,
			ItemModelOutput itemModelOut, BiConsumer<Identifier, ModelInstance> modelOut) {
		luminousPlant(ChromaBlocks.GLOW_DAISY.get(), "plant/flower_6", 4, blockStateOut, itemModelOut, modelOut);
		luminousPlant(ChromaBlocks.GLOW_ROOT.get(), "plant/flower_7", 3, blockStateOut, itemModelOut, modelOut);
	}

	private static void luminousPlant(Block block, String texturePath, float inset,
			Consumer<BlockModelDefinitionGenerator> blockStateOut, ItemModelOutput itemModelOut,
			BiConsumer<Identifier, ModelInstance> modelOut) {
		Identifier texture = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/" + texturePath);
		Identifier model = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID,
				"block/" + BuiltInRegistries.BLOCK.getKey(block).getPath());
		modelOut.accept(model, () -> cropTypePlantModel(texture, inset));
		blockStateOut.accept(MultiVariantGenerator.dispatch(block,
				new MultiVariant(WeightedList.of(new Variant(model)))));
		Identifier itemModel = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(block.asItem()),
				new TextureMapping().put(TextureSlot.LAYER0, new Material(texture)), modelOut);
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(itemModel));
	}

	private static JsonObject cropTypePlantModel(Identifier texture, float inset) {
		JsonObject root = new JsonObject();
		root.addProperty("ambientocclusion", false);
		JsonObject textures = new JsonObject();
		textures.addProperty("plant", texture.toString());
		textures.addProperty("particle", texture.toString());
		root.add("textures", textures);
		JsonArray elements = new JsonArray();
		elements.add(plantPlane(inset, 0, inset, 16, true));
		elements.add(plantPlane(16 - inset, 0, 16 - inset, 16, true));
		elements.add(plantPlane(0, inset, 16, inset, false));
		elements.add(plantPlane(0, 16 - inset, 16, 16 - inset, false));
		root.add("elements", elements);
		return root;
	}

	private static JsonObject plantPlane(float x1, float z1, float x2, float z2, boolean xConstant) {
		JsonObject element = new JsonObject();
		element.add("from", modelVector(x1, 0, z1));
		element.add("to", modelVector(x2, 16, z2));
		element.addProperty("shade", false);
		JsonObject faces = new JsonObject();
		JsonObject front = new JsonObject();
		front.addProperty("texture", "#plant");
		JsonObject back = new JsonObject();
		back.addProperty("texture", "#plant");
		faces.add(xConstant ? "east" : "south", front);
		faces.add(xConstant ? "west" : "north", back);
		element.add("faces", faces);
		return element;
	}

	private static JsonArray modelVector(float x, float y, float z) {
		JsonArray array = new JsonArray();
		array.add(x);
		array.add(y);
		array.add(z);
		return array;
	}
	private static void registerSimple(Block block, Identifier model,
			Consumer<BlockModelDefinitionGenerator> blockStateOut, ItemModelOutput itemModelOut) {
		blockStateOut.accept(MultiVariantGenerator.dispatch(block,
				new MultiVariant(WeightedList.of(new Variant(model)))));
		itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
	}
}
