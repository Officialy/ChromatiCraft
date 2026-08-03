package reika.chromaticraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import reika.chromaticraft.client.ClientPayloadHandlers;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.client.PylonAttackOverlay;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.render.particle.ChromaParticle;

public final class ChromaNetwork {

	private ChromaNetwork() {}

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(ChromatiCraft.MODID).versioned("1");
		registrar.playToClient(AttackBeam.TYPE, AttackBeam.CODEC, ChromaNetwork::handleAttackBeam);
		registrar.playToClient(Discharge.TYPE, Discharge.CODEC, ChromaNetwork::handleDischarge);
		registrar.playToClient(AttackReceive.TYPE, AttackReceive.CODEC, ChromaNetwork::handleAttackReceive);
		registrar.playToClient(JarRejection.TYPE, JarRejection.CODEC, ChromaNetwork::handleJarRejection);
		registrar.playToClient(PowerCrystalDestroy.TYPE, PowerCrystalDestroy.CODEC, ChromaNetwork::handlePowerCrystalDestroy);
		registrar.playToClient(PylonCrystalBreak.TYPE, PylonCrystalBreak.CODEC, ChromaNetwork::handlePylonCrystalBreak);
		registrar.playToClient(RepeaterConnections.TYPE, RepeaterConnections.CODEC, ChromaNetwork::handleRepeaterConnections);
		registrar.playToClient(ProgressionNote.TYPE, ProgressionNote.CODEC, ChromaNetwork::handleProgressionNote);
	}

	public static void sendAttack(ServerLevel level, BlockPos source, LivingEntity target, CrystalElement color, float size) {
		BlockPos end = BlockPos.containing(target.getX(), target.getY() + target.getBbHeight() * 0.65, target.getZ());
		PacketDistributor.sendToPlayersNear(level, null, source.getX() + 0.5, source.getY() + 0.5,
				source.getZ() + 0.5, 128, new AttackBeam(source, end, color.ordinal()));
		if (target instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new AttackReceive(color.ordinal()));
	}

	public static void sendPowerCrystalDestroy(ServerLevel level, BlockPos crystal) {
		PacketDistributor.sendToPlayersNear(level, null, crystal.getX() + 0.5, crystal.getY() + 0.5,
				crystal.getZ() + 0.5, 32, new PowerCrystalDestroy(crystal));
	}

	public static void sendPylonCrystalBreak(ServerLevel level, BlockPos pylon, CrystalElement color) {
		PacketDistributor.sendToPlayersNear(level, null, pylon.getX() + 0.5, pylon.getY() + 0.5,
				pylon.getZ() + 0.5, 64, new PylonCrystalBreak(pylon, color.ordinal()));
	}
	public static void sendRepeaterConnections(ServerLevel level, BlockPos repeater) {
		PacketDistributor.sendToPlayersNear(level, null, repeater.getX() + 0.5, repeater.getY() + 0.5,
				repeater.getZ() + 0.5, 128, new RepeaterConnections(repeater));
	}
	public static void sendJarRejection(ServerLevel level, BlockPos pylon, CrystalElement color) {
		PacketDistributor.sendToPlayersNear(level, null, pylon.getX() + 0.5, pylon.getY() + 0.5,
				pylon.getZ() + 0.5, 128, new JarRejection(pylon, color.ordinal()));
	}

	public record AttackBeam(BlockPos source, BlockPos target, int color) implements CustomPacketPayload {
		public static final Type<AttackBeam> TYPE = createType("pylon_attack");
		public static final StreamCodec<ByteBuf, AttackBeam> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, AttackBeam::source, BlockPos.STREAM_CODEC, AttackBeam::target,
				ByteBufCodecs.VAR_INT, AttackBeam::color, AttackBeam::new);
		@Override public Type<AttackBeam> type() { return TYPE; }
	}

	public record Discharge(BlockPos source, int targetId, int color, float size) implements CustomPacketPayload {
		public static final Type<Discharge> TYPE = createType("pylon_discharge");
		public static final StreamCodec<ByteBuf, Discharge> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Discharge::source, ByteBufCodecs.VAR_INT, Discharge::targetId,
				ByteBufCodecs.VAR_INT, Discharge::color, ByteBufCodecs.FLOAT, Discharge::size, Discharge::new);
		@Override public Type<Discharge> type() { return TYPE; }
	}

	public record AttackReceive(int color) implements CustomPacketPayload {
		public static final Type<AttackReceive> TYPE = createType("pylon_attack_receive");
		public static final StreamCodec<ByteBuf, AttackReceive> CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_INT, AttackReceive::color, AttackReceive::new);
		@Override public Type<AttackReceive> type() { return TYPE; }
	}

	/**
	 * V33a fires this whenever a player gains a progression stage; the client answers with
	 * ChromaSounds.GAINPROGRESS. Without it a granted stage is completely silent, so there is no way
	 * to tell a working trigger from a broken one.
	 */
	public record ProgressionNote(int stage) implements CustomPacketPayload {
		public static final Type<ProgressionNote> TYPE = createType("progression_note");
		public static final StreamCodec<ByteBuf, ProgressionNote> CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_INT, ProgressionNote::stage, ProgressionNote::new);
		@Override public Type<ProgressionNote> type() { return TYPE; }
	}

	public record JarRejection(BlockPos source, int color) implements CustomPacketPayload {
		public static final Type<JarRejection> TYPE = createType("pylon_jar_rejection");
		public static final StreamCodec<ByteBuf, JarRejection> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, JarRejection::source, ByteBufCodecs.VAR_INT, JarRejection::color,
				JarRejection::new);
		@Override public Type<JarRejection> type() { return TYPE; }
	}

	public record PowerCrystalDestroy(BlockPos source) implements CustomPacketPayload {
		public static final Type<PowerCrystalDestroy> TYPE = createType("power_crystal_destroy");
		public static final StreamCodec<ByteBuf, PowerCrystalDestroy> CODEC =
				StreamCodec.composite(BlockPos.STREAM_CODEC, PowerCrystalDestroy::source, PowerCrystalDestroy::new);
		@Override public Type<PowerCrystalDestroy> type() { return TYPE; }
	}

	public record PylonCrystalBreak(BlockPos source, int color) implements CustomPacketPayload {
		public static final Type<PylonCrystalBreak> TYPE = createType("pylon_crystal_break");
		public static final StreamCodec<ByteBuf, PylonCrystalBreak> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, PylonCrystalBreak::source, ByteBufCodecs.VAR_INT,
				PylonCrystalBreak::color, PylonCrystalBreak::new);
		@Override public Type<PylonCrystalBreak> type() { return TYPE; }
	}
	public record RepeaterConnections(BlockPos source) implements CustomPacketPayload {
		public static final Type<RepeaterConnections> TYPE = createType("repeater_connections");
		public static final StreamCodec<ByteBuf, RepeaterConnections> CODEC =
				StreamCodec.composite(BlockPos.STREAM_CODEC, RepeaterConnections::source, RepeaterConnections::new);
		@Override public Type<RepeaterConnections> type() { return TYPE; }
	}
	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createType(String path) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, path));
	}

	// Every body below is a one-line hand-off. The lambdas must not touch Minecraft here: they are
	// compiled into synthetic methods of THIS class, which the dedicated server loads and verifies
	// during mod construction, and a ClientLevel/LocalPlayer descriptor in any of them fails the load.
	private static void handleAttackBeam(AttackBeam payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.attackBeam(
				payload.source, payload.target, element(payload.color)));
	}
	private static void handleDischarge(Discharge payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.discharge(
				payload.source, payload.targetId, element(payload.color)));
	}
	private static void handleProgressionNote(ProgressionNote payload, IPayloadContext context) {
		context.enqueueWork(ClientPayloadHandlers::progressionNote);
	}

	/** Ticked from the client so the progress-sound cooldown drains. */
	public static void tickProgressSoundCooldown() {
		ClientPayloadHandlers.tickProgressSoundCooldown();
	}

	/** Server-side sender; kept here beside its payload. */
	public static void sendProgressionNote(net.minecraft.server.level.ServerPlayer player, int stageOrdinal) {
		// GameTest mock players (and any connection that never negotiated our channel) throw on send,
		// so check before distributing rather than letting progression grants blow up.
		if (player.connection == null || !player.connection.hasChannel(ProgressionNote.TYPE))
			return;
		PacketDistributor.sendToPlayer(player, new ProgressionNote(stageOrdinal));
	}

	private static void handleAttackReceive(AttackReceive payload, IPayloadContext context) {
		context.enqueueWork(() -> PylonAttackOverlay.trigger(element(payload.color)));
	}
	private static void handleJarRejection(JarRejection payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.jarRejection(payload.source, element(payload.color)));
	}
	private static void handlePowerCrystalDestroy(PowerCrystalDestroy payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.powerCrystalDestroy(payload.source));
	}
	private static void handleRepeaterConnections(RepeaterConnections payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.repeaterConnections(payload.source));
	}
	private static void handlePylonCrystalBreak(PylonCrystalBreak payload, IPayloadContext context) {
		context.enqueueWork(() -> ClientPayloadHandlers.pylonCrystalBreak(payload.source, element(payload.color)));
	}
	private static CrystalElement element(int ordinal) {
		CrystalElement[] elements = CrystalElement.elements;
		return elements[Mth.clamp(ordinal, 0, elements.length - 1)];
	}
}
