package reika.chromaticraft.auxiliary;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import reika.chromaticraft.modinterface.SatisforestrySonar;
import reika.dragonapi.interfaces.entity.TameHostile;

import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Common, client-independent V33a sonar classification and expanding scan. */
public final class MobSonar {
    public static final int RANGE = 64;
    public static final int DURATION = 20;
    private MobSonar() {}

    public enum EntitySonarType {
        DOGGO(MusicKey.A5, 0xff7f60, 2),
        TAMEHOSTILE(MusicKey.E4, 0xffff44, 0),
        TAMEABLE(MusicKey.C6, 0xba69ff, 1),
        ANIMAL(MusicKey.G5, 0x22ff22, 0),
        MOB(MusicKey.C4, 0xff2222, 0),
        WATER(MusicKey.G4, 0x22aaff, 0),
        OTHER(MusicKey.C5, 0xffffff, 0);

        public final int baseColor;
        public final float soundPitch;
        public final int highlight;

        EntitySonarType(MusicKey key, int color, int highlight) {
            baseColor = color;
            soundPitch = (float)MusicKey.C5.getRatio(key);
            this.highlight = highlight;
        }

        public boolean enabled(int flags) { return (flags & 1 << ordinal()) != 0; }
        public static int getAllFlags() { return (1 << values().length) - 1; }

        public static EntitySonarType getFromEntity(Entity entity) {
            if (SatisforestrySonar.isDoggo(entity)) return DOGGO;
            if (entity instanceof TameHostile) return TAMEHOSTILE;
            if (entity instanceof TamableAnimal) return TAMEABLE;
            if (entity instanceof Animal) return ANIMAL;
            if (entity instanceof Monster) return MOB;
            if (entity instanceof WaterAnimal || entity instanceof AgeableWaterCreature) return WATER;
            return OTHER;
        }
    }

    /**
     * V33a expands an axis-aligned cube by 1/8 block each tick. Its distance/shell check was
     * commented out: preserve the cube (including corners), and report each mob only once.
     */
    public static final class Ping {
        public final Vec3 center;
        private final double maxRadius;
        private final Set<Integer> usedIds = new HashSet<>();
        private double radius;

        public Ping(Vec3 center, double range) {
            this.center = center;
            maxRadius = range;
        }

        public List<Mob> tick(Level level, Entity viewer) {
            radius += 0.125;
            AABB bounds = new AABB(center, center).inflate(radius);
            return level.getEntitiesOfClass(Mob.class, bounds,
                    entity -> entity != viewer && usedIds.add(entity.getId()));
        }

        public boolean finished() { return radius >= maxRadius; }
        public double radius() { return radius; }
    }
}
