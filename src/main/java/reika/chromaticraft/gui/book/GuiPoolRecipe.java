/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.gui.book;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import reika.chromaticraft.ChromatiCraft;
import reika.chromaticraft.auxiliary.ChromaBookData;
import reika.chromaticraft.auxiliary.ChromaStacks;
import reika.chromaticraft.auxiliary.recipemanagers.PoolRecipes;
import reika.chromaticraft.auxiliary.recipemanagers.poolrecipes.PoolRecipe;
import reika.chromaticraft.base.GuiBookSection;
import reika.chromaticraft.magic.progression.ChromaResearchManager;
import reika.chromaticraft.registry.ChromaGuis;
import reika.chromaticraft.registry.ChromaPackets;
import reika.chromaticraft.registry.ChromaResearch;
import reika.chromaticraft.registry.ChromaSounds;
import reika.dragonapi.ModList;
import reika.dragonapi.instantiable.event.NEIRecipeCheckEvent;
import reika.dragonapi.instantiable.gui.CustomSoundGuiButton;
import reika.dragonapi.instantiable.gui.customsoundguibutton.CustomSoundImagedGuiButton;
import reika.dragonapi.instantiable.io.PacketTarget;
import reika.dragonapi.libraries.io.ReikaChatHelper;
import reika.dragonapi.libraries.io.ReikaPacketHelper;
import reika.dragonapi.libraries.io.ReikaSoundHelper;
import reika.dragonapi.libraries.java.ReikaObfuscationHelper;
import reika.dragonapi.libraries.rendering.ReikaGuiAPI;
import reika.dragonapi.libraries.rendering.ReikaRenderHelper;
import reika.dragonapi.modinteract.itemhandlers.AppEngHandler;

import codechicken.nei.NEIClientConfig;

public class GuiPoolRecipe extends GuiBookSection {

	private static final Comparator<PoolRecipe> recipeSorter = new Comparator<PoolRecipe>() {

		@Override
		public int compare(PoolRecipe o1, PoolRecipe o2) {
			return String.CASE_INSENSITIVE_ORDER.compare(o1.ID, o2.ID);
		}

	};

	private final ArrayList<PoolRecipe> recipes;
	private int index = 0;
	private boolean centeredMouse = false;

	public GuiPoolRecipe(EntityPlayer ep, int offset, boolean nei) {
		super(ChromaGuis.ALLOYING, ep, ChromaResearch.ALLOYS, 256, 220, nei);
		recipes = new ArrayList(PoolRecipes.instance.getAllPoolRecipesForPlayer(ep));
		Collections.sort(recipes, recipeSorter);
		index = offset;
	}

	private PoolRecipe getActiveRecipe() {
		return recipes.get(index);
	}

	@Override
	public void initGui() {
		super.initGui();

		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;

		String file = "Textures/GUIs/Handbook/buttons.png";
		if (recipes.size() > 1) {
			buttonList.add(new CustomSoundImagedGuiButton(0, j+205, k-3, 10, 12, 183, 6, file, ChromatiCraft.class, this));
			buttonList.add(new CustomSoundImagedGuiButton(1, j+215, k-3, 10, 12, 193, 6, file, ChromatiCraft.class, this));
		}

		if (ModList.APPENG.isLoaded() && ChromaResearchManager.instance.playerHasFragment(player, ChromaResearch.CHROMACRAFTER)) {
			if (this.getActiveRecipe().allowDoubling())
				buttonList.add(new CustomSoundGuiButton(4, j+xSize-27-20-48, k-2, 20, 20, " ", this));
			buttonList.add(new CustomSoundGuiButton(5, j+xSize-27-20-28, k-2, 20, 20, " ", this));
		}
		if (ReikaObfuscationHelper.isDeObfEnvironment())
			buttonList.add(new CustomSoundGuiButton(6, j+xSize-27, k+18, 20, 20, " ", this));

		if (NEItrigger && !centeredMouse) {
			Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
			centeredMouse = true;
		}
	}

