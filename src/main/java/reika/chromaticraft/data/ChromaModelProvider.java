package reika.chromaticraft.data;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Block;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;

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

		// cave_crystal: the COLOR property (0..15 = CrystalElement) maps to a per-colour cube model
		// using the dedicated block/crystal/crystal_<colour> textures.
		Block cave = ChromaBlocks.CAVE_CRYSTAL.get();
		Identifier[] colorModels = new Identifier[CrystalElement.elements.length];
		for (int i = 0; i < colorModels.length; i++) {
			String cn = CrystalElement.elements[i].name().toLowerCase(Locale.ENGLISH);
			colorModels[i] = ModelTemplates.CUBE_ALL.create(
					Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/cave_crystal_" + cn),
					TextureMapping.cube(new Material(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "block/crystal/crystal_" + cn))),
					modelOut);
		}
		PropertyDispatch<MultiVariant> dispatch = PropertyDispatch
				.initial(CrystalTypeBlock.COLOR)
				.generate(i -> new MultiVariant(WeightedList.of(new Variant(colorModels[i]))));
		blockStateOut.accept(MultiVariantGenerator.dispatch(cave).with(dispatch));
		itemModelOut.accept(cave.asItem(), ItemModelUtils.plainModel(colorModels[CrystalElement.WHITE.ordinal()]));
	}
}
