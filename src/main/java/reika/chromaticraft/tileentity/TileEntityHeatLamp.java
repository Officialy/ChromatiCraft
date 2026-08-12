package reika.chromaticraft.tileentity;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import reika.chromaticraft.block.BlockHeatLamp;
import reika.chromaticraft.container.MenuHeatLamp;
import reika.chromaticraft.registry.ChromaBlockEntities;
import reika.dragonapi.interfaces.blockentity.ThermalTile;

/** Persistent temperature controller and source-faithful thermal target loop. */
public final class TileEntityHeatLamp extends BlockEntity implements MenuProvider {

	public static final int MAXTEMP = 615;
	public static final int MAXTEMP_COLD = 15;
	public static final int MINTEMP = 20;
	public static final int MINTEMP_COLD = -60;

	private int temperature;
	private int furnaceAssistProgress;

	public TileEntityHeatLamp(BlockPos pos, BlockState state) {
		super(ChromaBlockEntities.HEAT_LAMP.get(), pos, state);
		temperature = isCold() ? MINTEMP_COLD : MINTEMP;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityHeatLamp lamp) {
		lamp.setTemperature(lamp.temperature);
		BlockPos targetPos = pos.relative(state.getValue(BlockHeatLamp.FACING).getOpposite());
		BlockEntity target = level.getBlockEntity(targetPos);
		if (target instanceof ThermalTile thermal && lamp.canHeat(target)) {
			int targetTemperature = thermal.getTemperature();
			if (!lamp.isCold() && lamp.temperature > targetTemperature)
				thermal.setTemperature(targetTemperature + 1);
			else if (lamp.isCold() && lamp.temperature < targetTemperature)
				thermal.setTemperature(targetTemperature - 1);
		}
		if (!lamp.isCold() && target instanceof AbstractFurnaceBlockEntity furnace
				&& level instanceof ServerLevel server)
			lamp.tickFurnace(server, furnace);
		else
			lamp.furnaceAssistProgress = 0;
	}

	private boolean canHeat(BlockEntity target) {
		// V33a deliberately excludes ReactorCraft reactor-core tiles from generic external heating.
		return !(target instanceof reika.reactorcraft.auxiliary.ReactorCoreTE);
	}

	private void tickFurnace(ServerLevel level, AbstractFurnaceBlockEntity furnace) {
		if (temperature < 200 || furnace.getItem(0).isEmpty()) {
			furnaceAssistProgress = Math.min(Math.max(0, temperature), 199);
			return;
		}
		double chance = Math.min(1, 1.25 * temperature / 1000D);
		if (level.getRandom().nextDouble() >= chance) return;
		SingleRecipeInput input = new SingleRecipeInput(furnace.getItem(0));
		Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getServer().getRecipeManager()
				.getRecipeFor(RecipeType.SMELTING, input, level);
		if (recipe.isEmpty()) {
			furnaceAssistProgress = 0;
			return;
		}
		ItemStack result = recipe.get().value().assemble(input).copy();
		ItemStack output = furnace.getItem(2);
		if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
				|| output.getCount() + result.getCount() > Math.min(output.getMaxStackSize(), furnace.getMaxStackSize())))
			return;
		if (++furnaceAssistProgress < recipe.get().value().cookingTime()) return;
		furnaceAssistProgress = 0;
		ItemStack source = furnace.getItem(0);
		source.shrink(1);
		furnace.setItem(0, source.isEmpty() ? ItemStack.EMPTY : source);
		if (output.isEmpty()) furnace.setItem(2, result);
		else output.grow(result.getCount());
		furnace.setChanged();
	}

	public boolean isCold() {
		return getBlockState().getBlock() instanceof BlockHeatLamp lamp && lamp.isCold();
	}

	public int getTemperature() { return temperature; }

	public void setTemperature(int value) {
		int clamped = Mth.clamp(value, isCold() ? MINTEMP_COLD : MINTEMP,
				isCold() ? MAXTEMP_COLD : MAXTEMP);
		if (temperature != clamped) {
			temperature = clamped;
			setChanged();
		}
	}

	@Override public Component getDisplayName() {
		return Component.translatable(isCold() ? "block.chromaticraft.cold_lamp" : "block.chromaticraft.heat_lamp");
	}

	@Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MenuHeatLamp(id, inventory, this);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("temperature", temperature);
		output.putInt("furnace_progress", furnaceAssistProgress);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		temperature = input.getIntOr("temperature", isCold() ? MINTEMP_COLD : MINTEMP);
		furnaceAssistProgress = Math.max(0, input.getIntOr("furnace_progress", 0));
	}
}
