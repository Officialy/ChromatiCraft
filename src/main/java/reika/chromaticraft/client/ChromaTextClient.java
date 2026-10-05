package reika.chromaticraft.client;

import net.minecraft.ChatFormatting;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import reika.chromaticraft.auxiliary.render.ChromaText;
import reika.chromaticraft.magic.progression.ChromaNameKnowledge;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.magic.progression.ResearchFragmentData;
import reika.chromaticraft.registry.ChromaItems;

public final class ChromaTextClient {
    private ChromaTextClient() {}

    public static void init(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening event) -> ChromaText.rerandomize());
        NeoForge.EVENT_BUS.addListener(ChromaTextClient::tooltip);
        bus.addListener((AddClientReloadListenersEvent event) -> event.addListener(ChromaText.FONT,
                (ResourceManagerReloadListener)manager -> ChromaText.rerandomize()));
    }

    private static void tooltip(ItemTooltipEvent event) {
        var player = event.getEntity();
        if (player == null) return; // Startup/search-tree collection must not consume another player's knowledge.
        var stack = event.getItemStack();
        var lines = event.getToolTip();
        if (stack.is(ChromaItems.INFO_FRAGMENT.get())) {
            var page = ResearchFragmentData.read(stack).page();
            if (page == null) return;
            boolean has = PlayerResearch.hasFragment(player, page);
            boolean read = ChromaNameKnowledge.canReadFragment(player, page);
            var title = page.section().title().copy().append(": ").append(page.title());
            var line = ChromaText.conceal(title, !read).copy()
                    .withStyle(has ? ChatFormatting.GREEN : read ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RESET);
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).getString().equals(title.getString())) {
                    lines.set(i, line);
                    break;
                }
            }
        }
        else if ((stack.getItem() instanceof reika.chromaticraft.item.ItemCrystalShard
                || stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && blockItem.getBlock() instanceof reika.chromaticraft.base.CrystalBlock)
                && ChromaNameKnowledge.color(stack) != null
                && !reika.chromaticraft.magic.progression.ProgressionManager.instance
                        .hasPlayerDiscoveredColor(player, ChromaNameKnowledge.color(stack))) {
            // V33a hides the crystal/shard effect information as well as its name.
            lines.replaceAll(ChromaText::scramble);
        }
    }
}
