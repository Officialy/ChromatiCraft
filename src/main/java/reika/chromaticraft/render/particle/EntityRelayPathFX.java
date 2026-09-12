/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render.particle;

import java.util.List;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.registry.CrystalElement;

public final class EntityRelayPathFX extends ChromaParticle {

    private static final String[] ICONS = {"bigflare", "rotating_flare_pulse", "blurflare2", "flare7"};
    private final List<BlockPos> targets;
    private final boolean steer;
    private int targetIndex;

    public EntityRelayPathFX(ClientLevel level, CrystalElement color, BlockPos source, BlockPos target) {
        this(level, color, List.of(source, target), false);
        this.xd = (target.getX() - source.getX()) * 0.1;
        this.yd = (target.getY() - source.getY()) * 0.1;
        this.zd = (target.getZ() - source.getZ()) * 0.1;
    }

    public EntityRelayPathFX(ClientLevel level, CrystalElement color, List<BlockPos> path) {
        this(level, color, path, true);
    }

    private EntityRelayPathFX(ClientLevel level, CrystalElement color, List<BlockPos> path, boolean steer) {
        super(level, path.getFirst().getX() + 0.5, path.getFirst().getY() + 0.5,
                path.getFirst().getZ() + 0.5, ICONS[level.getRandom().nextInt(ICONS.length)], true);
        if (path.size() < 2) throw new IllegalArgumentException("A relay particle needs two endpoints");
        this.targets = path.subList(1, path.size()).stream().map(BlockPos::immutable).toList();
        this.steer = steer;
        this.lifetime = Integer.MAX_VALUE;
        this.quadSize = 0.1F * (2.5F + this.random.nextFloat() * 1.5F);
        this.gravity = 0;
        this.friction = 0.98F;
        this.xd = this.yd = this.zd = 0;
        this.setRgb(color.getColor());
    }

    @Override
    public void tick() {
        super.tick();
        this.updateTarget();
        if (this.xd * this.xd + this.yd * this.yd + this.zd * this.zd < 0.125 * 0.125)
            this.remove();
    }

    private void updateTarget() {
        Vec3 delta = Vec3.atCenterOf(this.targets.get(this.targetIndex)).subtract(this.x, this.y, this.z);
        double distance = delta.length();
        if (distance < 0.125) {
            if (++this.targetIndex == this.targets.size()) this.remove();
            else this.updateTarget();
        }
        else if (this.steer) {
            double speed = 0.5 + 0.125 * Math.sin(this.hashCode());
            this.xd = speed * delta.x / distance;
            this.yd = speed * delta.y / distance;
            this.zd = speed * delta.z / distance;
        }
    }
}
