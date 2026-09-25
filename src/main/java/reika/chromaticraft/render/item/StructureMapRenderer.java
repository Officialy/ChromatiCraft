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

import java.util.Map.Entry;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.items.tools.ItemStructureMap;
import reika.chromaticraft.items.tools.ItemStructureMap.StructureMapData;
import reika.chromaticraft.items.tools.ItemStructureMap.StructurePresence;
import reika.chromaticraft.items.tools.ItemStructureMap.StructureSearchType;
import reika.dragonapi.instantiable.rendering.HeldMapRenderers;

/**
 * V33a {@code StructureMapRenderer} plus {@code ItemStructureMap.renderMap}: in first person the map is held like
 * a vanilla map (both arms when the off hand is free; the arm and swing transforms V33a copied from vanilla are
 * vanilla's own here, via {@link HeldMapRenderers}), showing its parchment, the overlay for the structure type it
 * seeks, and a coloured marker per site -- yellow planned, green generated. Other views draw the flat icon.
 *
 * <p>Map space is 0..128 with the origin at the top left; markers are placed around the scan's origin at 40/range
 * pixels per block. Stronghold and tower maps (range -1) are centred on the bounds of what they found and scaled
 * to its farthest extent.
 */
public final class StructureMapRenderer {

	private static final Identifier BACKGROUND = texture("bcg");
	private static final Identifier OVERLAY = texture("overlay_typed");
	private static final Identifier MARKER = texture("marker");

	private StructureMapRenderer() {}

	private static Identifier texture(String name) {
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "textures/gui/structure_map/" + name + ".png");
	}

	public static void register(net.minecraft.world.item.Item map) {
		HeldMapRenderers.register(map, StructureMapRenderer::render);
	}

	/** V33a {@code drawBackground} then {@code renderMap}. */
	public static void render(PoseStack poseStack, SubmitNodeCollector collector, int light, ItemStack item) {
		int b0 = 7;
		quad(poseStack, collector, RenderTypes.text(BACKGROUND), -b0, -b0, 128 + b0, 128 + b0, 0, 0, 0, 1, 1, 0xffffffff, light);

		StructureMapData data = ItemStructureMap.getMapData(item);
		if (data == null)
			return;
		StructureSearchType type = ItemStructureMap.getMode(item);
		float ou = (type.ordinal() % 4) / 4F;
		float ov = (type.ordinal() / 4) / 4F;
		// V33a drew the overlay and markers at full brightness (setBrightness(240)).
		int bright = 0xF000F0;
		float rz = -0.01F;
		quad(poseStack, collector, RenderTypes.text(OVERLAY), -b0, -b0, 128 + b0, 128 + b0, rz, ou, ov, ou + 0.25F,
				ov + 0.25F, 0xffffffff, bright);

		int px = data.originX;
		int pz = data.originZ;
		int max = type.getSearchRange();
		if (max == -1) {
			px = data.bounds.getCenterX();
			pz = data.bounds.getCenterZ();
			BlockPos far = data.bounds.getFarthestPointFrom(px, data.bounds.getCenterY(), pz);
			max = Math.max(Math.abs(far.getX() - px), Math.abs(far.getZ() - pz));
		}
		if (max == 0)
			max = 1;
		float sc = 2;
		RenderType markers = RenderTypes.text(MARKER);
		for (Entry<BlockPos, StructurePresence> e : data.map.entrySet()) {
			BlockPos loc = e.getKey();
			int x = (int)Math.round((loc.getX() - px) * 40D / max + 64);
			int z = (int)Math.round((loc.getZ() - pz) * 40D / max + 64);
			quad(poseStack, collector, markers, x - sc, z - sc, x + sc, z + sc, rz * 2, 0, 0, 1, 1,
					0xff000000 | e.getValue().renderColor, bright);
		}
	}

	private static void quad(PoseStack poseStack, SubmitNodeCollector collector, RenderType type, float x0, float y0,
			float x1, float y1, float z, float u0, float v0, float u1, float v1, int color, int light) {
		collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
			buffer.addVertex(pose, x0, y1, z).setColor(color).setUv(u0, v1).setLight(light);
			buffer.addVertex(pose, x1, y1, z).setColor(color).setUv(u1, v1).setLight(light);
			buffer.addVertex(pose, x1, y0, z).setColor(color).setUv(u1, v0).setLight(light);
			buffer.addVertex(pose, x0, y0, z).setColor(color).setUv(u0, v0).setLight(light);
		});
	}

}
