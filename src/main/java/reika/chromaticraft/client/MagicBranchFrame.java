package reika.chromaticraft.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import reika.chromaticraft.items.ItemMagicBranch;

/** Original wall-clock animation: 32 ms times speed four, with a four-frame offset per variant. */
public record MagicBranchFrame() implements RangeSelectItemModelProperty {
    public static final MapCodec<MagicBranchFrame> MAP_CODEC = MapCodec.unit(new MagicBranchFrame());
    @Override public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return stack.getItem() instanceof ItemMagicBranch branch ? branch.frameAt(System.currentTimeMillis()) : 0;
    }
    @Override public MapCodec<MagicBranchFrame> type() { return MAP_CODEC; }
}
