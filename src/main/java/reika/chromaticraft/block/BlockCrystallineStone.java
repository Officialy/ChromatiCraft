package reika.chromaticraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;


/**
 * One crystalline-stone variant as its own registry identity.
 *
 * <p>V33a packed all sixteen into a single block's metadata. That is legacy metadata, and the
 * project rule is that such families become concrete per-variant blocks with stable registry names —
 * these are sixteen distinct decorative blocks, not one block with runtime state, so nothing about
 * them belongs in a blockstate property. Structure matching, recipes and the network code all
 * compare block identity directly now instead of reading a {@code type} integer.
 */
public class BlockCrystallineStone extends Block {

	/**
	 * The sixteen decorative variants. This lived on the old single metadata block; it belongs with
	 * the concrete blocks now, since there is no longer a block whose state it indexes.
	 */
	public enum StoneTypes {
		SMOOTH(),
		BEAM(),
		COLUMN(),
		GLOWCOL(),
		GLOWBEAM(),
		FOCUS(),
		CORNER(),
		ENGRAVED(),
		EMBOSSED(),
		FOCUSFRAME(),
		GROOVE1(),
		GROOVE2(),
		BRICKS(),
		MULTICHROMIC(),
		STABILIZER(),
		RESORING();

		public static final StoneTypes[] list = values();

		public boolean needsSilkTouch() {
			return this == GLOWCOL || this == GLOWBEAM || this == FOCUS;
		}

		public boolean isBeam() {
			return this == BEAM || this == GLOWBEAM;
		}

		public boolean isColumn() {
			return this == COLUMN || this == GLOWCOL;
		}

		public boolean glows() {
			return this == GLOWCOL || this == GLOWBEAM || this == FOCUS || this == RESORING || this == STABILIZER || this == MULTICHROMIC;
		}

		/** The variant dropped when mined without silk touch (glow variants degrade to their base form). */
		public StoneTypes getDropVariant() {
			switch (this) {
				case GLOWCOL:
					return COLUMN;
				case GLOWBEAM:
					return BEAM;
				case FOCUS:
					return FOCUSFRAME;
				default:
					return this;
			}
		}

		public StoneTypes getGlowingVariant() {
			switch (this) {
				case BEAM:
					return GLOWBEAM;
				case COLUMN:
					return GLOWCOL;
				case FOCUSFRAME:
					return FOCUS;
				default:
					return null;
			}
		}
	}

	private final StoneTypes type;

	public BlockCrystallineStone(BlockBehaviour.Properties props, StoneTypes type) {
		super(props);
		this.type = type;
	}

	public StoneTypes getStoneType() {
		return type;
	}

	/** True when {@code other} is the same crystalline-stone variant. */
	public static boolean isType(Block other, StoneTypes type) {
		return other instanceof BlockCrystallineStone stone && stone.type == type;
	}

	public static boolean isCrystallineStone(Block other) {
		return other instanceof BlockCrystallineStone;
	}
}
