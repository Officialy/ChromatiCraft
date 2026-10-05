package reika.chromaticraft.auxiliary;


public interface OverlayColor {

	int color();

	record IntOverlayColor(int color) implements OverlayColor {


	}
}
