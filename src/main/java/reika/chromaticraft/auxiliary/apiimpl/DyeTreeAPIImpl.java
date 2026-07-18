package reika.chromaticraft.auxiliary.apiimpl;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import reika.chromaticraft.api.crystalelementaccessor.CrystalElementProxy;
import reika.chromaticraft.api.DyeTreeAPI;
import reika.chromaticraft.block.dye.BlockDyeLeaf;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.CrystalElement;


public class DyeTreeAPIImpl implements DyeTreeAPI {

	public boolean isCCLeaf(Block b) {
		return b instanceof BlockDyeLeaf || b == ChromaBlocks.RAINBOWLEAF.getBlockInstance();
	}

	public ItemStack getDyeSapling(CrystalElementProxy e) {
		return ChromaBlocks.DYESAPLING.getStackOf((CrystalElement)e);
	}

	public ItemStack getDyeFlower(CrystalElementProxy e) {
		return ChromaBlocks.DYEFLOWER.getStackOf((CrystalElement)e);
	}

	public ItemStack getDyeLeaf(CrystalElementProxy e, boolean natural) {
		ChromaBlocks b = natural ? ChromaBlocks.DECAY : ChromaBlocks.DYELEAF;
		return b.getStackOf((CrystalElement)e);
	}

	public Block getRainbowLeaf() {
		return ChromaBlocks.RAINBOWLEAF.getBlockInstance();
	}

	public Block getRainbowSapling() {
		return ChromaBlocks.RAINBOWSAPLING.getBlockInstance();
	}

	@Override
	public Block getDyeSapling() {
		return ChromaBlocks.DYESAPLING.getBlockInstance();
	}

	@Override
	public Block getDyeFlower() {
		return ChromaBlocks.DYEFLOWER.getBlockInstance();
	}

	@Override
	public Block getDyeLeaf(boolean natural) {
		ChromaBlocks b = natural ? ChromaBlocks.DECAY : ChromaBlocks.DYELEAF;
		return b.getBlockInstance();
	}

	@Override
	public Block getDecoFlower() {
		return ChromaBlocks.DECOFLOWER.getBlockInstance();
	}

}
