package reika.chromaticraft.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.gui.ScreenAbilitySelect;

/** V33a's middle-mouse ability selector entry point. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class ClientAbilityController {

	private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
			Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "general"));
	private static final KeyMapping SELECT_ABILITY = new KeyMapping("key.chromaticraft.ability",
			InputConstants.Type.MOUSE, 2, CATEGORY);

	private ClientAbilityController() {}

	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent event) {
		event.registerCategory(CATEGORY);
		event.register(SELECT_ABILITY);
	}

	@SubscribeEvent
	public static void clientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		while (SELECT_ABILITY.consumeClick()) {
			if (minecraft.player != null && minecraft.gui.screen() == null)
				minecraft.gui.setScreen(new ScreenAbilitySelect(minecraft.player));
		}
	}
}
