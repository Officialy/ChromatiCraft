package reika.chromaticraft.auxiliary.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import reika.dragonapi.interfaces.registry.CustomDistanceSound;
import reika.dragonapi.interfaces.registry.DynamicSound;
import reika.dragonapi.interfaces.registry.StreamableSound;

public interface ChromaSound extends DynamicSound, StreamableSound, CustomDistanceSound {

	void playSoundAtBlock(BlockEntity te, float vol, float pitch);
	void playSoundAtBlock(Level world, BlockPos pos, float vol, float pitch);
	void playSoundAtBlockNoAttenuation(BlockEntity te, float vol, float pitch, int range);
	boolean hasWiderPitchRange();
	ChromaSound getUpshiftedPitch();
	ChromaSound getDownshiftedPitch();
	float getRangeInterval();

}
