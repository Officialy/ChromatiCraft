/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;
import reika.chromaticraft.base.ItemChromaTool;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.tileentity.recipe.TileEntityCastingTable;
import reika.chromaticraft.tileentity.recipe.TileEntityItemStand;
import reika.dragonapi.libraries.registry.ReikaItemHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** V33a Recipe Tablet: move a table's auxiliary stacks into one tablet and restore them to that table. */
public class ItemRecipeCacheCapsule extends ItemChromaTool {
    private static final String CACHE = "recipe";

    public ItemRecipeCacheCapsule(Properties properties) { super(properties, 4); }

    private record StoredStand(BlockPos position, ItemStack item) {
        private static final Codec<StoredStand> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(StoredStand::position),
                ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(StoredStand::item)
        ).apply(instance, StoredStand::new));
    }

    private record CachedRecipe(String id, String result, String dimension, BlockPos table, List<StoredStand> items) {
        private static final Codec<CachedRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("recipeID").forGetter(CachedRecipe::id),
                Codec.STRING.fieldOf("result").forGetter(CachedRecipe::result),
                Codec.STRING.fieldOf("dimension").forGetter(CachedRecipe::dimension),
                BlockPos.CODEC.fieldOf("location").forGetter(CachedRecipe::table),
                StoredStand.CODEC.listOf().validate(list -> list.size() <= 24
                        && list.stream().map(StoredStand::position).distinct().count() == list.size()
                        ? DataResult.success(list) : DataResult.error(() -> "Invalid cached casting stands"))
                        .fieldOf("items").forGetter(CachedRecipe::items)
        ).apply(instance, CachedRecipe::new));
    }

    public static boolean hasRecipe(ItemStack stack) {
        var tag = ReikaItemHelper.getStackTag(stack);
        return tag != null && tag.contains(CACHE);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TileEntityCastingTable table)
                || player == null || !table.isOwnedByPlayer(player)) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        var stands = table.getOtherStands();
        // Never alter ingredients locked by an in-progress craft.
        if (table.isCrafting() || stands.values().stream().anyMatch(TileEntityItemStand::isLocked))
            return reject(table);
        if (hasRecipe(stack)) {
            var cached = load(stack, level.registryAccess());
            if (cached == null || !cached.table().equals(table.getBlockPos())
                    || !cached.dimension().equals(level.dimension().identifier().toString())) return reject(table);
            Map<BlockPos, TileEntityItemStand> destinations = new HashMap<>();
            stands.values().forEach(stand -> destinations.put(stand.getBlockPos(), stand));
            if (cached.items().stream().anyMatch(stored -> !destinations.containsKey(stored.position())))
                return reject(table);
            // Validate every destination before dropping or replacing any inventory.
            for (var stand : stands.values()) if (!stand.getItem(0).isEmpty()) {
                stand.dropSlot();
                stand.syncAfterCraft();
            }
            for (var stored : cached.items()) {
                var stand = destinations.get(stored.position());
                stand.setItem(0, stored.item().copy());
                stand.syncAfterCraft();
            }
            ReikaItemHelper.updateStackTag(stack, tag -> tag.remove(CACHE));
            if (!player.isCreative()) stack.shrink(1);
            table.syncAllData(true);
            ChromaSounds.CAST.playSoundAtBlock(table, 0.5F, 2F);
        } else {
            var closest = closestRecipe(level, stands);
            List<StoredStand> contents = new ArrayList<>();
            stands.values().forEach(stand -> contents.add(new StoredStand(stand.getBlockPos(), stand.getItem(0).copy())));
            var cached = new CachedRecipe(closest == null ? "" : closest.id(), closest == null ? "" : closest.result(),
                    level.dimension().identifier().toString(), table.getBlockPos(), List.copyOf(contents));
            var encoded = CachedRecipe.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), cached)
                    .result().orElse(null);
            if (encoded == null) return reject(table);
            // A filled tablet is a single inventory, even when empty tablets were stacked.
            ItemStack capsule = stack.getCount() > 1 ? stack.split(1) : stack;
            ReikaItemHelper.updateStackTag(capsule, tag -> tag.put(CACHE, encoded));
            for (var stand : stands.values()) {
                stand.setItem(0, ItemStack.EMPTY);
                stand.syncAfterCraft();
            }
            table.syncAllData(true);
            if (capsule != stack && !player.getInventory().add(capsule)) ReikaItemHelper.dropItem(player, capsule);
            ChromaSounds.USE.playSoundAtBlock(table);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult reject(TileEntityCastingTable table) {
        ChromaSounds.ERROR.playSoundAtBlock(table);
        return InteractionResult.FAIL;
    }

    private record RecipeDisplay(String id, String result) {}

    private static RecipeDisplay closestRecipe(ServerLevel level, Map<BlockPos, TileEntityItemStand> stands) {
        RecipeDisplay closest = null;
        int differences = Integer.MAX_VALUE;
        for (var holder : level.getServer().getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof CastingTableRecipe recipe)
                    || recipe.tier().ordinal() < CastingTableRecipe.Tier.MULTIBLOCK.ordinal()) continue;
            int count = 0;
            for (var entry : stands.entrySet()) {
                var required = recipe.stands().stream().filter(at -> at.offset().equals(entry.getKey())).findFirst().orElse(null);
                if (required != null ? !required.ingredient().test(entry.getValue().getItem(0)) : !entry.getValue().getItem(0).isEmpty()) count++;
            }
            if (closest == null || count < differences) {
                differences = count;
                closest = new RecipeDisplay(holder.id().identifier().toString(), recipe.output().getItem().getDescriptionId());
            }
        }
        return closest;
    }

    private static CachedRecipe load(ItemStack stack, HolderLookup.Provider registries) {
        var tag = ReikaItemHelper.getStackTag(stack);
        return tag == null ? null : CachedRecipe.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(CACHE))
                .result().orElse(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        var tag = ReikaItemHelper.getStackTag(stack);
        if (!hasRecipe(stack)) tooltip.accept(Component.translatable("item.chromaticraft.recipe_tablet.empty"));
        else {
            // Tooltip metadata does not require decoding the contained inventories or recipe registry.
            var cached = tag.getCompoundOrEmpty(CACHE);
            String result = cached.getStringOr("result", "");
            var location = BlockPos.CODEC.parse(NbtOps.INSTANCE, cached.get("location")).result().orElse(BlockPos.ZERO);
            tooltip.accept(Component.translatable("item.chromaticraft.recipe_tablet.stored",
                    result.isEmpty() ? Component.translatable("item.chromaticraft.recipe_tablet.unknown") : Component.translatable(result),
                    location.toShortString()));
        }
    }
}
