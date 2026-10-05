package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.items.tools.ItemAuraPouch;
import reika.chromaticraft.items.tools.ItemEfficiencyCrystal;
import reika.chromaticraft.render.particle.ChromaParticle;
import reika.dragonapi.instantiable.Orbit;
import reika.dragonapi.instantiable.particlecontroller.OrbitMotionController;

/** V33a Efficiency Crystal orbit particles, moved out of the now server-only inventory tick. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class EfficiencyCrystalClient {
    private EfficiencyCrystalClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;
        for (var stack : mc.player.getInventory())
            if (stack.getItem() instanceof ItemEfficiencyCrystal) spawn(mc.player);
        for (var stack : mc.player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() instanceof ItemAuraPouch pouch) {
                var contents = pouch.getInventory(stack);
                var active = pouch.getActiveSlots(stack);
                for (int i = 0; i < contents.length; i++)
                    if (active[i] && contents[i].getItem() instanceof ItemEfficiencyCrystal) spawn(mc.player);
            }
        }
    }

    private static void spawn(Entity entity) {
        var random = entity.getRandom();
        if (random.nextInt(16) != 0) return;
        Orbit orbit = new Orbit(1+random.nextDouble()*2, random.nextDouble()*0.75,
                (random.nextDouble()*2-1)*90, random.nextDouble()*360, 0, 0);
        OrbitMotionController controller = new OrbitMotionController(orbit, entity.getX(), entity.getY(), entity.getZ())
                .trackEntity(entity);
        controller.thetaSpeed = 1.5+random.nextDouble()*3.5;
        int life = 60+random.nextInt(120);
        float scale = (float)(1+(random.nextDouble()*2-1)*0.25)*0.5F;
        ChromaParticle.spawnControlledGlow(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                0xffffff, life, scale, controller, "chroma_particle", false);
    }
}
