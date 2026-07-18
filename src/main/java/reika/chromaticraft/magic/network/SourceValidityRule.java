package reika.chromaticraft.magic.network;

import reika.chromaticraft.magic.interfaces.CrystalSource;
import reika.chromaticraft.registry.CrystalElement;

public class SourceValidityRule {

	public int minThroughput;
	public int minAmount;
	public float fullOverride;

	public static final SourceValidityRule ALWAYS = new SourceValidityRule();

	private SourceValidityRule() {
		this(0, 0, -1);
	}

	public SourceValidityRule(int thresh) {
		this(0, thresh, -1);
	}

	public SourceValidityRule(int thru, int amt, float full) {
		minThroughput = thru;
		minAmount = amt;
		fullOverride = full;
	}

	public boolean isValid(CrystalSource s, CrystalElement e) {
		if (minThroughput > 0) {
			if (s.maxThroughput() < minThroughput)
				return false;
		}
		if (minAmount > 0) {
			int has = s.getEnergy(e);
			if (has < minAmount && !(fullOverride >= 0 && has >= s.getMaxStorage(e)*fullOverride))
				return false;
		}
		return true;
	}

}
