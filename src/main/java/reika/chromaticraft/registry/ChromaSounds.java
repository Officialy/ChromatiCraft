/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.registry;

import java.net.URL;
import java.util.HashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.interfaces.ChromaSound;

/**
 * ChromatiCraft's sound registry, ported to 26.2 on the {@link SoundEvent}-registry pattern
 * ReactorCraft uses ({@link DeferredRegister} of {@code SoundEvent}s and vanilla
 * server→client playback). Implements {@link ChromaSound}.
 *
 * <p>Port note (deferred): the 1.7.10 pitch-variant sub-sound system ({@code SoundVariant}/
 * {@code ChromaSoundVariant} — the wide-pitch LO/HI variants and the ORB/MONUMENT sub-sounds) is left
 * out; {@link #getUpshiftedPitch}/{@link #getDownshiftedPitch} return {@code this} (no pitch shift) and
 * no variant events are registered. Nothing on the crystal-network/casting path uses variants; they are
 * re-added with the music/orchestra subsystem. Volume modulation ({@code ChromaOptions}-driven) is also
 * deferred (getModulatedVolume returns 1).
 */
public enum ChromaSounds implements ChromaSound {

	RIFT("rift"),
	POWERDOWN("powerdown-2"),
	DISCHARGE("discharge"),
	CAST("cast3"),
	POWER("ambient"),
	CRAFTING("ambient1_short"),
	CRAFTING_BOOST("ambient1_short_boost"),
	CRAFTDONE("craftdone2"),
	UPGRADE("upgrade"),
	ABILITY("ability"),
	ERROR("error"),
	INFUSE("infuse"),
	INFUSION("infuse2"),
	INFUSION_SHORT("infuse2s"),
	USE("use2"),
	TRAP("slam2"),
	DING("ding2", true),
	SHOCKWAVE("shockwave3"),
	BALLLIGHTNING("balllightning"),
	ITEMSTAND("stand"),
	GLOWCLOUD("powercrystal"),
	GUICLICK("gui2"),
	GUISEL("gui4"),
	DRONE("drone2"),
	DRONE_HI("drone2_hi"),
	PORTAL("portal2"),
	ORB("orb/orb", true),
	GOTODIM("todim"),
	OVERLOAD("discharge2"),
	PYLONFLASH("pylonboost"),
	PYLONTURBO("pylonturbo"),
	PYLONBOOSTRITUAL("pylonboost_ritual_short"),
	PYLONBOOSTSTART("pylonbooststart"),
	DASH("dash"),
	REPEATERSURGE("repeatersurge"),
	REPEATERSURGE_WEAK("repeatersurge_weak"),
	FIRE("fire"),
	LASER("laser"),
	MONUMENT("monument/s"),
	MONUMENTRAY("monumentray"),
	BUFFERWARNING("buffer_warning"),
	BUFFERWARNING_LOW("buffer_warning2"),
	BUFFERWARNING_EMPTY("buffer_warning3"),
	KILLAURA("killaura"),
	KILLAURA_CHARGE("killaura_charge"),
	POWERCRAFT("powercraft"),
	METEOR("meteor"),
	IMPACT("impact"),
	NOCLIPON("rumble-in"),
	NOCLIPOFF("rumble-out"),
	NOCLIPRUN("rumble"),
	FLAREATTACK("flareattack"),
	BOUNCE("bounce"),
	SKYRIVER("lumenstream"),
	PING("ping2"),
	GAINPROGRESS("progress2"),
	AVOLASER("avolaser2"),
	CLIFFSOUND("cliffambience"),
	CLIFFSOUND2("cliffambience2"),
	CLIFFSOUND3("cliffambience3"),
	CLIFFSOUND4("cliffambience4"),
	INSCRIBE("inscribe"),
	LOREHEX("lore"),
	LORECOMPLETE("lorecomplete2"),
	LIGHTCAST("lightcast"),
	WATERLOCK("waterlock"),
	REPEATERRING("repeaterring"),
	FAIL("fail"),
	DIMENSIONHUM("dimensionhum"),
	DIMENSIONHUM_HI("dimensionhum_hi"),
	RADIANCE("radiance2"),
	CASTHARMONIC("castingharmonic2"),
	ABILITYCOMPLETE("abilityfinish2"),
	NETWORKOPT("networkopt"),
	NETWORKOPTCHARGE("networkopt_charge2"),
	TUNNELNUKERAMBIENT("nukerfly2"),
	TUNNELNUKERCALL("nukercall"),
	TOWEREXTEND1("tower_extend_1"),
	TOWEREXTEND2("tower_extend_2b"),
	TOWERAMBIENT("tower_ambient2"),
	CASTTUNEREJECT("casttunereject"),
	DINGCHARGE("dingcharge"),
	ARTEALLOY("artealloy2b"),
	ARTEALLOYHIT("artealloy-hit2"),
	LOWAMBIENT("lowambient_fade"),
	LOWAMBIENT_SHORT("lowambient_fade_short"),
	AURALOCUS("auralocus"),
	;

