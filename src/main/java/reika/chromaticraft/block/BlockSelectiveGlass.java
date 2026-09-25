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
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.dragonapi.interfaces.entity.CustomProjectile;

/**
 * V33a {@code BlockSelectiveGlass}, Defensive Glass: a connected-texture pane solid to everything except the
 * projectiles a player fires -- arrows, thrown things and fireballs a player launched fly straight through, while
 * anything a mob fires stops. Its faces are drawn by {@code SelectiveGlassModel}; faces between two panes are
 * hidden.
 *
 * <p>V33a made projectiles pass by an ASM hook on the collision box ({@code ChromaAux.getInterceptedCollisionBox});
 * in 26.2 projectiles find blocks through the collision shape with themselves as the context, so the block
 * answers directly. Its sides are all solid ({@code isSideSolid}), so it keeps the full collision shape
 * otherwise.
 */
public class BlockSelectiveGlass extends Block {

	public BlockSelectiveGlass(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext ec && ec.getEntity() != null && isProjectile(ec.getEntity())
				&& canEntityPass(ec.getEntity()))
			return Shapes.empty();
		return super.getCollisionShape(state, level, pos, context);
	}

	@Override
	protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
		return neighbor.is(this) || super.skipRendering(state, neighbor, direction);
	}

	/** V33a's {@code IProjectile || EntityFireball || CustomProjectile} test in the collision hook. */
	private static boolean isProjectile(Entity e) {
		return e instanceof Projectile || e instanceof CustomProjectile;
	}

	/**
	 * V33a {@code canEntityPass}: throwables, arrows and fireballs pass when a player fired them. V33a also let
	 * its item-vacuum projectile and Blood Magic spell shots through.
	 */
	public static boolean canEntityPass(Entity e) {
		if (e instanceof Projectile p)
			return p.getOwner() instanceof Player;
		// CHROMA-PORT: V33a EntityVacuum (a CustomProjectile) always passes; add it here when it is ported.
		// Blood Magic's spell shot passed too; that mod has no 26.2 port.
		return false;
	}

}
