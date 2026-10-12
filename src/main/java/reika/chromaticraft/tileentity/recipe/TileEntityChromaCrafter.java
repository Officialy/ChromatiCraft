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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import reika.chromaticraft.auxiliary.interfaces.FocusAcceleratable;
import reika.chromaticraft.auxiliary.interfaces.OperationInterval;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.auxiliary.recipemanagers.PoolAlloyingRecipe;
import reika.chromaticraft.base.tileentity.InventoriedRelayPowered;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.registry.*;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.dragonapi.instantiable.data.maps.ItemHashMap;
import reika.dragonapi.interfaces.blockentity.BreakAction;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

/** V33a automated pool alloying: two glass shields, 3,000 mB, relay energy and retained surplus inputs. */
public final class TileEntityChromaCrafter extends InventoriedRelayPowered
        implements OwnedTile, OperationInterval, BreakAction, FocusAcceleratable {
    public static final int CRAFTING_DURATION = 300;
    public static final int CAPACITY = 3000;
    private static final ElementTagCompound REQUIRED = new ElementTagCompound();
    static {
        REQUIRED.setTag(CrystalElement.BLACK, 1000);
        REQUIRED.setTag(CrystalElement.GRAY, 500);
        REQUIRED.setTag(CrystalElement.PURPLE, 2500);
        REQUIRED.setTag(CrystalElement.CYAN, 200);
        REQUIRED.setTag(CrystalElement.BROWN, 200);
    }

    private final ArrayList<ItemStack> recipeItems = new ArrayList<>();
    private List<ItemStack> renderItems = List.of();
    private Identifier recipeId;
    private PoolAlloyingRecipe recipe;
    private int recipeTick;
    private int hasEtherBerries;
    private int chromaLevel;
    private FluidResource chromaFluid = FluidResource.EMPTY;
    private final ChromaFluidJournal fluidJournal = new ChromaFluidJournal();
    private final ChromaFluidHandler[] fluidViews = new ChromaFluidHandler[Direction.values().length + 1];

    public TileEntityChromaCrafter(BlockPos pos, BlockState state) {
        super(ChromaBlockEntities.CHROMA_CRAFTER.get(), pos, state);
        for (Direction side : Direction.values()) fluidViews[side.ordinal()] = new ChromaFluidHandler(side);
        fluidViews[Direction.values().length] = new ChromaFluidHandler(null);
    }

    @Override public ChromaTiles getTile() { return ChromaTiles.CHROMACRAFTER; }
    @Override public int getSizeInventory() { return 2; }
    @Override public int getMaxStackSize() { return 64; }
    @Override public boolean onlyAllowOwnersToUse() { return true; }
    public int getChromaLevel() { return chromaLevel; }
    public int getBerryCount() { return hasEtherBerries; }
    public boolean hasEtherBerries() { return hasEtherBerries > 0; }
    public PoolAlloyingRecipe getActiveRecipe() { return recipe; }
    public Identifier getActiveRecipeId() { return recipeId; }
    public int getRecipeTicks() { return recipeTick; }
    public List<ItemStack> getStoredRecipeItems() { return recipeItems.stream().map(ItemStack::copy).toList(); }
    public List<ItemStack> getRenderItems() {
        if (recipeId == null) return List.of();
        ArrayList<ItemStack> result = new ArrayList<>(renderItems.stream().map(ItemStack::copy).toList());
        if (hasEtherBerries()) result.add(ChromaItems.craftingStack(ChromaCraftingItems.ETHER_BERRIES));
        return result;
    }

    public ItemHashMap<Integer> getCurrentItems() {
        ItemHashMap<Integer> result = new ItemHashMap<>();
        for (ItemStack stack : recipeItems) ItemHashMap.add(result, stack, stack.getCount());
        return result;
    }

    @Override public void updateEntity(Level world, BlockPos pos) {
        super.updateEntity(world, pos);
        if (world.isClientSide()) {
            // Source client prediction keeps the Manipulator progress display moving between server snapshots.
            if (recipeId != null && recipeTick > 0 && canWork()) recipeTick--;
            return;
        }
        updateChamberState();
        refreshRecipe();
        ItemStack input = inv.get(0);
        if (!input.isEmpty()) {
            if (input.is(ChromaItems.CRAFTING.get(ChromaCraftingItems.ETHER_BERRIES).get())) {
                int taken = Math.min(input.getCount(), Math.max(0, 512 - hasEtherBerries));
                if (taken > 0) { hasEtherBerries += taken; input.shrink(taken); contentsChanged(); }
            } else if (recipe == null) {
                int taken = Math.min(input.getCount(), Math.max(0, 64 - getItemCount(input)));
                if (taken > 0) {
                    recipeItems.add(input.copyWithCount(taken));
                    var collated = ReikaItemHelper.collateItemList(recipeItems);
                    recipeItems.clear(); recipeItems.addAll(collated);
                    input.shrink(taken);
                    selectRecipe();
                    contentsChanged();
                }
            }
            if (input.isEmpty()) inv.set(0, ItemStack.EMPTY);
        }
        if (recipe != null && recipeTick > 0 && canWork()) {
            recipeTick--;
            setChanged();
            if (recipeTick == 0) craft();
        } else if (recipe != null && recipeTick == 0 && canWork()) craft();
    }

    @Override protected void animateWithTick(Level world, BlockPos pos) {
        if (!world.isClientSide()) return;
        if (hasStructure()) reika.chromaticraft.render.particle.ChromaParticle.spawnCrafterFocusHints(world, pos, getRelativeFocusCrystalLocations());
        if (recipeTick > 0) reika.chromaticraft.render.particle.ChromaParticle.spawnChromaCrafter(world, pos, rand);
    }

    private void updateChamberState() {
        boolean formed = hasStructure();
        BlockState state = getBlockState();
        if (state.getValue(reika.chromaticraft.block.BlockChromaCrafter.FORMED) != formed)
            level.setBlock(worldPosition, state.setValue(reika.chromaticraft.block.BlockChromaCrafter.FORMED, formed), 2);
        for (int height = 1; height <= 2; height++) {
            BlockPos pos = worldPosition.above(height);
            BlockState glass = level.getBlockState(pos);
            if (glass.is(ChromaBlocks.shielding(ChromaShieldTypes.GLASS).get())) {
                var property = reika.chromaticraft.block.worldgen26.BlockStructureShield.CRAFTER_SECTION;
                int section = formed ? height : 0;
                if (glass.getValue(property) != section) level.setBlock(pos, glass.setValue(property, section), 2);
            }
        }
    }

    private void refreshRecipe() {
        if (!(level instanceof ServerLevel server) || recipeId == null) return;
        var holder = server.getServer().getRecipeManager().recipeMap().byType(ChromaRecipeTypes.POOL_ALLOYING.get())
                .stream().filter(h -> h.id().identifier().equals(recipeId)).findFirst().orElse(null);
        if (holder == null || consumptionPlan(holder.value()) == null) {
            recipe = null; recipeId = null; recipeTick = 0; contentsChanged();
        } else recipe = holder.value();
    }

    private void selectRecipe() {
        if (!(level instanceof ServerLevel server)) return;
        for (RecipeHolder<PoolAlloyingRecipe> holder : server.getServer().getRecipeManager().recipeMap().byType(ChromaRecipeTypes.POOL_ALLOYING.get())) {
            if (consumptionPlan(holder.value()) != null) {
                recipe = holder.value(); recipeId = holder.id().identifier(); recipeTick = getCraftingDuration();
                ArrayList<ItemStack> display = new ArrayList<>();
                display.add(recipeItems.stream().filter(recipe.catalyst()::test).findFirst().orElseThrow().copyWithCount(1));
                for (var ingredient : recipe.ingredients()) display.add(recipeItems.stream().filter(ingredient.ingredient()::test).findFirst().orElseThrow().copyWithCount(1));
                renderItems = List.copyOf(display);
                return;
            }
        }
    }

    /** Reserve catalyst and counted inputs together; one physical item cannot satisfy both. */
    private int[] consumptionPlan(PoolAlloyingRecipe candidate) {
        for (int catalyst = 0; catalyst < recipeItems.size(); catalyst++) {
            if (!candidate.catalyst().test(recipeItems.get(catalyst))) continue;
            int[] remaining = recipeItems.stream().mapToInt(ItemStack::getCount).toArray();
            remaining[catalyst]--;
            boolean valid = true;
            for (var ingredient : candidate.ingredients()) {
                int needed = ingredient.count();
                for (int i = 0; i < remaining.length && needed > 0; i++) {
                    if (ingredient.ingredient().test(recipeItems.get(i))) {
                        int taken = Math.min(remaining[i], needed); remaining[i] -= taken; needed -= taken;
                    }
                }
                if (needed > 0) { valid = false; break; }
            }
            if (valid) {
                int[] consumed = new int[remaining.length];
                for (int i = 0; i < remaining.length; i++) consumed[i] = recipeItems.get(i).getCount() - remaining[i];
                return consumed;
            }
        }
        return null;
    }

    public boolean hasStructure() {
        return level != null && level.getBlockState(worldPosition.above()).is(ChromaBlocks.shielding(ChromaShieldTypes.GLASS).get())
                && level.getBlockState(worldPosition.above(2)).is(ChromaBlocks.shielding(ChromaShieldTypes.GLASS).get());
    }
    public int getCraftingDuration() {
        float factor = getAccelerationFactor();
        return factor >= 1 ? Math.max(1, (int)(CRAFTING_DURATION / factor)) : CRAFTING_DURATION;
    }
    private boolean canWork() { return hasStructure() && chromaLevel == CAPACITY && energy.containsAtLeast(REQUIRED); }

    private void craft() {
        int[] consumed = consumptionPlan(recipe);
        if (consumed == null) { recipe = null; recipeId = null; recipeTick = 0; contentsChanged(); return; }
        ItemStack result = recipe.result();
        boolean berries = recipe.allowDoubling() && hasEtherBerries >= reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma.ETHER_SATURATION;
        if (berries) result.setCount(result.getCount() * 2);
        var owner = getPlacer();
        if (owner != null && !(owner instanceof FakePlayer) && ChromaAbilityData.hasDoubleCraft(owner)) result.setCount(result.getCount() * 2);
        ItemStack output = inv.get(1);
        int limit = Math.min(getMaxStackSize(), result.getMaxStackSize());
        if ((!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, result))
                || result.getCount() + output.getCount() > limit) return;
        for (int i = recipeItems.size() - 1; i >= 0; i--) {
            recipeItems.get(i).shrink(consumed[i]);
            if (recipeItems.get(i).isEmpty()) recipeItems.remove(i);
        }
        if (output.isEmpty()) inv.set(1, result); else output.grow(result.getCount());
        if (berries) hasEtherBerries -= reika.chromaticraft.block.BlockChromaFluid.TileEntityChroma.ETHER_SATURATION;
        chromaLevel -= CAPACITY;
        chromaFluid = FluidResource.EMPTY;
        energy.subtract(REQUIRED);
        recipe = null; recipeId = null; recipeTick = 0;
        ChromaSounds.INFUSE.playSoundAtBlock(this);
        contentsChanged();
    }

    private int getItemCount(ItemStack template) {
        return recipeItems.stream().filter(stack -> ReikaItemHelper.matchStacks(stack, template)).mapToInt(ItemStack::getCount).sum();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && !stack.isEmpty() && hasStructure()
                && (stack.is(ChromaItems.CRAFTING.get(ChromaCraftingItems.ETHER_BERRIES).get()) ? hasEtherBerries + stack.getCount() <= 512 : getItemCount(stack) + stack.getCount() <= 64);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }
    public boolean canConnectFluidPipe(Direction side) { return side != null && side.getAxis().isHorizontal(); }
    public boolean canConnectItemPipe(Direction side) { return side != null && side != Direction.UP; }
    // CHROMA-PORT: a future native BuildCraft adapter must consume these original FLUID/ITEM connection policies.
    public ResourceHandler<FluidResource> fluidHandler(Direction side) { return fluidViews[side != null ? side.ordinal() : Direction.values().length]; }
    @Override protected boolean canReceiveFrom(CrystalElement e, Direction side) { return side != Direction.UP || chromaLevel <= 1000; }
    @Override public int getMaxStorage(CrystalElement e) { return 10000; }
    @Override public ElementTagCompound getRequiredEnergy() { return REQUIRED.copy(); }
    @Override public boolean isAcceptingColor(CrystalElement e) { return REQUIRED.contains(e); }
    @Override public float getOperationFraction() { return recipeId == null ? 0 : 1 - (float)recipeTick / getCraftingDuration(); }
    @Override public OperationState getState() { return recipeId == null ? OperationState.INVALID : energy.containsAtLeast(REQUIRED) ? OperationState.RUNNING : OperationState.PENDING; }
    public boolean hasWork() { return getState() == OperationState.RUNNING; }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).expandTowards(0, 2, 0); }
    @Override public float getAccelerationFactor() { return TileEntityFocusCrystal.getSummedFocusFactorDirect(this, getRelativeFocusCrystalLocations()) / 2; }
    @Override public float getMaximumAcceleratability() { return 4 * TileEntityFocusCrystal.CrystalTier.TURBOCHARGED.efficiencyFactor(); }
    @Override public float getProgressToNextStep() { return 0; }
    @Override public void recountFocusCrystals() { getAccelerationFactor(); }
    @Override public Collection<BlockPos> getRelativeFocusCrystalLocations() {
        return List.of(new BlockPos(-2, 0, 0), new BlockPos(2, 0, 0), new BlockPos(0, 0, -2), new BlockPos(0, 0, 2));
    }
    @Override public void breakBlock() {
        if (level == null || level.isClientSide()) return;
        for (ItemStack stack : recipeItems) ReikaItemHelper.dropItem(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, stack.copy());
        for (int count = hasEtherBerries; count > 0;) {
            int amount = Math.min(64, count); count -= amount;
            ReikaItemHelper.dropItem(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, new ItemStack(ChromaItems.CRAFTING.get(ChromaCraftingItems.ETHER_BERRIES).get(), amount));
        }
        recipeItems.clear(); hasEtherBerries = 0;
    }
    private void contentsChanged() { setChanged(); syncAllData(true); }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("recipeItems", ItemStack.CODEC.listOf(), recipeItems);
        output.store("renderItems", ItemStack.CODEC.listOf(), renderItems);
        output.putInt("etherb", hasEtherBerries); output.putInt("recipe", recipeTick); output.putInt("chromaLevel", chromaLevel);
        output.store("chromaFluid", FluidResource.OPTIONAL_CODEC, chromaFluid);
        if (recipeId != null) output.store("recipetype", Identifier.CODEC, recipeId);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        recipeItems.clear(); recipeItems.addAll(input.read("recipeItems", ItemStack.CODEC.listOf()).orElse(List.of()));
        renderItems = input.read("renderItems", ItemStack.CODEC.listOf()).orElse(List.of());
        hasEtherBerries = Math.max(0, input.getIntOr("etherb", 0)); recipeTick = Math.max(0, input.getIntOr("recipe", 0));
        chromaLevel = Math.clamp(input.getIntOr("chromaLevel", 0), 0, CAPACITY);
        chromaFluid = chromaLevel == 0 ? FluidResource.EMPTY : input.read("chromaFluid", FluidResource.OPTIONAL_CODEC).orElse(FluidResource.of(ChromaFluids.CHROMA.get()));
        if (chromaLevel > 0 && (chromaFluid.isEmpty() || chromaFluid.getFluid() != ChromaFluids.CHROMA.get())) { chromaLevel = 0; chromaFluid = FluidResource.EMPTY; }
        recipeId = input.read("recipetype", Identifier.CODEC).orElse(null); recipe = null;
    }
    @Override protected void writeSyncTag(CompoundTag tag) {
        super.writeSyncTag(tag); tag.putInt("etherb", hasEtherBerries); tag.putInt("recipe", recipeTick); tag.putInt("chromaLevel", chromaLevel);
        if (recipeId != null) tag.putString("recipetype", recipeId.toString());
    }
    @Override protected void readSyncTag(CompoundTag tag) {
        super.readSyncTag(tag); hasEtherBerries = tag.getIntOr("etherb", 0); recipeTick = tag.getIntOr("recipe", 0); chromaLevel = tag.getIntOr("chromaLevel", 0);
        recipeId = Identifier.tryParse(tag.getStringOr("recipetype", ""));
    }

    private record FluidState(int amount, FluidResource resource) {}
    /** Every sided view journals the same tank, including its component identity. */
    private final class ChromaFluidJournal extends SnapshotJournal<FluidState> {
        @Override protected FluidState createSnapshot() { return new FluidState(chromaLevel, chromaFluid); }
        @Override protected void revertToSnapshot(FluidState state) { chromaLevel = state.amount(); chromaFluid = state.resource(); }
        @Override protected void onRootCommit(FluidState previous) { contentsChanged(); }
    }

    private final class ChromaFluidHandler implements ResourceHandler<FluidResource> {
        private final Direction side;
        ChromaFluidHandler(Direction side) { this.side = side; }
        @Override public int size() { return 1; }
        @Override public FluidResource getResource(int index) { return index == 0 && chromaLevel > 0 ? chromaFluid : FluidResource.EMPTY; }
        @Override public long getAmountAsLong(int index) { return index == 0 ? chromaLevel : 0; }
        @Override public long getCapacityAsLong(int index, FluidResource resource) { return index == 0 && (resource.isEmpty() || isValid(index, resource)) ? CAPACITY : 0; }
        @Override public boolean isValid(int index, FluidResource resource) { return index == 0 && !resource.isEmpty() && resource.getFluid() == ChromaFluids.CHROMA.get(); }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (!isValid(index, resource) || (side != null && !canConnectFluidPipe(side)) || !hasStructure() || (chromaLevel > 0 && !chromaFluid.equals(resource))) return 0;
            int accepted = Math.min(25, Math.min(amount, CAPACITY - chromaLevel));
            if (accepted > 0) { fluidJournal.updateSnapshots(transaction); chromaLevel += accepted; chromaFluid = resource; }
            return accepted;
        }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount); return 0; // Original input-only tank.
        }
    }
}