	public static final ChromaSounds[] soundList = values();

	private static final String SOUND_FOLDER = "sounds/";
	private static final String SOUND_EXT = ".ogg";

	public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
			DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ChromatiCraft.MODID);

	private static final HashMap<ChromaSounds, DeferredHolder<SoundEvent, SoundEvent>> SOUND_EVENT_MAP = new HashMap<>();

	static {
		for (ChromaSounds sound : values()) {
			SOUND_EVENT_MAP.put(sound, SOUND_EVENTS.register(sound.eventName, () -> {
				Identifier id = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, sound.eventName);
				float d = sound.getAudibleDistance();
				return d > 0 ? SoundEvent.createFixedRangeEvent(id, d) : SoundEvent.createVariableRangeEvent(id);
			}));
		}
	}

	private final Identifier path;
	private final String relative;
	private final String eventName;
	private final boolean widePitch;
	private boolean isVolumed = false;

	private ChromaSounds(String n) {
		this(n, false);
	}

	private ChromaSounds(String n, boolean wide) {
		if (n.startsWith("#")) {
			isVolumed = true;
			n = n.substring(1);
		}
		relative = n;
		// Sound sub-folders (orb/, monument/) map straight into the event id; the registry name allows '/'.
		eventName = n;
		widePitch = wide;
		path = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, SOUND_FOLDER + n + SOUND_EXT);
	}

	@Override
	public SoundEvent getSoundEvent() {
		return SOUND_EVENT_MAP.get(this).get();
	}

	@Override
	public float getModulatedVolume() {
		return 1F; //volume modulation (ChromaOptions) deferred
	}

	public void playSound(Entity e) {
		this.playSound(e, 1, 1);
	}

	@Override
	public void playSound(Entity e, float vol, float pitch) {
		this.playSound(e.level(), e.getX(), e.getY(), e.getZ(), vol, pitch);
	}

	@Override
	public void playSound(Level world, BlockPos pos, float vol, float pitch) {
		this.playSound(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, vol, pitch);
	}

	public void playSound(Level world, double x, double y, double z, float vol, float pitch) {
		if (world.isClientSide())
			return;
		world.playSound(null, x, y, z, this.getSoundEvent(), this.getCategory(),
				vol * this.getModulatedVolume(), pitch);
	}

	@Override
	public void playSound(Level world, BlockPos pos, float vol, float pitch, boolean attenuate) {
		if (world.isClientSide())
			return;
		this.playSound(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, vol, pitch, attenuate);
	}

	public void playSound(Level world, double x, double y, double z, float vol, float pitch, boolean attenuate) {
		if (world.isClientSide())
			return;
		// SoundEvent is now a real registry entry. Vanilla playback is the authoritative 26.2
		// path; the old DragonAPI library gate only knew its pre-registry sound collection.
		float effectiveVolume = attenuate ? vol : Math.max(vol, 16F);
		world.playSound(null, x, y, z, this.getSoundEvent(), this.getCategory(),
				effectiveVolume * this.getModulatedVolume(), pitch);
	}

	@Override
	public void playSoundAtBlock(Level world, BlockPos pos, float vol, float pitch) {
		this.playSound(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, vol, pitch);
	}

	public void playSoundAtBlock(Level world, BlockPos pos) {
		this.playSoundAtBlock(world, pos, 1, 1);
	}

	@Override
	public void playSoundAtBlock(BlockEntity te, float vol, float pitch) {
		this.playSoundAtBlock(te.getLevel(), te.getBlockPos(), vol, pitch);
	}

	public void playSoundAtBlock(BlockEntity te) {
		this.playSoundAtBlock(te, 1, 1);
	}

	@Override
	public void playSoundAtBlockNoAttenuation(BlockEntity te, float vol, float pitch, int broadcast) {
		this.playSoundNoAttenuation(te.getLevel(), te.getBlockPos(), vol, pitch, broadcast);
	}

	@Override
	public void playSoundNoAttenuation(Level world, BlockPos pos, float vol, float pitch, int broadcast) {
		if (world.isClientSide())
			return;
		// Registered vanilla events can be heard at range by raising their effective volume.
		float rangedVolume = Math.max(vol, broadcast / 16F);
		world.playSound(null, pos, this.getSoundEvent(), this.getCategory(),
				rangedVolume * this.getModulatedVolume(), pitch);
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public Identifier getPath() {
		return path;
	}

	public URL getURL() {
		return ChromatiCraft.class.getResource("/" + SOUND_FOLDER + relative + SOUND_EXT);
	}

	@Override
	public SoundSource getCategory() {
		if (this == GLOWCLOUD || this == BALLLIGHTNING)
			return SoundSource.NEUTRAL;
		return SoundSource.MASTER;
	}

	@Override
	public boolean canOverlap() {
		return this == RIFT || this == CAST || this == USE || this == ERROR || this == INFUSE || this == DING || this == DRONE || this == ITEMSTAND || this == KILLAURA_CHARGE || this == DASH || this == ORB;
	}

	@Override
	public boolean attenuate() {
		return this != GOTODIM && this != PYLONTURBO && this != PYLONFLASH && this != PYLONBOOSTRITUAL && this != PYLONBOOSTSTART && this != REPEATERSURGE && this != MONUMENT && this != MONUMENTRAY && this != GAINPROGRESS && this != LORECOMPLETE;
	}

	@Override
	public boolean hasWiderPitchRange() {
		return widePitch;
	}

	@Override
	public ChromaSound getUpshiftedPitch() {
		return this; //pitch-variant sub-sounds deferred
	}

	@Override
	public ChromaSound getDownshiftedPitch() {
		return this; //pitch-variant sub-sounds deferred
	}

	@Override
	public boolean preload() {
		switch (this) {
			case MONUMENT:
			case CRAFTING:
			case CRAFTING_BOOST:
			case POWERCRAFT:
			case INFUSION:
			case ABILITY:
			case GOTODIM:
			case PYLONBOOSTRITUAL:
			case REPEATERSURGE:
				return true;
			default:
				return false;
		}
	}

	@Override
	public float getAudibleDistance() {
		switch (this) {
			case POWER:
				return 27;
			default:
				return -1;
		}
	}

	@Override
	public String getRelativePath() {
		return SOUND_FOLDER + relative + SOUND_EXT;
	}
    public String getEventName() {
        return eventName;
    }

    /** Resource location below assets/chromaticraft/sounds/, without the .ogg extension. */
    public String getSoundFile() {
        return relative;
    }

	@Override
	public float getRangeInterval() {
		return 4;
	}

	@Override
	public boolean isStreamed() {
		return this == MONUMENT;
	}
}


