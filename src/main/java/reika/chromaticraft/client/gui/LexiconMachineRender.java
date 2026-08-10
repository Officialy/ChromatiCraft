package reika.chromaticraft.client.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.rendering.structure.StructureRenderState;
import reika.dragonapi.instantiable.rendering.structure.StructureRenderer;

/**
 * V33a {@code GuiMachineDescription.drawMachineRender}: the slowly turning model of the construct a
 * machine page describes, drawn to the right of its title.
 *
 * <p>It rides the same picture-in-picture element the structure viewer does — the page needs a real
 * 3D model with its block-entity renderer running, and that is the only route into 26.2 GUI space.
 * The controller is separate from {@code StructureRenderer} deliberately: none of that class applies
 * here. Upstream's machine render has no size tiers, no slice, no tally and no hooks, its yaw runs
 * off a free-running clock rather than the mouse, and its scale is a flat 48 pixels per block.
 */
final class LexiconMachineRender {

	/** V33a: {@code double sc = 48}. */
	private static final float SCALE = 48;

	/** V33a: {@code double x = posX+167, y = posY+44}, with {@code posY} the frame origin less eight. */
	private static final int ANCHOR_X = 167;
	private static final int ANCHOR_Y = 36;

	/** V33a: {@code renderq} starts here and the drag clamps it to +/-45. */
	private static final float DEFAULT_PITCH = 22.5F;
	private static final float PITCH_LIMIT = 45;

	/**
	 * Which way a drag tips the model. The structure viewer needed upstream's pitch sign flipped, and
	 * this page's drag is the same shape, so it takes the same convention -- positive screen-space dy
	 * tips the model the same way there and here. Confirmed in game on the structure page.
	 */
	static final double PITCH_DRAG = 1;

	private static final Map<Block, BlockEntity> BLOCK_ENTITIES = new HashMap<>();

	private float pitch = DEFAULT_PITCH;
	private Block machine;

	void reset() {
		pitch = DEFAULT_PITCH;
		machine = null;
	}

	/** V33a's drag: one degree per unit of mouse travel, clamped either side of level. */
	void drag(double dy) {
		pitch = (float)Math.clamp(pitch + PITCH_DRAG * dy, -PITCH_LIMIT, PITCH_LIMIT);
	}

	boolean isOver(double mouseX, double mouseY, int left, int top) {
		// V33a: a 64-wide, 128-tall box around the anchor.
		double x = left + ANCHOR_X;
		double y = top + ANCHOR_Y + 8;
		return mouseX >= x - 32 && mouseX <= x + 32 && mouseY >= y - 64 && mouseY <= y + 64;
	}

	/**
	 * @param icon the entry's item, which is where the block to draw comes from
	 * @return whether anything was drawn, so the caller can fall back to a flat icon
	 */
	boolean render(GuiGraphicsExtractor graphics, ItemStack icon, int left, int top,
			int screenWidth, int screenHeight, float partialTick) {
		if (!(icon.getItem() instanceof BlockItem blockItem))
			return false;
		Block block = blockItem.getBlock();
		if (block != machine) {
			machine = block;
			pitch = DEFAULT_PITCH;
		}
		BlockState state = block.defaultBlockState();

		// V33a: r = (nanoTime()/20000000) % 360 -- one revolution roughly every seven seconds,
		// independent of frame rate and of the game tick.
		float yaw = (int)(System.nanoTime() / 20000000L) % 360;
		// V33a: y -= 8*sin(|renderq|), so tipping the model also lifts it slightly.
		float lift = (float)(8 * Math.sin(Math.abs(Math.toRadians(pitch))));

		float offsetX = left + ANCHOR_X - screenWidth / 2F;
		float offsetY = top + ANCHOR_Y - lift - screenHeight / 2F;

		graphics.submitPictureInPictureRenderState(new StructureRenderState(
				List.of(new StructureRenderState.Block(BlockPos.ZERO, state, false)),
				false, blockEntityState(block, state, partialTick), List.of(),
				// V33a passes (a, 0, b) with a = b = -0.5 to renderTileEntityAt, so the model spins
				// about its own centre horizontally but about its base vertically.
				0.5F, 0, 0.5F,
				pitch, yaw, 0,
				offsetX, offsetY,
				0, 0, screenWidth, screenHeight,
				SCALE,
				graphics.peekScissorStack()));
		return true;
	}

