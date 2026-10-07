/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2017
 * All rights reserved.
 * Distribution requires the owner's explicit prior permission.
 ******************************************************************************/
package reika.chromaticraft.items.tools.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import reika.chromaticraft.api.interfaces.CustomExcavationStarBehavior;
import reika.chromaticraft.base.ItemBlockChangingWand;
import reika.chromaticraft.block.decoration.BlockEtherealLight;
import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaEnchants;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.TileEntityStructureController;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker;
import reika.dragonapi.auxiliary.ProgressiveRecursiveBreaker.ProgressiveBreaker;
import reika.dragonapi.instantiable.data.blockstruct.BlockArray;
import reika.dragonapi.instantiable.data.immutable.BlockKey;
import reika.dragonapi.libraries.ReikaPlayerAPI;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.*;

/**
 * V33a Excavation Star, including variant spreading, protected machinery, loot enchantments and cave light.
 */
public final class ItemExcavationWand extends ItemBlockChangingWand {
    private static final ThreadLocal<Boolean> PERMISSION_CHECK = ThreadLocal.withInitial(() -> false);
    private final Map<ProgressiveBreaker, Operation> operations = new HashMap<>();

    public ItemExcavationWand(Properties properties) {
        super(properties);
        addEnergyCost(CrystalElement.YELLOW, 2);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ItemExcavationWand::onBreakBlock);
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        // DragonAPI posts this same event to probe permissions for each queued block.
        if (event.isCanceled() || PERMISSION_CHECK.get()) return;
        Player player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemExcavationWand wand)) return;
        Level level = player.level();
        BlockPos pos = event.getPos();
        if (!wand.canSpreadOn(level, pos, event.getState()) || event.getState().isAir()) return;
        // Like the source onBlockStartBreak, replace the completed ordinary harvest with the wave.
        event.setCanceled(true);
        event.setNotifyClient(true);
        if (!level.isClientSide() && player instanceof ServerPlayer server) wand.startExcavation(stack, pos, server);
    }

    private static int enchantment(ItemStack stack, Level level, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        return stack.getEnchantmentLevel(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(key));
    }

    private static boolean active(Player player, Level level) {
        return !player.isRemoved() && player.level() == level && level.getPlayerByUUID(player.getUUID()) == player;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 5;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    public boolean canSpreadOn(Level level, BlockPos pos, BlockState state) {
        if (level.dimensionType().hasSkyLight() && state.is(Blocks.STONE))
            return false;
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos) && state.is(Blocks.NETHERRACK))
            return false;
        if (level.dimension().equals(Level.END) && (state.is(Blocks.END_STONE) || state.is(Blocks.OBSIDIAN)))
            return false;
        if (state.is(ChromaBlocks.BEDROCK_CRACK.get())) return false;
        var tile = level.getBlockEntity(pos);
        return !(tile instanceof CrystalNetworkTile) && !(tile instanceof TileEntityStructureController);
    }

    public void startExcavation(ItemStack stack, BlockPos pos, ServerPlayer player) {
        ServerLevel level = player.level();

        if (!player.getAbilities().mayBuild || !sufficientEnergy(player)
                || !canSpreadOn(level, pos, level.getBlockState(pos))) {
            return;
        }

        ProgressiveBreaker breaker =
                ProgressiveRecursiveBreaker.instance.addBlockPosWithReturn(
                        level,
                        pos,
                        getDepth(player, level, pos)
                );

        breaker.call = this;
        breaker.player = player;
        breaker.lootTool = stack.copy();
        breaker.silkTouch = enchantment(stack, level, Enchantments.SILK_TOUCH) > 0;
        breaker.fortune = enchantment(stack, level, Enchantments.FORTUNE);
        breaker.drops = !player.getAbilities().instabuild;
        breaker.extraSpread = player.isShiftKeyDown();
        breaker.taxiCabDistance = true;
        breaker.hungerFactor = 0.125F;
        breaker.setBlocks(false, spreadBlocks(level, pos));

        if (ChromaEnchants.AUTOCOLLECT.getLevel(stack) > 0) {
            breaker.dropConsumer = (at, drop) -> {
                player.getInventory().add(drop);

                if (!drop.isEmpty()) {
                    ReikaItemHelper.dropItem(
                            level,
                            at.getX(),
                            at.getY(),
                            at.getZ(),
                            drop
                    );
                }
            };
        }

        operations.put(breaker, new Operation(player, stack.copy()));
    }

    private Set<BlockKey> spreadBlocks(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        Set<BlockKey> blocks = new HashSet<>();
        blocks.add(new BlockKey(state));
        if (block instanceof RedStoneOreBlock) {
            for (var variant : block.getStateDefinition().getPossibleStates()) blocks.add(new BlockKey(variant));
        } else if (block instanceof StairBlock || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
            // Modern trees retain one block identity per species, with axis/decay/waterlogged states.
            // All states replace the original vanilla/ModWoodList metadata loops.
            for (var variant : block.getStateDefinition().getPossibleStates()) blocks.add(new BlockKey(variant));
        } else if (block instanceof CustomExcavationStarBehavior behavior) {
            Collection<BlockKey> additional = behavior.getSpreadBlocks(level, pos.getX(), pos.getY(), pos.getZ());
            if (additional != null) blocks.addAll(additional);
        }
        var id = BuiltInRegistries.BLOCK.getKey(block);
        // The old Nagastone metadata includes both the serpent head and body, now separate blocks.
        // Aurora's colour variants remain states of aurora_block. Its pillars/slabs are distinct materials.
        Set<String> twilightFamily = switch (id.getPath()) {
            case "nagastone", "nagastone_head" -> Set.of("nagastone", "nagastone_head");
            case "aurora_block", "aurora" -> Set.of(id.getPath());
            default -> Set.of();
        };
        if (id.getNamespace().equals("twilightforest") && !twilightFamily.isEmpty())
            for (Block candidate : BuiltInRegistries.BLOCK) {
                var candidateId = BuiltInRegistries.BLOCK.getKey(candidate);
                if (candidateId.getNamespace().equals("twilightforest") && twilightFamily.contains(candidateId.getPath()))
                    for (var variant : candidate.getStateDefinition().getPossibleStates())
                        blocks.add(new BlockKey(variant));
            }
        return blocks;
    }

    @Override
    public int getDepth(Player player, Level level, BlockPos pos) {
        int depth = canUseBoostedEffect(player) ? 18 : 12;
        if (level.getBlockState(pos).getBlock() instanceof CustomExcavationStarBehavior behavior)
            depth = Math.max(depth, behavior.getRange(level, pos.getX(), pos.getY(), pos.getZ(), player));
        return depth;
    }

    @Override
    public void onPreBreak(ProgressiveBreaker breaker, Level level, BlockPos pos, Block block) {
    }

    @Override
    public void onPostBreak(ProgressiveBreaker breaker, Level level, BlockPos pos, Block block) {
        Operation operation = operations.get(breaker);
        if (operation == null) return;
        if (enchantment(operation.tool(), level, Enchantments.FLAME) > 0
                && level.getBrightness(LightLayer.BLOCK, pos) < 8 && !level.canSeeSky(pos))
            level.setBlock(pos, ChromaBlocks.ETHEREAL_LIGHT.get().defaultBlockState().setValue(BlockEtherealLight.PARTICLES, true), 3);
        if (active(operation.player(), level)) drainPlayer(operation.player());
        else breaker.terminate();
    }

    @Override
    public boolean canBreak(ProgressiveBreaker breaker, Level level, BlockPos pos, Block block) {
        Operation operation = operations.get(breaker);
        if (operation == null || !active(operation.player(), level)) {
            breaker.terminate();
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!sufficientEnergy(operation.player()) || state.getDestroySpeed(level, pos) < 0
                || !canSpreadOn(level, pos, state) || !(level instanceof ServerLevel server)
                || !(operation.player() instanceof ServerPlayer player)) return false;
        boolean previous = PERMISSION_CHECK.get();
        PERMISSION_CHECK.set(true);
        try {
            return ReikaPlayerAPI.playerCanBreakAt(server, pos, player);
        } finally {
            if (previous) PERMISSION_CHECK.set(true);
            else PERMISSION_CHECK.remove();
        }
    }

    @Override
    public void onFinish(ProgressiveBreaker breaker) {
        operations.remove(breaker);
    }

    @Override
    public void getSpreadBlocks(Level level, BlockPos pos, BlockArray blocks, Player player, ItemStack stack) {
        blocks.extraSpread = player.isShiftKeyDown();
        blocks.taxiCabDistance = true;
        blocks.recursiveAddMultipleWithBounds(level, pos.getX(), pos.getY(), pos.getZ(), spreadBlocks(level, pos),
                pos.getX() - 32, pos.getY() - 32, pos.getZ() - 32, pos.getX() + 32, pos.getY() + 32, pos.getZ() + 32);
    }

    private record Operation(Player player, ItemStack tool) {
    }
}

