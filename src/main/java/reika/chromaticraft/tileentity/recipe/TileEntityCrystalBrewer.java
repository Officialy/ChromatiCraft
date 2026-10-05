/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import reika.chromaticraft.auxiliary.interfaces.OperationInterval;
import reika.chromaticraft.base.tileentity.InventoriedChromaticBase;
import reika.chromaticraft.container.MenuCrystalBrewer;
import reika.chromaticraft.item.ItemCrystalShard;
import reika.chromaticraft.items.tools.ItemCrystalPotion;
import reika.chromaticraft.items.tools.ItemSplashCrystalPotion;
import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

/**
 * V33a {@code TileEntityCrystalBrewer}, the Crystal Brewery: a crystal shard in the top slot turns every bottle below it
 * into that colour's potion after 400 ticks, consuming the shard. A boosted shard makes the level II, extended form.
 * Black and purple shards instead lengthen or strengthen a potion that already has an effect.
 *
 * <p>V33a worked in 1.7.10's potion damage values, brewing the shard's ingredient string into an awkward potion
 * ({@code PotionHelper.applyIngredient}). The results are fixed per colour, so they are tabled here from 1.7.10's own
 * potion algebra: every plain result is the same-named 26.2 potion with the same duration; a boosted result is the
 * {@code long_} potion where the effect has no level II in 1.7.10, and a custom level II extended effect with 1.7.10's
 * duration where it has.
 */
public class TileEntityCrystalBrewer extends InventoriedChromaticBase implements OperationInterval, MenuProvider {

	public static final int BREW_TIME = 400;

	private int time = BREW_TIME;

	/** 1.7.10's brewed result for each plain colour, and its boosted (level II + extended bits) form. */
	private record PlainResult(Holder<Potion> potion, PotionContents boosted) {}

	private static final EnumMap<CrystalElement, PlainResult> PLAIN = new EnumMap<>(CrystalElement.class);

	static {
		// Golden carrot -> night vision; boosted: no amplifier in 1.7.10, so 8:00.
		PLAIN.put(CrystalElement.BLUE, new PlainResult(Potions.NIGHT_VISION, new PotionContents(Potions.LONG_NIGHT_VISION)));
		// Sugar, then fermented spider eye -> slowness 1:30; boosted 4:00.
		PLAIN.put(CrystalElement.GRAY, new PlainResult(Potions.SLOWNESS, new PotionContents(Potions.LONG_SLOWNESS)));
		// Spider eye -> poison 0:45; boosted: poison II for 1:00.
		PLAIN.put(CrystalElement.GREEN, new PlainResult(Potions.POISON, custom(MobEffects.POISON, 1200, "poison")));
		// Sugar -> swiftness 3:00; boosted: swiftness II for 4:00.
		PLAIN.put(CrystalElement.LIGHTBLUE, new PlainResult(Potions.SWIFTNESS, custom(MobEffects.SPEED, 4800, "swiftness")));
		// Blaze powder, then fermented spider eye -> weakness 1:30; boosted 4:00.
		PLAIN.put(CrystalElement.LIGHTGRAY, new PlainResult(Potions.WEAKNESS, new PotionContents(Potions.LONG_WEAKNESS)));
		// Ghast tear -> regeneration 0:45; boosted: regeneration II for 1:00.
		PLAIN.put(CrystalElement.MAGENTA, new PlainResult(Potions.REGENERATION, custom(MobEffects.REGENERATION, 1200, "regeneration")));
		// Magma cream -> fire resistance; boosted 8:00.
		PLAIN.put(CrystalElement.ORANGE, new PlainResult(Potions.FIRE_RESISTANCE, new PotionContents(Potions.LONG_FIRE_RESISTANCE)));
		// Blaze powder -> strength 3:00; boosted: strength II for 4:00.
		PLAIN.put(CrystalElement.PINK, new PlainResult(Potions.STRENGTH, custom(MobEffects.STRENGTH, 4800, "strength")));
		// Golden carrot, then fermented spider eye -> invisibility; boosted 8:00.
		PLAIN.put(CrystalElement.WHITE, new PlainResult(Potions.INVISIBILITY, new PotionContents(Potions.LONG_INVISIBILITY)));
	}

