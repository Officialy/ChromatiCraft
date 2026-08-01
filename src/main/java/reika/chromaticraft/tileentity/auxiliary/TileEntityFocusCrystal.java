package reika.chromaticraft.tileentity.auxiliary;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.auxiliary.interfaces.FocusAcceleratable;
import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.interfaces.blockentity.BreakAction;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/** Complete 26.2 port of V33a's focus crystal and its accelerator connection contract. */
public final class TileEntityFocusCrystal extends TileEntityChromaticBase implements NBTTile, BreakAction {

    public enum CrystalTier {
        FLAWED(0.0625F),
        DEFAULT(0.125F),
        REFINED(0.375F),
        EXQUISITE(0.875F),
        TURBOCHARGED(1.375F);

        public static final CrystalTier[] VALUES = values();
        private final float efficiencyFactor;

        CrystalTier(float efficiencyFactor) {
            this.efficiencyFactor = efficiencyFactor;
        }

        public float efficiencyFactor() { return efficiencyFactor; }
        public boolean usesOrganizedModel() { return ordinal() >= REFINED.ordinal(); }
        public boolean isMaximumPower() { return ordinal() >= EXQUISITE.ordinal(); }
        public int effectiveOrdinal() { return this == TURBOCHARGED ? EXQUISITE.ordinal() : ordinal(); }

        public String textureSuffix() {
            return switch (this) {
                case FLAWED -> "_cracked";
                case EXQUISITE, TURBOCHARGED -> "_sparkle";
                default -> "";
            };
        }

        public String displayPrefix() {
            return switch (this) {
                case DEFAULT -> "";
                case TURBOCHARGED -> EXQUISITE.displayPrefix();
                default -> name().charAt(0) + name().substring(1).toLowerCase();
            };
        }

        public int renderColor(float tick) {
            return switch (this) {
                case FLAWED -> 0x30e040;
                case DEFAULT, REFINED -> 0xe06060;
                case EXQUISITE -> 0x22aaff;
                case TURBOCHARGED -> turboColor(tick);
            };
        }

        private static int turboColor(float tick) {
            int blue = 0x22aaff;
            int purple = 0x7010ff;
            int[] cycle = {blue, blue, blue, purple, purple, blue, blue, blue};
            double position = Math.floorMod((long)Math.floor(tick), 70L * cycle.length) / 70D;
            int index = (int)position;
            float fraction = (float)(position - index);
            return ReikaColorAPI.mixColors(cycle[(index + 1) % cycle.length], cycle[index], fraction);
        }

        public ItemStack craftedItem() {
            ItemStack stack = new ItemStack(ChromaTiles.FOCUSCRYSTAL.getBlock());
            CompoundTag tag = new CompoundTag();
            tag.putInt("tier", ordinal());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return stack;
        }
    }

    /** Source-relative offset plus the target identity used by renderer and invalidation logic. */
    public record FocusConnection(BlockPos relativeLocation, BlockPos target, Class<?> tileClass) {}

    private CrystalTier tier = CrystalTier.FLAWED;
    private FocusConnection connection;

    public TileEntityFocusCrystal(BlockPos pos, BlockState state) {
        super(ChromaBlockEntities.FOCUS_CRYSTAL.get(), pos, state);
    }

    @Override public ChromaTiles getTile() { return ChromaTiles.FOCUSCRYSTAL; }

    @Override
    public void updateEntity(Level world, BlockPos pos) {
        if (world.isClientSide() && tier.isMaximumPower())
            ChromaParticle.spawnFocusCrystal(world, pos, tier.renderColor(this.getTicksExisted()), rand);
        if (connection != null && this.getTicksExisted() % 8 == 0) {
            BlockEntity target = world.getBlockEntity(connection.target());
            if (!(target instanceof FocusAcceleratable)) {
                connection = null;
                this.setChanged();
                if (!world.isClientSide()) this.syncAllData(true);
            }
        }
    }

    @Override protected void animateWithTick(Level world, BlockPos pos) {}

