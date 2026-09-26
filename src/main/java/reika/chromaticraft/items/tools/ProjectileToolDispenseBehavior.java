package reika.chromaticraft.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import reika.chromaticraft.api.interfaces.ProjectileFiringTool;

/**
 * V33a {@code ToolDispenserHandlers.ProjectileToolDispenserAction}: a projectile tool in a dispenser fires as if held
 * by a fake player standing in the dispenser, its eyes at the dispenser's centre, facing the way it points (level
 * aim; up and down are unaimed, as V33a set only the yaw). The tool is kept.
 *
 * <p>CHROMA-PORT: the same V33a class also held the Manipulator's (item stand / aura infuser click) and the Chroma
 * bucket's dispenser actions; they land with those interactions' ports.
 */
public final class ProjectileToolDispenseBehavior extends DefaultDispenseItemBehavior {

	public static final ProjectileToolDispenseBehavior INSTANCE = new ProjectileToolDispenseBehavior();

	private ProjectileToolDispenseBehavior() {}

	public static void register(net.minecraft.world.level.ItemLike item) {
		DispenserBlock.registerBehavior(item, INSTANCE);
	}

	@Override
	protected ItemStack execute(BlockSource ref, ItemStack is) {
		if (is.getItem() instanceof ProjectileFiringTool tool) {
			ServerLevel world = ref.level();
			BlockPos pos = ref.pos();
			Direction dir = ref.state().getValue(DispenserBlock.FACING);
			FakePlayer ep = FakePlayerFactory.getMinecraft(world);
			float yaw = dir.getAxis().isHorizontal() ? dir.toYRot() : 0;
			ep.snapTo(pos.getX()+0.5, pos.getY()+0.5-1.62, pos.getZ()+0.5, yaw, 0);
			tool.fire(is, world, ep, false);
		}
		return is;
	}

}
