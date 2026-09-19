package reika.chromaticraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import reika.chromaticraft.api.interfaces.ManipulatorInteraction;
import reika.chromaticraft.auxiliary.interfaces.SneakPop;
import reika.chromaticraft.block.worldgen26.BlockCliffStone;
import reika.chromaticraft.magic.PylonCharging;
import reika.chromaticraft.magic.interfaces.ChargingPoint;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaDimensions;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityDataNode;
import reika.chromaticraft.tileentity.TileEntityDummyAux;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.chromaticraft.tileentity.recipe.TileEntityRitualTable;
import reika.chromaticraft.world.dimension.DimensionTuningManager;
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

	/** V33a ordinary Manipulator reach; the REACH ability extends this to 96 when that port lands. */
	private static final double CHARGE_REACH = 24;

	public ItemManipulator(Properties properties) {
		super(properties);
	}

	/**
	 * V33a {@code onItemRightClick}/{@code onUsingTick}: charging only occurs while the player is
	 * actively holding right click. Merely carrying the Manipulator and looking at a pylon must not
	 * drain it.
	 */
	@Override
	public void onUseTick(Level level, LivingEntity owner, ItemStack stack, int ticksRemaining) {
		if (level.isClientSide() || !(owner instanceof Player player))
			return;
		if (!ProgressStage.PYLON.isPlayerAtStage(player))
			return;
		if (!(player.pick(CHARGE_REACH, 1, false) instanceof BlockHitResult hit)
				|| hit.getType() != HitResult.Type.BLOCK)
			return;

		BlockEntity tile = level.getBlockEntity(hit.getBlockPos());
		// CHROMA-PORT: V33a also resolves BlockPowerTree's aux blocks to their centre here, so a
		// player can charge from any part of a power tree. TileEntityPowerTree is not ported.
		if (tile instanceof TileEntityDummyAux dummy && dummy.getLinkedTile() != null)
			tile = dummy.getLinkedTile();
		if (!(tile instanceof ChargingPoint point))
			return;

		BlockPos at = hit.getBlockPos();
		CrystalElement colour = point.getDeliveredColor(player, level, at.getX(), at.getY(), at.getZ());
		// CHROMA-PORT: the client half of upstream's call draws ChromaFX.createPylonChargeBeam between
		// the crystal and the player. ChromaFX is not ported, so charging is currently silent to look at.
		PylonCharging.chargePlayerFromPylon(player, point, colour, ticksRemaining, true);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
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
		// Structure dummy blocks are the real hit target for tall models such as DATANODE. Resolve
		// their controller before the manipulator dispatch, matching V33a relayManipulatorClick.
		if (tile instanceof TileEntityDummyAux dummy && dummy.getLinkedTile() != null)
			tile = dummy.getLinkedTile();

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
		if (tile instanceof TileEntityRitualTable table) {
			if (level.isClientSide())
				return InteractionResult.SUCCESS;
			return table.triggerRitual(player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}

		// Holding use against a deployed lore tower advances its original 120-tick scan.
		if (tile instanceof TileEntityDataNode node) {
			if (!level.isClientSide())
				node.scan(player);
			return InteractionResult.SUCCESS;
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

		// V33a's STRUCTCONTROL branch, which is the only way the monument ritual is ever started.
		if (tile instanceof reika.chromaticraft.tileentity.TileEntityStructureController structure) {
			// Upstream's creative debug branch: sneak-click marks a controller as the monument's by hand.
			// Kept because it is the only recovery if a monument generates without being marked.
			if (player.hasInfiniteMaterials() && reika.dragonapi.DragonAPI.debugtest) {
				if (!level.isClientSide() && player.isShiftKeyDown()) {
					structure.setMonument();
					return InteractionResult.SUCCESS;
				}
			}
			if (level.isClientSide())
				return InteractionResult.SUCCESS;
			// Upstream gates on prerequisites, not on holding the stage itself: the ritual is what
			// grants CTM, so requiring it first would make the monument unreachable.
			if (ProgressStage.CTM.playerHasPrerequisites(player) && structure.isMonument()
					&& (level.dimension() != ChromaDimensions.PROXIMA
							|| DimensionTuningManager.TuningThresholds.MONUMENT.isSufficientlyTuned(player))
					&& structure.triggerMonument(player)) {
				ChromaSounds.USE.playSoundAtBlockNoAttenuation(structure, 1, 1, 128);
				return InteractionResult.SUCCESS;
			}
			// One refusal sound for every way this can fail, exactly as upstream: the monument does not
			// tell the player which requirement they are short of.
			ChromaSounds.ERROR.playSoundAtBlock(structure);
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
