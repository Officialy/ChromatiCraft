package reika.chromaticraft.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import reika.chromaticraft.magic.progression.ProgressStage;
import reika.chromaticraft.magic.progression.ProgressionDescriptions;
import reika.chromaticraft.magic.progression.ProgressionManager;
import reika.chromaticraft.registry.ChromaItems;

import java.util.*;
import java.util.function.BiConsumer;

/** Advancements mirror authored progression. The research DAG remains the authority for unlocking content. */
public final class ChromaAdvancementProvider extends AdvancementProvider {
    public ChromaAdvancementProvider() { super(List.of(Generator::new)); }

    public static Identifier id(ProgressStage stage) {
        return Identifier.fromNamespaceAndPath("chromaticraft", "progression/" + stage.name().toLowerCase(Locale.ROOT));
    }

    public static void addTranslations(BiConsumer<String, String> add) {
        add.accept("advancements.chromaticraft.root.title", "ChromatiCraft");
        add.accept("advancements.chromaticraft.root.description", "Open the path to crystal research with the Chromic Lexicon");
        for (ProgressStage stage : ProgressStage.list) {
            if (stage == ProgressStage.NEVER) continue;
            String key = "advancements.chromaticraft." + stage.name().toLowerCase(Locale.ROOT);
            add.accept(key + ".title", ProgressionDescriptions.title(stage));
            String text = ProgressionDescriptions.reveal(stage);
            if (text.isBlank()) text = ProgressionDescriptions.hint(stage);
            if (text.isBlank()) text = ProgressionDescriptions.description(stage);
            add.accept(key + ".description", text.replaceAll("\\s+", " ").trim());
        }
    }

    private static final class Generator extends AdvancementSubProvider {
        private static final Set<ProgressStage> CHALLENGES = EnumSet.of(
                ProgressStage.CTM, ProgressStage.ALLCORES, ProgressStage.TURBOCHARGE,
                ProgressStage.DIMENSION, ProgressStage.STRUCTCOMPLETE);

        Generator(BootstrapContext<Advancement> output) { super(output); }

        @Override
        public void generate() {
            var lexicon = ChromaItems.LEXICON.get();
            var root = Advancement.Builder.advancement()
                    .rootDisplay(lexicon, Component.translatable("advancements.chromaticraft.root.title"),
                            Component.translatable("advancements.chromaticraft.root.description"),
                            Identifier.withDefaultNamespace("textures/block/obsidian.png"), AdvancementType.TASK, true, true, false)
                    .addCriterion("trigger", InventoryChangeTrigger.TriggerInstance.hasItems(lexicon))
                    .save(output, "chromaticraft:root");
            var built = new EnumMap<ProgressStage, AdvancementHolder>(ProgressStage.class);
            var remaining = new ArrayList<>(List.of(ProgressStage.list));
            remaining.remove(ProgressStage.NEVER);
            while (!remaining.isEmpty()) {
                boolean progressed = false;
                var iterator = remaining.iterator();
                while (iterator.hasNext()) {
                    ProgressStage stage = iterator.next();
                    // An advancement has one visual parent; research still enforces every DAG prerequisite.
                    ProgressStage parentStage = ProgressionManager.instance.getPrereqs(stage).stream()
                            .filter(p -> p != ProgressStage.NEVER).min(java.util.Comparator.comparingInt(Enum::ordinal)).orElse(null);
                    if (parentStage != null && !built.containsKey(parentStage)) continue;
                    var parent = parentStage == null ? root : built.get(parentStage);
                    String key = stage.name().toLowerCase(Locale.ROOT);
                    var builder = Advancement.Builder.advancement().parent(parent)
                            .display(lexicon, Component.translatable("advancements.chromaticraft." + key + ".title"),
                                    Component.translatable("advancements.chromaticraft." + key + ".description"),
                                    CHALLENGES.contains(stage) ? AdvancementType.CHALLENGE : AdvancementType.TASK,
                                    false, false, true)
                            .addCriterion("trigger", new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance()));
                    built.put(stage, builder.save(output, id(stage).toString()));
                    iterator.remove();
                    progressed = true;
                }
                if (!progressed) throw new IllegalStateException("Cyclic ChromatiCraft advancement parents: " + remaining);
            }
        }
    }
}
