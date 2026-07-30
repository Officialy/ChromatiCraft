package reika.chromaticraft.tileentity.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipeInput;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.AuraRequirement;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.GridIngredient;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.StandIngredient;
import reika.chromaticraft.base.tileentity.InventoriedCrystalReceiver;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.castingtuning.CastingTuningRegistry;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaRecipeTypes;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.container.MenuCastingTable;

import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystalPort;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
/** Complete server-authoritative controller for all four V33a casting tiers. */
public final class TileEntityCastingTable extends InventoriedCrystalReceiver implements OwnedTile, NBTTile, MenuProvider {

    public enum TableTier {
        CRAFTING(0), TEMPLE(250), MULTIBLOCK(2000), PYLON(15000);
        private final int minimumXP;
        TableTier(int xp) { minimumXP = xp; }
        public int minimumXP() { return minimumXP; }
        public static TableTier forXP(int xp) {
            TableTier result = CRAFTING;
            for (TableTier tier : values()) if (xp >= tier.minimumXP) result = tier;
            return result;
        }
    }
    private static final List<BlockPos> CASTING_FOCUS_LOCATIONS = List.of(
            new BlockPos(-1,1,-3), new BlockPos(1,1,-3), new BlockPos(3,1,-1), new BlockPos(3,1,1),
            new BlockPos(1,1,3), new BlockPos(-1,1,3), new BlockPos(-3,1,1), new BlockPos(-3,1,-1));


    private static final int[][] CASTING_REPEATER_RING = {
            {-6,-8},{-2,-8},{2,-8},{6,-8},{-6,8},{-2,8},{2,8},{6,8},
            {-8,-6},{-8,-2},{-8,2},{-8,6},{8,-6},{8,-2},{8,2},{8,6}
    };

    private RecipeHolder<CastingTableRecipe> activeRecipe;
    private boolean isTuned;
    private ResourceKey<Recipe<?>> activeRecipeKey;
    private UUID craftingPlayer;
    private int craftingTick;
    private int craftingAmount;
    private int tableXP;
    private boolean hasTemple;
    private float throughputBonus;
    private boolean hasMultiblock;
    private boolean hasPylonStructure;
    private boolean mutatingInventory;
    private boolean recipeDirty = true;
    private final Set<ResourceKey<Recipe<?>>> completedRecipes = new HashSet<>();
    private ItemStack clientRecipeOutput = ItemStack.EMPTY;
    private final ElementTagCompound clientRecipeAura = new ElementTagCompound();
    private final Map<String, Integer> craftedItems = new HashMap<>();

    public TileEntityCastingTable(BlockPos pos, BlockState state) {
        super(ChromaBlockEntities.CASTING_TABLE.get(), pos, state);
    }

