package reika.chromaticraft.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * V33a {@code EntityAurora.AuroraData}: everything that decides what one aurora ribbon looks like.
 *
 * <p>Two endpoints give the ribbon its line and length, the two colours are gradiented along it, and
 * the speed is how fast its spline drifts. A record rather than a mutable holder because nothing changes
 * one after the generator has built it — upstream's setters exist only for its two deserialization
 * paths, which are a codec and a buffer read here.
 *
 * <p>Upstream also declares {@code variance} and {@code segmentSize} and serialises them, but every
 * assignment to both is commented out, so they are always zero and the renderer parameters that would
 * consume them are dead. They are not carried; there is nothing to carry.
 */
public record AuroraData(Vec3 from, Vec3 to, int colorFrom, int colorTo, double speed) {

	/** What an aurora with no data at all reads as, so a malformed load cannot leave a null. */
	public static final AuroraData EMPTY = new AuroraData(Vec3.ZERO, Vec3.ZERO, 0, 0, 0);

	public static final Codec<AuroraData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Vec3.CODEC.fieldOf("from").forGetter(AuroraData::from),
			Vec3.CODEC.fieldOf("to").forGetter(AuroraData::to),
			Codec.INT.fieldOf("color_from").forGetter(AuroraData::colorFrom),
			Codec.INT.fieldOf("color_to").forGetter(AuroraData::colorTo),
			Codec.DOUBLE.fieldOf("speed").forGetter(AuroraData::speed))
			.apply(instance, AuroraData::new));

	/** Where the entity sits: V33a averages the two endpoints. */
	public Vec3 centre() {
		return from.add(to).scale(0.5);
	}

	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeDouble(from.x);
		buffer.writeDouble(from.y);
		buffer.writeDouble(from.z);
		buffer.writeDouble(to.x);
		buffer.writeDouble(to.y);
		buffer.writeDouble(to.z);
		buffer.writeDouble(speed);
		buffer.writeInt(colorFrom);
		buffer.writeInt(colorTo);
	}

	public static AuroraData read(RegistryFriendlyByteBuf buffer) {
		Vec3 from = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		Vec3 to = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		double speed = buffer.readDouble();
		return new AuroraData(from, to, buffer.readInt(), buffer.readInt(), speed);
	}
}
