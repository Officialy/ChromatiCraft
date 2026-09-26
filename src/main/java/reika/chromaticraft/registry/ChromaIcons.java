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

import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;
import reika.dragonapi.auxiliary.trackers.SpecialDayTracker;
import reika.dragonapi.interfaces.IconEnum;

/**
 * V33a {@code ChromaIcons}: ChromatiCraft's named effect sprites. V33a stitched each into the terrain atlas (the lattice
 * into the item atlas) as {@code chromaticraft:icons/<name>}; here each is a sprite id -- {@code block/icons/<name>} in the
 * block atlas, or {@code item/icons/<name>} in the item atlas -- which particles and renderers resolve when drawing. The
 * port's files use underscores where two V33a names had spaces. REGIONS and CAUSTICS_GENTLE_ALPHA had no texture in
 * V33a either (only their animation metadata shipped).
 */
public enum ChromaIcons implements IconEnum {


	TRANSPARENT("transparent"),
	GUARDIANOUTER("guardian_outer"),
	GUARDIANMIDDLE("guardian_middle"),
	GUARDIANINNER("guardian_inner"),
	SPARKLEPARTICLE("sparkle-particle"),
	SPARKLE("sparkle2"),
	SPARKLE_ROUNDED("roundsparkle"),
	GLOWSECTION("glowsections"),
	RADIATE("plant_glow"),
	FLARE("flare"),
	RINGFLARE("flare4"),
	REPEATER("repeater"),
	MULTIREPEATER("multirepeater"),
	BROADCAST("broadcaster"),
	SPINFLARE("rotating flare_pulse"),
	LASER("laser_2"),
	LASEREND("laser_end_2"),
	BIGFLARE("bigflare"),
	ROUNDFLARE(SpecialDayTracker.instance.loadAprilTextures() ? "smileflare" : SpecialDayTracker.instance.loadXmasTextures() ? "xmasflare" : "roundflare"),
	RINGS("ringrow_fade"),
	RIFT("rift"),
	RIFTHALO("rift_halo"),
	FADE(SpecialDayTracker.instance.loadXmasTextures() ? "fade_snowflake" : "fade"),
	FADE_SNOW("fade_snowflake"),
	FADE_BASICBLEND("fade_basic"),
	FADE_GENTLE("fade_gentle"),
	FADE_STAR("fade_star"),
	FADE_RAY("fade_ray"),
	FADE_CLOUD("fade_cloud"),
	FADE_LIQUID("fade_liquid"),
	CLOUDGROUP("cloudgroup"),
	CLOUDGROUP_TRANS("cloudgroup_trans"),
	CLOUDGROUP_TRANS_BLUR("cloudgroup_trans_blur"),
	TRANSFADE("transfade"),
	CENTER("centerblur3"),
	CHROMA("chroma_particle"),
	NOENTER("noentry"),
	CHECK("check"),
	MARKER("marker"),
	X("x"),
	DIAMOND("diamond"),
	QUESTION("question"),
	BLUEFIRE("bluefire"),
	BATTERY("battery_outer"),
	BLANK("blank"),
	GLOWFRAME("glowframe"),
	GLOWFRAMEDOT("glowframe_dot"),
	GLOWFRAME_TRANS("glowframe2"),
	GLOWFRAMEDOT_TRANS("glowframe_dot2"),
	ALLCOLORS("allcolors"),
	NODE("node"),
	NODE2("node2"),
	TURBO("turbo"),
	TRIDOT("tridot-strip"),
	PURPLESPIN("purplespin"),
	ROSES("roses"),
	ROSES_WHITE("roses_w"),
	BASICFADE("basicfade"),
	BASICFADE_FAST("basicfade_f"),
	FRAME("frame"),
	//PINWHEEL("pinwheel"),
	LATTICE("lattice"),
	LATTICEITEM("lattice"),
	REGIONS("regions"),
	CAUSTICS("caustics"),
	CAUSTICS_GENTLE("caustics-g"),
	CAUSTICS_GENTLE_ALPHA("caustics-g-a"),
	CAUSTICS_TINY("caustics-tiny"),
	CAUSTICS_TINY_ALPHA("caustics-tiny-a"),
	CAUSTICS_CRYSTAL("caustics-crystal3"),
	OVALS("oval-rings"),
	WEAKREPEATER("weakrepeater"),
	SMOKE("smoke"),
	BLACKHOLE("blackhole"),
	WHITEHOLE("blackhole_white"),
	ALPHAHOLE("blackhole_alpha"),
	BLURFLARE("blurflare2"),
	HOLE("hole"),
	PINWHEEL("pinwheel"),
	HEXFLARE("hexflare"),
	HEXFLARE2("hexflare2"),
	CONCENTRIC("concentric"),
	CONCENTRIC2("concentric2"),
	CONCENTRIC2REV("concentric2rev"),
	FAN("fan"),
	SIDEDFLOW("sidedflow"),
	SIDEDFLOWBI("sidedflow-bi"),
	RING0("ring0"),
	RING1("ring1"),
	RING2("ring2"),
	RING3("ring3"),
	WIDEBAR("widebar"),
	PORTALRING("portalring"),
	CONDENSEFLARE("condenseflare4"),
	STARFLARE("starflare"),
	AVOLASER("avolaser"),
	AVOLASER_CORE("avolaser_2"),
	HIVE("hive"),
	HIVESPARKS("hivesparks"),
	FLARE7("flare7"),
	CELLFLARE("cellflare"),
	ECLIPSEFLARE("eclipseflare"),
	SUNFLARE("sunflare"),
	COLORWHIRL("colorwhirl3"),
	COLORWHIRLFLARE("colorwhirlflare"),
	RAINFLARE("rainflare"),
	STATIC("static"),
	BLASTFLARE("blastflare"),
	ROSEFLARE("roseflare"),
	ANGLER("anglerlight"),
	ANGLER_BASIC("anglerlight_basic"),
	COLORRING("color-ring"),
	;

