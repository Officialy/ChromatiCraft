package reika.chromaticraft.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import reika.chromaticraft.registry.CrystalElement;

/** Common altar effect transport; client particle implementations are registered only on the client. */
public final class ColoredAltarNetwork {
    private ColoredAltarNetwork() {}

    public record Visual(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Visual> TYPE = new Type<>(Identifier.fromNamespaceAndPath("chromaticraft", "altar_visual"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Visual> CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Visual::data, Visual::new);
        public Type<Visual> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Visual.TYPE, Visual.CODEC);
    }

    public static void send(ServerLevel level, BlockPos pos, Vec3 target, CrystalElement element, String action) {
        CompoundTag data = new CompoundTag();
        data.putString("dimension", level.dimension().identifier().toString()); data.putLong("pos", pos.asLong());
        data.putDouble("x", target.x); data.putDouble("y", target.y); data.putDouble("z", target.z);
        data.putInt("color", element.ordinal()); data.putString("action", action);
        for (var player : level.players())
            if (player.connection != null && player.connection.hasChannel(Visual.TYPE)
                    && player.position().distanceToSqr(Vec3.atLowerCornerOf(pos)) < 64 * 64)
                PacketDistributor.sendToPlayer(player, new Visual(data));
    }
}