	/**
	 * The machine's block-entity renderer, run over a stand-in instance, so a construct that is mostly
	 * its renderer does not show up as a bare cube. Same arrangement as the structure viewer's: the
	 * instance is never placed, holds the client level only so its renderer has one, and is forced to
	 * full brightness because otherwise it samples whatever is at the origin of the real world.
	 */
	private static List<BlockEntityRenderState> blockEntityState(Block block, BlockState state, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !(block instanceof EntityBlock entityBlock))
			return List.of();
		BlockEntity be = BLOCK_ENTITIES.computeIfAbsent(block, ignored -> {
			BlockEntity made = entityBlock.newBlockEntity(BlockPos.ZERO, state);
			if (made != null)
				made.setLevel(mc.level);
			return made;
		});
		if (be == null)
			return List.of();
		BlockEntityRenderDispatcher dispatcher = mc.getBlockEntityRenderDispatcher();
		BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = dispatcher.getRenderer(be);
		if (renderer == null)
			return List.of();
		BlockEntityRenderState beState = renderer.createRenderState();
		// Same flag the structure viewer sets, for the same reason: this instance is not where it
		// claims to be, so a renderer that reads the level around it has to be told to stop.
		StructureRenderer.setRenderingTiles(true);
		try {
			renderer.extractRenderState(be, beState, partialTick, Vec3.ZERO, null);
		}
		finally {
			StructureRenderer.setRenderingTiles(false);
		}
		beState.lightCoords = LightCoordsUtil.FULL_BRIGHT;
		return List.of(beState);
	}

	/**
	 * V33a {@code GuiMachineDescription.getUsedEnergy}: what the construct costs to run, if anything.
	 *
	 * <p>Upstream branches on {@code ChromaTiles} predicates -- {@code isChargedCrystalPowered},
	 * {@code isRelayPowered}, {@code isPylonPowered}. Testing the stand-in block entity directly is the
	 * same question asked of the object rather than of a parallel enum, and it needs no new registry.
	 *
	 * <p>CHROMA-PORT: only the pylon-powered branch is live. The other two read
	 * {@code getRequiredEnergy()} off {@code ChargedCrystalPowered} and {@code TileEntityRelayPowered},
	 * two abstract block-entity bases that are not yet in the build; restore them alongside those.
	 *
	 * @return null when the machine needs no lumen energy, which is upstream's signal to skip the page
	 */
	static ElementTagCompound usedEnergy(ItemStack icon, long guiTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !(icon.getItem() instanceof BlockItem blockItem))
			return null;
		Block block = blockItem.getBlock();
		if (!(block instanceof EntityBlock))
			return null;
		BlockEntity be = BLOCK_ENTITIES.get(block);
		if (be == null) {
			blockEntityState(block, block.defaultBlockState(), 1);
			be = BLOCK_ENTITIES.get(block);
		}
		if (!(be instanceof CrystalReceiver receiver))
			return null;
		// V33a's pylon-powered display is illustrative rather than a real cost: it shows which
		// elements the construct conducts, breathing so the wheel is never static.
		ElementTagCompound tag = new ElementTagCompound();
		for (int i = 0; i < CrystalElement.elements.length; i++) {
			CrystalElement e = CrystalElement.elements[i];
			if (receiver.isConductingElement(e))
				tag.addValueToColor(e, 25 + (int)(20 * Math.sin(i + guiTick / 20D)));
		}
		return tag.isEmpty() ? null : tag;
	}

	/** Dropped when the client disconnects, so nothing holds a stale level. */
	static void clearCache() {
		BLOCK_ENTITIES.clear();
	}
}