	@Override
	public final void keyTyped(char c, int key) {
		super.keyTyped(c, key);

		if (ModList.NEI.isLoaded() && key == NEIClientConfig.getKeyBinding("gui.recipe") && !NEIClientConfig.isHidden()) {
			int x = ReikaGuiAPI.instance.getMouseRealX();
			int y = ReikaGuiAPI.instance.getMouseRealY();
			int j = (width - xSize) / 2;
			int k = (height - ySize) / 2;
			if (x >= j && y >= k && x < j+xSize && y < k+ySize) {
				ItemStack is = ReikaGuiAPI.instance.getItemRenderAt(x, y);
				if (is != null) {
					ReikaSoundHelper.playClientSound(ChromaSounds.GUICLICK, player, 0.33F, 1);
					if (!MinecraftForge.EVENT_BUS.post(new NEIRecipeCheckEvent(null, is)))
						codechicken.nei.recipe.GuiCraftingRecipe.openRecipeGui("item", is);
				}
			}
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (System.currentTimeMillis()-buttoncooldown >= 50) {
			if (button.id == 0 && index > 0) {
				index--;
				subpage = Math.min(subpage, this.getMaxSubpage());
			}
			else if (button.id == 1 && index < recipes.size()-1) {
				index++;
				subpage = Math.min(subpage, this.getMaxSubpage());
			}
			else if (button.id == 4 || button.id == 5) {
				PoolRecipe pr = this.getActiveRecipe();
				if (pr != null)
					ReikaPacketHelper.sendStringIntPacket(ChromatiCraft.packetChannel, ChromaPackets.ALLOYPATTERN.ordinal(), PacketTarget.server, pr.ID, button.id == 5 && this.getActiveRecipe().allowDoubling() ? 1 : 0);
			}
			else if (button.id == 6) {
				PoolRecipe pr = this.getActiveRecipe();
				if (pr != null)
					ReikaPacketHelper.sendStringPacket(ChromatiCraft.packetChannel, ChromaPackets.ALLOYITEMS.ordinal(), pr.ID, PacketTarget.server);
			}
		}
		//renderq = 22.5F;
		super.actionPerformed(button);
		this.initGui();
	}

	private final void drawRecipes() {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		int posX = (width - xSize) / 2;
		int posY = (height - ySize) / 2;
		try {
			this.drawAuxData(posX, posY);
		}
		catch (Exception e) {
			ReikaChatHelper.write(Arrays.toString(e.getStackTrace()));
			e.printStackTrace();
		}
	}

	protected void drawAuxData(int posX, int posY) {
		ChromaBookData.drawPoolRecipe(fontRendererObj, ri, this.getActiveRecipe(), subpage, posX, posY);
	}

	private final void drawGraphics() {
		int posX = (width - xSize) / 2-2;
		int posY = (height - ySize) / 2-8;

		ReikaRenderHelper.disableLighting();
		int msx = ReikaGuiAPI.instance.getMouseRealX();
		int msy = ReikaGuiAPI.instance.getMouseRealY();

		this.drawAuxGraphics(posX, posY);
	}

	@Override
	public int getMaxSubpage() {
		return 0;
	}

	protected void drawAuxGraphics(int posX, int posY) {
		if (ModList.APPENG.isLoaded() && ChromaResearchManager.instance.playerHasFragment(player, ChromaResearch.CHROMACRAFTER)) {
			if (this.getActiveRecipe().allowDoubling())
				api.drawItemStack(itemRender, fontRendererObj, new ItemStack(AppEngHandler.getInstance().getEncodedPattern()), posX+xSize-27-16-48, posY+8);
			api.drawItemStack(itemRender, fontRendererObj, new ItemStack(AppEngHandler.getInstance().getEncodedPattern()), posX+xSize-27-16-28, posY+8);
			if (this.getActiveRecipe().allowDoubling()) {
				double sc = 0.5;
				GL11.glScaled(sc, sc, sc);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				api.drawItemStack(itemRender, fontRendererObj, ChromaStacks.etherBerries, (int)((posX+xSize-27-16-20)/sc), (int)((posY+16)/sc));
				GL11.glEnable(GL11.GL_DEPTH_TEST);
				GL11.glScaled(1/sc, 1/sc, 1/sc);
			}
		}
	}

	@Override
	public final void drawScreen(int x, int y, float f)
	{
		super.drawScreen(x, y, f);

		int posX = (width - xSize) / 2;
		int posY = (height - ySize) / 2 - 8;

		//if (subpage == 0 && !disable)
		this.drawRecipes();

		this.drawGraphics();

		RenderHelper.disableStandardItemLighting();
	}

	@Override
	public String getPageTitle() {
		return this.getActiveRecipe().getOutput().getDisplayName();//+" Casting";
	}

	@Override
	protected int getTitleOffset() {
		return 6;
	}

	@Override
	protected PageType getGuiLayout() {
		return PageType.POOL;
	}

}