    @Override public ChromaTiles getTile() { return ChromaTiles.TABLE; }
    @Override public int getSizeInventory() { return 10; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < 9 && !this.isCrafting(); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 9; }
    @Override public boolean onlyAllowOwnersToMine() { return true; }
    @Override public boolean onlyAllowOwnersToUse() { return true; }
    @Override public boolean isOwnedByPlayer(Player player) { return placerUUID == null || placerUUID.equals(player.getUUID()); }
    @Override public Component getDisplayName() { return Component.translatable("block.chromaticraft.casting_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MenuCastingTable(id, inventory, this);
    }

    @Override
    public void updateEntity(Level world, BlockPos pos) {
        super.updateEntity(world, pos);
        if (world.isClientSide()) return;
        if (activeRecipe == null && activeRecipeKey != null && craftingTick > 0) this.restoreActiveRecipe();
        if (this.getTicksExisted() == 1 || this.getTicksExisted() % 40 == 0) this.validateStructure();
        if (recipeDirty && !this.isCrafting()) this.refreshActiveRecipe();
        if (!this.isCrafting()) return;
        if (!this.craftStateStillValid()) { this.cancelCraft(); return; }
        if (!this.hasRequiredAura()) {
            if (this.getTicksExisted() % 20 == 0) this.requestEnergyDifference(this.requiredAura(craftingAmount), true);
            return;
        }
        if (--craftingTick <= 0) this.completeCraft();
    }

    @Override
    protected void onInventorySlotChanged(int slot) {
        if (mutatingInventory) return;
        recipeDirty = true;
        if (slot < 9 && this.isCrafting()) this.cancelCraft();
    }

    public boolean triggerCrafting(Player player) {
        if (this.getLevel() == null || this.getLevel().isClientSide() || this.isCrafting() || !this.isOwnedByPlayer(player)) return false;
        this.validateStructure();
        this.refreshActiveRecipe();
        if (activeRecipe == null) return false;
        if (!this.playerCanRun(activeRecipe.value(), player)) return false;
        int amount = this.calculateCraftableAmount(activeRecipe.value());
        if (amount <= 0 || !this.canAccept(activeRecipe.value().output(), amount)) return false;
        craftingAmount = amount;
        int duration = activeRecipe.value().duration() * amount;
        if (activeRecipe.value().tier().ordinal() >= CastingTableRecipe.Tier.MULTIBLOCK.ordinal() && duration > 20)
            duration = Math.max(20, (int)(duration / this.getAccelerationFactor()));
        craftingTick = Math.max(1, duration);
        craftingPlayer = player.getUUID();
        activeRecipeKey = activeRecipe.id();
        this.linkAndLockStands(true);
        if (!this.hasRequiredAura()) this.requestEnergyDifference(this.requiredAura(craftingAmount), true);
        this.setChanged();
        this.syncAllData(false);
        return true;
    }

    private void restoreActiveRecipe() {
        if (!(this.getLevel() instanceof ServerLevel server) || activeRecipeKey == null || craftingTick <= 0) return;
        RecipeHolder<?> holder = server.getServer().getRecipeManager().byKey(activeRecipeKey).orElse(null);
        if (holder != null && holder.value() instanceof CastingTableRecipe recipe) {
            @SuppressWarnings("unchecked") RecipeHolder<CastingTableRecipe> cast = (RecipeHolder<CastingTableRecipe>)(RecipeHolder<?>)holder;
            activeRecipe = cast;
        }
    }

    private void refreshActiveRecipe() {
        recipeDirty = false;
        activeRecipe = null;
        ResourceKey<Recipe<?>> previousRecipe = activeRecipeKey;
        activeRecipeKey = null;
        if (!(this.getLevel() instanceof ServerLevel server)) return;
        CastingRecipeInput input = this.snapshot();
        for (RecipeHolder<?> holder : server.getServer().getRecipeManager().getRecipes()) {
            if (holder.value().getType() != ChromaRecipeTypes.CASTING.get() || !(holder.value() instanceof CastingTableRecipe recipe)) continue;
            if (!this.canUseTier(recipe.tier()) || !recipe.matchesIgnoringAura(input)) continue;
            @SuppressWarnings("unchecked") RecipeHolder<CastingTableRecipe> cast = (RecipeHolder<CastingTableRecipe>)(RecipeHolder<?>)holder;
            if (activeRecipe == null || recipe.tier().ordinal() > activeRecipe.value().tier().ordinal()) activeRecipe = cast;
        }
        if (activeRecipe != null) activeRecipeKey = activeRecipe.id();
        if (!Objects.equals(previousRecipe, activeRecipeKey))
            this.syncAllData(false);
    }

    private boolean canUseTier(CastingTableRecipe.Tier tier) {
        if (tier.ordinal() > this.getTier().ordinal()) return false;
        return switch (tier) {
            case CRAFTING -> true;
            case TEMPLE -> hasTemple;
            case MULTIBLOCK -> hasMultiblock;
            case PYLON -> hasPylonStructure;
        };
    }

    private boolean playerCanRun(CastingTableRecipe recipe, Player player) {
        if (!ProgressStage.CRYSTALS.isPlayerAtStage(player)) return false;
        return switch (recipe.tier()) {
            case CRAFTING -> true;
            case TEMPLE -> ProgressStage.RUNEUSE.isPlayerAtStage(player);
            case MULTIBLOCK -> ProgressStage.RUNEUSE.isPlayerAtStage(player)
                    && ProgressStage.MULTIBLOCK.isPlayerAtStage(player);
            case PYLON -> ProgressStage.RUNEUSE.isPlayerAtStage(player)
                    && ProgressStage.MULTIBLOCK.isPlayerAtStage(player)
                    && ProgressStage.PYLON.isPlayerAtStage(player)
                    && ProgressStage.REPEATER.isPlayerAtStage(player);
        };
    }

    public void validateStructure() {
        if (this.getLevel() == null) return;
        BlockPos anchor = this.getBlockPos().below();
        hasTemple = ChromaStructures.CASTING1.getArray(this.getLevel(), anchor.getX(), anchor.getY(), anchor.getZ()).matchInWorld();
        hasMultiblock = ChromaStructures.CASTING2.getArray(this.getLevel(), anchor.getX(), anchor.getY(), anchor.getZ()).matchInWorld();
        hasPylonStructure = ChromaStructures.CASTING3.getArray(this.getLevel(), anchor.getX(), anchor.getY(), anchor.getZ()).matchInWorld();
        this.linkAndLockStands(this.isCrafting());
        recipeDirty = true;
        isTuned = placerUUID != null && CastingTuningRegistry.instance.getTuningKey(this.getLevel(), placerUUID).matches(this.getCurrentTuningMap());
        this.applyRepeaterGroupingBonus();
        if (isTuned && this.getLevel() instanceof ServerLevel server) {
            Player owner = server.getPlayerByUUID(placerUUID);
            if (owner != null) ProgressStage.TUNECAST.stepPlayerTo(owner);
        }

        this.setChanged();
    }

    private CastingRecipeInput snapshot() {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) grid.add(this.getItem(slot));
        Map<BlockPos, ItemStack> stands = new HashMap<>();
        for (Map.Entry<BlockPos, TileEntityItemStand> entry : this.getOtherStands().entrySet()) {
            ItemStack stack = entry.getValue().getItem(0);
            if (!stack.isEmpty()) stands.put(entry.getKey(), stack);
        }
        Map<BlockPos, CrystalElement> runes = new HashMap<>();
        BlockPos table = this.getBlockPos();
        for (int dx = -8; dx <= 8; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -8; dz <= 8; dz++) {
            BlockState state = this.getLevel().getBlockState(table.offset(dx, dy, dz));
            if (ChromaBlocks.isRune(state)) runes.put(new BlockPos(dx, dy, dz), BlockCrystalRune.getColor(state));
        }
        Map<CrystalElement, Integer> aura = new EnumMap<>(CrystalElement.class);
        for (CrystalElement element : CrystalElement.elements) aura.put(element, this.getEnergy(element));
        return new CastingRecipeInput(grid, stands, runes, aura);
    }

