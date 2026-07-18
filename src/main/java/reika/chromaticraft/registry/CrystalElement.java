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

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.api.CrystalElementAccessor.CrystalElementProxy;
import reika.chromaticraft.api.ProgressionAPI;
import reika.chromaticraft.auxiliary.OverlayColor;
import reika.chromaticraft.magic.ElementMixer;
import reika.chromaticraft.magic.progression.ProgressAccess;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.instantiable.data.maps.MultiMap.CollectionType;
import reika.dragonapi.libraries.java.ReikaJavaLibrary;
import reika.dragonapi.libraries.registry.ReikaDyeHelper;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

public enum CrystalElement implements OverlayColor, ProgressAccess, CrystalElementProxy {

	BLACK("Kuro", 0x191919, ChatFormatting.BLACK), //Magic
	RED("Karmir", 0xFF0000, ChatFormatting.DARK_RED), //Endurance
	GREEN("Kijani", 0x007F0E, ChatFormatting.DARK_GREEN), //Nature
	BROWN("Ruskea", 0x724528, ChatFormatting.GOLD), //Mineral
	BLUE("Nila", 0x0026FF, ChatFormatting.BLUE), //Light
	PURPLE("Zambarau", 0x8C00EA, ChatFormatting.DARK_PURPLE), //Enhancement
	CYAN("Vadali", 0x009FBF, ChatFormatting.DARK_AQUA), //Water
	LIGHTGRAY("Argia", 0x979797, ChatFormatting.GRAY), //Deception
	GRAY("Ykri", 0x404040, ChatFormatting.DARK_GRAY), //Change
	PINK("Ruzova", 0xFFBAD9, ChatFormatting.RED), //Aggression
	LIME("Asveste", 0x00FF00, ChatFormatting.GREEN), //Motion
	YELLOW("Kitrino", 0xFFFF00, ChatFormatting.YELLOW), //Energy
	LIGHTBLUE("Galazio", 0x7FD4FF, ChatFormatting.AQUA), //Time
	MAGENTA("Kurauri", 0xFF00DC, ChatFormatting.LIGHT_PURPLE), //Life
	ORANGE("Portokali", 0xFF6A00, ChatFormatting.GOLD), //Fire
	WHITE("Tahara", 0xFFFFFF, ChatFormatting.WHITE); //Purity/Harmony

	private final ReikaDyeHelper color;
	public final String displayName;
	private final int rgb;
	private final ChatFormatting chat;

	private final float[] hsb;

	private static final Random rand = new Random();

	public static final CrystalElement[] elements = values();
	private static final MultiMap<Integer, CrystalElement> levelMap = new MultiMap(CollectionType.HASHSET);
	private static final HashMap<String, CrystalElement> nameMap = new HashMap();
	private static final HashMap<CrystalElement, Integer> colorMap = new HashMap();

	private CrystalElement(String n, int rgb, ChatFormatting c) {
		color = ReikaDyeHelper.dyes[this.ordinal()];
		displayName = n;
		this.rgb = 0xff000000 | rgb;
		chat = c;

		hsb = Color.RGBtoHSB(this.getRed(), this.getGreen(), this.getBlue(), null);
	}

	public String getEnglishName() {
		return color.dye.getName();
	}

	public int getColor() {
		return rgb;
	}

	public int getHue() {
		return (int)(hsb[0]*360F);
	}

	public int getSaturation() {
		return (int)(hsb[1]*255F);
	}

	public int getValue() {
		return (int)(hsb[2]*255F);
	}

	public int getRed() {
		return ReikaColorAPI.getRed(this.getColor());
	}

	public int getGreen() {
		return ReikaColorAPI.getGreen(this.getColor());
	}

	public int getBlue() {
		return ReikaColorAPI.getBlue(this.getColor());
	}

	public Color getJavaColor() {
		return new Color(rgb);
	}

	public int getLevel() {
		switch(this) {
			case BLACK:
			case BLUE:
			case BROWN:
			case GREEN:
			case RED:
			case WHITE:
			case YELLOW:
				return 0;
			case CYAN:
			case LIGHTBLUE:
			case GRAY:
			case LIME:
			case ORANGE:
			case PURPLE:
			case PINK:
				return 1;
			case LIGHTGRAY:
			case MAGENTA:
				return 2;
			default:
				return -1;
		}
	}

	public CrystalElement mixWith(CrystalElement e) {
		return ElementMixer.instance.getMix(this, e);
	}

	public CrystalElement subtract(CrystalElement e) {
		return ElementMixer.instance.subtract(this, e);
	}

	public boolean isCompatible(CrystalElement e) {
		return ElementMixer.instance.isCompatible(this, e);
	}

	public boolean isPrimary() {
		return this.getLevel() == 0;
	}

	public static CrystalElement randomElement() {
		return elements[rand.nextInt(elements.length)];
	}

	public static CrystalElement randomElement(int level) {
		Collection<CrystalElement> li = levelMap.get(level);
		return li != null ? ReikaJavaLibrary.getRandomCollectionEntry(rand, li) : null;
	}

	public static CrystalElement randomPrimaryElement() {
		return randomElement(0);
	}

	public static int getBlendedColor(int tick, int mod) {
		if (tick < 0)
			tick = -tick;
		CrystalElement e = CrystalElement.elements[(tick/mod)%16];
		CrystalElement e2 = CrystalElement.elements[(tick/mod+1)%16];
		float mix = tick%mod/(float)mod;
		return ReikaColorAPI.mixColors(e2.getColor(), e.getColor(), mix);
	}

	static {
		for (int i = 0; i < elements.length; i++) {
			CrystalElement e = elements[i];
			int lvl = e.getLevel();
			levelMap.addValue(lvl, e);
			nameMap.put(e.displayName, e);
			colorMap.put(e, e.getColor());
		}
	}

	public String getChatColorString() {
		return chat.toString();
	}

	public static CrystalElement getByName(String s) {
		return nameMap.get(s);
	}

	public static Map<CrystalElement, Integer> getColorMap() {
		return Collections.unmodifiableMap(colorMap);
	}

	@Override
	public boolean playerHas(Player ep) {
		// Route through the ProgressRegistry API interface rather than the concrete ProgressionManager
		// hub (which pulls in the casting/rendering/registry batch). ProgressionManager sets
		// ProgressionAPI.instance.progressManager on load; playerDiscoveredElement delegates straight
		// to hasPlayerDiscoveredColor, so this is behaviourally identical.
		return ProgressionAPI.instance.progressManager.playerDiscoveredElement(ep, this);
	}

	@Override
	public String displayName() {
		return displayName;
	}

}
