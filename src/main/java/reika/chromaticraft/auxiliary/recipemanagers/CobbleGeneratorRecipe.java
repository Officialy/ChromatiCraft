package reika.chromaticraft.auxiliary.recipemanagers;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidInstance;

import reika.chromaticraft.registry.ChromaRecipeSerializers;
import reika.chromaticraft.registry.ChromaRecipeTypes;
import reika.chromaticraft.registry.CrystalElement;

/** Data-owned form of V33a's ordered {@code TileEntityCobbleGen.FluidMix} entries. */
public final class CobbleGeneratorRecipe implements Recipe<CobbleGeneratorInput> {
	private final Holder<Fluid> primary;
	private final Holder<Fluid> secondary;
	private final ItemStackTemplate result;
	private final int duration;
	private final float primaryConsumptionChance;
	private final float secondaryConsumptionChance;
	private final int order;
	private final List<CrystalElement> effectElements;

	public CobbleGeneratorRecipe(Holder<Fluid> primary, Holder<Fluid> secondary,
			ItemStackTemplate result, int duration, float primaryConsumptionChance,
			float secondaryConsumptionChance, int order, List<CrystalElement> effectElements) {
		if (duration <= 0) throw new IllegalArgumentException("Cobble-generator duration must be positive");
		validateChance(primaryConsumptionChance, "primary");
		validateChance(secondaryConsumptionChance, "secondary");
		this.primary = primary;
		this.secondary = secondary;
		this.result = result;
		this.duration = duration;
		this.primaryConsumptionChance = primaryConsumptionChance;
		this.secondaryConsumptionChance = secondaryConsumptionChance;
		this.order = order;
		this.effectElements = List.copyOf(effectElements);
	}

	private static void validateChance(float chance, String name) {
		if (chance < 0 || chance > 100)
			throw new IllegalArgumentException(name + " fluid consumption chance must be in [0,100]");
	}

	@Override
	public boolean matches(CobbleGeneratorInput input, Level level) {
		return input.primary() == primary.value() && input.secondary() == secondary.value();
	}

	@Override public ItemStack assemble(CobbleGeneratorInput input) { return result.create(); }
	public Fluid primaryFluid() { return primary.value(); }
	public Fluid secondaryFluid() { return secondary.value(); }
	public ItemStack result() { return result.create(); }
	public int duration() { return duration; }
	public float primaryConsumptionChance() { return primaryConsumptionChance; }
	public float secondaryConsumptionChance() { return secondaryConsumptionChance; }
	public int order() { return order; }
	public List<CrystalElement> effectElements() { return effectElements; }
	public int requiredPrimaryAmount() { return requiredAmount(primaryConsumptionChance); }
	public int requiredSecondaryAmount() { return requiredAmount(secondaryConsumptionChance); }

	private static int requiredAmount(float chance) {
		return Math.max(1, (int)(1000 * chance / 100D));
	}

	@Override public boolean isSpecial() { return true; }
	@Override public boolean showNotification() { return true; }
	@Override public String group() { return "chromaticraft_cobble_generator"; }
	@Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
	@Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
	@Override public RecipeSerializer<? extends Recipe<CobbleGeneratorInput>> getSerializer() { return ChromaRecipeSerializers.COBBLE_GENERATOR.get(); }
	@Override public RecipeType<? extends Recipe<CobbleGeneratorInput>> getType() { return ChromaRecipeTypes.COBBLE_GENERATOR.get(); }

	private static final Codec<CrystalElement> ELEMENT_CODEC = Codec.STRING.comapFlatMap(name -> {
		try { return DataResult.success(CrystalElement.valueOf(name.toUpperCase(Locale.ROOT))); }
		catch (IllegalArgumentException e) { return DataResult.error(() -> "Unknown crystal element '" + name + "'"); }
	}, element -> element.name().toLowerCase(Locale.ROOT));

	public static final MapCodec<CobbleGeneratorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			FluidInstance.FLUID_HOLDER_CODEC.fieldOf("primary").forGetter(recipe -> recipe.primary),
			FluidInstance.FLUID_HOLDER_CODEC.fieldOf("secondary").forGetter(recipe -> recipe.secondary),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration").forGetter(recipe -> recipe.duration),
			Codec.FLOAT.fieldOf("primary_consumption_chance").forGetter(recipe -> recipe.primaryConsumptionChance),
			Codec.FLOAT.fieldOf("secondary_consumption_chance").forGetter(recipe -> recipe.secondaryConsumptionChance),
			Codec.INT.optionalFieldOf("order", 0).forGetter(recipe -> recipe.order),
			ELEMENT_CODEC.listOf().optionalFieldOf("effect_elements", List.of()).forGetter(recipe -> recipe.effectElements)
	).apply(instance, CobbleGeneratorRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, CobbleGeneratorRecipe> STREAM_CODEC = StreamCodec.of(
			CobbleGeneratorRecipe::encode, CobbleGeneratorRecipe::decode);

	private static void encode(RegistryFriendlyByteBuf buffer, CobbleGeneratorRecipe recipe) {
		FluidInstance.FLUID_HOLDER_STREAM_CODEC.encode(buffer, recipe.primary);
		FluidInstance.FLUID_HOLDER_STREAM_CODEC.encode(buffer, recipe.secondary);
		ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.result);
		buffer.writeVarInt(recipe.duration);
		buffer.writeFloat(recipe.primaryConsumptionChance);
		buffer.writeFloat(recipe.secondaryConsumptionChance);
		buffer.writeVarInt(recipe.order);
		ByteBufCodecs.collection(java.util.ArrayList::new, ByteBufCodecs.VAR_INT)
				.encode(buffer, new java.util.ArrayList<>(
						recipe.effectElements.stream().map(Enum::ordinal).toList()));
	}

	private static CobbleGeneratorRecipe decode(RegistryFriendlyByteBuf buffer) {
		Holder<Fluid> primary = FluidInstance.FLUID_HOLDER_STREAM_CODEC.decode(buffer);
		Holder<Fluid> secondary = FluidInstance.FLUID_HOLDER_STREAM_CODEC.decode(buffer);
		ItemStackTemplate result = ItemStackTemplate.STREAM_CODEC.decode(buffer);
		int duration = buffer.readVarInt();
		float primaryChance = buffer.readFloat();
		float secondaryChance = buffer.readFloat();
		int order = buffer.readVarInt();
		List<CrystalElement> elements = ByteBufCodecs.collection(java.util.ArrayList::new, ByteBufCodecs.VAR_INT)
				.decode(buffer).stream().map(CobbleGeneratorRecipe::decodeElement).toList();
		return new CobbleGeneratorRecipe(primary, secondary, result, duration,
				primaryChance, secondaryChance, order, elements);
	}

	private static CrystalElement decodeElement(int ordinal) {
		if (ordinal < 0 || ordinal >= CrystalElement.elements.length)
			throw new IllegalArgumentException("Invalid crystal-element ordinal " + ordinal);
		return CrystalElement.elements[ordinal];
	}
}
