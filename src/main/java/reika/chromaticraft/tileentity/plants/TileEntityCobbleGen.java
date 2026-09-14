/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.tileentity.plants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

import reika.chromaticraft.auxiliary.CobbleGeneratorItemExpiry;
import reika.chromaticraft.auxiliary.interfaces.OperationInterval;
import reika.chromaticraft.auxiliary.recipemanagers.CobbleGeneratorRecipe;
import reika.chromaticraft.base.tileentity.TileEntityMagicPlant;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.chromaticraft.registry.ChromaBlocks;
import reika.chromaticraft.registry.ChromaRecipeTypes;
import reika.chromaticraft.registry.ChromaTiles;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.CobbleGeneratorParticles;
import reika.chromaticraft.tileentity.auxiliary.TileEntityFunctionRelay;
import reika.dragonapi.instantiable.StepTimer;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.interfaces.block.FluidBlockSurrogate;
import reika.dragonapi.libraries.io.NBTCompat;

/** V33a Coalescence Orchid: combines two nearby source fluids into a short-lived block stack. */
public final class TileEntityCobbleGen extends TileEntityMagicPlant implements OperationInterval {
	public static final int XZ_RANGE = 4;
	public static final int Y_RANGE = 5;
	public static final int RANDOM_SCANS = 2;

	private final Map<Fluid, Set<BlockPos>> fluidLocations = new HashMap<>();
	private final StepTimer areaScan = new StepTimer(100);

	private CobbleGeneratorRecipe activeRecipe;
	private ResourceKey<Recipe<?>> activeRecipeKey;
	private int recipeTick;
	private int recipeDuration;
	private BlockPos primaryLocation;
	private BlockPos secondaryLocation;
	private Fluid syncedPrimaryFluid;
	private Fluid syncedSecondaryFluid;
	private int effectMask;
	private OutputModifier modifier;