    public CrystalTier getTier() { return tier; }
    public FocusConnection getConnection() { return connection; }
    public BlockPos getConnectedTarget() { return connection != null ? connection.target() : null; }

    public void setTier(CrystalTier tier) {
        this.tier = tier != null ? tier : CrystalTier.FLAWED;
        this.setChanged();
        this.syncAllData(true);
    }

    public void connectTo(BlockPos target) {
        if (target == null) {
            connection = null;
        }
        else {
            BlockEntity tile = this.getLevel() != null ? this.getLevel().getBlockEntity(target) : null;
            connection = new FocusConnection(this.getBlockPos().subtract(target), target.immutable(),
                    tile != null ? tile.getClass() : BlockEntity.class);
        }
        this.setChanged();
    }

    public void addConnection(FocusAcceleratable source, boolean sync) {
        if (!(source instanceof BlockEntity target))
            throw new IllegalArgumentException("FocusAcceleratable must also be a BlockEntity");
        connection = new FocusConnection(this.getBlockPos().subtract(target.getBlockPos()),
                target.getBlockPos().immutable(), target.getClass());
        this.setChanged();
        if (sync) this.syncAllData(true);
    }

    /** V33a helper used by all acceleratable tiles: base factor one plus every installed focus. */
    public static float getSummedFocusFactorDirect(FocusAcceleratable target,
            Collection<BlockPos> relativeLocations) {
        if (!(target instanceof BlockEntity tile) || tile.getLevel() == null) return 1;
        return getSummedFocusFactor(target, tile.getLevel(), tile.getBlockPos(), relativeLocations);
    }

    public static float getSummedFocusFactor(FocusAcceleratable target, Level level, BlockPos origin,
            Collection<BlockPos> relativeLocations) {
        float sum = 1;
        for (BlockPos offset : relativeLocations) {
            if (level.getBlockEntity(origin.offset(offset)) instanceof TileEntityFocusCrystal focus) {
                sum += focus.tier.efficiencyFactor();
                if (!level.isClientSide()) focus.addConnection(target, true);
            }
        }
        return sum;
    }

    @Override
    public void breakBlock() {
        if (this.getLevel() != null && connection != null
                && this.getLevel().getBlockEntity(connection.target()) instanceof FocusAcceleratable target)
            target.recountFocusCrystals();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("tier", tier.ordinal());
        if (connection != null) output.store("connection", BlockPos.CODEC, connection.target());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tier = CrystalTier.VALUES[Mth.clamp(input.getIntOr("tier", 0), 0, CrystalTier.VALUES.length - 1)];
        BlockPos target = input.read("connection", BlockPos.CODEC).orElse(null);
        connection = target != null ? new FocusConnection(this.getBlockPos().subtract(target), target,
                BlockEntity.class) : null;
    }

    @Override protected void writeSyncTag(CompoundTag tag) {
        super.writeSyncTag(tag);
        tag.putInt("tier", tier.ordinal());
        if (connection != null) tag.putLong("connection", connection.target().asLong());
    }

    @Override protected void readSyncTag(CompoundTag tag) {
        super.readSyncTag(tag);
        tier = CrystalTier.VALUES[Mth.clamp(tag.getIntOr("tier", 0), 0, CrystalTier.VALUES.length - 1)];
        BlockPos target = tag.contains("connection") ? BlockPos.of(tag.getLongOr("connection", 0)) : null;
        connection = target != null ? new FocusConnection(this.getBlockPos().subtract(target), target,
                BlockEntity.class) : null;
    }

    @Override public void getTagsToWriteToStack(CompoundTag tag) { tag.putInt("tier", tier.ordinal()); }

    @Override public void setDataFromItemStackTag(ItemStack stack) {
        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA) != null
                ? stack.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
        tier = CrystalTier.VALUES[Mth.clamp(tag.getIntOr("tier", 0), 0, CrystalTier.VALUES.length - 1)];
    }

    @Override public void addTooltipInfo(List list, boolean shift) {
        String prefix = tier.displayPrefix();
        list.add(Component.literal(prefix.isEmpty() ? "Focus Crystal" : prefix + " Focus Crystal"));
    }
}