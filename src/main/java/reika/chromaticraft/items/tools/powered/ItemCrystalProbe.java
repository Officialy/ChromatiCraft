/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools.powered;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import reika.chromaticraft.auxiliary.interfaces.ColoredMultiBlockChromaTile;
import reika.chromaticraft.auxiliary.interfaces.MultiBlockChromaTile;
import reika.chromaticraft.auxiliary.interfaces.SpecialStructureTile;
import reika.chromaticraft.auxiliary.render.StructureErrorOverlays;
import reika.chromaticraft.auxiliary.structure.RitualStructure;
import reika.chromaticraft.base.ItemPoweredChromaTool;
import reika.chromaticraft.magic.ToolChargingSystem;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.auxiliary.trackers.TickScheduler;
import reika.dragonapi.base.BlockMultiBlock;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.event.ScheduledTickEvent;
import reika.dragonapi.instantiable.event.scheduledtickevent.ScheduledSoundEvent;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.function.Consumer;

/**
 * V33a {@code ItemCrystalProbe}, Examination Crystals: a blue-powered tool that, held for 2.5 seconds and released while
 * looking at a block within 5, runs one of three inspections (sneak-use cycles them):
 * <ul>
 * <li>repeater connectivity (60 energy): for a crystal receiver, which elements it conducts and whether each reaches a
 * source, shown beside the crosshair;</li>
 * <li>structure check (1000): a ChromatiCraft multiblock's structure is matched, marking the first wrong block with a
 * static cube, or chiming a rising triad when it is whole;</li>
 * <li>multiblock (1500): the same for a DragonAPI multiblock block, checked from the face looked at.</li>
 * </ul>
 */
public class ItemCrystalProbe extends ItemPoweredChromaTool {

	public static final int CHARGE_TIME = 50;

	public ItemCrystalProbe(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack is, TooltipContext context, TooltipDisplay display, Consumer<Component> li,
			TooltipFlag flag) {
		super.appendHoverText(is, context, display, li, flag);
		li.accept(Component.literal("Mode: "+Inspections.list[getActionType(is)].displayName()));
	}

	@Override
	public boolean releaseUsing(ItemStack is, Level world, LivingEntity entity, int remaining) {
		int count = Mth.clamp(this.getUseDuration(is, entity)-remaining, 0, CHARGE_TIME);
		if (count >= CHARGE_TIME && entity instanceof Player ep)
			this.fire(is, world, ep);
		return true;
	}

	private void fire(ItemStack is, Level world, Player ep) {
		if (world.isClientSide())
			return;
		BlockHitResult mov = ReikaPlayerAPI.getLookedAtBlock(ep, 5, false);
		if (mov != null && mov.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = mov.getBlockPos();
			Inspections i = Inspections.list[getActionType(is)];
			if (i != null && i.isValid(world, pos) && ToolChargingSystem.instance.getCharge(is) >= i.energyCost) {
				ChromaSounds.LOREHEX.playSound(ep, 1, 1);
				if (i.doEffect(world, pos, mov, ep, is)) {
					ToolChargingSystem.instance.removeCharge(is, i.energyCost, ep);
				}
			}
		}
	}

	@Override
	public int getUseDuration(ItemStack is, LivingEntity user) {
		return 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack is) {
		return ItemUseAnimation.BLOCK;
	}

