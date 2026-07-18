/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft;

import net.minecraft.world.World;

import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.instantiable.io.DynamicSoundLoader;
import reika.dragonapi.instantiable.io.remotesourcedasset.RemoteSourcedAssetRepository;

public class ChromaCommon {

	public static int armor;

	public static final RemoteSourcedAssetRepository dynamicAssets = new RemoteSourcedAssetRepository(ChromatiCraft.instance, ChromatiCraft.class, "https://raw.githubusercontent.com/ReikaKalseki/ChromatiCraft/master", "Reika/ChromatiCraft/AssetDL");
	public static final DynamicSoundLoader soundLoader = new DynamicSoundLoader(ChromaSounds.class, dynamicAssets);

	public void registerRenderers()
	{
		//unused server side. -- see ClientProxy for implementation
	}

	public void addArmorRenders() {}

	public World getClientWorld() {
		return null;
	}

	public void registerRenderInformation() {

	}

	public void initAssetLoaders() {

	}

	public void registerSounds() {

	}

	public void registerKeys() {

	}

	public void addDonatorRender() {

	}

}
