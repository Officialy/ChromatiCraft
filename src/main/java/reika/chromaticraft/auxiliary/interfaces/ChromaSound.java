package reika.chromaticraft.auxiliary.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.dragonapi.interfaces.registry.CustomDistanceSound;
import reika.dragonapi.interfaces.registry.DynamicSound;
import reika.dragonapi.interfaces.registry.StreamableSound;

public interface ChromaSound extends DynamicSound, StreamableSound, CustomDistanceSound {

	public void playSoundAtBlock(BlockEntity te, float vol, float pitch);
	public void playSoundAtBlock(Level world, BlockPos pos, float vol, float pitch);
	public void playSoundAtBlockNoAttenuation(BlockEntity te, float vol, float pitch, int range);
	public boolean hasWiderPitchRange();
	public ChromaSound getUpshiftedPitch();
	public ChromaSound getDownshiftedPitch();
	public float getRangeInterval();

}
