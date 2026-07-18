package reika.chromaticraft.modinterface.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.gui.GuiAbilitySelect;
import reika.chromaticraft.registry.ChromaPackets;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.Chromabilities;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.io.ReikaSoundHelper;


public class GuiAbilityFocus extends GuiAbilitySelect {

	public GuiAbilityFocus(EntityPlayer ep) {
		super(ep);
	}

	@Override
	protected void selectAbility() {
		if (ability != null && Chromabilities.playerHasAbility(player, ability)) {
			ReikaPacketHelper.sendStringIntPacket(ChromatiCraft.packetChannel, ChromaPackets.ABILITYFOCUS.ordinal(), PacketTarget.server, ability.getID(), data);
			ReikaSoundHelper.playClientSound(ChromaSounds.GUICLICK, player, 0.75F, 1);
			player.closeScreen();
		}
	}

}
