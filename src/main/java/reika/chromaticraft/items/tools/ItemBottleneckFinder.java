/*******************************************************************************
 * @author Reika Kalseki
 * Copyright 2018. All rights reserved.
 * Distribution requires explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.network.PacketDistributor;
import reika.chromaticraft.base.ItemChromaTool;
import reika.chromaticraft.base.tileentity.TileEntityLocusPoint;
import reika.chromaticraft.magic.interfaces.CrystalNetworkTile;
import reika.chromaticraft.magic.interfaces.CrystalReceiver;
import reika.chromaticraft.magic.interfaces.CrystalRepeater;
import reika.chromaticraft.magic.network.CrystalNetworker;
import reika.chromaticraft.network.ChromaNetwork;
import reika.chromaticraft.registry.ChromaIcons;
import reika.chromaticraft.registry.ChromaSounds;
import reika.chromaticraft.registry.CrystalElement;
import reika.chromaticraft.tileentity.aoe.TileEntityAuraPoint;
import reika.dragonapi.auxiliary.trackers.TickScheduler;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.event.ScheduledTickEvent;
import reika.dragonapi.instantiable.event.scheduledtickevent.ScheduledSoundEvent;
import reika.dragonapi.libraries.mathsci.ReikaMusicHelper.MusicKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

/** V33a network diagnostic: blink every connected colour, then mark its three worst throughput/loss groups. */
public class ItemBottleneckFinder extends ItemChromaTool {
    public ItemBottleneckFinder(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var world = context.getLevel();
        var tile = world.getBlockEntity(context.getClickedPos());
        if (world.isClientSide() || !(context.getPlayer() instanceof ServerPlayer player)
                || !(tile instanceof CrystalReceiver receiver)) return InteractionResult.PASS;
        boolean connected = false;
        boolean point = TileEntityLocusPoint.isPointWithin(TileEntityAuraPoint.class, world, context.getClickedPos(), 512);
        for (CrystalElement element : CrystalElement.elements) {
            if (!receiver.isConductingElement(element)) continue;
            var path = CrystalNetworker.instance.getConnectivity(element, receiver);
            if (path == null) continue;
            connected = true;
            path.blink(200, receiver);
            List<Measurement> measurements = new ArrayList<>();
            for (CrystalNetworkTile node : path.getTileList())
                measurements.add(new Measurement(new WorldLocation(node.getWorld(), new BlockPos(node.getX(), node.getY(), node.getZ())),
                        node.maxThroughput(), node instanceof CrystalRepeater repeater ? repeater.getSignalDegradation(point) : 0));
            for (Warning warning : rank(measurements, element)) {
                if (player.connection == null || !player.connection.hasChannel(ChromaNetwork.BottleneckWarning.TYPE)) continue;
                PacketDistributor.sendToPlayer(player, new ChromaNetwork.BottleneckWarning(
                        warning.location().getDimension().identifier(), warning.location().pos,
                        warning.level().ordinal(), warning.throughput(), element.ordinal()));
            }
        }
        if (connected) {
            MusicKey[] notes = {MusicKey.C4, MusicKey.C5, MusicKey.E5, MusicKey.G5, MusicKey.C6};
            for (int i = 0; i < notes.length; i++) {
                var sound = new ScheduledSoundEvent(ChromaSounds.REPEATERRING, player, 1, (float)notes[i].getRatio(MusicKey.C5));
                sound.attenuate = false;
                TickScheduler.instance.scheduleEvent(new ScheduledTickEvent(sound), 1 + 13*i);
            }
        } else ChromaSounds.ERROR.playSoundAtBlock(tile);
        return InteractionResult.SUCCESS;
    }

    public record Measurement(WorldLocation location, int throughput, int loss) {}
    public record Warning(WorldLocation location, CrystalElement color, WarningLevels level, boolean throughput) {}

    /** Equal values share a rank; repeated nodes get only one warning per category and colour. */
    public static List<Warning> rank(Collection<Measurement> nodes, CrystalElement color) {
        TreeMap<Integer, List<WorldLocation>> throughput = new TreeMap<>();
        TreeMap<Integer, List<WorldLocation>> loss = new TreeMap<>();
        for (Measurement node : nodes) {
            throughput.computeIfAbsent(node.throughput(), key -> new ArrayList<>()).add(node.location());
            if (node.loss() > 0) loss.computeIfAbsent(node.loss(), key -> new ArrayList<>()).add(node.location());
        }
        List<Warning> warnings = new ArrayList<>();
        HashSet<WorldLocation> sentThroughput = new HashSet<>(), sentLoss = new HashSet<>();
        for (WarningLevels level : WarningLevels.list) {
            if (!throughput.isEmpty()) for (WorldLocation location : throughput.pollFirstEntry().getValue())
                if (sentThroughput.add(location)) warnings.add(new Warning(location, color, level, true));
            if (!loss.isEmpty()) for (WorldLocation location : loss.pollLastEntry().getValue())
                if (sentLoss.add(location)) warnings.add(new Warning(location, color, level, false));
        }
        return List.copyOf(warnings);
    }

    public enum WarningLevels {
        FIRST, SECOND, THIRD;
        public static final WarningLevels[] list = values();
        public Identifier getIcon() { return ChromaIcons.BLACKHOLE.getIcon(); }
    }
}