	public TileEntityCrystalBrewer(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.CRYSTAL_BREWER.get(), pos, state);
	}

	private static PotionContents custom(Holder<MobEffect> effect, int duration, String name) {
		return new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(effect, duration, 1)),
				Optional.of(name));
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (world.isClientSide())
			return;
		if (this.canBrew()) {
			time--;
			if (time <= 0) {
				this.brew();
				time = BREW_TIME;
			}
		}
		else {
			time = BREW_TIME;
		}
	}

	private boolean canBrew() {
		if (!(inv.get(0).getItem() instanceof ItemCrystalShard))
			return false;
        return this.isSlotModifiable(1) || this.isSlotModifiable(2) || this.isSlotModifiable(3);
    }

	private boolean isSlotModifiable(int i) {
		ItemStack is = inv.get(i);
		CrystalElement color = ((ItemCrystalShard)inv.get(0).getItem()).element();
		if (is.isEmpty())
			return false;
		if (isVanillaPotion(is)) {
			boolean actual = is.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).hasEffects();
			if (CrystalPotionController.instance.isPotionModifier(color))
				return actual;
			else
				return !actual;
		}
		else if (isCrystalPotion(is)) {
			return CrystalPotionController.instance.isPotionModifier(color);
		}
		return false;
	}

	/** V33a converts every occupied bottle slot, not only the ones that made the brew valid. */
	private void brew() {
		ItemCrystalShard shard = (ItemCrystalShard)inv.get(0).getItem();
		CrystalElement color = shard.element();
		boolean boost = shard.boosted();
		inv.set(0, ItemStack.EMPTY);

		for (int i = 1; i < 4; i++) {
			if (!inv.get(i).isEmpty()) {
				inv.set(i, getPotionStackFromColor(inv.get(i), color, boost));
			}
		}
		this.setChanged();
	}

	/**
	 * V33a {@code getPotionStackFromColor}, the brewed form of one bottle. The colours needing a custom potion give a
	 * Crystal Potion; black (redstone: lengthen) and purple (glowstone: strengthen) modify the bottle's own potion; the
	 * rest give their fixed brew as a drinkable potion, whatever the bottle held.
	 */
	public static ItemStack getPotionStackFromColor(ItemStack input, CrystalElement color, boolean boost) {
		CrystalPotionController cpc = CrystalPotionController.instance;
		if (cpc.requiresCustomPotion(color))
			return ItemCrystalPotion.create(color, false, boost, boost);
		if (cpc.isPotionModifier(color))
			return modify(input, color == CrystalElement.BLACK, boost);
		PlainResult r = PLAIN.get(color);
		ItemStack is = new ItemStack(Items.POTION);
		is.set(DataComponents.POTION_CONTENTS, boost ? r.boosted() : new PotionContents(r.potion()));
		return is;
	}

	/**
	 * 1.7.10's redstone ({@code -5+6-7}) sets the extended bit and clears the level II bit, glowstone ({@code +5-6-7})
	 * the reverse; a boosted shard then sets both. In 26.2 terms: the {@code long_} or {@code strong_} member of the
	 * potion's family if it has one, else its base; with both, the base's effects at the strong level for half the long
	 * duration (1.7.10 halved the duration per level). The bottle keeps its form -- drinkable, splash or lingering.
	 *
	 * <p>V33a treated a Crystal Potion here as a vanilla potion whose damage value was the crystal potion's, which gave
	 * an unrelated potion. It now lengthens or strengthens the Crystal Potion itself.
	 */
	private static ItemStack modify(ItemStack input, boolean extend, boolean boost) {
		boolean ext = boost || extend;
		boolean amp = boost || !extend;
		if (input.getItem() instanceof ItemCrystalPotion cp)
			return ItemCrystalPotion.create(cp.getColor(), false, ext, amp);
		if (input.getItem() instanceof ItemSplashCrystalPotion cp)
			return ItemCrystalPotion.create(cp.getColor(), true, ext, amp);

		ItemStack is = input.copyWithCount(1);
		PotionContents contents = is.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		Identifier base = baseId(contents);
		if (base == null)
			return is;
		Optional<Holder.Reference<Potion>> basePotion = BuiltInRegistries.POTION.get(base);
		if (basePotion.isEmpty())
			return is;
		Optional<Holder.Reference<Potion>> longPotion = variant(base, "long_");
		Optional<Holder.Reference<Potion>> strongPotion = variant(base, "strong_");
		if (!boost) {
			Holder<Potion> target = (extend ? longPotion : strongPotion).map(h -> (Holder<Potion>)h).orElse(basePotion.get());
			is.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(target), contents.customColor(),
					List.of(), Optional.empty()));
			return is;
		}
		List<MobEffectInstance> effects = new ArrayList<>();
		List<MobEffectInstance> baseEffects = basePotion.get().value().getEffects();
		for (int i = 0; i < baseEffects.size(); i++) {
			MobEffectInstance e = baseEffects.get(i);
			int duration = longPotion.isPresent() && i < longPotion.get().value().getEffects().size()
					? longPotion.get().value().getEffects().get(i).getDuration() : e.getDuration();
			int level = e.getAmplifier();
			if (strongPotion.isPresent() && i < strongPotion.get().value().getEffects().size()) {
				level = strongPotion.get().value().getEffects().get(i).getAmplifier();
				if (!e.getEffect().value().isInstantaneous())
					duration /= 2;
			}
			effects.add(new MobEffectInstance(e.getEffect(), duration, level));
		}
		is.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), contents.customColor(), effects,
				Optional.of(basePotion.get().value().name())));
		return is;
	}

	/** The family's base potion: the potion's id without {@code long_}/{@code strong_}, or a custom potion's name. */
	private static Identifier baseId(PotionContents contents) {
		if (contents.potion().isPresent()) {
			Identifier id = contents.potion().get().unwrapKey().map(k -> k.identifier()).orElse(null);
			if (id == null)
				return null;
			String path = id.getPath();
			if (path.startsWith("long_"))
				path = path.substring(5);
			else if (path.startsWith("strong_"))
				path = path.substring(7);
			return id.withPath(path);
		}
		return contents.customName().map(Identifier::tryParse).orElse(null);
	}

	private static Optional<Holder.Reference<Potion>> variant(Identifier base, String prefix) {
		return BuiltInRegistries.POTION.get(base.withPath(prefix + base.getPath()));
	}

	private static boolean isVanillaPotion(ItemStack is) {
		return is.is(Items.POTION) || is.is(Items.SPLASH_POTION) || is.is(Items.LINGERING_POTION);
	}

	private static boolean isCrystalPotion(ItemStack is) {
		return is.getItem() instanceof ItemCrystalPotion || is.getItem() instanceof ItemSplashCrystalPotion;
	}

	@Override
	protected void animateWithTick(Level world, BlockPos pos) {

	}

	@Override
	public int getSizeInventory() {
		return 4;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public boolean stillValid(Player ep) {
		return ep.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.getBlockPos())) <= 64;
	}

	/** V33a: shards (and anything with a potion effect, which never brews) on top, bottles below. */
	@Override
	public boolean canPlaceItem(int i, ItemStack is) {
		if (isCrystalPotion(is))
			return i != 0;
		if (is.getItem() instanceof ItemCrystalShard)
			return i == 0;
		if (isVanillaPotion(is))
			return i != 0;
		if (this.getLevel() != null && this.getLevel().recipeAccess()
				.propertySet(net.minecraft.world.item.crafting.RecipePropertySet.BREWING_REAGENTS).test(is))
			return i == 0;
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int i, ItemStack itemstack, Direction side) {
		return i != 0;
	}

	public int getBrewTime() {
		return time;
	}

	public void setBrewTime(int par2) {
		time = par2;
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		time = NBT.getIntOr("BrewTime", BREW_TIME);
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);
		NBT.putInt("BrewTime", time);
	}

	@Override
	public int getRedstoneOverride() {
		return this.canBrew() ? 0 : 15;
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.BREWER;
	}

	@Override
	public float getOperationFraction() {
		return !this.canBrew() ? 0 : 1-time/(float)BREW_TIME;
	}

	@Override
	public OperationState getState() {
		return this.canBrew() ? OperationState.RUNNING : OperationState.INVALID;
	}

	public boolean hasWork() {
		return this.getState() == OperationState.RUNNING;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.chromaticraft.crystal_brewer");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MenuCrystalBrewer(id, inventory, this);
	}

}
