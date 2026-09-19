package reika.chromaticraft.auxiliary.recipemanagers;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
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

import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.registry.ChromaRecipeSerializers;
import reika.chromaticraft.registry.ChromaRecipeTypes;

/**
 * Data-owned V33a Pool Recipe. The catalyst is consumed once; the counted, unordered ingredients
 * are consumed from all item entities occupying the same Liquid Chroma source block.
 */
public final class PoolAlloyingRecipe implements Recipe<PoolAlloyingInput> {
	public record CountedIngredient(Ingredient ingredient, int count) {
		public static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Ingredient.CODEC.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
				Codec.intRange(1, 64).fieldOf("count").forGetter(CountedIngredient::count)
		).apply(instance, CountedIngredient::new));
	}

	private final Ingredient catalyst;
	private final List<CountedIngredient> ingredients;
	private final ItemStackTemplate result;
	private final List<ProgressStage> requiredProgress;
	private final boolean allowDoubling;
	private final int minimumDuration;

	public PoolAlloyingRecipe(Ingredient catalyst, List<CountedIngredient> ingredients,
			ItemStackTemplate result, List<ProgressStage> requiredProgress,
			boolean allowDoubling, int minimumDuration) {
		if (ingredients.isEmpty()) throw new IllegalArgumentException("Pool alloying recipes need ingredients");
		this.catalyst = catalyst;
		this.ingredients = List.copyOf(ingredients);
		this.result = result;
		this.requiredProgress = List.copyOf(requiredProgress);
		this.allowDoubling = allowDoubling;
		this.minimumDuration = minimumDuration;
	}

	@Override
	public boolean matches(PoolAlloyingInput input, Level level) {
		if (!catalyst.test(input.catalyst())) return false;
		int[] available = input.items().stream().mapToInt(ItemStack::getCount).toArray();
		for (CountedIngredient required : ingredients) {
			int remaining = required.count();
			for (int i = 0; i < input.items().size() && remaining > 0; i++) {
				ItemStack stack = input.items().get(i);
				if (available[i] > 0 && required.ingredient().test(stack)) {
					int reserved = Math.min(remaining, available[i]);
					available[i] -= reserved;
					remaining -= reserved;
				}
			}
			if (remaining > 0) return false;
		}
		return true;
	}

	public boolean playerHasProgress(Player player) {
		return requiredProgress.stream().allMatch(stage -> stage.isPlayerAtStage(player));
	}

	@Override public ItemStack assemble(PoolAlloyingInput input) { return result.create(); }
	public Ingredient catalyst() { return catalyst; }
	public List<CountedIngredient> ingredients() { return ingredients; }
	public ItemStack result() { return result.create(); }
	public List<ProgressStage> requiredProgress() { return requiredProgress; }
	public boolean allowDoubling() { return allowDoubling; }
	public int minimumDuration() { return minimumDuration; }

	@Override public boolean isSpecial() { return true; }
	@Override public boolean showNotification() { return true; }
	@Override public String group() { return "chromaticraft_pool_alloying"; }
	@Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
	@Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
	@Override public RecipeSerializer<? extends Recipe<PoolAlloyingInput>> getSerializer() { return ChromaRecipeSerializers.POOL_ALLOYING.get(); }
	@Override public RecipeType<? extends Recipe<PoolAlloyingInput>> getType() { return ChromaRecipeTypes.POOL_ALLOYING.get(); }

	private static final Codec<ProgressStage> PROGRESS_CODEC = Codec.STRING.comapFlatMap(name -> {
		try { return DataResult.success(ProgressStage.valueOf(name.toUpperCase(Locale.ROOT))); }
		catch (IllegalArgumentException e) { return DataResult.error(() -> "Unknown progress stage '" + name + "'"); }
	}, stage -> stage.name().toLowerCase(Locale.ROOT));

	public static final MapCodec<PoolAlloyingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Ingredient.CODEC.fieldOf("catalyst").forGetter(recipe -> recipe.catalyst),
			CountedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
			PROGRESS_CODEC.listOf().optionalFieldOf("required_progress", List.of()).forGetter(recipe -> recipe.requiredProgress),
			Codec.BOOL.optionalFieldOf("allow_doubling", true).forGetter(recipe -> recipe.allowDoubling),
			Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("minimum_duration", 0).forGetter(recipe -> recipe.minimumDuration)
	).apply(instance, PoolAlloyingRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, PoolAlloyingRecipe> STREAM_CODEC = StreamCodec.of(
			PoolAlloyingRecipe::encode, PoolAlloyingRecipe::decode);

	private static void encode(RegistryFriendlyByteBuf buffer, PoolAlloyingRecipe recipe) {
		Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.catalyst);
		buffer.writeVarInt(recipe.ingredients.size());
		for (CountedIngredient entry : recipe.ingredients) {
			Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, entry.ingredient());
			buffer.writeVarInt(entry.count());
		}
		ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.result);
		buffer.writeVarInt(recipe.requiredProgress.size());
		for (ProgressStage stage : recipe.requiredProgress) buffer.writeVarInt(stage.ordinal());
		buffer.writeBoolean(recipe.allowDoubling);
		buffer.writeVarInt(recipe.minimumDuration);
	}

	private static PoolAlloyingRecipe decode(RegistryFriendlyByteBuf buffer) {
		Ingredient catalyst = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
		List<CountedIngredient> ingredients = java.util.stream.IntStream.range(0, buffer.readVarInt())
				.mapToObj(i -> new CountedIngredient(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt()))
				.toList();
		ItemStackTemplate result = ItemStackTemplate.STREAM_CODEC.decode(buffer);
		List<ProgressStage> progress = java.util.stream.IntStream.range(0, buffer.readVarInt())
				.mapToObj(i -> decodeProgressStage(buffer.readVarInt()))
				.toList();
		return new PoolAlloyingRecipe(catalyst, ingredients, result, progress,
				buffer.readBoolean(), buffer.readVarInt());
	}

	private static ProgressStage decodeProgressStage(int ordinal) {
		if (ordinal < 0 || ordinal >= ProgressStage.list.length)
			throw new IllegalArgumentException("Invalid ChromatiCraft progress-stage ordinal " + ordinal);
		return ProgressStage.list[ordinal];
	}
}
