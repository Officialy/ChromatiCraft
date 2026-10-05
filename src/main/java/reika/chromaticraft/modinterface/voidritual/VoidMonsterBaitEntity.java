package reika.chromaticraft.modinterface.voidritual;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import reika.chromaticraft.entity.EntityMonsterBait;
/** Isolated optional interface: ordinary ChromatiCraft bait also works without Void Monster. */
public final class VoidMonsterBaitEntity extends EntityMonsterBait implements reika.voidmonster.auxiliary.VoidMonsterBait {
    public VoidMonsterBaitEntity(EntityType<? extends EntityMonsterBait> type, Level level) { super(type,level); }
    public static EntityType.EntityFactory<EntityMonsterBait> factory() { return VoidMonsterBaitEntity::new; }
}
