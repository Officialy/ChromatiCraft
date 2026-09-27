/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.storage;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.api.interfaces.ManipulatorInteraction;
import reika.chromaticraft.base.tileentity.TileEntityMassStorage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.instantiable.data.KeyedItemStack;

/**
 * V33a {@code TileEntityToolStorage}, the Tool Compression Crate: holds up to 360,000 tools of one kind, chosen with the
 * Manipulator while it is empty -- pickaxes, axes, shovels, swords, bows, shears, each armour piece, enchanted books,
 * horse armour, potions, or any other unstackable item -- showing that kind floating above it.
 */
public class TileEntityToolStorage extends TileEntityMassStorage implements ManipulatorInteraction {

	private static final int MAX_COUNT = 360000;

	private ToolType filter = ToolType.OTHER;

	public TileEntityToolStorage(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.TOOL_STORAGE.get(), pos, state);
	}

	public boolean stepMode() {
		if (this.getPendingInput().isEmpty() && this.getItems().isEmpty()) {
			filter = filter.next();
			while (!filter.isValid())
				filter = filter.next();
			this.resetWorkTimer();
			this.setChanged();
			this.syncAllData(true);
			return true;
		}
		return false;
	}

	/** V33a's Manipulator branch for TOOLSTORAGE: the use sound on a change, the error sound when not empty. */
	@Override
	public boolean onManipulatorInteract(Player ep, int side) {
		if (!level.isClientSide()) {
			if (this.stepMode())
				ChromaSounds.USE.playSoundAtBlock(this);
			else
				ChromaSounds.ERROR.playSoundAtBlock(this);
		}
		return true;
	}

	public ToolType getFilter() {
		return filter;
	}

	@Override
	public ItemStack getFilterItemRender() {
		return filter.getRenderItem();
	}

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.TOOLSTORAGE;
	}

	/** Counted per kind: ignoring damage and components unless the filter distinguishes by them (V33a meta and NBT). */
	@Override
	protected KeyedItemStack key(ItemStack is) {
		return new KeyedItemStack(is).setIgnoreNBT(!filter.isDamageImportant() && !filter.isNBTImportant()).setSized(false).setSimpleHash(true);
	}

	@Override
	public boolean isItemValid(ItemStack is) {
		return filter.isItemValid(is);
	}

	@Override
	public int maxItemCount() {
		return MAX_COUNT;
	}

	@Override
	protected void writeSyncTag(CompoundTag NBT) {
		super.writeSyncTag(NBT);

		NBT.putInt("mode", filter.ordinal());
	}

	@Override
	protected void readSyncTag(CompoundTag NBT) {
		super.readSyncTag(NBT);

		int mode = NBT.getIntOr("mode", ToolType.OTHER.ordinal());
		filter = ToolType.list[Math.clamp(mode, 0, ToolType.list.length-1)];
	}

	public static enum ToolType {
		PICK(() -> new ItemStack(Items.DIAMOND_PICKAXE)),
		AXE(() -> new ItemStack(Items.IRON_AXE)),
		SHOVEL(() -> new ItemStack(Items.GOLDEN_SHOVEL)),
		SWORD(() -> new ItemStack(Items.DIAMOND_SWORD)),
		BOW(() -> new ItemStack(Items.BOW)),
		SHEARS(() -> new ItemStack(Items.SHEARS)),
		HELMET(() -> new ItemStack(Items.DIAMOND_HELMET)),
		CHESTPLATE(() -> new ItemStack(Items.DIAMOND_CHESTPLATE)),
		LEGS(() -> new ItemStack(Items.DIAMOND_LEGGINGS)),
		BOOTS(() -> new ItemStack(Items.DIAMOND_BOOTS)),
		BOOK(() -> new ItemStack(Items.ENCHANTED_BOOK)),
		HORSEARMOR(() -> new ItemStack(Items.GOLDEN_HORSE_ARMOR)),
		POTION(() -> PotionContents.createItemStack(Items.POTION, Potions.REGENERATION)),
		// CHROMA-PORT: V33a's Tinkers' Construct mode (a hammer icon), only offered with TiC loaded; no 26.2 TiC here.
		TINKER(() -> ItemStack.EMPTY),
		OTHER(() -> ItemStack.EMPTY);

		private static final ToolType[] list = values();

		/** Built on first use: an ItemStack made during class initialisation can precede its components being bound. */
		private final java.util.function.Supplier<ItemStack> iconFactory;
		private ItemStack icon;

		private ToolType(java.util.function.Supplier<ItemStack> is) {
			iconFactory = is;
		}

		public ItemStack getRenderItem() {
			if (icon == null)
				icon = iconFactory.get();
			return icon;
		}

		public ToolType next() {
			return list[(this.ordinal()+1)%list.length];
		}

		public boolean isDamageImportant() {
			return switch(this) {
				case POTION, OTHER -> true;
				default -> false;
			};
		}

		public boolean isValid() {
			return switch(this) {
				case TINKER -> false;
				default -> true;
			};
		}

		public boolean isNBTImportant() {
			return switch(this) {
				case TINKER, BOOK, OTHER -> true;
				default -> false;
			};
		}

		public boolean isItemValid(ItemStack is) {
			return getTypeForTool(is) == this;
		}

		private static ToolType getTypeForTool(ItemStack is) {
			var equippable = is.get(DataComponents.EQUIPPABLE);
			if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
				switch(equippable.slot()) {
					case HEAD:
						return HELMET;
					case CHEST:
						return CHESTPLATE;
					case LEGS:
						return LEGS;
					case FEET:
						return BOOTS;
					default:
						break;
				}
			}
			if (is.is(ItemTags.AXES))
				return AXE;
			if (is.getItem() instanceof BowItem)
				return BOW;
			if (is.is(ItemTags.PICKAXES))
				return PICK;
			if (is.getItem() instanceof ShearsItem)
				return SHEARS;
			if (is.is(ItemTags.SHOVELS))
				return SHOVEL;
			if (is.is(ItemTags.SWORDS))
				return SWORD;
			if (is.is(Items.ENCHANTED_BOOK))
				return BOOK;
			if (is.is(Items.IRON_HORSE_ARMOR) || is.is(Items.GOLDEN_HORSE_ARMOR) || is.is(Items.DIAMOND_HORSE_ARMOR))
				return HORSEARMOR;
			if (is.getItem() instanceof PotionItem)
				return POTION;
			// CHROMA-PORT: V33a also counted Botania brews as potions.
			return isValidMiscToolItem(is) ? OTHER : null;
		}

		/** CHROMA-PORT: V33a also refused AE storage cells and Forestry genetic items here. */
		private static boolean isValidMiscToolItem(ItemStack is) {
			return is.getMaxStackSize() == 1;
		}

		public static String getTypesAsString() {
			StringBuilder sb = new StringBuilder();
			for (ToolType type : list) {
				sb.append(type.displayName());
				if (type != ToolType.OTHER)
					sb.append(", ");
			}
			return sb.toString();
		}

		/** V33a {@code ReikaStringParser.capFirstChar}: first letter upper case, the rest lower. */
		public String displayName() {
			String n = this.name();
			return n.substring(0, 1)+n.substring(1).toLowerCase(Locale.ENGLISH);
		}
	}

}
