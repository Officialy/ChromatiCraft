package reika.chromaticraft.modinterface.voidritual;
import net.minecraft.world.entity.EntityType;
import reika.chromaticraft.entity.EntityMonsterBait;
public final class VoidBaitFactory {
    private VoidBaitFactory() {}
    @SuppressWarnings("unchecked")
    public static EntityType.EntityFactory<EntityMonsterBait> factory() {
        if (!reika.dragonapi.ModList.VOIDMONSTER.isLoaded()) return EntityMonsterBait::new;
        try { return (EntityType.EntityFactory<EntityMonsterBait>)Class.forName("reika.chromaticraft.modinterface.voidritual.VoidMonsterBaitEntity").getMethod("factory").invoke(null); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot initialize Void Monster bait integration",exception); }
    }
}