	private final String iconName;

	public static final ChromaIcons[] iconList = values();

	private ChromaIcons(String icon) {
		iconName = icon;
	}

	/** The V33a texture name (the sprite file under {@code icons/}). */
	public String getIconName() {
		return iconName;
	}

	/** The sprite id: in the item atlas for the lattice (V33a registered it on the item sheet), else the block atlas. */
	@Override
	public Identifier getIcon() {
		String file = iconName.replace(' ', '_');
		return Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, (this.isOnSheet(1) ? "item/icons/" : "block/icons/")+file);
	}

	/** V33a registered only the lattice on the item sheet (1); everything else on the terrain sheet (0). */
	public boolean isOnSheet(int sheet) {
		switch(this) {
			case LATTICEITEM:
				return sheet == 1;
			default:
				return sheet == 0;
		}
	}

	public boolean allowTextureModification() {
		switch(this) {
			case QUESTION:
			case FRAME:
			case GLOWFRAME:
			case GLOWFRAMEDOT:
			case GLOWFRAME_TRANS:
			case GLOWFRAMEDOT_TRANS:
			case BASICFADE:
			case BASICFADE_FAST:
			case REPEATER:
			case MULTIREPEATER:
			case BROADCAST:
			case WEAKREPEATER:
			case SPARKLEPARTICLE:
				return true;
			default:
				return false;
		}
	}

	public boolean isTransparent() {
		switch(this) {
			case BASICFADE:
			case FADE_BASICBLEND:
			case BASICFADE_FAST:
			case FRAME:
			case BATTERY:
			case REPEATER:
			case MULTIREPEATER:
			case BROADCAST:
			case WEAKREPEATER:
			case SPARKLEPARTICLE:
			case TRANSPARENT:
			case GUARDIANOUTER:
			case GUARDIANMIDDLE:
			case GLOWFRAME_TRANS:
			case GLOWFRAMEDOT_TRANS:
			case PURPLESPIN:
			case TRANSFADE:
			case CHROMA:
			case NOENTER:
			case CHECK:
			case DIAMOND:
			case QUESTION:
			case CLOUDGROUP_TRANS:
			case CLOUDGROUP_TRANS_BLUR:
			case SMOKE:
				return true;
			default:
				return false;
		}
	}

}
