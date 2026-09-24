package reika.chromaticraft.block.worldgen26;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.dragonapi.libraries.mathsci.ReikaPhysicsHelper;

/**
 * V33a {@code BlockUnknownArtefact}: the half-buried relic that rings each lore tower.
 *
 * <p>It is not a puzzle block or a container — its whole behaviour is that it throws you. Touching or
 * hitting it has a one-in-eight chance of launching the player directly away from it, with the
 * vertical component clamped to 1.25-1.75 so the throw always lifts. That is deliberately hostile:
 * the artefact ring is a warning marker, and upstream gives it resistance 300000 so it cannot simply
 * be blown up either.
 */
public class BlockUnknownArtefact extends Block {

	/** V33a setBlockBounds(0, 0, 0, 1, 0.75F, 1): sunk into the ground. */
	private static final VoxelShape SHAPE = box(0, 0, 0, 16, 12, 16);

	private final MapCodec<BlockUnknownArtefact> codec = MapCodec.unit(this);

	public BlockUnknownArtefact(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends BlockUnknownArtefact> codec() {
		return codec;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** V33a onBlockActivated. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		this.onInteracted(level, pos, player);
		return InteractionResult.SUCCESS;
	}

	/** V33a onBlockClicked: hitting it counts too, not just right-clicking. */
	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		this.onInteracted(level, pos, player);
	}

	/**
	 * V33a onInteracted. The direction comes from the player's offset in polar terms, converted back
	 * to a 6-magnitude cartesian push with the source's exact {@code -90} angle corrections, so the
	 * throw is radially outward from the block rather than along the look vector.
	 */
	private void onInteracted(Level level, BlockPos pos, Player player) {
		if (level.isClientSide() || level.getRandom().nextInt(8) != 0)
			return;
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;
		reika.chromaticraft.registry.ChromaSounds.MONUMENTRAY.playSoundNoAttenuation(level, pos, 1, 0.625F, 64);
		double[] angles = ReikaPhysicsHelper.cartesianToPolar(
				player.getX() - x, player.getY() - y, player.getZ() - z);
		double[] v = ReikaPhysicsHelper.polarToCartesian(6, angles[1] - 90, -angles[2] - 90);
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x + v[0], Mth.clamp(v[1], 1.25, 1.75), motion.z + v[2]);
		player.hurtMarked = true;
		if (level instanceof net.minecraft.server.level.ServerLevel server)
			reika.chromaticraft.network.ChromaNetwork.sendArtefactClick(server, pos);
	}

	/** V33a {@code randomDisplayTick}: the artefact field, while the viewer is within 24 blocks. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
		var viewer = net.minecraft.client.Minecraft.getInstance().player;
		if (viewer != null && viewer.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 576)
			reika.chromaticraft.render.particle.ChromaParticle.spawnUnknownArtefactField(level, pos.getX() + 0.5,
					pos.getY() + 0.5 + 1.5 * random.nextDouble(), pos.getZ() + 0.5, false);
	}

	/**
	 * V33a {@code removedByPlayer}: breaking it with Silk Touch is what counts as finding an artefact
	 * (ARTEFACT). The drops themselves are the loot table's.
	 */
	@Override
	public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
			net.minecraft.world.level.block.entity.BlockEntity blockEntity, net.minecraft.world.item.ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);
		var silk = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
		if (net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0)
			reika.chromaticraft.magic.progression.ProgressStage.ARTEFACT.stepPlayerTo(player);
	}
}