	@Override
	public InteractionResult use(Level world, Player ep, InteractionHand hand) {
		ItemStack is = ep.getItemInHand(hand);
		if (ep.isShiftKeyDown()) {
			if (!world.isClientSide()) {
				int type = (getActionType(is)+1)%Inspections.list.length;
				ReikaItemHelper.updateStackTag(is, tag -> tag.putInt("type", type));
				ChromaSounds.USE.playSound(ep);
			}
			return InteractionResult.SUCCESS;
		}
		else if (!this.handleUseAllowance(ep) && this.isActivated(ep, is, true)) {
			ep.startUsingItem(hand);
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	public static int getActionType(ItemStack is) {
		var tag = ReikaItemHelper.getStackTag(is);
		int type = tag != null ? tag.getIntOr("type", 0) : 0;
		return Mth.clamp(type, 0, Inspections.list.length-1);
	}

	@Override
	public void onUseTick(Level world, LivingEntity entity, ItemStack is, int remaining) {
		int count = Mth.clamp(this.getUseDuration(is, entity)-remaining, 0, CHARGE_TIME);
		if (!world.isClientSide()) {
			ChromaSounds.USE.playSound(entity, 0.25F+2F*count/CHARGE_TIME, Math.max(0.5F, 2F*count/CHARGE_TIME));
		}
	}

	@Override
	public boolean isFoil(ItemStack is) {
		return ToolChargingSystem.instance.getCharge(is)/(float)this.getMaxCharge() > 0.8;
	}

	@Override
	public CrystalElement getColor(ItemStack is) {
		return CrystalElement.BLUE;
	}

	@Override
	public int getMaxCharge() {
		return 12000;
	}

	@Override
	public int getChargeStates() {
		return 4;
	}

	@Override
	public int getChargeState(float frac) {
		return frac >= Inspections.getMostExpensiveOperation().energyCost/(float)this.getMaxCharge() ? Math.max(1, super.getChargeState(frac)) : 0;
	}

	@Override
	public boolean isActivated(Player e, ItemStack is, boolean held) {
		return ToolChargingSystem.instance.getCharge(is) >= this.getRequiredCharge(is);
	}

	private int getRequiredCharge(ItemStack is) {
		return Inspections.list[getActionType(is)].energyCost;
	}

	@Override
	public int getChargeConsumptionRate(Player e, Level world, ItemStack is) {
		return 0;
	}

	@Override
	protected boolean doTick(ItemStack is, Level world, Player e, boolean held) {
		return false;
	}

	@Override
	public String getNotes(int subpage) {
		StringBuilder sb = new StringBuilder();
		sb.append("Action Types:\n");
		for (Inspections i : Inspections.list) {
			sb.append("  ");
			sb.append(i.displayName());
			sb.append("\n");
		}
		sb.append("\n");
		sb.append(super.getNotes(subpage));
		return sb.toString();
	}

	private enum Inspections {
		REPEATER_CONNECTIVITY(60, CrystalReceiver.class),
		STRUCTURE_CHECK(1000, MultiBlockChromaTile.class),
		MULTIBLOCK(1500, BlockMultiBlock.class),
		//IMMERSIVE(1500, ImmersiveEng.class),
		;

		public final int energyCost;
		private final Class trigger;

		private static final Inspections[] list = values();

		Inspections(int energy, Class check) {
			energyCost = energy;
			trigger = check;
		}

		private boolean doEffect(Level world, BlockPos pos, BlockHitResult hit, Player ep, ItemStack is) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			switch(this) {
				case REPEATER_CONNECTIVITY:
					for (CrystalElement e : CrystalElement.elements) {
						CrystalReceiver te = (CrystalReceiver)world.getBlockEntity(pos);
						boolean can = te.isConductingElement(e);
						boolean flag = can && CrystalNetworker.instance.checkConnectivity(e, te);
						if (ep instanceof ServerPlayer sp)
							ChromaNetwork.sendProbeConnectivity(sp, e, flag, can);
					}
					return true;
				case STRUCTURE_CHECK: {
					MultiBlockChromaTile te = (MultiBlockChromaTile)world.getBlockEntity(pos);
					if (te.canStructureBeInspected()) {
						ChromaStructures str = te.getPrimaryStructure();
						if (str != null) {
							Coordinate c = te.getStructureOffset();
							if (c == null)
								c = new Coordinate(0, 0, 0);
							str.getStructure().resetToDefaults();
							switch(str) {
								case RITUAL:
									((RitualStructure)str.getStructure()).initializeEnhance(true, false);
									break;
								default:
									break;
							}
							CrystalElement e = str.requiresColor ? ((ColoredMultiBlockChromaTile)te).getColor() : null;
							FilledBlockArray arr = str.requiresColor ? str.getArray(world, x, y, z, e) : str.getArray(world, x+ c.xCoord(), y+ c.yCoord(), z+ c.zCoord());
							if (arr.matchInWorld(StructureErrorOverlays.instance)) {
								this.playSuccess(world, pos);
							}
							return true;
						}
						else if (te instanceof SpecialStructureTile) {
							te.validateStructure();
							if (te.hasStructure()) {
								this.playSuccess(world, pos);
							}
							else
								((SpecialStructureTile)te).inspectStructure(StructureErrorOverlays.instance);
						}
					}
					return false;
				}
				case MULTIBLOCK: {
					BlockMultiBlock b = (BlockMultiBlock)world.getBlockState(pos).getBlock();
					b.checkForFullMultiBlock(world, x, y, z, hit.getDirection().getOpposite(), StructureErrorOverlays.instance);
					return true;
				}
				default:
					return false;
			}
		}

		/** V33a: CAST at G4/C5, then 1 five ticks later, then G5/C5 at ten -- a rising triad. */
		private void playSuccess(Level world, BlockPos pos) {
			double x = pos.getX()+0.5;
			double y = pos.getY()+0.5;
			double z = pos.getZ()+0.5;
			ChromaSounds.CAST.playSound(world, pos, 1, (float)MusicKey.G4.getRatio(MusicKey.C5));
			TickScheduler.instance.scheduleEvent(new ScheduledTickEvent(new ScheduledSoundEvent(ChromaSounds.CAST, world, x, y, z, 1, 1)), 5);
			TickScheduler.instance.scheduleEvent(new ScheduledTickEvent(new ScheduledSoundEvent(ChromaSounds.CAST, world, x, y, z, 1, (float)MusicKey.G5.getRatio(MusicKey.C5))), 10);
		}

		/** V33a {@code WordUtils.capitalize(name(), ' ', '_')}, which only upper-cases, so the constant name as it is. */
		public String displayName() {
			StringBuilder sb = new StringBuilder(this.name());
			for (int i = 0; i < sb.length(); i++) {
				if (i == 0 || sb.charAt(i-1) == ' ' || sb.charAt(i-1) == '_')
					sb.setCharAt(i, Character.toTitleCase(sb.charAt(i)));
			}
			return sb.toString();
		}

		private boolean isValid(Level world, BlockPos pos) {
			if (trigger.isAssignableFrom(world.getBlockState(pos).getBlock().getClass()))
				return true;
			BlockEntity te = world.getBlockEntity(pos);
			return te != null && trigger.isAssignableFrom(te.getClass());
		}

		public static Inspections getMostExpensiveOperation() {
			Inspections ret = null;
			for (Inspections i : values()) {
				if (ret == null || ret.energyCost < i.energyCost)
					ret = i;
			}
			return ret;
		}
	}

}
