package reika.chromaticraft.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.api.abilityapi.Ability;
import reika.chromaticraft.magic.ChromaAbilityData;
import reika.chromaticraft.magic.progression.LexiconCatalog;
import reika.chromaticraft.magic.progression.PlayerResearch;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaOptions;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.Chromabilities;

import java.util.ArrayList;
import java.util.List;

/** V33a's non-pausing ability carousel, modernized onto the 26.2 extracted GUI renderer. */
public final class ScreenAbilitySelect extends Screen {

	private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
			ChromatiCraft.MODID, "textures/gui/abilityselect.png");
	private static final int ICON = 50;
	private static final int SELECTED_ICON = 75;
	private static final int STEP = 72;

	private final Player player;
	private final List<Ability> abilities = new ArrayList<>();
	private int selected;
	private int power;

	public ScreenAbilitySelect(Player player) {
		super(Component.literal("Chromabilities"));
		this.player = player;
		for (Ability ability : Chromabilities.getAbilities()) {
			if (!ability.isAvailableToPlayer(player) || !hasFragment(player, ability))
				continue;
			if (ChromaOptions.ABILITYSHOWONLY.getState()
					&& !ChromaAbilityData.hasAbility(player, ability))
				continue;
			abilities.add(ability);
		}
	}

	private static boolean hasFragment(Player player, Ability ability) {
		if (!(ability instanceof Chromabilities))
			return true;
		LexiconCatalog.Entry page = LexiconCatalog.entries().stream()
				.filter(entry -> entry.section() == LexiconCatalog.Section.ABILITIES
						&& entry.sourceId().equals(ability.getID()))
				.findFirst().orElse(null);
		return page == null || PlayerResearch.hasFragment(player, page);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		if (abilities.isEmpty()) {
			graphics.centeredText(font, Component.literal("No abilities available"), width / 2,
					height / 2, 0xffffffff);
			return;
		}

		int panelY = height / 2 - 62;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, width / 2 - 128, panelY,
				0, 79, 256, 177, 256, 256);
		Ability active = abilities.get(selected);
		for (int index = 0; index < abilities.size(); index++) {
			Ability ability = abilities.get(index);
			int size = index == selected ? SELECTED_ICON : ICON;
			int x = width / 2 + (index - selected) * STEP - size / 2;
			int y = height / 2 + 51 - size / 2 - (index == selected ? 2 : 0);
			graphics.blit(RenderPipelines.GUI_TEXTURED, ability.getTexture(
					!ChromaAbilityData.hasAbility(player, ability)), x, y, 0, 0,
					size, size, ICON, ICON, ICON, ICON);
		}

		graphics.centeredText(font, active instanceof Chromabilities chroma ? chroma.getDisplayName(player)
				: Component.literal(active.getDisplayName()), width / 2,
				height / 2 + 94, 0xffffffff);
		if (active.getMaxPower() > 0) {
			int bar = power * 64 / active.getMaxPower();
			graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, width / 2 - 12,
					height / 2 + 5 - bar, 116, 76 - bar, 24, bar, 256, 256);
			graphics.centeredText(font, Component.literal(power + "/" + maxPower(active)),
					width / 2, height / 2 - 72, 0xffffffff);
			String description = active.getPowerDesc(power);
			if (description != null && !description.isBlank()) {
				String[] lines = description.split("\\n");
				int y = height / 2 - 10 * lines.length;
				for (String line : lines) {
					graphics.text(font, line, width / 2 + 20, y, 0xffffffff, true);
					y += 10;
				}
			}
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return switch (event.key()) {
			case InputConstants.KEY_LEFT -> selectOffset(-1);
			case InputConstants.KEY_RIGHT -> selectOffset(1);
			case InputConstants.KEY_UP -> changePower(1);
			case InputConstants.KEY_DOWN -> changePower(-1);
			case InputConstants.KEY_HOME -> select(0);
			case InputConstants.KEY_END -> select(abilities.size() - 1);
			case InputConstants.KEY_RETURN, InputConstants.KEY_SPACE -> activate();
			default -> super.keyPressed(event);
		};
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY == 0)
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		return selectOffset(scrollY > 0 ? 1 : -1);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && !abilities.isEmpty()
				&& Math.abs(event.x() - width / 2D) <= SELECTED_ICON / 2D
				&& Math.abs(event.y() - (height / 2D + 49)) <= SELECTED_ICON / 2D)
			return activate();
		return super.mouseClicked(event, doubleClick);
	}

	private boolean selectOffset(int amount) {
		return select(selected + amount);
	}

	private boolean select(int index) {
		if (abilities.isEmpty())
			return false;
		int next = Mth.clamp(index, 0, abilities.size() - 1);
		if (next == selected)
			return false;
		selected = next;
		power = 0;
		ChromaSounds.GUISEL.playSound(player, 1, 1);
		return true;
	}

	private boolean changePower(int amount) {
		if (abilities.isEmpty() || !ChromaAbilityData.hasAbility(player, abilities.get(selected)))
			return false;
		int next = Mth.clamp(power + amount, 0, maxPower(abilities.get(selected)));
		if (next == power)
			return false;
		power = next;
		ChromaSounds.GUISEL.playSound(player, 0.75F, 1);
		return true;
	}

	private int maxPower(Ability ability) {
		return Chromabilities.maxPower(player, ability);
	}

	private boolean activate() {
		if (abilities.isEmpty())
			return false;
		Ability ability = abilities.get(selected);
		if (!ChromaAbilityData.hasAbility(player, ability)) {
			ChromaSounds.ERROR.playSound(player, 1, 1);
			return false;
		}
		ClientPacketDistributor.sendToServer(new ChromaNetwork.ActivateAbility(ability.getID(), power));
		ChromaSounds.GUICLICK.playSound(player, 0.75F, 1);
		onClose();
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
