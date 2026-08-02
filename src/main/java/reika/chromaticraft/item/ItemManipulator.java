package reika.chromaticraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.chromaticraft.api.interfaces.ManipulatorInteraction;
import reika.chromaticraft.auxiliary.interfaces.SneakPop;
import reika.chromaticraft.block.worldgen26.BlockCliffStone;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.dragonapi.APIPacketHandler;
import reika.dragonapi.DragonAPI;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.io.ReikaPacketHelper;

/**
 * V33a {@code ItemManipulator} — the mod's universal "interact with a ChromatiCraft block" tool.
 *
 * <p>Its most load-bearing job, and the reason it gates the casting tier: right-clicking a Casting
 * Table with it is what <em>starts a craft</em> ({@code TileEntityCastingTable.triggerCrafting}).
 * Right-clicking the table by hand only opens the GUI, so without this item a player can fill a
 * table's grid but never cast anything.
 *
 * <p>V33a's {@code onItemUse} is one long dispatch over roughly thirty block-entity types. Only the
 * branches whose tiles exist in the port are live below; every other branch is preserved as a
 * {@code CHROMA-PORT} note against the tile it needs rather than deleted, so it can be restored
 * verbatim as those tiles land.
 */
public class ItemManipulator extends Item {

	public ItemManipulator(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (player == null || ReikaPlayerAPI.isFake(player))
			return InteractionResult.PASS;

		BlockCliffStone.transparify(level, pos, player);


		BlockEntity tile = level.getBlockEntity(pos);

		// Sneak + manipulator pops a droppable tile out of the world.
		if (tile instanceof SneakPop pop && player.isShiftKeyDown()) {
			if (pop.canDrop(player)) {
				if (!level.isClientSide()) {
					pop.drop();
					ChromaSounds.RIFT.playSoundAtBlock(tile);
				}
				return InteractionResult.SUCCESS;
			}
		}

		// The casting trigger. V33a: if (t == ChromaTiles.TABLE) return triggerCrafting(ep);
		if (tile instanceof TileEntityCastingTable table) {
			if (level.isClientSide())
				return InteractionResult.SUCCESS;
			return table.triggerCrafting(player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}

		// V33a repeater diagnostic/reorientation branch. All active repeater subtypes inherit this tile.
		if (tile instanceof TileEntityCrystalRepeater repeater) {
			repeater.refreshConnectionRender();
			if (level.isClientSide()) {
				repeater.updateRangeAlpha();
				return InteractionResult.SUCCESS;
			}
			if (player.isShiftKeyDown()) {
				if (repeater.isOwnedByPlayer(player)) {
					repeater.redirect(context.getClickedFace().get3DDataValue());
					var sound = Blocks.STONE.defaultBlockState().getSoundType();
					level.playSound(null, pos, sound.getStepSound(), net.minecraft.sounds.SoundSource.BLOCKS,
							2F, 0.5F);
				}
			}
			else if (repeater.checkConnectivity()) {
				var element = repeater.getActiveColor();
				ChromaSounds.CAST.playSoundAtBlock(repeater);
				if (element != null) {
					int color = element.getColor();
					ReikaPacketHelper.sendDataPacketWithRadius(DragonAPI.packetChannel,
							APIPacketHandler.PacketIDs.COLOREDPARTICLE.ordinal(), repeater, 128,
							color >> 16 & 255, color >> 8 & 255, color & 255, 32, 8);
					ReikaPacketHelper.sendDataPacketWithRadius(DragonAPI.packetChannel,
							APIPacketHandler.PacketIDs.NUMBERPARTICLE.ordinal(), repeater, 128,
							repeater.getSignalDepth(element));
				}
			}
			else {
				ChromaSounds.ERROR.playSoundAtBlock(repeater);
			}
			return InteractionResult.SUCCESS;
		}

		// Generic API hook, so third-party and not-yet-ported tiles can claim the click themselves.
		if (tile instanceof ManipulatorInteraction hook) {
			Direction face = context.getClickedFace();
			if (hook.onManipulatorInteract(player, face.get3DDataValue()))
				return InteractionResult.SUCCESS;
		}

		// CHROMA-PORT: the remaining V33a dispatch, each blocked on its own unported tile —
		//   RIFT setDirection, RITUAL triggerRitual, PROGRESSLINK trigger, MINER triggerDigging,
		//   ITEMRIFT flip, CONSOLE setFacing, AURAPOINT togglePVP, BIOMEPAINTER safeMode,
		//   LANDMARK anchor, ASPECTJAR lock, ESSENTIARELAY, POWERTREE aux, CRYSTALGLOW,
		//   CRYSTALFENCE aux, DUMMYAUX relayManipulatorClick, and the Chromability/
		//   PlayerElementBuffer charge-drain right-click behaviour.
		return InteractionResult.PASS;
	}
}
