package reika.chromaticraft.magic.progression;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import reika.chromaticraft.auxiliary.recipemanagers.CastingTableRecipe;

/** The world/casting milestone that must be completed before a research tier becomes available. */
public enum ResearchLevel implements ProgressAccess {
	ENTRY,
	RAWEXPLORE,
	BASICCRAFT,
	RUNECRAFT,
	ENERGY,
	MULTICRAFT,
	NETWORKING,
	PYLONCRAFT,
	ENDGAME,
	CTM;

	public static final ResearchLevel[] levelList = values();

	/** Exact V33a gate ordering; casting checks are player-owned, not tied to the current table. */
	public boolean canProgressTo(Player player) {
		return switch (this) {
			case ENTRY -> true;
			case RAWEXPLORE -> ProgressStage.CRYSTALS.isPlayerAtStage(player);
			case BASICCRAFT -> ProgressStage.ANYSTRUCT.isPlayerAtStage(player);
			case RUNECRAFT -> CastingProgression.hasCrafted(player, CastingTableRecipe.Tier.CRAFTING);
			case ENERGY -> ProgressStage.CHARGE.isPlayerAtStage(player)
					&& ProgressStage.PYLON.isPlayerAtStage(player);
			case MULTICRAFT -> CastingProgression.hasCrafted(player, CastingTableRecipe.Tier.TEMPLE);
			case NETWORKING -> CastingProgression.hasCrafted(player, CastingTableRecipe.Tier.MULTIBLOCK);
			case PYLONCRAFT -> ProgressStage.REPEATER.isPlayerAtStage(player);
			case ENDGAME -> CastingProgression.hasCrafted(player, CastingTableRecipe.Tier.PYLON);
			case CTM -> ProgressStage.CTM.isPlayerAtStage(player);
		};
	}

	public Component getDisplayName() {
		return Component.translatable("chromaresearch." + this.name().toLowerCase(Locale.ROOT));
	}

	public ResearchLevel pre() {
		return this.ordinal() > 0 ? levelList[this.ordinal() - 1] : this;
	}

	public ResearchLevel post() {
		return this.ordinal() < levelList.length - 1 ? levelList[this.ordinal() + 1] : this;
	}

	@Override
	public boolean playerHas(Player player) {
		return ResearchProgress.getLevel(player).isAtLeast(this);
	}

	public boolean giveToPlayer(Player player, boolean notify) {
		return ResearchProgress.setLevel(player, this, notify);
	}

	public boolean isAtLeast(ResearchLevel other) {
		return other.ordinal() <= this.ordinal();
	}

	public int getDifference(ResearchLevel other) {
		return Math.abs(other.ordinal() - this.ordinal());
	}
}