	public TileEntityCobbleGen(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.COBBLE_GENERATOR.get(), pos, state);
	}

	@Override public ChromaTiles getTile() { return ChromaTiles.COBBLEGEN; }
	@Override public Direction getGrowthDirection() { return Direction.DOWN; }

	@Override
	protected void onFirstTick(Level world, BlockPos pos) {
		super.onFirstTick(world, pos);
		if (!world.isClientSide()) {
			areaScan.setTick(areaScan.getCap() - 1);
			this.doScan(world, pos);
		}
	}

	@Override
	protected void writeSyncTag(CompoundTag tag) {
		super.writeSyncTag(tag);
		tag.putInt("recipeTick", recipeTick);
		tag.putInt("recipeDuration", recipeDuration);
		tag.putInt("effectMask", effectMask);
		tag.putInt("modifier", modifier != null ? modifier.ordinal() : -1);
		if (activeRecipeKey != null) tag.putString("recipe", activeRecipeKey.identifier().toString());
		if (syncedPrimaryFluid != null)
			tag.putString("primaryFluid", BuiltInRegistries.FLUID.getKey(syncedPrimaryFluid).toString());
		if (syncedSecondaryFluid != null)
			tag.putString("secondaryFluid", BuiltInRegistries.FLUID.getKey(syncedSecondaryFluid).toString());
		if (primaryLocation != null) tag.putLong("primaryLocation", primaryLocation.asLong());
		if (secondaryLocation != null) tag.putLong("secondaryLocation", secondaryLocation.asLong());
	}

	@Override
	protected void readSyncTag(CompoundTag tag) {
		super.readSyncTag(tag);
		recipeTick = NBTCompat.getInt(tag, "recipeTick", 0);
		recipeDuration = NBTCompat.getInt(tag, "recipeDuration", 0);
		effectMask = NBTCompat.getInt(tag, "effectMask", 0);
		int modifierIndex = NBTCompat.getInt(tag, "modifier", -1);
		modifier = modifierIndex >= 0 && modifierIndex < OutputModifier.values().length
				? OutputModifier.values()[modifierIndex] : null;
		Identifier recipeId = Identifier.tryParse(NBTCompat.getString(tag, "recipe", ""));
		activeRecipeKey = recipeId != null ? ResourceKey.create(Registries.RECIPE, recipeId) : null;
		syncedPrimaryFluid = fluid(tag, "primaryFluid");
		syncedSecondaryFluid = fluid(tag, "secondaryFluid");
		primaryLocation = tag.contains("primaryLocation")
				? BlockPos.of(NBTCompat.getLong(tag, "primaryLocation", 0)) : null;
		secondaryLocation = tag.contains("secondaryLocation")
				? BlockPos.of(NBTCompat.getLong(tag, "secondaryLocation", 0)) : null;
		activeRecipe = null;
	}

	private static Fluid fluid(CompoundTag tag, String key) {
		Identifier id = Identifier.tryParse(NBTCompat.getString(tag, key, ""));
		return id != null ? BuiltInRegistries.FLUID.getOptional(id).orElse(null) : null;
	}

	@Override
	public void updateEntity(Level world, BlockPos pos) {
		if (world.isClientSide()) {
			this.doParticles(world, pos);
			return;
		}
		if (!(world instanceof ServerLevel server)) return;
		this.resolveActiveRecipe(server);
		this.doScan(world, pos);
		if (activeRecipe != null && recipeTick > 0) this.doRecipeTick(server, pos);
		else if (recipeTick <= 0) this.tryStartRecipe(server);
	}

	private void resolveActiveRecipe(ServerLevel world) {
		if (activeRecipe != null || activeRecipeKey == null) return;
		for (RecipeHolder<CobbleGeneratorRecipe> holder : recipes(world)) {
			if (holder.id().equals(activeRecipeKey)) {
				activeRecipe = holder.value();
				return;
			}
		}
		this.clearRecipe();
	}

	private void tryStartRecipe(ServerLevel world) {
		for (RecipeHolder<CobbleGeneratorRecipe> holder : recipes(world)) {
			CobbleGeneratorRecipe recipe = holder.value();
			BlockPos primary = this.getFluid(world, recipe.primaryFluid());
			BlockPos secondary = this.getFluid(world, recipe.secondaryFluid());
			if (primary == null || secondary == null) continue;
			activeRecipe = recipe;
			activeRecipeKey = holder.id();
			recipeTick = recipe.duration() / 3;
			recipeDuration = recipe.duration();
			primaryLocation = primary;
			secondaryLocation = secondary;
			syncedPrimaryFluid = recipe.primaryFluid();
			syncedSecondaryFluid = recipe.secondaryFluid();
			effectMask = recipe.effectElements().stream().mapToInt(element -> 1 << element.ordinal())
					.reduce(0, (left, right) -> left | right);
			this.setChanged();
			this.syncAllData(false);
			return;
		}
	}

	private static List<RecipeHolder<CobbleGeneratorRecipe>> recipes(ServerLevel world) {
		ArrayList<RecipeHolder<CobbleGeneratorRecipe>> recipes = new ArrayList<>(world.getServer()
				.getRecipeManager().recipeMap().byType(ChromaRecipeTypes.COBBLE_GENERATOR.get()));
		recipes.sort(Comparator.comparingInt((RecipeHolder<CobbleGeneratorRecipe> holder) -> holder.value().order())
				.thenComparing(holder -> holder.id().identifier().toString()));
		return recipes;
	}

	private void doRecipeTick(ServerLevel world, BlockPos pos) {
		recipeTick--;
		if (recipeTick == 0) {
			this.craft(world, pos);
			return;
		}
		FluidProbe primary = this.getFluidAtBlock(world, primaryLocation);
		FluidProbe secondary = this.getFluidAtBlock(world, secondaryLocation);
		if (primary == null || primary.fluid() != activeRecipe.primaryFluid()
				|| primary.amount() < activeRecipe.requiredPrimaryAmount()
				|| secondary == null || secondary.fluid() != activeRecipe.secondaryFluid()
				|| secondary.amount() < activeRecipe.requiredSecondaryAmount())
			this.terminateCrafting(world, pos, false);
	}

	private void craft(ServerLevel world, BlockPos pos) {
		int count = Math.min(64, 1 << Math.min(6, this.getAccelerationPlants()));
		ItemStack output = activeRecipe.result();
		output.setCount(count);
		if (modifier != null) output = modifier.getOutput(output);
		ItemEntity item = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.125,
				pos.getZ() + 0.5, output, 0, 0, 0);
		CobbleGeneratorItemExpiry.mark(item);
		world.addFreshEntity(item);

		this.consume(world, primaryLocation, activeRecipe.primaryFluid(),
				activeRecipe.primaryConsumptionChance(), activeRecipe.requiredPrimaryAmount());
		this.consume(world, secondaryLocation, activeRecipe.secondaryFluid(),
				activeRecipe.secondaryConsumptionChance(), activeRecipe.requiredSecondaryAmount());
		world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.75F, 2F);
		this.terminateCrafting(world, pos, true);
	}

	private void consume(ServerLevel world, BlockPos pos, Fluid expected, float chance,
			int requiredAmount) {
		if (chance <= 0 || pos == null || !world.hasChunkAt(pos)) return;
		BlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof FluidBlockSurrogate surrogate) {
			if (surrogate.supportsQuantization(world, pos))
				surrogate.drain(world, pos, expected, requiredAmount, true);
			else if (roll(world, chance)) surrogate.drain(world, pos, expected, FluidType.BUCKET_VOLUME, true);
		}
		else if (roll(world, chance)) world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	private static boolean roll(Level world, float chance) {
		return world.getRandom().nextFloat() * 100 < chance;
	}

	private void terminateCrafting(ServerLevel world, BlockPos pos, boolean success) {
		int element = this.randomEffectElement(world);
		world.blockEvent(pos, this.getBlockState().getBlock(), 1,
				((element + 1) << 1) | (success ? 1 : 0));
		this.clearRecipe();
		this.setChanged();
		this.syncAllData(false);
	}

	private int randomEffectElement(Level world) {
		if (effectMask == 0) return -1;
		List<Integer> elements = new ArrayList<>();
		for (int i = 0; i < CrystalElement.elements.length; i++)
			if ((effectMask & 1 << i) != 0) elements.add(i);
		return elements.get(world.getRandom().nextInt(elements.size()));
	}

	private void clearRecipe() {
		activeRecipe = null;
		activeRecipeKey = null;
		recipeTick = 0;
		recipeDuration = 0;
		primaryLocation = null;
		secondaryLocation = null;
		syncedPrimaryFluid = null;
		syncedSecondaryFluid = null;
		effectMask = 0;
	}

	@Override
	public boolean triggerEvent(int id, int value) {
		if (id == 1 && level != null && level.isClientSide()) {
			int elementIndex = (value >> 1) - 1;
			CrystalElement element = elementIndex >= 0 && elementIndex < CrystalElement.elements.length
					? CrystalElement.elements[elementIndex] : null;
			CobbleGeneratorParticles.spawnEnd(level, worldPosition, element, (value & 1) != 0);
			return true;
		}
		return super.triggerEvent(id, value);
	}

	private void doParticles(Level world, BlockPos pos) {
		if (modifier != null) CobbleGeneratorParticles.spawnModifier(world, pos, this.getTicksExisted());
		if (recipeTick > 0 && syncedPrimaryFluid != null && syncedSecondaryFluid != null
				&& primaryLocation != null && secondaryLocation != null)
			CobbleGeneratorParticles.spawnWorking(world, pos, primaryLocation, secondaryLocation,
					syncedPrimaryFluid, syncedSecondaryFluid, effectMask, world.getRandom());
	}

	private BlockPos getFluid(Level world, Fluid fluid) {
		Collection<BlockPos> locations = fluidLocations.getOrDefault(fluid, Set.of());
		Iterator<BlockPos> iterator = locations.iterator();
		while (iterator.hasNext()) {
			BlockPos pos = iterator.next();
			FluidProbe probe = this.getFluidAtBlock(world, pos);
			if (probe != null && probe.fluid() == fluid) return pos;
			iterator.remove();
		}
		if (locations.isEmpty()) areaScan.setTick(areaScan.getCap() - 1);
		return null;
	}

	private void doScan(Level world, BlockPos pos) {
		areaScan.update();
		if (areaScan.checkCap()) {
			fluidLocations.clear();
			for (int offsetX = -XZ_RANGE; offsetX <= XZ_RANGE; offsetX++)
				for (int offsetZ = -XZ_RANGE; offsetZ <= XZ_RANGE; offsetZ++)
					this.scanPosition(world, pos.offset(offsetX, 0, offsetZ), true);
		}
		else {
			for (int i = 0; i < RANDOM_SCANS; i++) {
				BlockPos column = pos.offset(world.getRandom().nextInt(XZ_RANGE * 2 + 1) - XZ_RANGE,
						0, world.getRandom().nextInt(XZ_RANGE * 2 + 1) - XZ_RANGE);
				this.scanPosition(world, column, false);
			}
		}
		OutputModifier found = this.getModifier(world, pos);
		if (found != modifier) {
			modifier = found;
			this.setChanged();
			this.syncAllData(false);
		}
	}

	private OutputModifier getModifier(Level world, BlockPos pos) {
		for (int distance = 1; distance < Y_RANGE; distance++) {
			BlockPos target = pos.below(distance);
			if (!world.hasChunkAt(target)) return null;
			BlockState state = world.getBlockState(target);
			if (state.is(ChromaBlocks.HEAT_LILY.get())) return OutputModifier.COBBLE_SMELT;
			if (state.isSolidRender()) return null;
		}
		return null;
	}

	private void scanPosition(Level world, BlockPos column, boolean all) {
		BlockPos endpoint = this.getYPosition(world, column);
		if (endpoint == null) return;
		if (world.getBlockEntity(endpoint) instanceof TileEntityFunctionRelay relay) {
			if (all) {
				for (Coordinate coordinate : relay.getCoordinates()) this.addFluid(world, coordinate.asBlockPos());
				return;
			}
			Coordinate coordinate = relay.getRandomCoordinate();
			if (coordinate == null) return;
			endpoint = coordinate.asBlockPos();
		}
		this.addFluid(world, endpoint);
	}

	private void addFluid(Level world, BlockPos pos) {
		FluidProbe probe = this.getFluidAtBlock(world, pos);
		if (probe != null) fluidLocations.computeIfAbsent(probe.fluid(), fluid -> new HashSet<>())
				.add(pos.immutable());
	}

	private FluidProbe getFluidAtBlock(Level world, BlockPos pos) {
		if (pos == null || !world.hasChunkAt(pos)) return null;
		BlockState state = world.getBlockState(pos);
		FluidState fluidState = state.getFluidState();
		if (!fluidState.isEmpty() && fluidState.isSource())
			return new FluidProbe(fluidState.getType(), FluidType.BUCKET_VOLUME);
		if (state.getBlock() instanceof FluidBlockSurrogate surrogate) {
			Fluid fluid = surrogate.getFluid(world, pos);
			if (fluid != null) return new FluidProbe(fluid,
					surrogate.drain(world, pos, fluid, FluidType.BUCKET_VOLUME, false));
		}
		return null;
	}

	private BlockPos getYPosition(Level world, BlockPos column) {
		if (!world.hasChunkAt(column)) return null;
		BlockPos.MutableBlockPos cursor = column.mutable();
		BlockState state = world.getBlockState(cursor);
		if (!state.isAir() && !state.is(ChromaBlocks.COBBLE_GENERATOR.get())) return null;
		int distance = 0;
		while (cursor.getY() > world.getMinY()
				&& (state.isAir() || state.is(ChromaBlocks.COBBLE_GENERATOR.get()))) {
			cursor.move(Direction.DOWN);
			distance++;
			state = world.getBlockState(cursor);
		}
		return distance <= Y_RANGE ? cursor.immutable() : null;
	}

	@Override protected void animateWithTick(Level world, BlockPos pos) {
	}

	@Override
	public float getOperationFraction() {
		return activeRecipeKey == null || recipeDuration <= 0 ? 0 : recipeTick / (float)recipeDuration;
	}

	@Override public OperationState getState() {
		return activeRecipeKey != null ? OperationState.RUNNING : OperationState.INVALID;
	}

	@Override
	public boolean isPlantable(Level world, BlockPos pos) {
		BlockPos above = pos.above();
		BlockState state = world.getBlockState(above);
		return state.is(ChromaBlocks.PLANT_ACCELERATOR.get())
				|| state.isCollisionShapeFullBlock(world, above) && state.isSolidRender();
	}

	public boolean hasWork() { return this.getState() == OperationState.RUNNING; }

	private record FluidProbe(Fluid fluid, int amount) {}

	private enum OutputModifier {
		COBBLE_SMELT;

		private ItemStack getOutput(ItemStack input) {
			return input.is(Blocks.COBBLESTONE.asItem())
					? new ItemStack(Blocks.STONE, input.getCount()) : input;
		}
	}
}
