# Magic Outpost (Biome Fragment): player guide to the melody puzzle

Minecraft 26.3 / NeoForge 26.3.0.51-beta. The Magic Outpost is V33a's Biome Fragment, the overworld
structure with a melody puzzle. This guide is for solving it in game. For what was broken and how it
was fixed, see the 2026-10-15 entry in `PORTING.md`.

## Finding one

- **Natural generation:** the Outpost generates only in the Rainbow Forest, Ender Forest and
  Luminous Cliffs biomes. It is sunk into the ground, so what you see on the surface is a stone ring
  roughly 15 blocks across, with stairs leading down into it.
- **Structure Map:** use the Structure Map's **Magic Outpost** entry.
- **Commands (testing):** run `/place feature chromaticraft:biome_fragment`. The structure's centre,
  its controller, is the block you place it at.

## Before you go in

Mining the Lock Keys and passing the central shaft both need the prerequisites of the
*An Abandoned Puzzle* stage. Those are *Enriching Solvent*, *More Complex Crafting*, *Liquid Effects*
and *Refined Transmission*. Without them:

- the Lock Keys will not break;
- the Panel Switches flick back down with an error sound;
- the Hover Field shaft over the centre lifts you and you cannot sink back down it.

## The melody

Come within 20 blocks of a freshly generated Outpost and it plays its melody once on its own. The
notes ring from the centre. Each one throws sparks in every crystal colour that can play that note,
which is a hint to which lamps to look for.

To hear it again, wait for the melody to finish and then a two-second cooldown. Then do either of
these:

- right-click the **Melody Replay Pedestal**, the block at the very top of the centre, ten blocks
  above the controller;
- hold **Examination Crystals** in your main hand anywhere within 20 blocks.

## Getting to the instruments

The structure has three stages. Each one opens the way to the next.

1. **Panel Switches** sit in the four upper corners, each with a red/green Light Panel indicator.
   - Every corner door has its own on/off pattern across all four switches. A door is never opened
     by its own switch alone.
   - When the switches match a door's pattern, that corner's Shift Lock walls open and its
     indicator turns green.
2. **Lock Keys:** two glowing keys float in the shaft. Mine them; they keep their channel. In the
   upper corner rooms, set them on top of the coloured **Crystal Runes**.
   - Each of the four **Color Locks** around the lower ring needs two particular colours.
   - A Color Lock opens while both of its colours have a key on their rune. It closes again when a
     key is taken off.
   - You only have two keys, so you can open one music room at a time.
3. **Music rooms:** behind each Color Lock is a room with two **Crystal Music Triggers**. Each
   trigger has a coloured crystal lamp standing on it.

## Playing the notes back

Each trigger's front face has four buttons. The crystal above it decides which notes they play:

| Button      | Note played                               |
|-------------|-------------------------------------------|
| top-left    | the crystal's base note (tonic)           |
| top-right   | its third (minor for the minor colours)   |
| bottom-right| its fifth                                 |
| bottom-left | its octave                                |

Left and right are as you see the face, standing in front of it. Clicking the gap between buttons,
or the top of the trigger, does nothing.

- Play the melody's notes in order. Rests in the melody are skipped, so you only play the notes.
- A correct note just rings.
- **A wrong note plays the error sound from the centre and restarts the melody from its first note.**
- Progress is kept while you walk away. The melody's notes are usually spread over more than one
  room. Play what you can in one room, move the keys, open the next room and carry on, as long as
  you never press a wrong note.
- High notes use the crystal's high recording, so above C6 every note still sounds distinct, as it
  did in 1.7.10.
- Redstone also plays a trigger. Signal strength 1–3 plays the top-left button, 4–7 top-right,
  8–11 bottom-right and 12–15 bottom-left.

## When it is solved

On the last correct note the Outpost plays its casting sound, and two things happen:

- the **Ethereal Barriers** in the floor of the central shaft, and the walls sealing the four
  lower corners, open for good;
- the four lower loot caches are unlocked.

Below the opened floor is the Outpost's liquid pool. It is lava, chroma, ender or luma, depending on
the biome.
