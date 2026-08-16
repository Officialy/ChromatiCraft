package reika.chromaticraft.block.dimension;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.registry.ProximaDecoTypes;
import reika.chromaticraft.world.dimension.DimensionTuningManager;

/**
 * V33a {@code BlockDimensionDeco}: the decoration materials Proxima's worldgen is built from.
 *
 * <p>Each variant is its own registered block (see {@link ProximaDecoTypes}); this class carries the
 * behaviour upstream expressed as a metadata switch. Hardness 0.75 and resistance 5 come from the
 * constructor, light and tool requirements from the block properties, and the rest is here:
 *
 * <ul>
 * <li>Only the solid variants have a collision box. Miasma, Lifewater and Lattice return null from
 *     {@code getCollisionBoundingBoxFromPool}, so they are walked through — which is what makes the
 *     first two able to affect an entity standing in them at all.</li>
 * <li>Harvesting anything here requires the player to be tuned to the dimension. Upstream returns -1
 *     from {@code getPlayerRelativeBlockHardness} below the {@code DECOHARVEST} threshold, which is
 *     vanilla's "cannot break" sentinel.</li>
 * <li>Lifewater heals the living two health a tick and burns the undead for four; Miasma stretches
 *     every beneficial potion effect on an entity to twenty minutes. Both are collision effects, so
 *     both depend on the block being walk-through.</li>
 * </ul>
 *
 * <p>What is deferred is the multi-layer rendering: six variants composite several textures with a
 * random per-position layer choice and a second pass. Until that model lands each draws its real
 * {@code layer_0}. Nothing else about these blocks is partial.
 */
public class BlockDimensionDeco extends Block {

	private final ProximaDecoTypes type;
	private final MapCodec<BlockDimensionDeco> codec = MapCodec.unit(this);

	public BlockDimensionDeco(BlockBehaviour.Properties properties, ProximaDecoTypes type) {
		super(properties);
		this.type = type;
	}

	@Override
	public MapCodec<? extends BlockDimensionDeco> codec() {
		return codec;
	}

	public ProximaDecoTypes getDecoType() {
		return type;
	}

	/**
	 * V33a {@code getCollisionBoundingBoxFromPool}: only the solid variants can be stood on. The
	 * others keep their full outline for selection and occlusion but have no collision at all.
	 */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return type.isSolid() ? super.getCollisionShape(state, level, pos, context) : Shapes.empty();
	}

	/**
	 * V33a {@code getPlayerRelativeBlockHardness}: an untuned player cannot harvest Proxima's
	 * decoration at all. Upstream returns -1, which vanilla reads as unbreakable; 26.2 expresses the
	 * same thing as zero progress per tick.
	 */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (!DimensionTuningManager.TuningThresholds.DECOHARVEST.isSufficientlyTuned(player))
			return 0;
		return super.getDestroyProgress(state, player, level, pos);
	}

	/** V33a {@code shouldSideBeRendered}: a variant hides its faces only against its own kind. */
	@Override
	protected boolean skipRendering(BlockState state, BlockState neighbour, net.minecraft.core.Direction side) {
		return neighbour.is(this) || super.skipRendering(state, neighbour, side);
	}

	// V33a isBeaconBase is Floatstone alone. 26.2 answers that with the BEACON_BASE_BLOCKS block tag
	// rather than a block method, so it is declared in the tag provider instead of overridden here.

	/**
	 * V33a {@code onEntityCollidedWithBlock}. Lifewater and Miasma are the two that act on whatever
	 * stands in them, and both are walk-through variants, so this runs every tick an entity is inside.
	 */
	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
			net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean isPrecise) {
		super.entityInside(state, level, pos, entity, effects, isPrecise);
		if (level.isClientSide() || !(entity instanceof LivingEntity living))
			return;
		switch (type) {
			case LIFEWATER -> {
				// V33a: undead take four magic damage, everything else heals two — a heart a tick.
				if (living.is(net.minecraft.tags.EntityTypeTags.UNDEAD))
					living.hurtServer((net.minecraft.server.level.ServerLevel)level,
							level.damageSources().magic(), 4);
				else
					living.heal(2);
			}
			case MIASMA -> extendBeneficialEffects(living);
			default -> { }
		}
	}

	/**
	 * V33a's Miasma effect: every non-harmful potion effect an entity carries is reapplied with its
	 * duration raised to twenty minutes, keeping its amplifier. Harmful effects are left alone, so
	 * standing in Miasma preserves what is helping you without preserving what is hurting you.
	 */
	private static void extendBeneficialEffects(LivingEntity living) {
		java.util.List<net.minecraft.world.effect.MobEffectInstance> extended = new java.util.ArrayList<>();
		for (net.minecraft.world.effect.MobEffectInstance effect : living.getActiveEffects()) {
			if (effect.getEffect().value().getCategory()
					== net.minecraft.world.effect.MobEffectCategory.HARMFUL)
				continue;
			int duration = Math.max(20 * 60 * 20, effect.getDuration());
			if (duration == effect.getDuration())
				continue;
			extended.add(new net.minecraft.world.effect.MobEffectInstance(effect.getEffect(), duration,
					effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
		}
		for (net.minecraft.world.effect.MobEffectInstance effect : extended) {
			living.removeEffect(effect.getEffect());
			living.addEffect(effect);
		}
	}

	/**
	 * V33a {@code addDrops}: one item normally, one to six for Glow Cave, then scaled by how tuned the
	 * breaking player is up to the variant's own ceiling. The base count and the ceiling live on the
	 * loot table and this multiplier is applied on top, so a silk-touch or fortune rule still reads
	 * from data rather than from here.
	 */
	public int tunedDropCount(Player player, int base) {
		if (player == null)
			return base;
		return DimensionTuningManager.instance.getTunedDropCount(player, base, 1, type.maxDrops());
	}

	// V33a isOpaqueCube/renderAsNormalBlock are both false; that is carried by noOcclusion() on the
	// block properties rather than an override.
}
