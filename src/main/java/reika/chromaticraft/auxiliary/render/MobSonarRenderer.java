/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.auxiliary.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import reika.dragonapi.client.ScreenParticleRendering;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.MobSonar;
import reika.chromaticraft.auxiliary.MobSonar.EntitySonarType;
import reika.chromaticraft.items.tools.wands.ItemMobSonar;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.render.ChromaRenderPipelines;
import reika.chromaticraft.client.render.IrisCompatibility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** V33a's twenty-tick, through-terrain sonar highlights, extracted for the native 26.3 world pass. */
@EventBusSubscriber(modid = ChromatiCraft.MODID, value = Dist.CLIENT)
public final class MobSonarRenderer {
    public static final MobSonarRenderer instance = new MobSonarRenderer();
    private static final ContextKey<List<Highlight>> FRAME = new ContextKey<>(
            Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "mob_sonar"));
    private final Map<Integer, Highlight> coordinates = new LinkedHashMap<>();
    private final List<MobSonar.Ping> pings = new ArrayList<>();
    private Object lastLevel;

    private MobSonarRenderer() {}
    private record Highlight(AABB box, EntitySonarType type, int ticks) {}

    public void addCoordinate(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof ItemMobSonar)) return;
        EntitySonarType type = EntitySonarType.getFromEntity(entity);
        if (!type.enabled(ItemMobSonar.getTypeFlags(mc.player.getMainHandItem()))) return;
        coordinates.put(entity.getId(), new Highlight(entity.getBoundingBox(), type, MobSonar.DURATION));
        ChromaSounds.DING.playSound(entity, 1, type.soundPitch);
        ChromaSounds.DING.playSound(mc.player, 0.2F, type.soundPitch);
    }

    public void addPing(Player player, int range) {
        resetForLevel(player.level());
        pings.add(new MobSonar.Ping(player.position(), range));
    }

    public void addSpawnPoint(BlockPos pos, Identifier typeId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        resetForLevel(mc.level);
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
        if (type == null) return;
        Entity doggo = type.create(mc.level, EntitySpawnReason.LOAD);
        if (doggo == null) return;
        doggo.setPos(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5);
        addCoordinate(doggo);
    }

    private void resetForLevel(Object level) {
        if (level != lastLevel) {
            coordinates.clear();
            pings.clear();
            lastLevel = level;
        }
    }

    // V33a listened to both phases without filtering: twenty decay steps are ten game ticks,
    // and the 512-step scan reaches 64 blocks in 256 game ticks. Keep that effective timing.
    @SubscribeEvent public static void tickPre(ClientTickEvent.Pre event) { tick(); }
    @SubscribeEvent public static void tickPost(ClientTickEvent.Post event) { tick(); }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        instance.resetForLevel(mc.level);
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        instance.coordinates.replaceAll((id, old) -> new Highlight(old.box(), old.type(), old.ticks()-1));
        instance.coordinates.values().removeIf(value -> value.ticks() <= 0);
        instance.pings.removeIf(ping -> {
            ping.tick(mc.level, mc.player).forEach(instance::addCoordinate);
            return ping.finished();
        });
    }

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        instance.resetForLevel(Minecraft.getInstance().level);
        event.getRenderState().setRenderData(FRAME, List.copyOf(instance.coordinates.values()));
    }

    @SubscribeEvent
    public static void submit(SubmitCustomGeometryEvent event) {
        if (IrisCompatibility.shadowPass()) return;
        List<Highlight> frame = event.getLevelRenderState().getRenderData(FRAME);
        if (frame == null || frame.isEmpty()) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        var geometry = new CustomFeatureRenderer.Submit(event.getPoseStack().last().copy(),
                ChromaRenderPipelines.mobSonarLines(), (pose, vertices) -> {
                    for (Highlight point : frame) {
                        float brightness = (float)point.ticks()/MobSonar.DURATION;
                        int rgb = point.type().baseColor;
                        int red = (int)((rgb >> 16 & 255)*brightness);
                        int green = (int)((rgb >> 8 & 255)*brightness);
                        int blue = (int)((rgb & 255)*brightness);
                        for (int i=0; i<=point.type().highlight; i++)
                            box(pose, vertices, point.box().inflate(i/8D).move(-camera.x,-camera.y,-camera.z), red,green,blue);
                    }
                });
        event.getSubmitNodeCollector().order(1).submitSpecial(ScreenParticleRendering.PHASE, geometry);
    }

    private static void box(PoseStack.Pose pose, VertexConsumer vertices, AABB box, int red, int green, int blue) {
        for (int axis=0;axis<3;axis++) for (int a=0;a<2;a++) for (int b=0;b<2;b++) {
            double[] first={box.minX,box.minY,box.minZ}, last={box.minX,box.minY,box.minZ};
            double[] max={box.maxX,box.maxY,box.maxZ};
            int u=(axis+1)%3, v=(axis+2)%3;
            if (a==1) first[u]=last[u]=max[u];
            if (b==1) first[v]=last[v]=max[v];
            last[axis]=max[axis];
            float nx=axis==0?1:0, ny=axis==1?1:0, nz=axis==2?1:0;
            vertices.addVertex(pose,(float)first[0],(float)first[1],(float)first[2])
                    .setColor(red,green,blue,255).setNormal(pose,nx,ny,nz).setLineWidth(1.5F);
            vertices.addVertex(pose,(float)last[0],(float)last[1],(float)last[2])
                    .setColor(red,green,blue,255).setNormal(pose,nx,ny,nz).setLineWidth(1.5F);
        }
        // Two upper corner edges were submitted twice in V33a: retain their additive overdraw.
        edge(pose,vertices,box.maxX,box.maxY,box.maxZ,box.maxX,box.minY,box.maxZ,red,green,blue,0,1,0);
        edge(pose,vertices,box.maxX,box.maxY,box.maxZ,box.maxX,box.maxY,box.minZ,red,green,blue,0,0,1);
    }

    private static void edge(PoseStack.Pose pose, VertexConsumer vertices, double x1,double y1,double z1,
            double x2,double y2,double z2,int r,int g,int b,float nx,float ny,float nz) {
        vertices.addVertex(pose,(float)x1,(float)y1,(float)z1).setColor(r,g,b,255).setNormal(pose,nx,ny,nz).setLineWidth(1.5F);
        vertices.addVertex(pose,(float)x2,(float)y2,(float)z2).setColor(r,g,b,255).setNormal(pose,nx,ny,nz).setLineWidth(1.5F);
    }
}
