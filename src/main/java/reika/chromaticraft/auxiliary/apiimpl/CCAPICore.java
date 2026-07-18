package reika.chromaticraft.auxiliary.apiimpl;

import java.lang.reflect.Field;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.AbilityAPI;
import reika.chromaticraft.api.AdjacencyUpgradeAPI;
import reika.chromaticraft.api.AuraLocusAPI;
import reika.chromaticraft.api.CastingAPI;
import reika.chromaticraft.api.ChromatiAPI;
import reika.chromaticraft.api.CrystalPotionAPI;
import reika.chromaticraft.api.DyeTreeAPI;
import reika.chromaticraft.api.ItemElementAPI;
import reika.chromaticraft.api.PlayerBufferAPI;
import reika.chromaticraft.api.ProgressionAPI;
import reika.chromaticraft.api.RitualAPI;
import reika.chromaticraft.api.RuneAPI;
import reika.chromaticraft.api.WorldgenAPI;
import reika.chromaticraft.auxiliary.ChromaAux;
import reika.chromaticraft.auxiliary.ability.AbilityHelper;
import reika.chromaticraft.auxiliary.recipemanagers.AbilityRituals;
import reika.chromaticraft.auxiliary.recipemanagers.RecipesCastingTable;
import reika.chromaticraft.magic.CrystalPotionController;
import reika.chromaticraft.magic.ItemElementCalculator;
import reika.chromaticraft.magic.PlayerElementBuffer;
import reika.dragonapi.exception.RegistrationException;

public class CCAPICore extends ChromatiAPI implements WorldgenAPI {

	private final AuraLocusAPIImpl aura = new AuraLocusAPIImpl();
	private final AdjacencyUpgradeAPIImpl adjacency = new AdjacencyUpgradeAPIImpl();
	private final DyeTreeAPIImpl trees = new DyeTreeAPIImpl();

	public static void load() {
		try {
			Field f = ChromatiAPI.class.getDeclaredField("core");
			f.setAccessible(true);
			f.set(null, new CCAPICore());
		}
		catch (Exception e) {
			throw new RegistrationException(ChromatiCraft.instance, "Could not construct API core", e);
		}
	}

	@Override
	public AbilityAPI abilities() {
		return AbilityHelper.instance;
	}

	@Override
	public CastingAPI recipes() {
		return RecipesCastingTable.instance;
	}

	@Override
	public RitualAPI rituals() {
		return AbilityRituals.instance;
	}

	@Override
	public ProgressionAPI research() {
		return ProgressionAPI.instance;
	}

	@Override
	public PlayerBufferAPI buffers() {
		return PlayerElementBuffer.instance;
	}

	@Override
	public AuraLocusAPI aura() {
		return aura;
	}

	@Override
	public AdjacencyUpgradeAPI adjacency() {
		return adjacency;
	}

	@Override
	public ItemElementAPI items() {
		return ItemElementCalculator.instance;
	}

	@Override
	public CrystalPotionAPI potions() {
		return CrystalPotionController.instance;
	}

	@Override
	public RuneAPI runes() {
		return RuneAPI.instance;
	}

	@Override
	public DyeTreeAPI trees() {
		return trees;
	}

	@Override
	public WorldgenAPI worldgen() {
		return this;
	}

	@Override
	public float getEndIslandBias(World world, int chunkX, int chunkZ) {
		int f = chunkX*2;
		int f1 = chunkZ*2;
		float orig = 100.0F - MathHelper.sqrt_float(f * f + f1 * f1) * 8.0F;
		return ChromaAux.getIslandBias(world, orig, f, f1, f, f1);
	}

}
