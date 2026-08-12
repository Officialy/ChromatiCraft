package reika.chromaticraft.world.dimension.structure.lightpanel;

import net.minecraft.util.StringRepresentable;

/** The three signal roles used by V33a's light-panel puzzles. */
public enum LightType implements StringRepresentable {
	TARGET(0x00FF00),
	BLOCK(0xFF0000),
	CANCEL(0x0000FF);

	public static final LightType[] list = values();
	public final int renderColor;

	LightType(int color) {
		renderColor = color;
	}

	@Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
}
