package reika.chromaticraft.magic.enchantment;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.PylonDamage;
import reika.chromaticraft.registry.ChromaEnchants;
import reika.chromaticraft.registry.Chromabilities;
import reika.dragonapi.instantiable.event.AttackAggroEvent;
import reika.dragonapi.instantiable.event.EnderAttackTPEvent;
import reika.dragonapi.instantiable.event.PigZombieAggroSpreadEvent;
import reika.dragonapi.interfaces.entity.ClampedDamage;
import reika.dragonapi.libraries.ReikaAABBHelper;
import reika.dragonapi.libraries.ReikaEntityHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;

/**
 * The effects of the ChromatiCraft enchantments: V33a {@code ChromaticEventManager}'s enchantment handlers (that
 * class is otherwise still unported). The DragonAPI events are the 26.2 successors of the ASM hooks V33a listened to.
 *
 * <p>V33a's {@code autoCollectDirect} (an {@code EntityJoinWorldEvent} handler keyed on a
 * {@code collectItemPlayer} field) is not here: nothing in V33a ever set that field, so it never ran.
 */
@EventBusSubscriber(modid = ChromatiCraft.MODID)
public final class ChromaEnchantEvents {

	/** Zombified piglins struck by a player with Aggro Mask: the player's id, so the piglin will not rally others. */
	private static final String AGGROMASK_TAG = "chromaticraft:aggromask_attacker";

	/** Mobs whose experience an Area Strike already paid out (V33a zeroed their {@code experienceValue}). */
	private static final Set<LivingEntity> XP_TAKEN = Collections.newSetFromMap(new WeakHashMap<>());

