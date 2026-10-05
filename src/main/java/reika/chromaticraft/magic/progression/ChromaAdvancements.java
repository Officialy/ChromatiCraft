package reika.chromaticraft.magic.progression;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import reika.dragonapi.libraries.AdvancementHelper;

/** A projection of saved research, including existing saves and command-driven stage removal. */
public final class ChromaAdvancements {
    private ChromaAdvancements() {}

    public static void sync(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer)) return;
        boolean any = false;
        for (ProgressStage stage : ProgressStage.list) {
            if (stage == ProgressStage.NEVER) continue;
            Identifier id = Identifier.fromNamespaceAndPath("chromaticraft",
                    "progression/" + stage.name().toLowerCase(java.util.Locale.ROOT));
            if (stage.active && stage.isPlayerAtStage(player)) {
                AdvancementHelper.grant(player, id);
                any = true;
            } else {
                AdvancementHelper.revoke(player, id);
            }
        }
        if (any)
            AdvancementHelper.grant(player, Identifier.fromNamespaceAndPath("chromaticraft", "root"));
    }
}
