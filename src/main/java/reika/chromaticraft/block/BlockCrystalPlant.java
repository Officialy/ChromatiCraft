/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.tileentity.plants.TileEntityCrystalPlant;

/**
 * V33a {@code BlockCrystalPlant} ("Crystal Bloom"), one block per element where V33a used metadata. It grows on a
 * random tick one time in twelve, from freshly planted ({@code growth} 2) through a budding pod (1) to ripe (0),
 * when it glows and a right click harvests it for seeds and, with {@code CRYSTALFARM}, the occasional shard. The
 * pod, glow and sparkle are drawn by {@link reika.chromaticraft.render.tesr.CrystalPlantRenderer}; the cross is
 * the block model, {@code plant} or, once Primal, {@code plant_primal}.
 */
public final class BlockCrystalPlant extends VegetationBlock implements EntityBlock {

	/** V33a {@code TileEntityCrystalPlant.growthTick}: 2 planted, 1 budding, 0 ripe. */
	public static final IntegerProperty GROWTH = IntegerProperty.create("growth", 0, 2);
	/** Mirrors the tile's lack of {@code Modifier.IMPURE}: a ripe pure bloom lights to 15, an impure one to 12. */
	public static final BooleanProperty PURE = BooleanProperty.create("pure");
	/** Mirrors {@code Modifier.PRIMAL}, which swapped V33a's cross texture. */
	public static final BooleanProperty PRIMAL = BooleanProperty.create("primal");

	/** V33a {@code setBlockBounds(0, 0, 0, 0.9375F, 0.9375F, 0.9375F)}: flush with the block's low corner. */
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 15, 15, 15);

	private final CrystalElement element;

	public BlockCrystalPlant(BlockBehaviour.Properties properties, CrystalElement element) {
		super(properties);
		this.element = element;
		this.registerDefaultState(stateDefinition.any().setValue(GROWTH, 2).setValue(PURE, true).setValue(PRIMAL, false));
	}

	public CrystalElement getElement() {
		return element;
	}

	/** V33a {@code getLightValue}: {@code te.emitsLight() ? te.isPure() ? 15 : 12 : 0}. */
	public static int lightLevel(BlockState state) {
		return state.getValue(GROWTH) == 0 ? state.getValue(PURE) ? 15 : 12 : 0;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(GROWTH, PURE, PRIMAL);
	}

	/**
	 * V33a {@code ReikaPlantHelper.SAPLING.canPlantAt}, i.e. {@code ReikaBlockHelper.isDirtType}: any 1.7.10 dirt
	 * metadata (plain, coarse, podzol) or grass. Not farmland, and not 26.x's dirt tag.
	 */
	public static boolean isDirtType(BlockState below) {
		return below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.PODZOL)
				|| below.is(Blocks.GRASS_BLOCK);
	}

	/** V33a {@code canBlockStay} and the seeds' soil check: dirt-type ground, or Chroma Mud. */
	public static boolean canGrowOn(BlockState below) {
		return below.is(ChromaBlocks.MUD.get()) || isDirtType(below);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return canGrowOn(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.canSurvive(level, pos)) {
			if (random.nextInt(12) == 0 && level.getBlockEntity(pos) instanceof TileEntityCrystalPlant te) {
				te.grow();
			}
		}
		else {
			// V33a die(): drop as broken, then clear.
			level.destroyBlock(pos, true);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof TileEntityCrystalPlant te && te.isPure())
			ChromaParticle.spawnCrystalPlant(level, pos, element.color(), te.canHarvest(),
					te.is(TileEntityCrystalPlant.Modifier.BOOSTED), te.is(TileEntityCrystalPlant.Modifier.PRIMAL),
					random);
	}

	/** V33a {@code onBlockActivated}: harvests a ripe bloom, and takes the click either way. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityCrystalPlant te && te.canHarvest()) {
			te.harvest(true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityCrystalPlant(pos, state);
	}

}
