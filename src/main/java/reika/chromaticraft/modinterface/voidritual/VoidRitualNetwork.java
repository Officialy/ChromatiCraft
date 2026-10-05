package reika.chromaticraft.modinterface.voidritual;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import reika.voidmonster.entity.EntityVoidMonster;

import java.util.Set;
import java.util.UUID;

public final class VoidRitualNetwork {
    private VoidRitualNetwork() {}
    public record Visual(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Visual> TYPE=new Type<>(Identifier.fromNamespaceAndPath("chromaticraft","void_ritual"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Visual> CODEC=StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,Visual::data,Visual::new);
        @Override public Type<Visual> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) { event.registrar("1").playToClient(Visual.TYPE,Visual.CODEC,(payload,context)->VoidMonsterRitualClientEffects.handle(payload.data())); }
    private static CompoundTag data(ServerLevel world,String action) { CompoundTag data=new CompoundTag(); data.putString("dimension",world.dimension().identifier().toString()); data.putString("action",action); return data; }
    public static void sync(ServerLevel world,Set<UUID> entities) {
        var data=data(world,"sync"); ListTag list=new ListTag(); entities.forEach(uuid->list.add(StringTag.valueOf(uuid.toString()))); data.put("entities",list);
        world.players().forEach(player->send(player,new Visual(data)));
    }
    public static void effect(EntityVoidMonster monster,VoidMonsterDestructionRitual.Effects effect) {
        ServerLevel world=(ServerLevel)monster.level(); var data=data(world,"effect"); data.putInt("entity",monster.getId()); data.putString("uuid",monster.getUUID().toString()); data.putInt("effect",effect.ordinal());
        sendNear(world,monster.position(),128,new Visual(data));
    }
    public static void eat(TileEntityVoidMonsterTrap trap) {
        ServerLevel world=(ServerLevel)trap.getLevel(); var data=data(world,"eat"); BlockPos pos=trap.getBlockPos(); data.putInt("x",pos.getX()); data.putInt("y",pos.getY()); data.putInt("z",pos.getZ());
        sendNear(world,net.minecraft.world.phys.Vec3.atLowerCornerOf(pos),32,new Visual(data));
    }
    private static void send(net.minecraft.server.level.ServerPlayer player,Visual packet) {
        if (player.connection!=null && player.connection.hasChannel(Visual.TYPE)) PacketDistributor.sendToPlayer(player,packet);
    }
    private static void sendNear(ServerLevel world,net.minecraft.world.phys.Vec3 center,int radius,Visual packet) {
        for (var player:world.players()) if (player.position().distanceToSqr(center)<radius*radius) send(player,packet);
    }

}
