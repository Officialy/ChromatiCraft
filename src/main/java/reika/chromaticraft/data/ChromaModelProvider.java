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
import reika.chromaticraft.block.BlockCrystallineStone;
import reika.chromaticraft.block.BlockCrystallineStone.StoneTypes;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.registry.ChromaClusterItems;
import reika.chromaticraft.registry.ChromaCraftingItems;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.ChromaTieredItems;
import reika.chromaticraft.render.item.ItemStandItemRenderer;

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
		networkTileModel(ChromaBlocks.FOCUS_CRYSTAL.get(), "focus_crystal", "block/crystal/chroma", blockStateOut, itemModelOut, modelOut);


		// The crystal-coloured blocks: the COLOR property (0..15 = CrystalElement) maps to a per-colour
		// cube model using the dedicated block/crystal/crystal_<colour> textures. (The renderBase base+arm
		// geometry of the lamp/super crystals is deferred; a coloured cube is the placeholder.)
		caveCrystalItems(itemModelOut, modelOut);
		// V33a ChromaItems.TOOL — flat item icon from items_tool.png sprite 32.
		Item manipulator = ChromaItems.MANIPULATOR.get();
		itemModelOut.accept(manipulator, ItemModelUtils.plainModel(ModelTemplates.FLAT_ITEM.create(
				ModelLocationUtils.getModelLocation(manipulator), TextureMapping.layer0(manipulator), modelOut)));
		// V33a draws lamps and potion crystals with the cave crystal's spikes plus the stone plinth
		// (CrystalRenderedBlock.renderBase()); the old coloured-cube placeholder was wrong for both.
		basedCrystalBlocks(ChromaBlocks.CRYSTAL_LAMPS, "crystal_lamp", blockStateOut, itemModelOut, modelOut);
		basedCrystalBlocks(ChromaBlocks.SUPER_CRYSTALS, "super_crystal", blockStateOut, itemModelOut, modelOut);
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



		pylonStructureBlock(blockStateOut, itemModelOut, modelOut);
		runeBlock(blockStateOut, itemModelOut, modelOut);
		encrustedBlock(blockStateOut, itemModelOut, modelOut);
		dyeTreeBlocks(blockStateOut, itemModelOut, modelOut);
		cliffBlocks(blockStateOut, itemModelOut, modelOut);
		luminousPlants(blockStateOut, itemModelOut, modelOut);
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
		Identifier model = ModelTemplates.CUBE_ALL.create(
				Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/encrusted_crystal"),
				TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(
						ChromatiCraft.MODID, "block/crystal/encrusted"))), modelOut);
		for (CrystalElement element : CrystalElement.elements) {
			Block block = ChromaBlocks.encrustedCrystal(element).get();
			// Deliberately no blockstate: the crust is built per block entity by
			// EncrustedCrystalModel, whose hand-authored blockstate this cube_all stub would
			// silently win the resource merge against. Only the inventory model is generated.
			itemModelOut.accept(block.asItem(), ItemModelUtils.plainModel(model));
		}
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
	 * <p>V33a's {@code getBaseBlock} returns {@code double_stone_slab} for the {@code UP} query, and
	 * {@code renderBase} uses that one query for the top, bottom and side faces alike, so the whole
	 * plinth is smooth stone.
	 */
	private static final Identifier CRYSTAL_BASE_TEXTURE =
			Identifier.fromNamespaceAndPath("minecraft", "block/smooth_stone");

	private static void basedCrystalBlocks(List<? extends net.neoforged.neoforge.registries.DeferredBlock<? extends Block>> blocks,
			String name, Consumer<BlockModelDefinitionGenerator> blockStateOut, ItemModelOutput itemModelOut,
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
							outline, element, java.util.Optional.of(CRYSTAL_BASE_TEXTURE))));
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
				.filter(h -> h.value() != ChromaBlocks.POWER_CRYSTAL.get());
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
        Material glowingTexture = new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/dimgen/glowleaf-light"));
        Identifier glowingModel = ModelTemplates.LEAVES.create(glowingLeaves, TextureMapping.cube(glowingTexture), modelOut);
        blockStateOut.accept(MultiVariantGenerator.dispatch(glowingLeaves,
                new MultiVariant(WeightedList.of(new Variant(glowingModel)))));
        itemModelOut.accept(glowingLeaves.asItem(), ItemModelUtils.plainModel(glowingModel));

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
