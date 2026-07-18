package reika.chromaticraft.magic.interfaces;

import reika.chromaticraft.registry.CrystalElement;

public interface ReactiveRepeater extends CrystalRepeater {

	public void onTransfer(CrystalSource src, CrystalReceiver r, CrystalElement element, int amt);

}
