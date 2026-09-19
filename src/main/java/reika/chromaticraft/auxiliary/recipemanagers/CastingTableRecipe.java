package reika.chromaticraft.auxiliary.recipemanagers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import reika.chromaticraft.registry.ChromaRecipeSerializers;
import reika.chromaticraft.registry.ChromaRecipeTypes;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.CrystalElement;

/** Data-driven physical contract shared by the four V33a casting tiers. */

public final class CastingTableRecipe implements Recipe<CastingRecipeInput> {
	private static final Codec<CrystalElement> ELEMENT_CODEC = Codec.STRING.comapFlatMap(name -> {
		try { return DataResult.success(CrystalElement.valueOf(name.toUpperCase(Locale.ROOT))); }
		catch (IllegalArgumentException e) { return DataResult.error(() -> "Unknown crystal element '" + name + "'"); }
	}, element -> element.name().toLowerCase(Locale.ROOT));
	public enum Tier {
		CRAFTING, TEMPLE, MULTIBLOCK, PYLON;
		public static final Codec<Tier> CODEC = Codec.STRING.comapFlatMap(name -> {
			try { return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT))); }
			catch (IllegalArgumentException e) { return DataResult.error(() -> "Unknown casting tier '" + name + "'"); }
		}, tier -> tier.name().toLowerCase(Locale.ROOT));
	}

	public record GridIngredient(int slot, Ingredient ingredient) {
		public static final Codec<GridIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, 8).fieldOf("slot").forGetter(GridIngredient::slot),
				Ingredient.CODEC.fieldOf("ingredient").forGetter(GridIngredient::ingredient)
		).apply(instance, GridIngredient::new));
	}
	public record StandIngredient(BlockPos offset, Ingredient ingredient) {
		public static final Codec<StandIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("offset").forGetter(StandIngredient::offset),
				Ingredient.CODEC.fieldOf("ingredient").forGetter(StandIngredient::ingredient)
		).apply(instance, StandIngredient::new));
	}
	public record RuneRequirement(BlockPos offset, CrystalElement element) {
		public static final Codec<RuneRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("offset").forGetter(RuneRequirement::offset),
				ELEMENT_CODEC.fieldOf("element").forGetter(RuneRequirement::element)
		).apply(instance, RuneRequirement::new));
	}
	public record AuraRequirement(CrystalElement element, int amount) {
		public static final Codec<AuraRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ELEMENT_CODEC.fieldOf("element").forGetter(AuraRequirement::element),
				Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(AuraRequirement::amount)
		).apply(instance, AuraRequirement::new));
	}
	/**
	 * Source recipe callbacks expressed as data instead of a runtime recipe-class hierarchy.
	 * Storage crystals use all three parts: carry the central crystal's energy forward, award the
	 * STORAGE stage on completion, and play the source 0.5/1/2 casting chord.
	 */
	public record CompletionBehavior(boolean copyCenterCustomData,
			List<ProgressStage> grantedProgress, List<Float> harmonics) {
		public static final CompletionBehavior DEFAULT = new CompletionBehavior(false, List.of(), List.of());
		public static final Codec<CompletionBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.BOOL.optionalFieldOf("copy_center_custom_data", false)
						.forGetter(CompletionBehavior::copyCenterCustomData),
				PROGRESS_CODEC.listOf().optionalFieldOf("granted_progress", List.of())
						.forGetter(CompletionBehavior::grantedProgress),
				Codec.FLOAT.listOf().optionalFieldOf("harmonics", List.of())
						.forGetter(CompletionBehavior::harmonics)
		).apply(instance, CompletionBehavior::new));

		public CompletionBehavior {
			grantedProgress = List.copyOf(grantedProgress);
			harmonics = List.copyOf(harmonics);
			if (harmonics.stream().anyMatch(pitch -> !Float.isFinite(pitch) || pitch <= 0))
				throw new IllegalArgumentException("Casting harmonics must be finite positive pitches");
		}
	}

	private final Tier tier;
	private final List<GridIngredient> grid;
	private final List<StandIngredient> stands;
	private final List<RuneRequirement> runes;
	private final List<AuraRequirement> aura;
	private final ItemStackTemplate output;
	private final int duration;
	private final int experience;
	private final float stackingFactor;
	private final boolean stackable;
	private final boolean requiresTuningKey;
	/**
	 * V33a {@code getPenaltyThreshold}/{@code getPenaltyMultiplier}: once this table has completed
	 * the recipe {@code penaltyThreshold} times, each further craft awards
	 * {@code penaltyMultiplier^(completed - threshold)} of the recipe experience. The source derives
	 * the threshold as {@code max(1, typicalCraftedAmount*3/4)}, except for its {@code CoreRecipe}
	 * marker classes, which are never penalised — that exemption is this field's default.
	 */
	private final int penaltyThreshold;
	private final float penaltyMultiplier;
	/**
	 * Progression the caster needs beyond whatever their recipe tier already implies. V33a declares
	 * this per recipe via {@code CastingRecipe.getRequiredProgress}; the base implementation adds
	 * CRYSTALS (covered here by the tier rules) and subclasses add their own — e.g. RuneRecipe adds
	 * ALLCOLORS, so runes stay gated behind discovering every colour even though they cast at the
	 * bare-table tier.
	 */
	private final List<ProgressStage> requiredProgress;
	private final CompletionBehavior completion;

	public CastingTableRecipe(Tier tier, List<GridIngredient> grid, List<StandIngredient> stands,
			List<RuneRequirement> runes, List<AuraRequirement> aura, ItemStackTemplate output,
			int duration, int experience) {
		this(tier, grid, stands, runes, aura, output, duration, experience, List.of());
	}

	public CastingTableRecipe(Tier tier, List<GridIngredient> grid, List<StandIngredient> stands,
			List<RuneRequirement> runes, List<AuraRequirement> aura, ItemStackTemplate output,
			int duration, int experience, List<ProgressStage> requiredProgress) {
		this(tier, grid, stands, runes, aura, output, duration, experience, requiredProgress, 0.75F,
				tier != Tier.PYLON, false);
	}

	public CastingTableRecipe(Tier tier, List<GridIngredient> grid, List<StandIngredient> stands,
			List<RuneRequirement> runes, List<AuraRequirement> aura, ItemStackTemplate output,
			int duration, int experience, List<ProgressStage> requiredProgress,
			float stackingFactor) {
		this(tier, grid, stands, runes, aura, output, duration, experience, requiredProgress,
				stackingFactor, tier != Tier.PYLON, false);
	}

	public CastingTableRecipe(Tier tier, List<GridIngredient> grid, List<StandIngredient> stands,
			List<RuneRequirement> runes, List<AuraRequirement> aura, ItemStackTemplate output,
			int duration, int experience, List<ProgressStage> requiredProgress,
			float stackingFactor, boolean stackable) {
		this(tier, grid, stands, runes, aura, output, duration, experience, requiredProgress,
				stackingFactor, stackable, false);
	}

	public CastingTableRecipe(Tier tier, List<GridIngredient> grid, List<StandIngredient> stands,
			List<RuneRequirement> runes, List<AuraRequirement> aura, ItemStackTemplate output,
			int duration, int experience, List<ProgressStage> requiredProgress,
			float stackingFactor, boolean stackable, boolean requiresTuningKey) {
		this(tier, grid, stands, runes, aura, output, duration, experience, requiredProgress,
				stackingFactor, stackable, requiresTuningKey, Integer.MAX_VALUE, 0.75F,
				CompletionBehavior.DEFAULT);
	}

	public CastingTableRecipe(Tier tier, List<GridIngredient> grid, List<StandIngredient> stands,
			List<RuneRequirement> runes, List<AuraRequirement> aura, ItemStackTemplate output,
			int duration, int experience, List<ProgressStage> requiredProgress,
			float stackingFactor, boolean stackable, boolean requiresTuningKey,
			int penaltyThreshold, float penaltyMultiplier, CompletionBehavior completion) {
		this.penaltyThreshold = penaltyThreshold;
		this.penaltyMultiplier = penaltyMultiplier;
		this.requiredProgress = List.copyOf(requiredProgress);
		this.completion = completion;
		this.tier = tier;
		this.grid = List.copyOf(grid);
		this.stands = List.copyOf(stands);
		this.runes = List.copyOf(runes);
		this.aura = List.copyOf(aura);
		this.output = output;
		this.duration = duration;
		this.experience = experience;
		this.stackingFactor = stackingFactor;
		this.stackable = stackable;
		this.requiresTuningKey = requiresTuningKey;
		this.validate();
	}

	public List<ProgressStage> requiredProgress() { return requiredProgress; }
	public CompletionBehavior completion() { return completion; }

	private void validate() {
		Set<Integer> slots = new HashSet<>();
		for (GridIngredient entry : grid)
			if (!slots.add(entry.slot())) throw new IllegalArgumentException("Duplicate casting grid slot " + entry.slot());
		Set<BlockPos> standPositions = new HashSet<>();
		for (StandIngredient entry : stands)
			if (!standPositions.add(entry.offset())) throw new IllegalArgumentException("Duplicate casting stand offset " + entry.offset());
		Set<BlockPos> runePositions = new HashSet<>();
		for (RuneRequirement entry : runes)
			if (!runePositions.add(entry.offset())) throw new IllegalArgumentException("Duplicate casting rune offset " + entry.offset());
		Set<CrystalElement> auraElements = new HashSet<>();
		for (AuraRequirement entry : aura)
			if (!auraElements.add(entry.element())) throw new IllegalArgumentException("Duplicate casting aura element " + entry.element());
		if (duration < 1 || experience < 0) throw new IllegalArgumentException("Invalid casting duration or experience");
		if (!(stackingFactor > 0 && stackingFactor <= 1))
			throw new IllegalArgumentException("Casting stacking factor must be in (0, 1]");
		if (tier.ordinal() < Tier.TEMPLE.ordinal() && !runes.isEmpty()) throw new IllegalArgumentException("Runes require a temple recipe");
		if (tier.ordinal() < Tier.MULTIBLOCK.ordinal() && !stands.isEmpty()) throw new IllegalArgumentException("Stands require a multiblock recipe");
		if (tier != Tier.PYLON && !aura.isEmpty()) throw new IllegalArgumentException("Aura is exclusive to pylon recipes");
	}

	@Override
	public boolean matches(CastingRecipeInput input, Level level) {
		return this.matches(input, true);
	}

	public boolean matchesIgnoringAura(CastingRecipeInput input) {
		return this.matches(input, false);
	}

	private boolean matches(CastingRecipeInput input, boolean checkAura) {
		Map<Integer, Ingredient> requiredGrid = new HashMap<>();
		grid.forEach(entry -> requiredGrid.put(entry.slot(), entry.ingredient()));
		for (int slot = 0; slot < 9; slot++) {
			Ingredient expected = requiredGrid.get(slot);
			ItemStack actual = input.getItem(slot);
			if (expected == null ? !actual.isEmpty() : !expected.test(actual)) return false;
		}
		if (input.stands().size() != stands.size()) return false;
		for (StandIngredient entry : stands)
			if (!entry.ingredient().test(input.stands().getOrDefault(entry.offset(), ItemStack.EMPTY))) return false;
		for (RuneRequirement entry : runes)
			if (input.runeAt(entry.offset()) != entry.element()) return false;
		if (checkAura) {
			for (AuraRequirement entry : aura)
				if (input.auraAvailable(entry.element()) < entry.amount()) return false;
		}
		return true;
	}

	@Override public ItemStack assemble(CastingRecipeInput input) { return this.output(input); }
	public Tier tier() { return tier; }
	public List<GridIngredient> grid() { return grid; }
	public List<StandIngredient> stands() { return stands; }
	public List<RuneRequirement> runes() { return runes; }
	public List<AuraRequirement> aura() { return aura; }
	public ItemStack output() { return output.create(); }
	/** Creates the committed output, including source-defined central-item NBT transfer. */
	public ItemStack output(CastingRecipeInput input) {
		ItemStack created = output.create();
		if (completion.copyCenterCustomData()) {
			var custom = input.getItem(4).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
			if (custom != null)
				created.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, custom);
		}
		return created;
	}
	public int duration() { return duration; }
	public int experience() { return experience; }
	public float stackingFactor() { return stackingFactor; }
	public boolean stackable() { return stackable; }
	public boolean requiresTuningKey() { return requiresTuningKey; }
	/** Datagen convenience: V33a threshold = max(1, typicalCraftedAmount*3/4) for non-core recipes. */
	public CastingTableRecipe withPenaltyThreshold(int threshold) {
		return new CastingTableRecipe(tier, grid, stands, runes, aura, output, duration, experience,
				requiredProgress, stackingFactor, stackable, requiresTuningKey, threshold,
				penaltyMultiplier, completion);
	}
	/** Datagen convenience for the few V33a recipes that override both penalty values. */
	public CastingTableRecipe withPenalty(int threshold, float multiplier) {
		return new CastingTableRecipe(tier, grid, stands, runes, aura, output, duration, experience,
				requiredProgress, stackingFactor, stackable, requiresTuningKey, threshold,
				multiplier, completion);
	}
	public CastingTableRecipe withCompletionBehavior(boolean copyCenterCustomData,
			List<ProgressStage> grantedProgress, List<Float> harmonics) {
		return new CastingTableRecipe(tier, grid, stands, runes, aura, output, duration, experience,
				requiredProgress, stackingFactor, stackable, requiresTuningKey, penaltyThreshold,
				penaltyMultiplier, new CompletionBehavior(copyCenterCustomData, grantedProgress, harmonics));
	}
	public int penaltyThreshold() { return penaltyThreshold; }
	public float penaltyMultiplier() { return penaltyMultiplier; }

	/** V33a TileEntityCastingTable.getXPModifier for a table that has already completed this many. */
	public float experienceModifier(int alreadyCrafted) {
		return alreadyCrafted >= penaltyThreshold
				? (float)Math.pow(penaltyMultiplier, alreadyCrafted - penaltyThreshold) : 1F;
	}

	/** V33a's geometric consecutive-crafting duration multiplier. */
	public float stackedTimeFactor(int amount) {
		if (!stackable || amount <= 1) return 1;
		return stackingFactor == 1 ? amount
				: (float)((1 - Math.pow(stackingFactor, amount)) / (1 - stackingFactor));
	}
	@Override public boolean isSpecial() { return true; }
	@Override public boolean showNotification() { return true; }
	@Override public String group() { return "chromaticraft_casting"; }
	@Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
	@Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
	@Override public RecipeSerializer<? extends Recipe<CastingRecipeInput>> getSerializer() { return ChromaRecipeSerializers.CASTING.get(); }
	@Override public RecipeType<? extends Recipe<CastingRecipeInput>> getType() { return ChromaRecipeTypes.CASTING.get(); }

	private static final Codec<ProgressStage> PROGRESS_CODEC = Codec.STRING.comapFlatMap(name -> {
		try { return DataResult.success(ProgressStage.valueOf(name.toUpperCase(Locale.ROOT))); }
		catch (IllegalArgumentException e) { return DataResult.error(() -> "Unknown progress stage '" + name + "'"); }
	}, stage -> stage.name().toLowerCase(Locale.ROOT));

	public static final MapCodec<CastingTableRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Tier.CODEC.fieldOf("tier").forGetter(recipe -> recipe.tier),
			GridIngredient.CODEC.listOf().optionalFieldOf("grid", List.of()).forGetter(recipe -> recipe.grid),
			StandIngredient.CODEC.listOf().optionalFieldOf("stands", List.of()).forGetter(recipe -> recipe.stands),
			RuneRequirement.CODEC.listOf().optionalFieldOf("runes", List.of()).forGetter(recipe -> recipe.runes),
			AuraRequirement.CODEC.listOf().optionalFieldOf("aura", List.of()).forGetter(recipe -> recipe.aura),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.output),
			Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("duration", 5).forGetter(recipe -> recipe.duration),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("experience", 0).forGetter(recipe -> recipe.experience),
			PROGRESS_CODEC.listOf().optionalFieldOf("required_progress", List.of()).forGetter(recipe -> recipe.requiredProgress),
			Codec.floatRange(0.0001F, 1F).optionalFieldOf("stacking_factor", 0.75F).forGetter(recipe -> recipe.stackingFactor),
			Codec.BOOL.optionalFieldOf("stackable", true).forGetter(recipe -> recipe.stackable),
			Codec.BOOL.optionalFieldOf("requires_tuning_key", false).forGetter(recipe -> recipe.requiresTuningKey),
			Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("penalty_threshold", Integer.MAX_VALUE).forGetter(recipe -> recipe.penaltyThreshold),
			Codec.floatRange(0F, 1F).optionalFieldOf("penalty_multiplier", 0.75F).forGetter(recipe -> recipe.penaltyMultiplier),
			CompletionBehavior.CODEC.optionalFieldOf("completion", CompletionBehavior.DEFAULT).forGetter(recipe -> recipe.completion)
	).apply(instance, CastingTableRecipe::new));


	public static final StreamCodec<RegistryFriendlyByteBuf, CastingTableRecipe> STREAM_CODEC = StreamCodec.of(CastingTableRecipe::encode, CastingTableRecipe::decode);
	private static void encode(RegistryFriendlyByteBuf buffer, CastingTableRecipe recipe) {
		buffer.writeVarInt(recipe.tier.ordinal());
		buffer.writeVarInt(recipe.grid.size());
		for (GridIngredient entry : recipe.grid) { buffer.writeVarInt(entry.slot()); Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, entry.ingredient()); }
		buffer.writeVarInt(recipe.stands.size());
		for (StandIngredient entry : recipe.stands) { buffer.writeBlockPos(entry.offset()); Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, entry.ingredient()); }
		buffer.writeVarInt(recipe.runes.size());
		for (RuneRequirement entry : recipe.runes) { buffer.writeBlockPos(entry.offset()); buffer.writeVarInt(entry.element().ordinal()); }
		buffer.writeVarInt(recipe.aura.size());
		for (AuraRequirement entry : recipe.aura) { buffer.writeVarInt(entry.element().ordinal()); buffer.writeVarInt(entry.amount()); }
		ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.output);
		buffer.writeVarInt(recipe.duration);
		buffer.writeVarInt(recipe.experience);
		buffer.writeVarInt(recipe.requiredProgress.size());
		for (ProgressStage stage : recipe.requiredProgress) buffer.writeVarInt(stage.ordinal());
		buffer.writeFloat(recipe.stackingFactor);
		buffer.writeBoolean(recipe.stackable);
		buffer.writeBoolean(recipe.requiresTuningKey);
		buffer.writeVarInt(recipe.penaltyThreshold);
		buffer.writeFloat(recipe.penaltyMultiplier);
		buffer.writeBoolean(recipe.completion.copyCenterCustomData());
		buffer.writeVarInt(recipe.completion.grantedProgress().size());
		for (ProgressStage stage : recipe.completion.grantedProgress()) buffer.writeVarInt(stage.ordinal());
		buffer.writeVarInt(recipe.completion.harmonics().size());
		for (float pitch : recipe.completion.harmonics()) buffer.writeFloat(pitch);
	}
	private static CastingTableRecipe decode(RegistryFriendlyByteBuf buffer) {
		int tierOrdinal = buffer.readVarInt();
		if (tierOrdinal < 0 || tierOrdinal >= Tier.values().length) throw new IllegalArgumentException("Invalid casting tier ordinal " + tierOrdinal);
		List<GridIngredient> grid = new ArrayList<>();
		for (int i = buffer.readVarInt(); i > 0; i--) grid.add(new GridIngredient(buffer.readVarInt(), Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)));
		List<StandIngredient> stands = new ArrayList<>();
		for (int i = buffer.readVarInt(); i > 0; i--) stands.add(new StandIngredient(buffer.readBlockPos(), Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)));
		List<RuneRequirement> runes = new ArrayList<>();
		for (int i = buffer.readVarInt(); i > 0; i--) runes.add(new RuneRequirement(buffer.readBlockPos(), element(buffer.readVarInt())));
		List<AuraRequirement> aura = new ArrayList<>();
		for (int i = buffer.readVarInt(); i > 0; i--) aura.add(new AuraRequirement(element(buffer.readVarInt()), buffer.readVarInt()));
		ItemStackTemplate result = ItemStackTemplate.STREAM_CODEC.decode(buffer);
		int duration = buffer.readVarInt();
		int experience = buffer.readVarInt();
		List<ProgressStage> progress = new ArrayList<>();
		for (int i = buffer.readVarInt(); i > 0; i--) progress.add(ProgressStage.list[buffer.readVarInt()]);
		float stackingFactor = buffer.readFloat();
		boolean stackable = buffer.readBoolean();
		boolean requiresTuningKey = buffer.readBoolean();
		int penaltyThreshold = buffer.readVarInt();
		float penaltyMultiplier = buffer.readFloat();
		boolean copyCenterCustomData = buffer.readBoolean();
		List<ProgressStage> grantedProgress = new ArrayList<>();
		for (int i = buffer.readVarInt(); i > 0; i--) grantedProgress.add(ProgressStage.list[buffer.readVarInt()]);
		List<Float> harmonics = new ArrayList<>();
		for (int i = buffer.readVarInt(); i > 0; i--) harmonics.add(buffer.readFloat());
		return new CastingTableRecipe(Tier.values()[tierOrdinal], grid, stands, runes, aura,
				result, duration, experience, progress, stackingFactor, stackable, requiresTuningKey,
				penaltyThreshold, penaltyMultiplier,
				new CompletionBehavior(copyCenterCustomData, grantedProgress, harmonics));
	}
	private static CrystalElement element(int ordinal) {
		if (ordinal < 0 || ordinal >= CrystalElement.elements.length) throw new IllegalArgumentException("Invalid crystal element ordinal " + ordinal);
		return CrystalElement.elements[ordinal];
	}
}
