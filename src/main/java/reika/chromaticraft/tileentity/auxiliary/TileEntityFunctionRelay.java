/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.auxiliary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.base.tileentity.TileEntityChromaticBase;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaTiles;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.data.immutable.Coordinate;

public final class TileEntityFunctionRelay extends TileEntityChromaticBase {
    private static final TreeMap<String, RelayedEffectDescription> SPECIAL_EFFECTS = new TreeMap<>();
    private final StepTimer scanTimer = new StepTimer(50);
    private final ArrayList<Coordinate> activeCoords = new ArrayList<>();

    public TileEntityFunctionRelay(BlockPos pos, BlockState state) {
        super(ChromaBlockEntities.FUNCTION_RELAY.get(), pos, state);
    }

    public static Collection<RelayedEffectDescription> getEffects() {
        setDefaultEffects();
        return Collections.unmodifiableCollection(SPECIAL_EFFECTS.values());
    }

    public static void setSpecialEffect(String description, ChromaTiles tile) {
        setSpecialEffect(description, new ItemStack(tile.getBlock()));
    }

    public static void setSpecialEffect(String description, ItemStack stack) {
        SPECIAL_EFFECTS.computeIfAbsent(description, RelayedEffectDescription::new).addItem(stack);
    }

    private static void setDeferredEffect(String description, String item) {
        SPECIAL_EFFECTS.computeIfAbsent(description, RelayedEffectDescription::new)
                .addItemKey(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, item));
    }

    public static void setDefaultEffects() {
        setSpecialEffect("Expands bookshelf search", new ItemStack(Items.ENCHANTING_TABLE));
        setDeferredEffect("Relays harvest AoE", "farmer");
        setDeferredEffect("Relays harvest AoE", "harvest_plant");
        setDeferredEffect("Relays area of effect", "crop_speed");
        setDeferredEffect("Relays area of effect", "reverter");
        setDeferredEffect("Expands fluid search", "cobble_generator");
    }

    @Override public ChromaTiles getTile() { return ChromaTiles.FUNCTIONRELAY; }

    @Override public void updateEntity(Level world, BlockPos pos) {
        scanTimer.update();
        if (scanTimer.checkCap()) this.doScan(world, pos);
    }

    @Override protected void onFirstTick(Level world, BlockPos pos) {
        super.onFirstTick(world, pos);
        this.doScan(world, pos);
    }

    private void doScan(Level world, BlockPos pos) {
        activeCoords.clear();
        for (int offsetX = -6; offsetX <= 6; offsetX++) {
            for (int offsetZ = -6; offsetZ <= 6; offsetZ++) {
                if (Math.abs(offsetX) + Math.abs(offsetZ) > 9) continue;
                for (int offsetY = -6; offsetY <= 2; offsetY++) {
                    BlockPos target = pos.offset(offsetX, offsetY, offsetZ);
                    if (world.hasChunkAt(target) && !world.isEmptyBlock(target))
                        activeCoords.add(new Coordinate(target.getX(), target.getY(), target.getZ()));
                }
            }
        }
    }

    @Override protected void animateWithTick(Level world, BlockPos pos) {
    }

    public Coordinate getRandomCoordinate() {
        return activeCoords.isEmpty() ? null : activeCoords.get(rand.nextInt(activeCoords.size()));
    }

    public Collection<Coordinate> getCoordinates() {
        return Collections.unmodifiableCollection(activeCoords);
    }

    public float getEnchantPowerInRange(Level world, BlockPos pos) {
        float power = 0;
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetX = -5; offsetX <= 5; offsetX++) {
                for (int offsetZ = -5; offsetZ <= 5; offsetZ++) {
                    BlockPos target = pos.offset(offsetX, offsetY, offsetZ);
                    if (!world.hasChunkAt(target)
                            || world.getBlockEntity(target) instanceof TileEntityFunctionRelay) continue;
                    power += world.getBlockState(target).getEnchantPowerBonus(world, target);
                }
            }
        }
        return power;
    }

    public static final class RelayedEffectDescription {
        private final String description;
        private final List<ItemStack> items = new ArrayList<>();
        private final List<Identifier> deferredItems = new ArrayList<>();

        private RelayedEffectDescription(String description) { this.description = description; }
        public String getDescription() { return description; }

        public void addItem(ItemStack stack) {
            if (!stack.isEmpty() && items.stream().noneMatch(item -> ItemStack.isSameItemSameComponents(item, stack)))
                items.add(stack.copy());
        }

        public void addItemKey(Identifier identifier) {
            if (!deferredItems.contains(identifier)) deferredItems.add(identifier);
        }

        public List<Identifier> getDeferredItems() { return List.copyOf(deferredItems); }

        public List<ItemStack> getItems() {
            List<ItemStack> result = new ArrayList<>();
            items.forEach(stack -> result.add(stack.copy()));
            for (Identifier identifier : deferredItems)
                BuiltInRegistries.ITEM.getOptional(identifier).ifPresent(item -> result.add(new ItemStack(item)));
            return List.copyOf(result);
        }
    }
}
