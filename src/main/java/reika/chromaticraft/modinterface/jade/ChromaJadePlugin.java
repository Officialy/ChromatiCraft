package reika.chromaticraft.modinterface.jade;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.magic.progression.ChromaNameKnowledge;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/**
 * Keeps Jade from naming ChromatiCraft blocks the player has not discovered. Jade can take a block's name from
 * {@code Block.getName()} or from server data rather than from the picked item, which bypasses the item-name gate
 * ({@code ItemStackNameMixin}); this replaces Jade's name line with the inventory's concealed name whenever that gate
 * would hide or disguise the block's item.
 */
@WailaPlugin
public final class ChromaJadePlugin implements IWailaPlugin {

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(ConcealedName.INSTANCE, Block.class);
	}

	private enum ConcealedName implements IBlockComponentProvider {
		INSTANCE;

		private static final Identifier UID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "concealed_name");

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			Block block = accessor.getBlock();
			if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(ChromatiCraft.MODID))
				return;
			var player = Minecraft.getInstance().player;
			ItemStack stack = accessor.getPickedResult();
			if (stack.isEmpty())
				stack = new ItemStack(block.asItem());
			if (stack.isEmpty() || player == null)
				return;
			if (ChromaNameKnowledge.disguise(stack, player) == null && !ChromaNameKnowledge.hidden(stack, player))
				return;
			// The mixin makes the hover name the concealed one, exactly as the inventory shows it.
			tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(stack.getHoverName()));
		}

		@Override
		public Identifier getUid() {
			return UID;
		}

		/** After Jade's own name provider, so its line exists to be replaced. */
		@Override
		public int getDefaultPriority() {
			return -10000;
		}

		@Override
		public boolean isRequired() {
			return true;
		}
	}
}
