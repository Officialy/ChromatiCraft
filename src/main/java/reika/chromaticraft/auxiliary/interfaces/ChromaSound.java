package reika.chromaticraft.auxiliary.interfaces;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import reika.dragonapi.interfaces.registry.CustomDistanceSound;
import reika.dragonapi.interfaces.registry.DynamicSound;
import reika.dragonapi.interfaces.registry.StreamableSound;

public interface ChromaSound extends DynamicSound, StreamableSound, CustomDistanceSound {

	public void playSoundAtBlock(TileEntity te, float vol, float pitch);
	public void playSoundAtBlock(World world, int x, int y, int z, float vol, float pitch);
	public void playSoundAtBlockNoAttenuation(TileEntity te, float vol, float pitch, int range);
	public boolean hasWiderPitchRange();
	public ChromaSound getUpshiftedPitch();
	public ChromaSound getDownshiftedPitch();
	public float getRangeInterval();

}
