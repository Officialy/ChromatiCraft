package reika.chromaticraft.tileentity.auxiliary;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;

/** Server-authoritative 26.2 focus crystal, retaining every V33a acceleration tier. */
public final class TileEntityFocusCrystalPort extends TileEntityChromaticBase implements NBTTile {

	public enum CrystalTier {
		FLAWED(0.0625F, 0x30e040),
		DEFAULT(0.125F, 0xe06060),
		REFINED(0.375F, 0xe06060),
		EXQUISITE(0.875F, 0x22aaff),
		TURBOCHARGED(1.375F, 0x7010ff);

		public static final CrystalTier[] VALUES = values();
		private final float efficiencyFactor;
		private final int renderColor;

		CrystalTier(float efficiencyFactor, int renderColor) {
			this.efficiencyFactor = efficiencyFactor;
			this.renderColor = renderColor;
		}

		public float efficiencyFactor() { return efficiencyFactor; }
		public int renderColor() { return renderColor; }
		public boolean usesOrganizedModel() { return ordinal() >= REFINED.ordinal(); }
		public boolean isMaximumPower() { return ordinal() >= EXQUISITE.ordinal(); }
		public int effectiveOrdinal() { return this == TURBOCHARGED ? EXQUISITE.ordinal() : ordinal(); }
	}

	private CrystalTier tier = CrystalTier.FLAWED;
	private BlockPos connectedTarget;

	public TileEntityFocusCrystalPort(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.FOCUS_CRYSTAL.get(), pos, state);
	}

	@Override public ChromaTiles getTile() { return ChromaTiles.FOCUSCRYSTAL; }

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (connectedTarget != null && this.getTicksExisted() % 8 == 0
				&& world.getBlockEntity(connectedTarget) == null) {
			connectedTarget = null;
			this.setChanged();
		}
	}

	@Override protected void animateWithTick(Level world, BlockPos pos) {}

	public CrystalTier getTier() { return tier; }
	public void setTier(CrystalTier tier) {
		this.tier = tier != null ? tier : CrystalTier.FLAWED;
		this.setChanged();
		this.syncAllData(true);
	}

	public void connectTo(BlockPos target) {
		connectedTarget = target != null ? target.immutable() : null;
		this.setChanged();
	}

	public BlockPos getConnectedTarget() { return connectedTarget; }

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("tier", tier.ordinal());
		if (connectedTarget != null) output.store("connection", BlockPos.CODEC, connectedTarget);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		tier = CrystalTier.VALUES[Mth.clamp(input.getIntOr("tier", 0), 0, CrystalTier.VALUES.length - 1)];
		connectedTarget = input.read("connection", BlockPos.CODEC).orElse(null);
	}

	@Override protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putInt("tier", tier.ordinal());
		if (connectedTarget != null) tag.putLong("connection", connectedTarget.asLong());
	}

	@Override protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		tier = CrystalTier.VALUES[Mth.clamp(tag.getIntOr("tier", 0), 0, CrystalTier.VALUES.length - 1)];
		connectedTarget = tag.contains("connection") ? BlockPos.of(tag.getLongOr("connection", 0)) : null;
	}

	@Override public void getTagsToWriteToStack(CompoundTag tag) { tag.putInt("tier", tier.ordinal()); }
	@Override public void setDataFromItemStackTag(ItemStack stack) {
		CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA) != null ? stack.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
		tier = CrystalTier.VALUES[Mth.clamp(tag.getIntOr("tier", 0), 0, CrystalTier.VALUES.length - 1)];
	}
	@Override public void addTooltipInfo(List list, boolean shift) {
		list.add(Component.literal("Focus tier: " + tier.name()));
	}
}
