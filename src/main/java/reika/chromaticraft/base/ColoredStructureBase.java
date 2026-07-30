package reika.chromaticraft.base;

import java.util.Stack;

import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.CrystalElement;
import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;

/** A {@link ChromaStructureBase} whose layout varies by {@link CrystalElement} colour (pushed per call). */
public abstract class ColoredStructureBase extends ChromaStructureBase {

	private final Stack<CrystalElement> currentColor = new Stack<>();

	public final synchronized FilledBlockArray getArray(Level world, int x, int y, int z, CrystalElement e) {
		currentColor.push(e);
		FilledBlockArray ret = this.getArray(world, x, y, z);
		currentColor.pop();
		return ret;
	}

	protected final CrystalElement getCurrentColor() {
		return currentColor.peek();
	}

	@Override
	protected void initDisplayData() {
		currentColor.clear();
		currentColor.push(CrystalElement.elements[(int)(System.currentTimeMillis() / 4000) % 16]);
	}

	@Override
	protected void finishDisplayCall() {
		currentColor.clear();
	}
}
