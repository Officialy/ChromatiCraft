/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.item;

import java.util.function.Consumer;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.tileentity.TileEntityAdjacencyUpgrade;
import reika.chromaticraft.items.ItemAdjacencyPlacer;
import reika.chromaticraft.registry.AdjacencyUpgrades;
import reika.chromaticraft.render.tesr.AdjacencyUpgradeRenderer;

/**
 * V33a drew each Adjacency Upgrade item with its colour's render-only tile, given the stack's tier: the out-of-world
 * branch of the renderer, at the world origin (so every item's rings share one phase).
 */
public final class AdjacencyUpgradeItemRenderer implements SpecialModelRenderer<AdjacencyUpgradeItemRenderer.Args> {

	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "adjacency_upgrade");

	private AdjacencyUpgradeItemRenderer() {

	}

	@Override
	public void submit(@Nullable Args args, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			int overlayCoords, boolean hasFoil, int outlineColor) {
		if (args == null)
			return;
		AdjacencyUpgradeRenderer.submitUpgrade(poseStack, collector, args.upgrade(), args.tier(), false, 0, null);
	}

	@Override
	public @Nullable Args extractArgument(ItemStack stack) {
		if (!(stack.getItem() instanceof ItemAdjacencyPlacer placer))
			return null;
		return new Args(placer.getUpgrade(), TileEntityAdjacencyUpgrade.getTier(stack));
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0, 0, 0));
		output.accept(new Vector3f(1, 1, 1));
	}

	public record Args(AdjacencyUpgrades upgrade, int tier) {}

	public record Unbaked() implements SpecialModelRenderer.Unbaked<Args> {
		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public AdjacencyUpgradeItemRenderer bake(SpecialModelRenderer.BakingContext context) {
			return new AdjacencyUpgradeItemRenderer();
		}
	}
}
