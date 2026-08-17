package reika.chromaticraft.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

import reika.dragonapi.base.InertEntity;

/**
 * V33a {@code EntityAurora}: one ribbon of Proxima's aurorae, carried by an entity so it persists and
 * follows the player's view rather than being redrawn from the sky renderer each frame.
 *
 * <p>The entity is inert — it has no collision, no AI and no frustum culling, since a ribbon a hundred
 * blocks long is almost always partly off screen. What it carries is the {@link AuroraData} the
 * generator gave it: two endpoints, two colours to gradient between, and a drift speed. It sits at the
 * midpoint of its own endpoints, which is what upstream's constructor computes.
 *
 * <p>Both persistence paths are here because a ribbon has to survive two different things. It is
 * written to and read from disk so an aurora is still overhead after a reload, and it is sent as custom
 * spawn data so a client that has just come into range gets the endpoints and colours in the spawn
 * packet — an aurora cannot be reconstructed from position alone.
 *
 * <p>Upstream's {@code variance} and {@code segmentSize} fields are not carried. They are declared and
 * serialised there but every assignment to them is commented out, so they are always zero and the
 * renderer's parameters for them are dead; reproducing them would only preserve two unused zeroes.
 *
 * <p>The ribbon itself is not yet drawn. V33a renders it through a client-only spline class using
 * immediate-mode GL, which is the same rendering effort the Proxima decoration layers and the glow-tree
 * overlays are waiting on. Everything that decides what an aurora <em>is</em> — where it hangs, how long
 * it is, its two colours, its speed, how it persists and syncs — is complete here.
 */
public class EntityAurora extends InertEntity implements IEntityWithComplexSpawn {

	private AuroraData data = AuroraData.EMPTY;

	public EntityAurora(EntityType<? extends EntityAurora> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	/** Places the ribbon at the midpoint of its own endpoints, as upstream's constructor does. */
	public void setAuroraData(AuroraData data) {
		this.data = data;
		Vec3 centre = data.centre();
		this.snapTo(centre.x, centre.y, centre.z, 0, 0);
	}

	public AuroraData getAuroraData() {
		return data;
	}

	/** Nothing is tracked through synched data: the ribbon's shape arrives once, as spawn data. */
	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
	}

	/**
	 * V33a {@code ignoreFrustumCheck} plus an unbounded {@code isInRangeToRenderDist}: a ribbon is long
	 * enough that its entity position is frequently outside the view even when the ribbon is not, so it
	 * must never be culled on either test.
	 */
	@Override
	public boolean shouldRenderAtSqrDistance(double distanceSquared) {
		return true;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		data = input.read("aurora", AuroraData.CODEC).orElse(AuroraData.EMPTY);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.store("aurora", AuroraData.CODEC, data);
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		data.write(buffer);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		data = AuroraData.read(buffer);
	}
}
