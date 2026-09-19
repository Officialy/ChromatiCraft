package reika.chromaticraft.world.dimension.structure.lightpanel;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persistent, generator-independent state for one Glowing Logic room. */
public final class LightPanelPuzzleState {

	private final int tier;
	private final int rowCount;
	private final int switchCount;
	private final int[][] connections;
	private int activeSwitches;

	public LightPanelPuzzleState(FixedLightPattern pattern) {
		this(pattern, null);
	}

	/**
	 * Builds the runtime wiring. V33a's fixed room keeps the authored row identities but shuffles
	 * which physical switch receives each authored switch group for every generated structure.
	 */
	public LightPanelPuzzleState(FixedLightPattern pattern, Random random) {
		tier = pattern.tier;
		rowCount = pattern.rowCount;
		switchCount = pattern.switchCount;
		connections = new int[LightType.list.length][switchCount];
		List<Integer> authoredSwitches = new ArrayList<>(switchCount);
		for (int sw = 0; sw < switchCount; sw++) authoredSwitches.add(sw);
		if (random != null) Collections.shuffle(authoredSwitches, random);
		for (int sw = 0; sw < switchCount; sw++) {
			LightGroup group = pattern.getConnections(authoredSwitches.get(sw));
			for (int row = 0; row < rowCount; row++)
				for (LightType type : LightType.list)
					if (group.containsLight(row, type))
						connections[type.ordinal()][sw] |= 1 << row;
		}
	}

	private LightPanelPuzzleState(int tier, int rows, int switches, int active, int[][] links) {
		this.tier = tier;
		rowCount = rows;
		switchCount = switches;
		activeSwitches = active & (1 << switches) - 1;
		connections = links;
	}

	public int tier() { return tier; }
	public int rowCount() { return rowCount; }
	public int switchCount() { return switchCount; }
	public int activeSwitches() { return activeSwitches; }

	public boolean setSwitch(int channel, boolean active) {
		if (channel < 0 || channel >= switchCount)
			throw new IllegalArgumentException("Glowing Logic switch " + channel);
		int before = activeSwitches;
		if (active)
			activeSwitches |= 1 << channel;
		else
			activeSwitches &= ~(1 << channel);
		return before != activeSwitches;
	}

	public boolean isSwitchActive(int channel) {
		return (activeSwitches & 1 << channel) != 0;
	}

	public boolean hasConnections(int channel) {
		if (channel < 0 || channel >= switchCount)
			throw new IllegalArgumentException("Glowing Logic switch " + channel);
		for (LightType type : LightType.list)
			if (connections[type.ordinal()][channel] != 0)
				return true;
		return false;
	}

	public boolean isLightActive(int row, LightType type) {
		if (row < 0 || row >= rowCount)
			throw new IllegalArgumentException("Glowing Logic row " + row);
		for (int sw = 0; sw < switchCount; sw++)
			if (isSwitchActive(sw) && (connections[type.ordinal()][sw] & 1 << row) != 0)
				return true;
		return false;
	}

	public boolean isComplete() {
		for (int row = 0; row < rowCount; row++)
			if (!isLightActive(row, LightType.TARGET)
					|| isLightActive(row, LightType.BLOCK) && !isLightActive(row, LightType.CANCEL))
				return false;
		return true;
	}

	public void save(ValueOutput output) {
		output.putInt("tier", tier);
		output.putInt("rows", rowCount);
		output.putInt("switches", switchCount);
		output.putInt("active", activeSwitches);
		for (LightType type : LightType.list)
			output.putIntArray(type.getSerializedName(), connections[type.ordinal()]);
	}

	public static LightPanelPuzzleState load(ValueInput input) {
		int tier = input.getIntOr("tier", 0);
		int rows = input.getIntOr("rows", LightPanelPatternLibrary.rowCount(tier));
		int switches = input.getIntOr("switches", LightPanelPatternLibrary.switchCount(tier));
		if (rows < 1 || rows > 16 || switches < 1 || switches > 16)
			throw new IllegalArgumentException("Invalid Glowing Logic state " + rows + "x" + switches);
		int[][] links = new int[LightType.list.length][switches];
		for (LightType type : LightType.list) {
			int[] read = input.getIntArray(type.getSerializedName()).orElseGet(() -> new int[0]);
			links[type.ordinal()] = Arrays.copyOf(read, switches);
		}
		return new LightPanelPuzzleState(tier, rows, switches,
				input.getIntOr("active", 0), links);
	}
}
