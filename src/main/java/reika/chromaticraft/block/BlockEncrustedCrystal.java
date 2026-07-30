package reika.chromaticraft.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import reika.chromaticraft.base.CrystalTypeBlock;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaItems;
import reika.chromaticraft.registry.CrystalElement;

/**
 * V33a encrusted crystal growth. One non-colliding host block stores up to six independently growing
 * crystal faces in a block entity; pylons and dimension structures manipulate the same state machine.
 */
public class BlockEncrustedCrystal extends Block implements EntityBlock {

    private static final Identifier STRUCTURE_SHIELD =
            Identifier.fromNamespaceAndPath("chromaticraft", "structure_shield");

    private final CrystalElement element;

    public BlockEncrustedCrystal(Properties properties, CrystalElement element) {
        super(properties);
        this.element = element;
    }

    public CrystalElement getColor() {
        return element;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileCrystalEncrusted(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof TileCrystalEncrusted tile) {
            for (Direction direction : Direction.values()) {
                if (CrystalGrowth.canExist(level, pos, direction))
                    tile.addGrowth(direction, 1 + level.getRandom().nextInt(8));
            }
            tile.markReady();
            tile.updateSides();
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof TileCrystalEncrusted tile) {
            tile.markReady();
            tile.grow();
            tile.updateSides();
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
            @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileCrystalEncrusted tile)
            tile.updateSides();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Entity breaker = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (breaker instanceof Player player && player.isCreative())
            return List.of();
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof TileCrystalEncrusted tile)
            return tile.createDrops();
        return List.of();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof TileCrystalEncrusted tile))
            return Shapes.block();
        Set<Direction> sides = tile.getSides();
        double minX = 0.5;
        double maxX = 0.5;
        double minY = 0.5;
        double maxY = 0.5;
        double minZ = 0.5;
        double maxZ = 0.5;
        if (sides.contains(Direction.UP) || sides.contains(Direction.DOWN)) {
            minX = 0;
            maxX = 1;
            minZ = 0;
            maxZ = 1;
        }
        if (sides.contains(Direction.EAST) || sides.contains(Direction.WEST)) {
            minY = 0;
            maxY = 1;
            minZ = 0;
            maxZ = 1;
        }
        if (sides.contains(Direction.NORTH) || sides.contains(Direction.SOUTH)) {
            minX = 0;
            maxX = 1;
            minY = 0;
            maxY = 1;
        }
        if (sides.contains(Direction.UP)) maxY = 1;
        if (sides.contains(Direction.DOWN)) minY = 0;
        if (sides.contains(Direction.EAST)) maxX = 1;
        if (sides.contains(Direction.WEST)) minX = 0;
        if (sides.contains(Direction.SOUTH)) maxZ = 1;
        if (sides.contains(Direction.NORTH)) minZ = 0;
        return minX == maxX || minY == maxY || minZ == maxZ
                ? Shapes.box(0.25, 0.25, 0.25, 0.75, 0.75, 0.75)
                : Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static void setColor(Level level, BlockPos pos, CrystalElement color) {
        BlockState state = level.getBlockState(pos);
        if (!ChromaBlocks.isEncrustedCrystal(state))
            return;
        TileCrystalEncrusted oldTile = level.getBlockEntity(pos) instanceof TileCrystalEncrusted tile ? tile : null;
        if (oldTile != null)
            oldTile.setColor(color);
        CrystalGrowth[] growths = oldTile != null ? oldTile.sides.clone() : null;
        boolean special = oldTile != null && oldTile.special;
        boolean ready = oldTile != null && oldTile.readyToUpdate;
        level.setBlock(pos, ChromaBlocks.encrustedCrystal(color).get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof TileCrystalEncrusted tile) {
            if (growths != null) {
                for (int i = 0; i < growths.length; i++)
                    tile.sides[i] = growths[i] != null ? growths[i].withColor(color) : null;
                tile.special = special;
                tile.readyToUpdate = ready;
            }
            tile.sync();
        }
    }

    public static boolean isEncrustedGrowable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(net.minecraft.world.level.block.Blocks.SNOW);
    }

    public static class TileCrystalEncrusted extends BlockEntity {

        private final CrystalGrowth[] sides = new CrystalGrowth[Direction.values().length];
        private boolean special;
        private boolean readyToUpdate;

        public TileCrystalEncrusted(BlockPos pos, BlockState state) {
            super(ChromaBlockEntities.ENCRUSTED.get(), pos, state);
        }

        public void setColor(CrystalElement color) {
            for (int i = 0; i < sides.length; i++) {
                if (sides[i] != null)
                    sides[i] = sides[i].withColor(color);
            }
            this.sync();
        }

        public void markReady() {
            readyToUpdate = true;
            this.setChanged();
        }

        public void makeSpecial() {
            special = true;
            this.sync();
        }

        public boolean isSpecial() {
            return special;
        }

        public boolean grow() {
            if (level == null)
                return false;
            RandomSource random = level.getRandom();
            if (random.nextInt(4) > 0) {
                for (CrystalGrowth growth : sides) {
                    if (growth != null && growth.grow()) {
                        this.sync();
                        return true;
                    }
                }
            }
            for (Direction direction : Direction.values()) {
                if (sides[direction.get3DDataValue()] == null && CrystalGrowth.canExist(level, worldPosition, direction)) {
                    this.addGrowth(direction);
                    return true;
                }
            }
            return false;
        }

        public void updateSides() {
            if (!readyToUpdate || level == null || level.isClientSide())
                return;
            for (Direction direction : Direction.values()) {
                int index = direction.get3DDataValue();
                CrystalGrowth growth = sides[index];
                if (growth != null && !CrystalGrowth.canExist(level, worldPosition, direction)) {
                    Block.popResource(level, worldPosition, growth.getDrop(level.getRandom()));
                    sides[index] = null;
                }
            }
            if (this.getGrowths().isEmpty())
                level.removeBlock(worldPosition, false);
            else
                this.sync();
        }

        public void addGrowth(Direction direction) {
            this.addGrowth(direction, 0);
        }

        public void addGrowth(Direction direction, int amount) {
            sides[direction.get3DDataValue()] = new CrystalGrowth(this.getColor(), direction, special, amount);
            this.sync();
        }

        public CrystalElement getColor() {
            return getBlockState().getBlock() instanceof BlockEncrustedCrystal crystal
                    ? crystal.getColor() : CrystalElement.WHITE;
        }

        public Set<Direction> getSides() {
            EnumSet<Direction> result = EnumSet.noneOf(Direction.class);
            for (Direction direction : Direction.values()) {
                if (sides[direction.get3DDataValue()] != null)
                    result.add(direction);
            }
            return result;
        }

        public Collection<CrystalGrowth> getGrowths() {
            List<CrystalGrowth> result = new ArrayList<>();
            for (CrystalGrowth growth : sides) {
                if (growth != null)
                    result.add(growth);
            }
            return List.copyOf(result);
        }

        public List<ItemStack> createDrops() {
            if (level == null)
                return List.of();
            List<ItemStack> drops = new ArrayList<>();
            for (CrystalGrowth growth : sides) {
                if (growth != null)
                    drops.add(growth.getDrop(level.getRandom()));
            }
            return drops;
        }

        private void sync() {
            this.setChanged();
            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            for (int i = 0; i < sides.length; i++) {
                if (sides[i] != null)
                    sides[i].save(output.child("side_" + i));
            }
            output.putBoolean("special", special);
            output.putBoolean("ready", readyToUpdate);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            for (int i = 0; i < sides.length; i++) {
                sides[i] = input.child("side_" + i).map(CrystalGrowth::load).orElse(null);
            }
            special = input.getBooleanOr("special", false);
            readyToUpdate = input.getBooleanOr("ready", false);
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
            return this.saveCustomOnly(provider);
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }

    public static final class CrystalGrowth {

        public final Direction side;
        public final CrystalElement color;
        public final boolean special;
        private int growthStage;

        private CrystalGrowth(CrystalElement color, Direction side, boolean special, int growthStage) {
            this.side = side;
            this.color = color;
            this.special = special;
            this.growthStage = Math.min(Math.max(0, growthStage), this.getMaxGrowth());
        }

        public CrystalGrowth withColor(CrystalElement newColor) {
            return new CrystalGrowth(newColor, side, special, growthStage);
        }

        public boolean grow() {
            if (growthStage < this.getMaxGrowth()) {
                growthStage++;
                return true;
            }
            return false;
        }

        private int getMaxGrowth() {
            return 6;
        }

        public ItemStack getDrop(RandomSource random) {
            int amount = Math.max(1 + growthStage, random.nextInt(growthStage * 2 + 1));
            ItemStack stack = ChromaItems.shardStack(color);
            stack.setCount(amount);
            return stack;
        }

        public int getGrowth() {
            return growthStage;
        }

        private void save(ValueOutput output) {
            output.putInt("grow", growthStage);
            output.putBoolean("special", special);
            output.putInt("dir", side.get3DDataValue());
            output.putInt("color", color.ordinal());
        }

        private static CrystalGrowth load(ValueInput input) {
            Direction direction = Direction.from3DDataValue(input.getIntOr("dir", Direction.DOWN.get3DDataValue()));
            int colorIndex = Math.max(0, Math.min(CrystalElement.elements.length - 1, input.getIntOr("color", 0)));
            return new CrystalGrowth(CrystalElement.elements[colorIndex], direction,
                    input.getBooleanOr("special", false), input.getIntOr("grow", 0));
        }

        public static boolean canExist(BlockGetter level, BlockPos origin, Direction side) {
            BlockPos supportPos = origin.relative(side);
            BlockState support = level.getBlockState(supportPos);
            Identifier id = BuiltInRegistries.BLOCK.getKey(support.getBlock());
            return STRUCTURE_SHIELD.equals(id) || support.isFaceSturdy(level, supportPos, side.getOpposite());
        }
    }
}