	/** 1.7.10's harvest levels as 26.2's tool tiers: a tool's tier is the {@code incorrect_for_*} tag it denies drops for. */
	private static final List<TagKey<Block>> TIER_LADDER = List.of(BlockTags.INCORRECT_FOR_WOODEN_TOOL,
			BlockTags.INCORRECT_FOR_STONE_TOOL, BlockTags.INCORRECT_FOR_IRON_TOOL, BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
			BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

	private static boolean applyingAOE;
	private static boolean applyingPhasing;

	private ChromaEnchantEvents() {}

	/** Identity Retention. */
	@SubscribeEvent
	public static void applyIdentityRetention(BreakBlockEvent evt) {
		if (!(evt.getLevel() instanceof ServerLevel world))
			return;
		if (EnchantmentDataKeeper.handleBreak(world, evt.getPos(), evt.getState(), evt.getPlayer())) {
			evt.setCanceled(true);
			world.setBlock(evt.getPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	/** Submergence; both sides, as the client moves its own player. */
	@SubscribeEvent
	public static void sinkingEnchantment(EntityTickEvent.Pre ev) {
		if (!(ev.getEntity() instanceof LivingEntity e))
			return;
		ItemStack boots = e.getItemBySlot(EquipmentSlot.FEET);
		if (!boots.isEmpty() && ChromaEnchants.FASTSINK.getLevel(boots) > 0) {
			if (!e.onGround() && e.isInWater() && e.getDeltaMovement().y < 0) {
				e.setDeltaMovement(e.getDeltaMovement().add(0, -0.0625, 0));
				e.hurtMarked = true;
			}
		}
	}

	/**
	 * Enhanced Utility. 1.7.10 raised the tool's harvest level for its tool class; 26.2 has tool tiers instead, each
	 * a tag of blocks it may not drop. The tool's tier is read from its {@code Tool} rules, raised by the level, and the
	 * block is harvestable if the raised tier's tag does not hold it and the tool's own kind (its other rules) does.
	 */
	@SubscribeEvent
	public static void boostHarvestLevel(PlayerEvent.HarvestCheck ev) {
		if (ev.canHarvest())
			return;
		ItemStack tool = ev.getEntity().getMainHandItem();
		int level = ChromaEnchants.HARVESTLEVEL.getLevel(tool);
		if (level > 0 && canHarvestAtBoostedTier(tool, ev.getTargetBlock(), level))
			ev.setCanHarvest(true);
	}

	private static boolean canHarvestAtBoostedTier(ItemStack is, BlockState state, int boost) {
		Tool tool = is.get(DataComponents.TOOL);
		if (tool == null)
			return false;
		int tier = -1;
		Boolean correct = null;
		for (Tool.Rule rule : tool.rules()) {
			int t = tierOf(rule.blocks());
			if (t >= 0) {
				tier = t;
			}
			else if (correct == null && rule.correctForDrops().isPresent() && state.is(rule.blocks())) {
				correct = rule.correctForDrops().get();
			}
		}
		if (tier < 0 || correct == null || !correct)
			return false;
		int boosted = Math.min(tier+boost, TIER_LADDER.size()-1);
		return !state.is(TIER_LADDER.get(boosted));
	}

	/** Gold is wood's tier and copper stone's, as 26.2 ranks them. */
	private static int tierOf(HolderSet<Block> blocks) {
		Optional<TagKey<Block>> key = blocks.unwrapKey();
		if (key.isEmpty())
			return -1;
		TagKey<Block> tag = key.get();
		if (tag.equals(BlockTags.INCORRECT_FOR_GOLD_TOOL))
			return 0;
		if (tag.equals(BlockTags.INCORRECT_FOR_COPPER_TOOL))
			return 1;
		return TIER_LADDER.indexOf(tag);
	}

	/**
	 * Inventory Insertion: the drops of a block the player broke go into their inventory, unless something vetoes
	 * the pickup. (V33a cleared every drop afterwards, so a vetoed one was destroyed; here it stays in the world.)
	 */
	@SubscribeEvent
	public static void autoCollectPost(BlockDropsEvent evt) {
		if (!(evt.getBreaker() instanceof Player ep) || ReikaPlayerAPI.isFake(ep))
			return;
		int level = ChromaEnchants.AUTOCOLLECT.getLevel(evt.getTool());
		if (level > 0) {
			Iterator<ItemEntity> it = evt.getDrops().iterator();
			while (it.hasNext()) {
				ItemEntity ei = it.next();
				ItemEntityPickupEvent.Pre pickup = EventHooks.fireItemPickupPre(ei, ep);
				if (pickup.canPickup() != TriState.FALSE) {
					ReikaPlayerAPI.addOrDropItem(ei.getItem(), ep);
					it.remove();
				}
			}
			RandomSource rand = ep.getRandom();
			ep.level().playSound(null, ep.getX(), ep.getY(), ep.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
					0.25F+0.25F*rand.nextFloat(), ((rand.nextFloat()-rand.nextFloat())*0.7F+1)*2);
		}
	}

	/**
	 * Reposession: 1.7.10's {@code dropEquipment(true, 100+100*level^2)} (so near-certain drops of everything the mob
	 * wears), then, with the same odds, its {@code dropRareDrop} and, half the time, its head.
	 */
	@SubscribeEvent
	public static void rareLootBoost(LivingDropsEvent ev) {
		if (!(ev.getSource().getEntity() instanceof LivingEntity src))
			return;
		int level = ChromaEnchants.RARELOOT.getLevel(src.getMainHandItem());
		if (level > 0) {
			LivingEntity e = ev.getEntity();
			RandomSource rand = e.getRandom();
			int sum = 100+100*level*level;
			if (e instanceof Mob mob)
				dropEquipment(mob, ev.getDrops(), sum);
			if (rand.nextInt(sum) >= 100) {
				dropRareDrop(e, ev.getDrops());
				if (rand.nextBoolean()) {
					ReikaEntityHelper.dropHead(e);
				}
			}
			// CHROMA-PORT: V33a also rolled level*5 % for a Thaumic Tinkerer kami resource (nether shard from a
			// zombie pigman in the Nether, ender shard from an enderman in the End). Thaumic Tinkerer has no 26.2 port.
		}
	}

	/** 1.7.10 {@code EntityLiving.dropEquipment}; 26.2 already emptied the slots whose items dropped normally. */
	private static void dropEquipment(Mob mob, Collection<ItemEntity> drops, int looting) {
		RandomSource rand = mob.getRandom();
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (slot.getType() != EquipmentSlot.Type.HAND && slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
				continue;
			ItemStack is = mob.getItemBySlot(slot);
			if (is.isEmpty() || EnchantmentHelper.has(is, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP))
				continue;
			float chance = mob.getDropChances().byEquipment(slot);
			boolean preserve = chance > 1.0F;
			if (rand.nextFloat()-looting*0.01F < chance) {
				if (!preserve && is.isDamageableItem()) {
					int k = Math.max(is.getMaxDamage()-25, 1);
					int l = is.getMaxDamage()-rand.nextInt(rand.nextInt(k)+1);
					if (l > k)
						l = k;
					if (l < 1)
						l = 1;
					is.setDamageValue(l);
				}
				addDrop(mob, drops, is);
				mob.setItemSlot(slot, ItemStack.EMPTY);
			}
		}
	}

	/** The 1.7.10 {@code dropRareDrop} overrides; 26.2 folded these into loot tables, where they cannot be singled out. */
	private static void dropRareDrop(LivingEntity e, Collection<ItemEntity> drops) {
		if (e instanceof ZombifiedPiglin) {
			addDrop(e, drops, new ItemStack(Items.GOLD_INGOT));
		}
		else if (e instanceof Zombie) {
			switch (e.getRandom().nextInt(3)) {
				case 0 -> addDrop(e, drops, new ItemStack(Items.IRON_INGOT));
				case 1 -> addDrop(e, drops, new ItemStack(Items.CARROT));
				case 2 -> addDrop(e, drops, new ItemStack(Items.POTATO));
			}
		}
		else if (e instanceof WitherSkeleton) {
			addDrop(e, drops, new ItemStack(Items.WITHER_SKELETON_SKULL));
		}
	}

	private static void addDrop(LivingEntity e, Collection<ItemEntity> drops, ItemStack is) {
		ItemEntity ei = new ItemEntity(e.level(), e.getX(), e.getY(), e.getZ(), is);
		ei.setDefaultPickUpDelay();
		drops.add(ei);
	}

	/** Aggro Mask II: a struck monster does not turn on its attacker. Also marks piglins for Aggro Mask I. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void applyAggroMask(AttackAggroEvent evt) {
		if (evt.source.getEntity() instanceof Player ep) {
			ItemStack weapon = ep.getMainHandItem();
			int level = ChromaEnchants.AGGROMASK.getLevel(weapon);
			if (evt.getMob() instanceof ZombifiedPiglin piglin) {
				if (level > 0 && EnchantmentAggroMask.hidePigmanSpreadDamage(level))
					piglin.getPersistentData().putString(AGGROMASK_TAG, ep.getStringUUID());
				else
					piglin.getPersistentData().remove(AGGROMASK_TAG);
			}
			if (level > 0 && EnchantmentAggroMask.hideDirectDamage(level)) {
				evt.setCanceled(true);
			}
		}
	}

	/**
	 * Aggro Mask I: a zombified piglin struck with the mask does not rally the others. 1.7.10 rallied them once, at the
	 * hit; 26.2 re-rallies every few seconds, so the piglin remembers who masked the hit.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void applyAggroMask(PigZombieAggroSpreadEvent evt) {
		if (evt.target instanceof Player ep
				&& ep.getStringUUID().equals(evt.getPiglin().getPersistentData().getStringOr(AGGROMASK_TAG, ""))) {
			evt.setCanceled(true);
		}
	}

	/** Ender Lock. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void applyEnderLock(EnderAttackTPEvent evt) {
		if (evt.source.getEntity() instanceof Player ep) {
			ItemStack weapon = ep.getMainHandItem();
			if (ChromaEnchants.ENDERLOCK.getLevel(weapon) > 0) {
				evt.setCanceled(true);
			}
		}
	}

	/**
	 * Durability Leech. V33a's LivingHurtEvent ran only for damage that got past the hurt cooldown, before armour;
	 * {@code LivingDamageEvent.Pre} is past the cooldown, so the repair uses the incoming amount and scales what is left.
	 */
	@SubscribeEvent
	public static void applyUseRepair(LivingDamageEvent.Pre evt) {
		if (evt.getSource().getEntity() instanceof Player ep) {
			ItemStack weapon = ep.getMainHandItem();
			int level = ChromaEnchants.USEREPAIR.getLevel(weapon);
			if (level > 0) {
				int rep = EnchantmentUseRepair.getRepairedDurability(weapon, level, evt.getOriginalDamage());
				weapon.setDamageValue(weapon.getDamageValue()-rep);
				evt.setNewDamage(evt.getNewDamage()*(float)Math.pow(0.9875, rep));
			}
		}
	}

	/** Capital Strike. */
	@SubscribeEvent
	public static void applyBossKill(LivingIncomingDamageEvent evt) {
		DamageSource src = evt.getSource();
		// CHROMA-PORT: V33a returned here for LumenTurretDamage, so a turret's shots never counted as the player's
		// Capital Strike. The Lumen Turret's damage type lands with the turret; test it here then.
		LivingEntity mob = evt.getEntity();
		if (!(mob.level() instanceof ServerLevel))
			return;
		if (mob.isRemoved() || mob.getHealth() <= 0)
			return;
		if (src.getEntity() instanceof Player ep) {
			ItemStack weapon = ep.getMainHandItem();
			int level = ChromaEnchants.BOSSKILL.getLevel(weapon);
			if (level > 0) {
				float dmg = EnchantmentBossKill.getDamageDealt(mob, level);
				if (mob instanceof ClampedDamage cd) {
					dmg = Math.min(cd.getDamageCap(src, dmg), dmg);
				}
				if (dmg > 0) {
					if (dmg >= mob.getHealth()) {
						mob.setHealth(0);
						mob.die(src);
					}
					else {
						PylonDamage.doPylonAttack(null, mob, dmg);
					}
				}
				else {
					weapon.hurtAndBreak(4, ep, EquipmentSlot.MAINHAND);
					ep.level().playSound(null, ep.getX(), ep.getY(), ep.getZ(), SoundEvents.ITEM_BREAK.value(),
							SoundSource.PLAYERS, 1, 1);
					if (mob instanceof Creeper creeper) {
						ReikaEntityHelper.chargeCreeper(creeper);
					}
				}
				evt.setCanceled(true);
			}
		}
	}

	/** Area Strike. */
	@SubscribeEvent
	public static void applyAOE(LivingIncomingDamageEvent evt) {
		if (applyingAOE || applyingPhasing)
			return;
		DamageSource src = evt.getSource();
		if (src.getEntity() instanceof Player ep && evt.getEntity().level() instanceof ServerLevel world) {
			LivingEntity mob = evt.getEntity();
			ItemStack weapon = ep.getMainHandItem();
			int level = ChromaEnchants.WEAPONAOE.getLevel(weapon);
			if (level > 0) {
				applyingAOE = true;
				try {
					double r = EnchantmentWeaponAOE.getRadius(level);
					AABB box = ReikaAABBHelper.getEntityCenteredAABB(mob, r);
					Class<? extends LivingEntity> cat = ReikaEntityHelper.getEntityCategoryClass(mob);
					List<? extends LivingEntity> li = world.getEntitiesOfClass(cat, box);
					for (LivingEntity e : li) {
						if (e != mob && e != ep) {
							Class<? extends LivingEntity> cat2 = ReikaEntityHelper.getEntityCategoryClass(e);
							if (cat2 == cat) {
								double d = e.distanceTo(mob);
								double f = EnchantmentWeaponAOE.getDamageFactor(level, d, r);
								if (f > 0) {
									float dmg2 = (float)(f*evt.getAmount());
									if (e instanceof Mob && dmg2 >= e.getHealth() && Chromabilities.RANGEDBOOST.enabledOn(ep)
											&& XP_TAKEN.add(e)) {
										int xp = e.getExperienceReward(world, ep);
										// CHROMA-PORT: V33a quadrupled this with a purple Crystal Pendant III in the
										// inventory, or doubled it with a purple Crystal Pendant; the pendants are
										// not yet ported.
										ep.giveExperiencePoints(xp);
									}
									e.hurtServer(world, src, dmg2);
								}
							}
						}
					}
				}
				finally {
					applyingAOE = false;
				}
			}
		}
	}

	/** Area Strike paid a mob's experience out when it was struck; it drops none when it dies. */
	@SubscribeEvent
	public static void noDoubleAOEExperience(LivingExperienceDropEvent evt) {
		if (XP_TAKEN.contains(evt.getEntity()))
			evt.setDroppedExperience(0);
	}

	/** Consistent Efficiency: at full level every block takes fifteen ticks, whatever its hardness. */
	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
	public static void forceConstantMiningSpeed(PlayerEvent.BreakSpeed evt) {
		ItemStack is = evt.getEntity().getMainHandItem();
		int lvl = ChromaEnchants.MINETIME.getLevel(is);
		if (lvl > 0) {
			BlockState state = evt.getState();
			Optional<BlockPos> pos = evt.getPosition();
			float h = pos.isPresent() ? state.getDestroySpeed(evt.getEntity().level(), pos.get())
					: state.getBlock().defaultDestroyTime();
			if (h > 0) {
				float f = (float)Math.pow(lvl/(float)ChromaEnchants.MINETIME.getEnchantment().getMaxLevel(), 1.5);
				float best = 2*h; //force same speed on everything, even superhard blocks
				evt.setNewSpeed(best*f+evt.getOriginalSpeed()*(1-f));
				evt.setCanceled(false);
			}
		}
	}

	/** Air Miner: undoes the fivefold airborne mining penalty (the Reach ability already does). */
	@SubscribeEvent
	public static void negateFlyMiningSlowdown(PlayerEvent.BreakSpeed evt) {
		Player ep = evt.getEntity();
		if (!ep.onGround()) {
			if (!Chromabilities.REACH.enabledOn(ep)) {
				ItemStack is = ep.getMainHandItem();
				if (ChromaEnchants.AIRMINER.getLevel(is) > 0) {
					evt.setNewSpeed(evt.getNewSpeed()*5);
				}
			}
		}
	}

	/**
	 * Armor Breach: part of the hit is re-dealt as absolute pylon energy, the rest as a normal hit of the same kind;
	 * the original blow is left to land too, and the hurt cooldown sorts out how much of it still gets through.
	 */
	@SubscribeEvent
	public static void applyDamagePhasing(LivingIncomingDamageEvent evt) {
		DamageSource src = evt.getSource();
		if (applyingPhasing)
			return;
		if (!src.is(DamageTypeTags.BYPASSES_EFFECTS) && evt.getEntity().level() instanceof ServerLevel world) {
			if (src.getEntity() instanceof LivingEntity elb) {
				ItemStack is = elb.getMainHandItem();
				int lvl = ChromaEnchants.PHASING.getLevel(is);
				if (lvl > 0) {
					applyingPhasing = true;
					try {
						float pierce = EnchantmentPhasingSequence.getPenetratingDamage(evt.getAmount(), lvl);
						float leftover = EnchantmentPhasingSequence.getSpilloverDamage(evt.getAmount(), lvl);
						evt.getEntity().hurtServer(world, src, leftover);
						PylonDamage.doPylonAttack(null, evt.getEntity(), pierce);
					}
					finally {
						applyingPhasing = false;
					}
				}
			}
		}
	}

}
