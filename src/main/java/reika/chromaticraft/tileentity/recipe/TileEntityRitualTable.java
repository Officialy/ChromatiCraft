package reika.chromaticraft.tileentity.recipe;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.api.event.RitualCompletionEvent;
import reika.chromaticraft.auxiliary.interfaces.MultiBlockChromaTile;
import reika.chromaticraft.auxiliary.interfaces.OperationInterval;
import reika.chromaticraft.auxiliary.interfaces.OwnedTile;
import reika.chromaticraft.auxiliary.interfaces.VariableTexture;
import reika.chromaticraft.auxiliary.recipemanagers.AbilityRituals;
import reika.chromaticraft.auxiliary.structure.RitualStructure;
import reika.chromaticraft.base.tileentity.InventoriedCrystalReceiver;
import reika.chromaticraft.block.BlockRitualTable;
import reika.chromaticraft.client.render.RitualTableVisuals;
import reika.chromaticraft.container.MenuRitualTable;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.ChromaStructures;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.Chromabilities;
import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.interfaces.blockentity.BreakAction;
import reika.dragonapi.interfaces.blockentity.TriggerableAction;
import reika.dragonapi.libraries.ReikaPlayerAPI;

public final class TileEntityRitualTable extends InventoriedCrystalReceiver implements BreakAction,
        TriggerableAction, OwnedTile, OperationInterval, MultiBlockChromaTile, VariableTexture, MenuProvider {

    private boolean hasStructure;
    private boolean hasEnhancedStructure;
    private boolean isEnhanced;
    private int abilityTick;
    private int abilitySoundTick = 2000;
    private int tickNoPlayer;
    private int tickPlayerOut;
    private boolean playerSteppedIn;
    private Ability ability;
    private UUID ritualPlayer;
    private boolean capturedNoGravity;
    private boolean priorNoGravity;
    private boolean completionVisualPending;

    public TileEntityRitualTable(BlockPos pos, BlockState state) {
        super(ChromaBlockEntities.RITUAL_TABLE.get(), pos, state);
    }

    @Override public ChromaTiles getTile() { return ChromaTiles.RITUAL; }
    @Override public int getSizeInventory() { return 0; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean onlyAllowOwnersToMine() { return true; }
    @Override public boolean onlyAllowOwnersToUse() { return true; }
    @Override public boolean isOwnedByPlayer(Player player) {
        return placerUUID == null || placerUUID.equals(player.getUUID());
    }
    @Override public Component getDisplayName() {
        return Component.translatable("block.chromaticraft.ritual_table");
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MenuRitualTable(id, inventory, this);
    }
    @Override public int getReceiveRange() { return 16; }
    @Override public boolean isConductingElement(CrystalElement element) { return element != null; }
    @Override public int maxThroughput() { return isFullyEnhanced() ? 2500 : 200; }
    @Override public boolean canConduct() { return true; }
    @Override public int getMaxStorage(CrystalElement element) {
        return AbilityRituals.instance.getMaxAbilityCost() * 3 / 2;
    }
    @Override public boolean allowsEfficiencyBoost() { return false; }

    @Override
    public void updateEntity(Level world, BlockPos pos) {
        super.updateEntity(world, pos);
        if (world.isClientSide())
            return;
        if (this.getTicksExisted() % 40 == 0)
            validateStructure();
        if (abilityTick > 0)
            onRitualTick(world, pos);
    }

    @Override
    protected void animateWithTick(Level world, BlockPos pos) {
        if (world == null || !world.isClientSide()) return;
        Player viewer = ritualPlayer == null ? null : world.getPlayerByUUID(ritualPlayer);
        if (abilityTick > 0 && ability != null)
            RitualTableVisuals.tick(world, pos, AbilityRituals.instance.getAura(ability),
                    this.getEnergy(), energy.containsAtLeast(AbilityRituals.instance.getAura(ability))
                            && viewer != null && playerBox().intersects(viewer.getBoundingBox()),
                    ritualPlayer, this.getTicksExisted());
        else
            RitualTableVisuals.stop(pos, completionVisualPending);
    }

    @Override
    protected void onFirstTick(Level world, BlockPos pos) {
        super.onFirstTick(world, pos);
        if (!world.isClientSide()) {
            AbilityRituals.addTable(this);
            validateStructure();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide())
            AbilityRituals.addTable(this);
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide())
            AbilityRituals.removeTable(this);
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        breakBlock();
        super.setRemoved();
    }

    private void onRitualTick(Level world, BlockPos pos) {
        if (ability == null) {
            killRitual();
            return;
        }
        ElementTagCompound cost = AbilityRituals.instance.getAura(ability);
        if (this.getCooldown() == 0 && checkTimer.checkCap())
            this.requestEnergyDifference(cost, true);

        Player player = ritualPlayer == null ? null : world.getPlayerByUUID(ritualPlayer);
        if (player == null || player.isRemoved()) {
            if (++tickNoPlayer > 200)
                terminateRitual();
            return;
        }
        tickNoPlayer = 0;
        boolean charged = energy.containsAtLeast(cost);
        boolean inside = playerBox().intersects(player.getBoundingBox());
        if (charged) {
            if (inside) {
                playerSteppedIn = true;
                tickPlayerOut = 0;
            }
            else if (playerSteppedIn) {
                if (++tickPlayerOut > 50)
                    terminateRitual();
                return;
            }
        }

        if (++abilitySoundTick >= 490 && abilityTick > 120) {
            abilitySoundTick = 0;
            ChromaSounds.ABILITY.playSoundAtBlock(this);
        }
        if (!(charged && inside))
            return;
        pullPlayer(player, pos);
        if (--abilityTick <= 0) {
            ChromaSounds.ABILITYCOMPLETE.playSound(player);
            completionVisualPending = giveAbility(player);
            this.drainEnergy(cost);
            restoreGravity(player);
            ritualPlayer = null;
            abilitySoundTick = 2000;
            ability = null;
            this.syncAllData(true);
        }
    }

    private void pullPlayer(Player player, BlockPos pos) {
        double dx = player.getX() - pos.getX() - 0.5;
        double dy = player.getY() - pos.getY() - 2.25
                - 0.75 * Math.sin(Math.toRadians(2 * this.getTicksExisted()));
        double dz = player.getZ() - pos.getZ() - 0.5;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > 1.0e-6) {
            double speed = 0.1875 / distance;
            player.setDeltaMovement(new Vec3(-speed * Math.copySign(dx * dx, dx),
                    -speed * Math.copySign(dy * dy, dy),
                    -speed * Math.copySign(dz * dz, dz)));
            player.hurtMarked = true;
        }
        player.resetFallDistance();
        if (!capturedNoGravity) {
            capturedNoGravity = true;
            priorNoGravity = player.isNoGravity();
        }
        player.setNoGravity(true);
    }

    private boolean giveAbility(Player player) {
        if (ReikaPlayerAPI.isFake(player))
            return false;
        LexiconCatalog.Entry fragment = ability instanceof Chromabilities nativeAbility
                ? LexiconCatalog.entries().stream()
                        .filter(entry -> entry.section() == LexiconCatalog.Section.ABILITIES
                                && entry.sourceId().equals(nativeAbility.getID())).findFirst().orElse(null)
                : null;
        if (fragment != null && !PlayerResearch.hasFragment(player, fragment)) {
            if (level instanceof ServerLevel server)
                server.sendParticles(ParticleTypes.EXPLOSION, worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        6, 0.5, 0.5, 0.5, 0.1);
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
            Vec3 away = player.position().subtract(Vec3.atCenterOf(worldPosition.below())).normalize();
            player.setDeltaMovement(away.scale(4));
            player.hurtMarked = true;
            player.fallDistance += 12;
            return false;
        }
        else {
            ChromaAbilityData.give(player, ability);
            NeoForge.EVENT_BUS.post(new RitualCompletionEvent(player, ability.getID()));
            return true;
        }
    }

    public void validateStructure() {
        if (level == null || level.isClientSide())
            return;
        BlockPos anchor = worldPosition.below(2);
        RitualStructure base = new RitualStructure();
        base.initializeEnhance(isEnhanced, false);
        hasStructure = base.getArray(level, anchor.getX(), anchor.getY(), anchor.getZ()).matchInWorld();
        hasEnhancedStructure = isEnhanced && hasStructure
                && new RitualStructure.Enhanced()
                        .getArray(level, anchor.getX(), anchor.getY(), anchor.getZ()).matchInWorld();
        BlockRitualTable.setEnhanced(level, worldPosition, isEnhanced);
        if (!hasStructure && abilityTick > 0)
            killRitual();
        this.syncAllData(true);
    }

    public void initEnhancementCheck(Player player) {
        isEnhanced = ProgressStage.DIMENSION.isPlayerAtStage(player);
        validateStructure();
    }

    public boolean isFullyEnhanced() { return isEnhanced && hasEnhancedStructure; }

    public boolean triggerRitual(Player player) {
        initEnhancementCheck(player);
        if (hasStructure && abilityTick == 0 && ability != null
                && AbilityRituals.instance.hasRitual(ability) && isOwnedByPlayer(player)) {
            ritualPlayer = player.getUUID();
            completionVisualPending = false;
            if (level.isClientSide())
                return true;
            this.requestEnergyDifference(AbilityRituals.instance.getAura(ability), true);
            abilityTick = AbilityRituals.instance.getDuration(ability);
            playerSteppedIn = false;
            tickNoPlayer = 0;
            tickPlayerOut = 0;
            ChromaSounds.USE.playSoundAtBlock(this);
            this.syncAllData(true);
            return true;
        }
        ChromaSounds.ERROR.playSoundAtBlock(this);
        return false;
    }

    public void setChosenAbility(Ability chosen) {
        killRitual();
        ability = chosen;
        this.syncAllData(true);
    }

    private void terminateRitual() {
        abilitySoundTick = 20000;
        abilityTick = 0;
        tickPlayerOut = 0;
        playerSteppedIn = false;
        restoreGravity(ritualPlayer == null || level == null ? null : level.getPlayerByUUID(ritualPlayer));
        if (level != null && level.isClientSide())
            RitualTableVisuals.stop(worldPosition, false);
        ritualPlayer = null;
        this.syncAllData(true);
    }

    private void killRitual() {
        abilitySoundTick = 2000;
        terminateRitual();
        ability = null;
    }

    private void restoreGravity(Player player) {
        if (capturedNoGravity && player != null)
            player.setNoGravity(priorNoGravity);
        capturedNoGravity = false;
    }

    private AABB playerBox() {
        return new AABB(worldPosition).move(0, 2, 0).inflate(0, 1, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("struct", hasStructure);
        output.putBoolean("structe", hasEnhancedStructure);
        output.putBoolean("enhance", isEnhanced);
        output.putInt("atick", abilityTick);
        output.putInt("soundTick", abilitySoundTick);
        output.putString("ability", ability == null ? "null" : ability.getID());
        if (ritualPlayer != null)
            output.putString("ritualPlayer", ritualPlayer.toString());
        output.putBoolean("capturedNoGravity", capturedNoGravity);
        output.putBoolean("priorNoGravity", priorNoGravity);
        output.putBoolean("completionVisualPending", completionVisualPending);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hasStructure = input.getBooleanOr("struct", false);
        hasEnhancedStructure = input.getBooleanOr("structe", false);
        isEnhanced = input.getBooleanOr("enhance", false);
        abilityTick = input.getIntOr("atick", 0);
        abilitySoundTick = input.getIntOr("soundTick", 2000);
        ability = Chromabilities.getAbility(input.getStringOr("ability", "null"));
        String playerId = input.getStringOr("ritualPlayer", "");
        ritualPlayer = playerId.isEmpty() ? null : UUID.fromString(playerId);
        capturedNoGravity = input.getBooleanOr("capturedNoGravity", false);
        priorNoGravity = input.getBooleanOr("priorNoGravity", false);
        completionVisualPending = input.getBooleanOr("completionVisualPending", false);
    }

    @Override
    protected void writeSyncTag(CompoundTag tag) {
        super.writeSyncTag(tag);
        tag.putBoolean("struct", hasStructure);
        tag.putBoolean("structe", hasEnhancedStructure);
        tag.putBoolean("enhance", isEnhanced);
        tag.putInt("atick", abilityTick);
        tag.putString("ability", ability == null ? "null" : ability.getID());
        tag.putString("ritualPlayer", ritualPlayer == null ? "" : ritualPlayer.toString());
        tag.putBoolean("completionVisualPending", completionVisualPending);
    }

    @Override
    protected void readSyncTag(CompoundTag tag) {
        super.readSyncTag(tag);
        hasStructure = tag.getBooleanOr("struct", false);
        hasEnhancedStructure = tag.getBooleanOr("structe", false);
        isEnhanced = tag.getBooleanOr("enhance", false);
        abilityTick = tag.getIntOr("atick", 0);
        ability = Chromabilities.getAbility(tag.getStringOr("ability", "null"));
        String playerId = tag.getStringOr("ritualPlayer", "");
        ritualPlayer = playerId.isEmpty() ? null : UUID.fromString(playerId);
        completionVisualPending = tag.getBooleanOr("completionVisualPending", false);
    }

    @Override public ElementTagCompound getRequestedTotal() {
        return ability != null && abilityTick > 0
                ? AbilityRituals.instance.getAura(ability) : new ElementTagCompound();
    }
    public boolean isActive() { return abilityTick > 0; }
    public boolean isPlayerUsing(Player player) {
        return isActive() && energy.containsAtLeast(AbilityRituals.instance.getAura(ability))
                && playerBox().intersects(player.getBoundingBox());
    }
    @Override public boolean trigger() {
        Player placer = this.getPlacer();
        return placer != null && triggerRitual(placer);
    }
    @Override public void breakBlock() {
        if (level != null && !level.isClientSide())
            AbilityRituals.removeTable(this);
        if (abilityTick > 0)
            terminateRitual();
    }
    @Override public int getIconState(int side) { return isEnhanced ? 1 : 0; }
    @Override public float getOperationFraction() {
        return ability == null ? 0 : 1 - abilityTick / (float) AbilityRituals.instance.getDuration(ability);
    }
    @Override public OperationState getState() {
        return ability != null && hasStructure
                ? energy.containsAtLeast(AbilityRituals.instance.getAura(ability))
                        ? OperationState.RUNNING : OperationState.PENDING
                : OperationState.INVALID;
    }
    @Override public ChromaStructures getPrimaryStructure() { return ChromaStructures.RITUAL; }
    @Override public Coordinate getStructureOffset() { return new Coordinate(0, -2, 0); }
    @Override public boolean canStructureBeInspected() { return true; }
    @Override public boolean hasStructure() { return hasStructure; }
    public boolean hasWork() { return getState() == OperationState.RUNNING; }
}
