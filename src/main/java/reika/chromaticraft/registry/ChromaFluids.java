package reika.chromaticraft.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import reika.chromaticraft.ChromatiCraft;

/** Complete 26.2 registry vertical for the V33a Liquid Chroma world fluid. */
public final class ChromaFluids {

	/**
	 * 26.2 does not automatically give an untagged custom fluid water movement. The default
	 * {@link FluidType#move} returns false and LivingEntity then deliberately performs no movement
	 * for a non-water/non-lava type, which was why Luma and Ender held players immobile. V33a's
	 * BlockFluidClassic fluids used the normal swimmable-fluid travel path, so reproduce that path
	 * here while keeping each fluid's own flow viscosity in its BaseFlowingFluid properties.
	 */
	private static final class SwimmableFluidType extends FluidType {

		private SwimmableFluidType(Properties properties) {
			super(properties);
		}

		@Override
		public boolean move(LivingEntity entity, Vec3 input, double gravity) {
			boolean falling = entity.getDeltaMovement().y <= 0;
			entity.moveRelative(0.02F, input);
			entity.move(MoverType.SELF, entity.getDeltaMovement());
			Vec3 movement = entity.getDeltaMovement();
			if (entity.horizontalCollision && entity.onClimbable())
				movement = new Vec3(movement.x, 0.2, movement.z);
			movement = movement.multiply(0.8, 0.8, 0.8);
			entity.setDeltaMovement(entity.getFluidFallingAdjustedMovement(gravity, falling, movement));
			return true;
		}
	}

	public static final DeferredRegister<Fluid> FLUIDS =
			DeferredRegister.create(BuiltInRegistries.FLUID, ChromatiCraft.MODID);
	public static final DeferredRegister<FluidType> FLUID_TYPES =
			DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ChromatiCraft.MODID);

	// V33a tuning (git show 5cde0068^:ChromatiCraft.java:274-280):
	//   chroma = new EnhancedFluid("chroma").setViscosity(300).setTemperature(288).setDensity(300).setLuminosity(15)
	//   luma   = new Fluid("luma").setViscosity(50).setDensity(1).setTemperature(250)  (no luminosity call -> 0, does not glow)
	// Both are thinner than water (viscosity 1000) and must flow faster than water's tickRate(5), not at it.
	// 1.7.10 reference: water viscosity 1000 -> tick 5, lava viscosity 6000 -> tick 30 (both overworld,
	// non-"fast lava"), i.e. tickRate scales linearly with viscosity: tickRate = 5 * viscosity / 1000.
	//   chroma: 5 * 300/1000 = 1.5  -> 2 (rounded; ~2.5x faster than water, matching viscosity 3.33x thinner)
	//   luma:   5 *  50/1000 = 0.25 -> 1 (clamped to the minimum tick delay; viscosity 20x thinner than water)
	// slopeFindDistance/levelDecreasePerBlock are left at BaseFlowingFluid's water-like defaults (4/1):
	// neither pristine BlockLiquidChroma nor BlockEtherealLuma (both plain BlockFluidClassic subclasses)
	// override spread distance or drop-per-block, so there is nothing to port here.
	// NOT ported: pristine BlockEtherealLuma calls setQuantaPerBlock(16) (double vanilla's 8 height
	// levels). NeoForge's BaseFlowingFluid/FlowingFluid hardcodes an 8-level BlockStateProperties.LEVEL
	// range with no builder or FluidType hook to widen it; replicating this would require forking
	// FlowingFluid's state definition and spread/render logic, which is out of scope here.
		public static final DeferredHolder<Fluid, FlowingFluid> LUMA = FLUIDS.register("luma",
			() -> new BaseFlowingFluid.Source(ChromaFluids.LUMA_PROPERTIES));
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_LUMA = FLUIDS.register("flowing_luma",
			() -> new BaseFlowingFluid.Flowing(ChromaFluids.LUMA_PROPERTIES));
	public static final DeferredHolder<FluidType, FluidType> LUMA_TYPE = FLUID_TYPES.register("luma",
			// V33a's low viscosity controls how readily the BlockFluidClassic spreads; its material is
			// still Material.water, so entities use water-style travel and can swim/jump back out.
			() -> new SwimmableFluidType(FluidType.Properties.create().density(1).viscosity(50)
					.temperature(250).canDrown(false).fallDistanceModifier(0.5F)));
	public static final BaseFlowingFluid.Properties LUMA_PROPERTIES =
			new BaseFlowingFluid.Properties(LUMA_TYPE, LUMA, FLOWING_LUMA)
					.bucket(() -> ChromaItems.LUMA_BUCKET.get())
					.block(() -> ChromaBlocks.LUMA.get())
					.explosionResistance(500F).tickRate(1);
public static final DeferredHolder<Fluid, FlowingFluid> CHROMA = FLUIDS.register("chroma",
			() -> new BaseFlowingFluid.Source(ChromaFluids.CHROMA_PROPERTIES));
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_CHROMA = FLUIDS.register("flowing_chroma",
			() -> new BaseFlowingFluid.Flowing(ChromaFluids.CHROMA_PROPERTIES));
	public static final DeferredHolder<FluidType, FluidType> CHROMA_TYPE = FLUID_TYPES.register("chroma",
			() -> new SwimmableFluidType(FluidType.Properties.create().density(300).viscosity(300).temperature(288).lightLevel(15)));

	public static final BaseFlowingFluid.Properties CHROMA_PROPERTIES =
			new BaseFlowingFluid.Properties(CHROMA_TYPE, CHROMA, FLOWING_CHROMA)
					.bucket(() -> ChromaItems.CHROMA_BUCKET.get())
					.block(() -> ChromaBlocks.CHROMA.get())
					.explosionResistance(500F)
					.tickRate(2);

	// V33a (git show 5cde0068^:ChromatiCraft.java:277):
	//   ender = new Fluid("ender").setViscosity(2000).setDensity(1500).setTemperature(270).setLuminosity(4)
	// Twice water's viscosity -> tickRate = 5 * 2000/1000 = 10, i.e. it creeps rather than flows.
	// BlockLiquidEnder is a plain BlockFluidClassic subclass, so slopeFindDistance and
	// levelDecreasePerBlock stay at the water-like defaults; its velocityToAddToEntity override is a
	// verbatim copy of Forge's BlockFluidBase default, so there is no custom current to carry over
	// and motionScale stays at the vanilla value.
	public static final DeferredHolder<Fluid, FlowingFluid> ENDER = FLUIDS.register("ender",
			() -> new BaseFlowingFluid.Source(ChromaFluids.ENDER_PROPERTIES));
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_ENDER = FLUIDS.register("flowing_ender",
			() -> new BaseFlowingFluid.Flowing(ChromaFluids.ENDER_PROPERTIES));
	public static final DeferredHolder<FluidType, FluidType> ENDER_TYPE = FLUID_TYPES.register("ender",
			() -> new SwimmableFluidType(FluidType.Properties.create()
					.density(1500).viscosity(2000).temperature(270).lightLevel(4)
					// V33a velocityToAddToEntity multiplies the normalized flow by
					// quantaPerBlock*4 = 8*4. NeoForge's ordinary water scale is 0.014.
					.motionScale(0.014D * 32)));
	public static final BaseFlowingFluid.Properties ENDER_PROPERTIES =
			new BaseFlowingFluid.Properties(ENDER_TYPE, ENDER, FLOWING_ENDER)
					.bucket(() -> ChromaItems.ENDER_BUCKET.get())
					.block(() -> ChromaBlocks.ENDER.get())
					.explosionResistance(500F)
					.tickRate(10);

	private ChromaFluids() {}
}
