/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.base;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import reika.chromaticraft.api.interfaces.CrystalEffectBoostArmor;
import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.interfaces.block.SemiUnbreakable;
import reika.dragonapi.libraries.registry.ReikaDyeHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * Base for the effect-giving crystal blocks. Port note: the 1.7.10 ISBRH/IIcon render surface is
 * stripped (getRenderType/getIcon/registerBlockIcons/canRenderInPass etc. gone) — 26.2 renders via a
 * model + a per-element tint colour provider using {@link #getTintColor}. The particle effects
 * (randomDisplayTick/addHitEffects, EntityCCBlurFX/FloatingSeedsFX + ChromaPackets) and the redstone
 * "ding" note are deferred (client cosmetic / sound framework unported). Thaumcraft IInfusionStabiliser
 * and DragonAPI Submergeable interfaces are deferred (unported). The potion effect (the block's core
 * function) is kept via {@link CrystalPotionController}.
 */
public abstract class CrystalBlock extends CrystalTypeBlock implements SemiUnbreakable {

	protected static final Random rand = new Random();

	protected CrystalBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public float getEnchantPowerBonus(BlockState state, BlockGetter level, BlockPos pos) {
		if (this == reika.chromaticraft.registry.ChromaBlocks.LAMP.get() || state.getValue(COLOR) != CrystalElement.PURPLE.ordinal())
			return 0;
		return this == reika.chromaticraft.registry.ChromaBlocks.SUPER.get() ? 1.5F : 1;
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (level instanceof Level l && this.isUnbreakable(l, pos))
			return 0;
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	public boolean isUnbreakable(Level world, BlockPos pos) {
		return false;
	}

	public final void updateEffects(Level world, BlockPos pos) {
		if (!world.isClientSide()) {
			CrystalElement color = this.getCrystalElement(world.getBlockState(pos));
			if (this.shouldMakeNoise()) {
				float f3 = 0.5F * ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.8F);
				world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.05F, f3);
			}
			int r = this.getRange();
			AABB box = new AABB(pos).inflate(r);
			List<LivingEntity> inbox = world.getEntitiesOfClass(LivingEntity.class, box);
			Collections.shuffle(inbox);
			this.applyEffect(color, inbox, pos, r);
		}
	}

	public final void applyEffect(CrystalElement color, List<LivingEntity> li, BlockPos pos, int r) {
		int level = this.getPotionLevel(color);
		int dura = this.getDuration(color);
		boolean player = false;
		for (LivingEntity e : li) {
			if (e instanceof Player) {
				if (player)
					continue;
				else
					player = true;
			}
			double dx = e.getX() - pos.getX() - 0.5;
			double dy = e.getY() + e.getEyeHeight() / 2F - pos.getY() - 0.5;
			double dz = e.getZ() - pos.getZ() - 0.5;
			if (Math.sqrt(dx * dx + dy * dy + dz * dz) <= r) {
				int dura2 = dura;
				int level2 = level;
				float slug = this.getSlugPower(e);
				if (slug > 0) {
					dura2 *= 1 - 0.2 * slug;
					level2 += slug;
				}
				CrystalPotionController.instance.applyEffectFromColor(dura2, level2, e, color, true);
			}
		}
	}

	private float getSlugPower(LivingEntity e) {
		float ret = 0;
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST}) {
			ItemStack is = e.getItemBySlot(slot);
			if (!is.isEmpty() && is.getItem() instanceof CrystalEffectBoostArmor a)
				ret += a.getPower(is);
		}
		return ret;
	}

	@Override
	public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity e) {
		return false;
	}

	/** Per-element tint colour for the block/item colour provider (replaces the ISBRH getTintColor). */
	public final int getTintColor(int meta) {
		int c0 = ReikaColorAPI.getModifiedSat(CrystalElement.elements[meta].getColor(), 0.65F);
		int c1 = ReikaDyeHelper.dyes[meta].color;
		return ReikaColorAPI.mixColors(c0, c1, 0.65F);
	}

	public abstract boolean shouldMakeNoise();

	public abstract boolean shouldGiveEffects(CrystalElement e);

	public abstract boolean performEffect(CrystalElement e);

	public abstract int getRange();

	public abstract int getDuration(CrystalElement e);

	public abstract int getPotionLevel(CrystalElement e);

	public abstract boolean renderBase();

	public boolean renderAllArms() {
		return this.renderBase();
	}
}
