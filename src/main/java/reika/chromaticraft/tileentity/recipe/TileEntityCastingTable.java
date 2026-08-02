package reika.chromaticraft.tileentity.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import reika.chromaticraft.auxiliary.interfaces.FocusAcceleratable;
import reika.chromaticraft.auxiliary.interfaces.NBTTile;
import reika.chromaticraft.auxiliary.interfaces.OperationInterval;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.auxiliary.recipemanagers.CastingRecipeInput;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.AuraRequirement;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.GridIngredient;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe.StandIngredient;
import reika.chromaticraft.base.tileentity.InventoriedCrystalReceiver;
import reika.chromaticraft.block.BlockCrystalRune;
import reika.chromaticraft.container.MenuCastingTable;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.castingtuning.CastingTuningRegistry;
import reika.chromaticraft.magic.progression.CastingProgression;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.*;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFocusCrystal;
import reika.chromaticraft.tileentity.networking.TileEntityCrystalRepeater;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.libraries.ReikaInventoryHelper;
import reika.dragonapi.libraries.ReikaPlayerAPI;

import java.util.*;

/** Complete server-authoritative controller for all four V33a casting tiers. */
public final class TileEntityCastingTable extends InventoriedCrystalReceiver
        implements OwnedTile, NBTTile, MenuProvider, OperationInterval, FocusAcceleratable {

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
    /** Total duration of the active batch; persisted so HUD progress survives reloads. */
    private int craftingDuration;
    private int craftingAmount;
    private int craftSoundTimer = 20000;
    private CastingTableRecipe.Tier clientRecipeTier = CastingTableRecipe.Tier.CRAFTING;
    private boolean clientWasCrafting;
    private int tableXP;
    private boolean hasTemple;
    private float throughputBonus;
    private boolean hasMultiblock;
    private boolean hasPylonStructure;
    /** V33a: set the first time a rune is placed against a temple-tier table. */
    private boolean hasRunes;
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
        if (world.isClientSide()) {
            if (craftingTick > 0) {
                clientWasCrafting = true;
                ChromaParticle.spawnCasting(world, pos, clientRecipeTier, hasTemple, hasMultiblock,
                        hasPylonStructure, clientRecipeAura, this.getTicksExisted(), rand);
                if (this.getState() != OperationState.PENDING) craftingTick--;
            }
            else if (clientWasCrafting) {
                clientWasCrafting = false;
                ChromaParticle.spawnCastingBurst(world, pos, rand);
            }
            return;
        }
        if (activeRecipe == null && activeRecipeKey != null && craftingTick > 0) this.restoreActiveRecipe();
        if (this.getTicksExisted() == 1 || this.getTicksExisted() % 40 == 0) this.validateStructure();
        if (recipeDirty && !this.isCrafting()) this.refreshActiveRecipe();
        if (!this.isCrafting()) return;
        if (!this.craftStateStillValid()) { this.cancelCraft(); return; }
        this.tickCraftingSound();
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
        if (this.getLevel() == null || this.getLevel().isClientSide()) return false;
        if (player == null || ReikaPlayerAPI.isFake(player) || this.isCrafting() || !this.isOwnedByPlayer(player)) return this.rejectCraftingTrigger();
        this.validateStructure();
        this.refreshActiveRecipe();
        if (activeRecipe == null) return this.rejectCraftingTrigger();
        if (!this.playerCanRun(activeRecipe.value(), player)) return this.rejectCraftingTrigger();
        CastingTableRecipe recipe = activeRecipe.value();
        int amount = this.calculateCraftableAmount(recipe);
        int outputAmount = recipe.stackable() ? amount : 1;
        if (amount <= 0 || !this.canAccept(recipe.output(), outputAmount)) return this.rejectCraftingTrigger();
        craftingAmount = amount;
        craftingTick = this.calculateCraftDuration(recipe, amount);
        craftingDuration = craftingTick;
        craftingPlayer = player.getUUID();
        activeRecipeKey = activeRecipe.id();
        ChromaSounds.CAST.playSoundAtBlock(this);
        this.linkAndLockStands(true);
        if (!this.hasRequiredAura()) this.requestEnergyDifference(this.requiredAura(craftingAmount), true);
        this.setChanged();
        this.syncAllData(false);
        return true;
    }

    /** V33a gives every rejected Manipulator start the same audible error response. */
    private boolean rejectCraftingTrigger() {
        ChromaSounds.ERROR.playSoundAtBlock(this);
        return false;
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
        return this.getMissingProgress(recipe, player).isEmpty()
                && (!recipe.requiresTuningKey() || isTuned);
    }

    /**
     * Source ordering for {@code CastingRecipe.getRequiredProgress}: the universal CRYSTALS gate,
     * each inherited casting-tier gate, then recipe-specific additions such as RuneRecipe's
     * ALLCOLORS requirement. Keeping the ordered set here gives the GUI the same information that
     * V33a displayed when hovering its no-entry overlay instead of reducing it to an opaque boolean.
     */
    private List<ProgressStage> requiredProgress(CastingTableRecipe recipe) {
        LinkedHashSet<ProgressStage> required = new LinkedHashSet<>();
        required.add(ProgressStage.CRYSTALS);
        if (recipe.tier().ordinal() >= CastingTableRecipe.Tier.TEMPLE.ordinal())
            required.add(ProgressStage.RUNEUSE);
        if (recipe.tier().ordinal() >= CastingTableRecipe.Tier.MULTIBLOCK.ordinal())
            required.add(ProgressStage.MULTIBLOCK);
        if (recipe.tier() == CastingTableRecipe.Tier.PYLON) {
            required.add(ProgressStage.PYLON);
            required.add(ProgressStage.REPEATER);
        }
        required.addAll(recipe.requiredProgress());
        return List.copyOf(required);
    }

    private List<ProgressStage> getMissingProgress(CastingTableRecipe recipe, Player player) {
        if (player == null) return this.requiredProgress(recipe);
        return this.requiredProgress(recipe).stream().filter(stage -> !stage.isPlayerAtStage(player)).toList();
    }

    public void validateStructure() {
        if (this.getLevel() == null) return;
        boolean previousTemple = hasTemple;
        boolean previousMultiblock = hasMultiblock;
        boolean previousPylonStructure = hasPylonStructure;
        boolean previousTuned = isTuned;
        float previousThroughputBonus = throughputBonus;
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
        // V33a: "if (hasStructure2) ProgressStage.MULTIBLOCK.stepPlayerTo(this.getPlacer())" --
        // completing the CASTING2 structure is the only way MULTIBLOCK is granted.
        if (hasMultiblock && placerUUID != null && this.getLevel() instanceof ServerLevel server) {
            Player owner = server.getPlayerByUUID(placerUUID);
            if (owner != null) ProgressStage.MULTIBLOCK.stepPlayerTo(owner);
        }

        this.setChanged();
        if (!this.getLevel().isClientSide()
                && (previousTemple != hasTemple
                || previousMultiblock != hasMultiblock
                || previousPylonStructure != hasPylonStructure
                || previousTuned != isTuned
                || Float.compare(previousThroughputBonus, throughputBonus) != 0)) {
            // V33a ended every validation with syncAllData(true). Preserve the client-visible
            // transition without resending identical structure NBT on the periodic 40-tick audit.
            this.syncAllData(true);
        }
    }

    /**
     * V33a {@code onAddRune}: placing a crystal rune against a temple-tier table is the only way
     * {@link ProgressStage#RUNEUSE} is granted, and RUNEUSE is what unlocks tier-2 (stand) casting.
     * Called from {@link reika.chromaticraft.block.BlockCrystalRune} on placement.
     */
    public void onAddRune(Player player) {
        // V33a gates this on isAtLeast(TEMPLE), which is the 250-XP tier *and* the structure — the
        // table has to be worked up to temple tier first. Checking only the structure would hand out
        // RUNEUSE, and with it stand casting, the moment the temple is built.
        if (!this.canUseTier(CastingTableRecipe.Tier.TEMPLE)) return;
        boolean firstRune = !hasRunes;
        hasRunes = true;
        ProgressStage.RUNEUSE.stepPlayerTo(player);
        this.setChanged();
        if (firstRune && this.getLevel() != null && !this.getLevel().isClientSide())
            this.syncAllData(false);
    }

    public boolean hasRunes() { return hasRunes; }

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
    @Override
    public float getAccelerationFactor() {
        return TileEntityFocusCrystal.getSummedFocusFactorDirect(this, CASTING_FOCUS_LOCATIONS);
    }

    @Override
    public float getMaximumAcceleratability() {
        return TileEntityFocusCrystal.CrystalTier.TURBOCHARGED.efficiencyFactor() * CASTING_FOCUS_LOCATIONS.size();
    }

    @Override
    public List<BlockPos> getRelativeFocusCrystalLocations() { return CASTING_FOCUS_LOCATIONS; }

    @Override
    public void recountFocusCrystals() { this.getAccelerationFactor(); }

    @Override
    public float getProgressToNextStep() { return 0; }

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
        if (activeRecipe == null || !this.canUseTier(activeRecipe.value().tier())) return false;
        CastingTableRecipe recipe = activeRecipe.value();
        int outputAmount = recipe.stackable() ? craftingAmount : 1;
        return recipe.matchesIgnoringAura(this.snapshot())
                && this.calculateCraftableAmount(recipe) >= craftingAmount
                && this.canAccept(recipe.output(), outputAmount);
    }

    private int calculateCraftDuration(CastingTableRecipe recipe, int amount) {
        int duration = (int)(recipe.duration() * recipe.stackedTimeFactor(amount));
        if (recipe.tier().ordinal() >= CastingTableRecipe.Tier.MULTIBLOCK.ordinal() && duration > 20)
            duration = Math.max(20, (int)(duration / this.getAccelerationFactor()));
        return Math.max(1, duration);
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

    private void tickCraftingSound() {
        if (activeRecipe == null || activeRecipe.value().duration() <= 20) return;
        craftSoundTimer++;
        int interval = switch (this.getTier()) {
            case CRAFTING, TEMPLE -> 1;
            case MULTIBLOCK, PYLON -> 152;
        };
        if (craftSoundTimer >= interval) {
            craftSoundTimer = 0;
            ChromaSounds.CRAFTING.playSoundAtBlock(this);
        }
    }
    private void completeCraft() {
        if (!this.craftStateStillValid() || !this.hasRequiredAura()) { this.cancelCraft(); return; }
        CastingTableRecipe recipe = activeRecipe.value();
        int amount = recipe.stackable() ? craftingAmount : 1;
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
        this.pushOutputToAdjacentInventories();
        mutatingInventory = false;
        TableTier previousTier = this.getTier();
        tableXP += recipe.experience() * amount;
        TableTier upgradedTier = this.getTier();
        if (recipeKey != null) completedRecipes.add(recipeKey);
        craftedItems.merge(output.getItem().toString(), output.getCount(), Integer::sum);
        Player player = this.getLevel().getPlayerByUUID(playerId);
        if (player != null) {
            CastingProgression.markCrafted(player, recipe.tier());
            ProgressStage.CASTING.stepPlayerTo(player);
            if (recipe.tier() == CastingTableRecipe.Tier.PYLON) ProgressStage.LINK.stepPlayerTo(player);
            player.giveExperiencePoints(recipe.experience() * amount / 4);
        }
        ChromaSounds.CRAFTDONE.playSoundAtBlock(this);
        if (upgradedTier != previousTier) ChromaSounds.UPGRADE.playSoundAtBlock(this);

        if (!recipe.stackable()) craftingAmount -= amount;
        if (this.canRepeatNonStackableCraft(recipe, recipeKey)) {
            craftingTick = this.calculateCraftDuration(recipe, 1);
            craftingDuration = craftingTick;
            craftSoundTimer = 20000;
            ChromaSounds.CAST.playSoundAtBlock(this);
            if (!this.hasRequiredAura()) this.requestEnergyDifference(this.requiredAura(craftingAmount), true);
            this.setChanged();
            this.syncAllData(false);
        }
        else {
            this.finishCraft();
        }
    }

    /**
     * V33a pylon recipes deliberately commit one craft per timer cycle. If automation moved the
     * output away, the table keeps its stand locks and starts the next queued cycle; a blocked
     * output slot ends the run cleanly after the completed item instead of deleting or overfilling it.
     */
    private boolean canRepeatNonStackableCraft(CastingTableRecipe recipe, ResourceKey<Recipe<?>> recipeKey) {
        return !recipe.stackable() && craftingAmount > 0 && this.getItem(9).isEmpty()
                && recipeKey != null && recipeKey.equals(activeRecipeKey)
                && recipe.matchesIgnoringAura(this.snapshot())
                && this.calculateCraftableAmount(recipe) >= craftingAmount
                && this.canAccept(recipe.output(), 1);
    }

    /** V33a attempts to move the completed output into each of the six adjacent inventories. */
    private void pushOutputToAdjacentInventories() {
        ItemStack output = this.getItem(9);
        if (output.isEmpty() || this.getLevel() == null) return;
        for (Direction direction : Direction.values()) {
            BlockEntity adjacent = this.getLevel().getBlockEntity(this.getBlockPos().relative(direction));
            if (adjacent instanceof Container inventory && inventory != this
                    && ReikaInventoryHelper.addToIInv(output, inventory)) {
                this.setItem(9, ItemStack.EMPTY);
                return;
            }
        }
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
        craftingDuration = 0;
        craftingAmount = 0;
        craftSoundTimer = 20000;
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

    /** V33a empty-hand sneak action: clear the auxiliary ring once the table has reached tier III. */
    public void dumpAllStands() {
        if (this.getTier().ordinal() < TableTier.MULTIBLOCK.ordinal()) return;
        for (TileEntityItemStand stand : this.getOtherStands().values()) {
            if (stand.getItem(0).isEmpty()) continue;
            stand.dropSlot();
            ChromaSounds.ITEMSTAND.playSoundAtBlock(stand);
            stand.syncAfterCraft();
        }
    }

    public boolean isCrafting() { return craftingTick > 0; }
    public int getCraftingTick() { return craftingTick; }
    public int getCraftingAmount() { return craftingAmount; }

    @Override
    public float getOperationFraction() {
        if (!this.isCrafting() || craftingDuration <= 0)
            return 0;
        return net.minecraft.util.Mth.clamp(1F - craftingTick / (float)craftingDuration, 0F, 1F);
    }

    @Override
    public OperationState getState() {
        if (!this.hasDisplayRecipe())
            return OperationState.INVALID;
        ElementTagCompound required = this.getDisplayAura();
        for (CrystalElement element : CrystalElement.elements) {
            if (this.getEnergy(element) < required.getValue(element))
                return OperationState.PENDING;
        }
        return OperationState.RUNNING;
    }
    public int getTableXP() { return tableXP; }
    public TableTier getTier() { return TableTier.forXP(tableXP); }

    /** V33a renderer input: the active tier's complete casting structure, anchored below the table. */
    public BlockArray getBlocks() {
        if (this.getLevel() == null) return null;
        BlockPos anchor = this.getBlockPos().below();
        return switch (this.getTier()) {
            case CRAFTING -> null;
            case TEMPLE -> ChromaStructures.CASTING1.getArray(this.getLevel(), anchor.getX(), anchor.getY(), anchor.getZ());
            case MULTIBLOCK -> ChromaStructures.CASTING2.getArray(this.getLevel(), anchor.getX(), anchor.getY(), anchor.getZ());
            case PYLON -> ChromaStructures.CASTING3.getArray(this.getLevel(), anchor.getX(), anchor.getY(), anchor.getZ());
        };
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.getBlockPos()).inflate(12, 6, 12);
    }
    public boolean isStructureValid(CastingTableRecipe.Tier tier) { return this.canUseTier(tier); }
    public RecipeHolder<CastingTableRecipe> getActiveRecipe() { return activeRecipe; }
    /** Server-authoritative equivalent of V33a CastingRecipe.canRunRecipe for the GUI overlay. */
    public boolean canRunDisplayedRecipe(Player player) {
        return activeRecipe != null && this.playerCanRun(activeRecipe.value(), player);
    }
    public List<ProgressStage> getMissingProgress(Player player) {
        return activeRecipe != null ? this.getMissingProgress(activeRecipe.value(), player) : List.of();
    }
    public boolean displayRecipeRequiresTuningKey() {
        return activeRecipe != null && activeRecipe.value().requiresTuningKey();
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
        // V33a scales from RecipeType.MULTIBLOCK.levelUp (2000), not the PYLON unlock (15000).
        int base = Math.min(1000, Math.max(100, 100 * (tableXP / TableTier.MULTIBLOCK.minimumXP() - 1)));
        return (int)(base * (1 + throughputBonus));
    }
    @Override public boolean allowsEfficiencyBoost() { return false; }
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
        output.putInt("craftingDuration", craftingDuration);
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
        craftingDuration = input.getIntOr("craftingDuration", craftingTick);
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
        tag.putInt("castingTick", craftingTick); tag.putInt("castingDuration", craftingDuration); tag.putInt("castingAmount", craftingAmount); tag.putInt("tableXP", tableXP);
        ItemStack preview = this.getDisplayOutput();
        if (!preview.isEmpty()) {
            RegistryAccess access = level == null ? RegistryAccess.EMPTY : level.registryAccess();
            ItemStack.CODEC.encodeStart(access.createSerializationContext(NbtOps.INSTANCE), preview).result()
                    .ifPresent(encoded -> tag.put("recipeOutput", encoded));
        }
        this.getDisplayAura().writeToNBT("recipeAura", tag);
        tag.putInt("recipeTier", activeRecipe != null ? activeRecipe.value().tier().ordinal() : clientRecipeTier.ordinal());
        tag.putBoolean("runes", hasRunes); tag.putBoolean("temple", hasTemple); tag.putBoolean("multiblock", hasMultiblock); tag.putBoolean("pylonStructure", hasPylonStructure);
        tag.putBoolean("tuned", isTuned);
        tag.putFloat("throughputBonus", throughputBonus);
    }
    @Override protected void readSyncTag(CompoundTag tag) {
        super.readSyncTag(tag);
        craftingTick = tag.getIntOr("castingTick", 0); craftingDuration = tag.getIntOr("castingDuration", craftingTick); craftingAmount = tag.getIntOr("castingAmount", 0); tableXP = tag.getIntOr("tableXP", 0);
        RegistryAccess access = level == null ? RegistryAccess.EMPTY : level.registryAccess();
        net.minecraft.nbt.Tag outputTag = tag.get("recipeOutput");
        clientRecipeOutput = outputTag == null ? ItemStack.EMPTY
                : ItemStack.CODEC.parse(access.createSerializationContext(NbtOps.INSTANCE), outputTag).result().orElse(ItemStack.EMPTY);
        clientRecipeAura.clear();
        clientRecipeAura.readFromNBT("recipeAura", tag);
        int recipeTier = tag.getIntOr("recipeTier", 0);
        clientRecipeTier = recipeTier >= 0 && recipeTier < CastingTableRecipe.Tier.values().length
                ? CastingTableRecipe.Tier.values()[recipeTier] : CastingTableRecipe.Tier.CRAFTING;
        hasRunes = tag.getBooleanOr("runes", false); hasTemple = tag.getBooleanOr("temple", false); hasMultiblock = tag.getBooleanOr("multiblock", false); hasPylonStructure = tag.getBooleanOr("pylonStructure", false);
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