    public Map<BlockPos, TileEntityItemStand> getOtherStands() {
        Map<BlockPos, TileEntityItemStand> result = new HashMap<>();
        if (this.getLevel() == null) return result;
        for (int dx = -4; dx <= 4; dx += 2) for (int dz = -4; dz <= 4; dz += 2) {
            if (dx == 0 && dz == 0) continue;
            int dy = Math.abs(dx) == 4 || Math.abs(dz) == 4 ? 1 : 0;
            BlockPos offset = new BlockPos(dx, dy, dz);
            BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getBlockPos().offset(offset));
            if (blockEntity instanceof TileEntityItemStand stand) result.put(offset, stand);
        }
        return result;
    }

    private void linkAndLockStands(boolean locked) {
        for (TileEntityItemStand stand : this.getOtherStands().values()) {
            stand.setTable(this.getBlockPos());
            stand.lock(locked);
            stand.syncAfterCraft();
        }
    }

    /** Returns only the twelve V33a personal-key rune locations, relative to this table. */
    public Map<BlockPos, CrystalElement> getCurrentTuningMap() {
        Map<BlockPos, CrystalElement> result = new HashMap<>();
        if (this.getLevel() == null) return result;
        for (BlockPos offset : CastingTuningRegistry.instance.locations()) {
            BlockState state = this.getLevel().getBlockState(this.getBlockPos().offset(offset));
            if (ChromaBlocks.isRune(state)) result.put(offset, BlockCrystalRune.getColor(state));
        }
        return result;
    }

    public boolean hasTuningKey() {
        return this.getCurrentTuningMap().size() == CastingTuningRegistry.instance.locations().size();
    }

    public boolean isTuned() { return isTuned; }

    /** V33a focus acceleration is additive around the eight dedicated casting-table sockets. */
    public float getAccelerationFactor() {
        float factor = 1;
        if (this.getLevel() == null) return factor;
        for (BlockPos offset : CASTING_FOCUS_LOCATIONS) {
            BlockEntity tile = this.getLevel().getBlockEntity(this.getBlockPos().offset(offset));
            if (tile instanceof TileEntityFocusCrystalPort focus) {
                factor += focus.getTier().efficiencyFactor();
                if (!this.getLevel().isClientSide()) focus.connectTo(this.getBlockPos());
            }
        }
        return factor;
    }

    public float getMaximumAcceleratability() {
        return TileEntityFocusCrystalPort.CrystalTier.TURBOCHARGED.efficiencyFactor() * CASTING_FOCUS_LOCATIONS.size();
    }

    public List<BlockPos> getRelativeFocusCrystalLocations() { return CASTING_FOCUS_LOCATIONS; }

    public void recountFocusCrystals() { this.getAccelerationFactor(); }

    private int calculateCraftableAmount(CastingTableRecipe recipe) {
        int amount = Integer.MAX_VALUE;
        for (GridIngredient required : recipe.grid()) amount = Math.min(amount, this.getItem(required.slot()).getCount());
        Map<BlockPos, TileEntityItemStand> stands = this.getOtherStands();
        for (StandIngredient required : recipe.stands()) {
            TileEntityItemStand stand = stands.get(required.offset());
            if (stand == null) return 0;
            amount = Math.min(amount, stand.getItem(0).getCount());
        }
        ItemStack output = recipe.output();
        ItemStack current = this.getItem(9);
        int room = current.isEmpty() ? output.getMaxStackSize() : ItemStack.isSameItemSameComponents(current, output) ? current.getMaxStackSize() - current.getCount() : 0;
        amount = Math.min(amount, room / output.getCount());
        return amount == Integer.MAX_VALUE ? 0 : Math.max(0, amount);
    }

    /** Restores the V33a four-side, four-color-group pylon casting throughput bonus. */
    private void applyRepeaterGroupingBonus() {
        throughputBonus = 0;
        List<List<TileEntityCrystalRepeater>> sides = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Set<CrystalElement> colors = new HashSet<>();
        if (this.getLevel() == null) return;
        for (int[] offset : CASTING_REPEATER_RING) {
            BlockEntity tile = this.getLevel().getBlockEntity(this.getBlockPos().offset(offset[0], 3, offset[1]));
            if (!(tile instanceof TileEntityCrystalRepeater repeater)) continue;
            repeater.markAsTableGrouped(false);
            CrystalElement color = repeater.getActiveColor();
            if (!hasPylonStructure || color == null || !colors.add(color)) continue;
            int side = offset[1] == -8 ? 0 : offset[0] == 8 ? 1 : offset[1] == 8 ? 2 : 3;
            sides.get(side).add(repeater);
        }
        if (!hasPylonStructure || colors.size() != CrystalElement.elements.length) return;
        for (List<TileEntityCrystalRepeater> side : sides) {
            int group = crystalGroup(side.get(0).getActiveColor());
            if (side.stream().anyMatch(repeater -> crystalGroup(repeater.getActiveColor()) != group)) continue;
            throughputBonus += 0.25F;
            side.forEach(repeater -> repeater.markAsTableGrouped(true));
        }
    }

    private static int crystalGroup(CrystalElement element) {
        return switch (element) {
            case RED, BLUE, PURPLE, MAGENTA -> 0;
            case YELLOW, CYAN, LIME, GREEN -> 1;
            case BROWN, PINK, ORANGE, LIGHTBLUE -> 2;
            case BLACK, GRAY, LIGHTGRAY, WHITE -> 3;
        };
    }

    public float getThroughputBonus() { return throughputBonus; }

    private boolean canAccept(ItemStack output, int amount) {
        ItemStack current = this.getItem(9);
        int total = output.getCount() * amount;
        return current.isEmpty() ? total <= output.getMaxStackSize()
                : ItemStack.isSameItemSameComponents(current, output) && current.getCount() + total <= current.getMaxStackSize();
    }

    private boolean craftStateStillValid() {
        return activeRecipe != null && this.canUseTier(activeRecipe.value().tier())
                && activeRecipe.value().matchesIgnoringAura(this.snapshot())
                && this.calculateCraftableAmount(activeRecipe.value()) >= craftingAmount
                && this.canAccept(activeRecipe.value().output(), craftingAmount);
    }

    private ElementTagCompound requiredAura(int amount) {
        ElementTagCompound required = new ElementTagCompound();
        if (activeRecipe != null) for (AuraRequirement aura : activeRecipe.value().aura()) required.put(aura.element(), aura.amount() * amount);
        return required;
    }

    private boolean hasRequiredAura() {
        if (activeRecipe == null) return false;
        for (AuraRequirement aura : activeRecipe.value().aura()) if (this.getEnergy(aura.element()) < aura.amount() * craftingAmount) return false;
        return true;
    }

    private void completeCraft() {
        if (!this.craftStateStillValid() || !this.hasRequiredAura()) { this.cancelCraft(); return; }
        CastingTableRecipe recipe = activeRecipe.value();
        int amount = craftingAmount;
        ResourceKey<Recipe<?>> recipeKey = activeRecipeKey;
        UUID playerId = craftingPlayer;
        mutatingInventory = true;
        for (GridIngredient required : recipe.grid()) this.consumeGridSlot(required.slot(), amount);
        Map<BlockPos, TileEntityItemStand> stands = this.getOtherStands();
        for (StandIngredient required : recipe.stands()) this.consumeStand(stands.get(required.offset()), amount);
        for (AuraRequirement aura : recipe.aura()) this.drainEnergy(aura.element(), aura.amount() * amount);
        ItemStack output = recipe.output();
        output.setCount(output.getCount() * amount);
        if (this.getItem(9).isEmpty()) this.setItem(9, output);
        else this.getItem(9).grow(output.getCount());
        mutatingInventory = false;
        tableXP += recipe.experience() * amount;
        if (recipeKey != null) completedRecipes.add(recipeKey);
        craftedItems.merge(output.getItem().toString(), output.getCount(), Integer::sum);
        Player player = this.getLevel().getPlayerByUUID(playerId);
        if (player != null) {
            ProgressStage.CASTING.stepPlayerTo(player);
            if (recipe.tier() == CastingTableRecipe.Tier.PYLON) ProgressStage.LINK.stepPlayerTo(player);
            player.giveExperiencePoints(recipe.experience() * amount / 4);
        }
        this.finishCraft();
    }

    private void consumeGridSlot(int slot, int amount) {
        ItemStack stack = this.getItem(slot);
        ItemStackTemplate template = stack.getItem().getCraftingRemainder();
        stack.shrink(amount);
        if (template != null) this.returnRemainder(slot, template.create(), amount);
    }

    private void consumeStand(TileEntityItemStand stand, int amount) {
        ItemStack stack = stand.getItem(0);
        ItemStackTemplate template = stack.getItem().getCraftingRemainder();
        stack.shrink(amount);
        if (template != null && stack.isEmpty()) stand.setItem(0, template.create().copyWithCount(amount));
        else if (template != null) Block.popResource(this.getLevel(), stand.getBlockPos().above(), template.create().copyWithCount(amount));
        stand.syncAfterCraft();
    }

    private void returnRemainder(int slot, ItemStack remainder, int amount) {
        ItemStack current = this.getItem(slot);
        if (current.isEmpty() && amount <= remainder.getMaxStackSize()) this.setItem(slot, remainder.copyWithCount(amount));
        else Block.popResource(this.getLevel(), this.getBlockPos().above(), remainder.copyWithCount(amount));
    }

    private void finishCraft() {
        craftingTick = 0;
        craftingAmount = 0;
        craftingPlayer = null;
        activeRecipe = null;
        activeRecipeKey = null;
        this.linkAndLockStands(false);
        recipeDirty = true;
        this.setChanged();
        this.syncAllData(true);
    }

    public void cancelCraft() { if (this.isCrafting()) this.finishCraft(); }
    public void breakBlock() {
        this.cancelCraft();
        for (TileEntityItemStand stand : this.getOtherStands().values()) if (this.getBlockPos().equals(stand.getTable())) stand.setTable(null);
    }

    public boolean isCrafting() { return craftingTick > 0; }
    public int getCraftingTick() { return craftingTick; }
    public int getCraftingAmount() { return craftingAmount; }
    public int getTableXP() { return tableXP; }
    public TableTier getTier() { return TableTier.forXP(tableXP); }
    public boolean isStructureValid(CastingTableRecipe.Tier tier) { return this.canUseTier(tier); }
    public RecipeHolder<CastingTableRecipe> getActiveRecipe() { return activeRecipe; }
    /** Server-authoritative equivalent of V33a CastingRecipe.canRunRecipe for the GUI overlay. */
    public boolean canRunDisplayedRecipe(Player player) {
        return activeRecipe != null && this.playerCanRun(activeRecipe.value(), player);
    }
    public ItemStack getDisplayOutput() {
        return activeRecipe != null ? activeRecipe.value().output() : clientRecipeOutput;
    }
    public boolean hasDisplayRecipe() { return !this.getDisplayOutput().isEmpty(); }
    public ElementTagCompound getDisplayAura() {
        if (activeRecipe != null) return this.requiredAura(this.isCrafting() ? craftingAmount : 1);
        return clientRecipeAura.copy();
    }
    public Set<ResourceKey<Recipe<?>>> getCompletedRecipes() { return Set.copyOf(completedRecipes); }
    public Map<String, Integer> getCraftedItems() { return Map.copyOf(craftedItems); }

    @Override public boolean canConduct() { return true; }
    @Override public boolean isConductingElement(CrystalElement element) { return true; }
    @Override public int getMaxStorage(CrystalElement element) { return Integer.MAX_VALUE; }
    @Override public int getReceiveRange() { return 24; }
    @Override public int maxThroughput() {
        int base = Math.min(1000, Math.max(100, 100 * (tableXP / TableTier.PYLON.minimumXP() - 1)));
        return (int)(base * (1 + throughputBonus));
    }
    @Override public ElementTagCompound getRequestedTotal() { return this.isCrafting() ? this.requiredAura(craftingAmount) : new ElementTagCompound(); }

    @Override
    protected void animateWithTick(Level world, BlockPos pos) {
        if (!this.isCrafting() || world.getRandom().nextInt(3) != 0) return;
        double x = pos.getX() + 0.2 + world.getRandom().nextDouble() * 0.6;
        double y = pos.getY() + 0.7 + world.getRandom().nextDouble() * 0.5;
        double z = pos.getZ() + 0.2 + world.getRandom().nextDouble() * 0.6;
        world.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.025, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("tableXP", tableXP);
        output.putInt("craftingTick", craftingTick);
        output.putInt("craftingAmount", craftingAmount);
        if (craftingPlayer != null) output.putString("craftingPlayer", craftingPlayer.toString());
        if (activeRecipeKey != null) output.store("activeRecipe", Recipe.KEY_CODEC, activeRecipeKey);
        ValueOutput.TypedOutputList<ResourceKey<Recipe<?>>> completed = output.list("completedRecipes", Recipe.KEY_CODEC);
        completedRecipes.stream().sorted(Comparator.comparing(key -> key.identifier().toString())).forEach(completed::add);
        ValueOutput.ValueOutputList counts = output.childrenList("craftedItems");
        craftedItems.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            ValueOutput child = counts.addChild(); child.putString("item", entry.getKey()); child.putInt("count", entry.getValue());
        });
        output.putBoolean("tuned", isTuned);
        output.putFloat("throughputBonus", throughputBonus);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tableXP = input.getIntOr("tableXP", 0);
        craftingTick = input.getIntOr("craftingTick", 0);
        craftingAmount = input.getIntOr("craftingAmount", 0);
        String player = input.getStringOr("craftingPlayer", "");
        craftingPlayer = player.isEmpty() ? null : UUID.fromString(player);
        activeRecipeKey = input.read("activeRecipe", Recipe.KEY_CODEC).orElse(null);
        completedRecipes.clear();
        input.listOrEmpty("completedRecipes", Recipe.KEY_CODEC).forEach(completedRecipes::add);
        craftedItems.clear();
        for (ValueInput child : input.childrenListOrEmpty("craftedItems")) {
            String item = child.getStringOr("item", "");
            if (!item.isEmpty()) craftedItems.put(item, child.getIntOr("count", 0));
        }
        recipeDirty = true;
        isTuned = input.getBooleanOr("tuned", false);
        throughputBonus = input.getFloatOr("throughputBonus", 0);
    }

    @Override protected void writeSyncTag(CompoundTag tag) {
        super.writeSyncTag(tag);
        tag.putInt("castingTick", craftingTick); tag.putInt("castingAmount", craftingAmount); tag.putInt("tableXP", tableXP);
        ItemStack preview = this.getDisplayOutput();
        if (!preview.isEmpty()) {
            RegistryAccess access = level == null ? RegistryAccess.EMPTY : level.registryAccess();
            ItemStack.CODEC.encodeStart(access.createSerializationContext(NbtOps.INSTANCE), preview).result()
                    .ifPresent(encoded -> tag.put("recipeOutput", encoded));
        }
        this.getDisplayAura().writeToNBT("recipeAura", tag);
        tag.putBoolean("temple", hasTemple); tag.putBoolean("multiblock", hasMultiblock); tag.putBoolean("pylonStructure", hasPylonStructure);
        tag.putBoolean("tuned", isTuned);
        tag.putFloat("throughputBonus", throughputBonus);
    }
    @Override protected void readSyncTag(CompoundTag tag) {
        super.readSyncTag(tag);
        craftingTick = tag.getIntOr("castingTick", 0); craftingAmount = tag.getIntOr("castingAmount", 0); tableXP = tag.getIntOr("tableXP", 0);
        RegistryAccess access = level == null ? RegistryAccess.EMPTY : level.registryAccess();
        net.minecraft.nbt.Tag outputTag = tag.get("recipeOutput");
        clientRecipeOutput = outputTag == null ? ItemStack.EMPTY
                : ItemStack.CODEC.parse(access.createSerializationContext(NbtOps.INSTANCE), outputTag).result().orElse(ItemStack.EMPTY);
        clientRecipeAura.clear();
        clientRecipeAura.readFromNBT("recipeAura", tag);
        hasTemple = tag.getBooleanOr("temple", false); hasMultiblock = tag.getBooleanOr("multiblock", false); hasPylonStructure = tag.getBooleanOr("pylonStructure", false);
        isTuned = tag.getBooleanOr("tuned", false);
        throughputBonus = tag.getFloatOr("throughputBonus", 0);
    }

    @Override public void getTagsToWriteToStack(CompoundTag tag) {
        super.getTagsToWriteToStack(tag); tag.putInt("tableXP", tableXP);
        if (placer != null && !placer.isEmpty()) tag.putString("place", placer);
        if (placerUUID != null) tag.putString("placeUUID", placerUUID.toString());
        tag.putBoolean("tuned", isTuned);
    }
    @Override public void setDataFromItemStackTag(ItemStack stack) {
        super.setDataFromItemStackTag(stack);
        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA) != null ? stack.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
        tableXP = tag.getIntOr("tableXP", 0); placer = tag.getStringOr("place", "");
        String owner = tag.getStringOr("placeUUID", ""); placerUUID = owner.isEmpty() ? null : UUID.fromString(owner);
        isTuned = tag.getBooleanOr("tuned", false);
    }
    @Override public void addTooltipInfo(List list, boolean shift) {
        list.add(Component.literal("Tier: " + this.getTier().name())); list.add(Component.literal("Casting XP: " + tableXP));
        if (placer != null && !placer.isEmpty()) list.add(Component.literal("Owner: " + placer));
    }

    // CHROMA-PORT: enhancement effects and optional Botania pool interaction remain forward references until those
    // registered subsystems land; none of their casting inputs or persistent data has been erased.
}


