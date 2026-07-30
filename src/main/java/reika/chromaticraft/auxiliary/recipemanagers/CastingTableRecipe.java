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

	private final Tier tier;
	private final List<GridIngredient> grid;
	private final List<StandIngredient> stands;
	private final List<RuneRequirement> runes;
	private final List<AuraRequirement> aura;
	private final ItemStackTemplate output;
	private final int duration;
	private final int experience;

	public CastingTableRecipe(Tier tier, List<GridIngredient> grid, List<StandIngredient> stands,
			List<RuneRequirement> runes, List<AuraRequirement> aura, ItemStackTemplate output,
			int duration, int experience) {
		this.tier = tier;
		this.grid = List.copyOf(grid);
		this.stands = List.copyOf(stands);
		this.runes = List.copyOf(runes);
		this.aura = List.copyOf(aura);
		this.output = output;
		this.duration = duration;
		this.experience = experience;
		this.validate();
	}

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

	@Override public ItemStack assemble(CastingRecipeInput input) { return output.create(); }
	public Tier tier() { return tier; }
	public List<GridIngredient> grid() { return grid; }
	public List<StandIngredient> stands() { return stands; }
	public List<RuneRequirement> runes() { return runes; }
	public List<AuraRequirement> aura() { return aura; }
	public ItemStack output() { return output.create(); }
	public int duration() { return duration; }
	public int experience() { return experience; }
	@Override public boolean isSpecial() { return true; }
	@Override public boolean showNotification() { return true; }
	@Override public String group() { return "chromaticraft_casting"; }
	@Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
	@Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
	@Override public RecipeSerializer<? extends Recipe<CastingRecipeInput>> getSerializer() { return ChromaRecipeSerializers.CASTING.get(); }
	@Override public RecipeType<? extends Recipe<CastingRecipeInput>> getType() { return ChromaRecipeTypes.CASTING.get(); }

	public static final MapCodec<CastingTableRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Tier.CODEC.fieldOf("tier").forGetter(recipe -> recipe.tier),
			GridIngredient.CODEC.listOf().optionalFieldOf("grid", List.of()).forGetter(recipe -> recipe.grid),
			StandIngredient.CODEC.listOf().optionalFieldOf("stands", List.of()).forGetter(recipe -> recipe.stands),
			RuneRequirement.CODEC.listOf().optionalFieldOf("runes", List.of()).forGetter(recipe -> recipe.runes),
			AuraRequirement.CODEC.listOf().optionalFieldOf("aura", List.of()).forGetter(recipe -> recipe.aura),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.output),
			Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("duration", 5).forGetter(recipe -> recipe.duration),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("experience", 0).forGetter(recipe -> recipe.experience)
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
		return new CastingTableRecipe(Tier.values()[tierOrdinal], grid, stands, runes, aura,
				ItemStackTemplate.STREAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readVarInt());
	}
	private static CrystalElement element(int ordinal) {
		if (ordinal < 0 || ordinal >= CrystalElement.elements.length) throw new IllegalArgumentException("Invalid crystal element ordinal " + ordinal);
		return CrystalElement.elements[ordinal];
	}
}
