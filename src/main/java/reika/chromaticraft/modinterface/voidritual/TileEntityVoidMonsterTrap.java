package reika.chromaticraft.modinterface.voidritual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import reika.chromaticraft.auxiliary.interfaces.MultiBlockChromaTile;
import reika.chromaticraft.base.tileentity.InventoriedCrystalReceiver;
import reika.chromaticraft.magic.ElementTagCompound;
import reika.chromaticraft.registry.*;
import reika.chromaticraft.tileentity.TileEntityLumenWire;
import reika.dragonapi.auxiliary.ChunkManager;
import reika.dragonapi.instantiable.data.immutable.Coordinate;
import reika.dragonapi.instantiable.data.immutable.DecimalPosition;
import reika.dragonapi.instantiable.data.immutable.WorldLocation;
import reika.dragonapi.instantiable.math.Spline.SplineType;
import reika.dragonapi.instantiable.math.VariableEndpointSpline;
import reika.dragonapi.interfaces.blockentity.ChunkLoadingTile;
import reika.dragonapi.interfaces.blockentity.Connectable;
import reika.dragonapi.interfaces.blockentity.InertIInv;
import reika.voidmonster.api.MonsterAPI;
import reika.voidmonster.entity.EntityVoidMonster;

import java.util.*;

/** V33a trap, native receiver/inventory, registry-key links, chunk tickets and UUID-qualified tethers. */
public class TileEntityVoidMonsterTrap extends InventoriedCrystalReceiver implements MultiBlockChromaTile,
        TileEntityLumenWire.WireWatcher, Connectable<WorldLocation>, ChunkLoadingTile, InertIInv {
    private static final ElementTagCompound REQUIRED=new ElementTagCompound();
    private static final Map<Level,Set<BlockPos>> CACHE=new WeakHashMap<>();
    static { REQUIRED.addTag(CrystalElement.BLACK,5); REQUIRED.addTag(CrystalElement.PINK,20); REQUIRED.addTag(CrystalElement.LIGHTGRAY,4); REQUIRED.addTag(CrystalElement.GRAY,1); REQUIRED.addTag(CrystalElement.WHITE,10); REQUIRED.addTag(CrystalElement.MAGENTA,2); }
    private static final int RING_DURATION=400;
    private final List<Coordinate> wireSeek=new ArrayList<>(), explosionSeek=new ArrayList<>();
    private final Map<UUID,VoidMonsterTether> tethers=new LinkedHashMap<>();
    private float flashFactor,shaderRotation,shaderRotationSpeed;
    private int outerRingActivation,innerRingActivation;
    private boolean hasStructure;
    private boolean previewNether;
    public void setPreviewNether(boolean value) { previewNether=value; }
    public boolean isPreviewNether() { return previewNether; }
    private WorldLocation link;
    private VoidMonsterDestructionRitual ritual;
    public TileEntityVoidMonsterTrap(BlockPos pos,BlockState state) { super(ChromaBlockEntities.VOID_MONSTER_TRAP.get(),pos,state); }
    public static ElementTagCompound getRequiredEnergy() { return REQUIRED.copy(); }
    public void activateOuterRing() { outerRingActivation=RING_DURATION; }
    public void activateInnerRing() { if (outerRingActivation>0) outerRingActivation=innerRingActivation=RING_DURATION; }
    @Override public void validateStructure() {
        if (level==null) return;
        var structure=getPrimaryStructure(); structure.getStructure().resetToDefaults();
        hasStructure=structure.getArray(level,worldPosition.getX(),worldPosition.getY(),worldPosition.getZ()).matchInWorld();
        syncAllData(false);
    }
    public boolean canAttractMonster() { return hasStructure && (link!=null || !isNether()) && !isActive(); }
    public boolean isNether() { return level!=null && level.dimension().equals(Level.NETHER); }
    @Override protected void onFirstTick(Level world,BlockPos pos) {
        super.onFirstTick(world,pos);
        if (isNether()) explosionSeek.addAll(VoidMonsterNetherStructure.getTNTLocations());
        else wireSeek.addAll(VoidMonsterRitualStructure.getWireLocations());
        CACHE.computeIfAbsent(world,k->new HashSet<>()).add(pos.immutable());
        validateStructure(); if (!world.isClientSide()) ChunkManager.instance.loadChunks(this);
    }
    @Override public Collection<ChunkPos> getChunksToLoad() { return ChunkManager.getChunkSquare(worldPosition.getX(),worldPosition.getZ(),2); }
    @Override public void onChunkUnloaded() { ChunkManager.instance.unloadChunks(this); super.onChunkUnloaded(); }
    @Override public void breakBlock() {
        Set<BlockPos> positions=CACHE.get(level); if (positions!=null) positions.remove(worldPosition);
        ChunkManager.instance.unloadChunks(this);
        if (ritual!=null) { ritual.onPrematureTermination(); ritual=null; }
        resetOther(); reset();
    }
    @Override public void updateEntity(Level world,BlockPos pos) {
        super.updateEntity(world,pos);
        if (world.isClientSide()) {
            tethers.values().removeIf(t->t.tick(world)); animate(); return;
        }
        if (hasStructure && getCooldown()==0 && checkTimer.checkCap())
            for (CrystalElement color:REQUIRED.elementSet()) if (getRemainingSpace(color)>0) requestEnergy(color,getRemainingSpace(color));
        if (!hasStructure && getTicksExisted()<5) validateStructure();
        if (inv.get(0).isEmpty() || inv.get(0).getCount()<getMaxStackSize()) searchForItems(world,pos);
        if (isNether()) {
            if (explosionSeek.size()<4 && hasStructure) hasStructure=false;
            if (canAttractMonster()) attractMonster();
        } else {
            if (!wireSeek.isEmpty()) {
                Coordinate offset=wireSeek.get(rand.nextInt(wireSeek.size()));
                BlockPos wire=pos.offset(offset.xCoord() -14, offset.yCoord() -8, offset.zCoord() -14);
                if (world.getBlockEntity(wire) instanceof TileEntityLumenWire watcher) watcher.addWatcher(this);
            }
            EntityVoidMonster monster=getMonster();
            if (monster!=null && inv.get(0).is(ChromaItems.VOID_MONSTER_ESSENCE.get()) && energy.containsAtLeast(REQUIRED)) {
                if (isActive()) { if (ritual.tick()) { ritual.onCompletion(); ritual=null; } }
                else if (canAttractMonster() && attractMonster()<1) ritual=new VoidMonsterDestructionRitual(this,monster);
                drainEnergy(REQUIRED);
                if (rand.nextInt(isActive()?20:60)==0) { inv.get(0).shrink(1); setChanged(); }
            }
        }
        if (outerRingActivation>0) outerRingActivation--;
        if (innerRingActivation>0) innerRingActivation--;
    }
    private void searchForItems(Level world,BlockPos pos) {
        for (ItemEntity item:world.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(8,5,8),e->e.getItem().is(ChromaItems.VOID_MONSTER_ESSENCE.get()))) {
            if (item.tickCount<=5) continue;
            Vec3 delta=Vec3.atCenterOf(pos).subtract(item.position()); double distance=delta.length();
            if (distance<.5) {
                ItemStack current=inv.get(0),incoming=item.getItem();
                if (current.isEmpty() || ItemStack.isSameItemSameComponents(current,incoming) && current.getCount()<getMaxStackSize()) {
                    if (current.isEmpty()) inv.set(0,incoming.copyWithCount(1)); else current.grow(1);
                    incoming.shrink(1); if (incoming.isEmpty()) item.discard(); else item.setItem(incoming);
                    setChanged(); VoidRitualNetwork.eat(this); ChromaSounds.BOUNCE.playSoundAtBlock(this,1,2);
                }
            } else {
                double f=Math.min(1,item.tickCount/80D); Vec3 velocity=delta.scale(2.5/distance);
                velocity=new Vec3(net.minecraft.util.Mth.clamp(velocity.x,-.0625,.0625),net.minecraft.util.Mth.clamp(velocity.y,-.0625,.0625),net.minecraft.util.Mth.clamp(velocity.z,-.0625,.0625));
                item.setDeltaMovement(item.getDeltaMovement().scale(1-f).add(velocity.scale(f)).add(0,item.getY()<pos.getY()?.125:0,0)); item.syncVelocity=true;
            }
        }
    }
    private EntityVoidMonster getMonster() { return ritual!=null?ritual.getEntity():(EntityVoidMonster)MonsterAPI.getNearestMonster(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5); }
    private double attractMonster() {
        EntityVoidMonster monster=getMonster(); if (monster==null) return Double.POSITIVE_INFINITY;
        double distance=monster.distanceToSqr(Vec3.atCenterOf(worldPosition));
        if (distance>(isNether()?256:64)) { tethers.remove(monster.getUUID()); return distance; }
        var tether=tethers.computeIfAbsent(monster.getUUID(),id->new VoidMonsterTether(worldPosition,monster));
        tether.distance=monster.moveTowards(worldPosition.getX()+.5,worldPosition.getY()-.5,worldPosition.getZ()+.5,(isNether()?1:1.5)*Math.min(1,20/distance));
        syncAllData(true); return tether.distance;
    }
    private void triggerTeleport() {
        EntityVoidMonster monster=getMonster();
        if (monster==null || link==null || !(level instanceof ServerLevel source)) return;
        ServerLevel target=source.getServer().getLevel(link.getDimension()); if (target==null) return;
        Vec3 destination=findDestination(target,monster,link.pos);
        boolean persisted=monster.forcePersist; monster.forcePersist=true;
        // The randomized exit can lie outside the linked trap's forced chunk square.
        // Native portal tickets let the destination entity lookup begin tracking the arrival.
        Entity moved=monster.teleport(new TeleportTransition(target,destination,monster.getDeltaMovement(),monster.getYRot(),monster.getXRot(),TeleportTransition.PLACE_PORTAL_TICKET));
        if (moved==null) { monster.forcePersist=persisted; return; }
        source.explode(null,worldPosition.getX()+.5,worldPosition.getY()-.5,worldPosition.getZ()+.5,9,true,Level.ExplosionInteraction.BLOCK);
        source.removeBlock(worldPosition,false);
    }
    private Vec3 findDestination(ServerLevel target,Entity monster,BlockPos center) {
        // V33a tested a stale box around the offset, then retried forever. Test actual destinations
        // and walk upward after randomized trials, guaranteeing a collision-free exit.
        for (int attempt=0;attempt<256;attempt++) {
            double radius=24+rand.nextDouble()*36,angle=rand.nextDouble()*Math.PI*2;
            Vec3 point=Vec3.atCenterOf(center).add(radius*Math.cos(angle),rand.nextDouble()*12-6,radius*Math.sin(angle));
            target.getChunk(BlockPos.containing(point));
            if (target.noCollision(monster,monster.getBoundingBox().move(point.subtract(monster.position())))) return point;
        }
        Vec3 point=Vec3.atCenterOf(center).add(24,0,0);
        while (point.y<target.getMaxY()+4 && !target.noCollision(monster,monster.getBoundingBox().move(point.subtract(monster.position())))) point=point.add(0,1,0);
        return point;
    }
    @Override protected void animateWithTick(Level world,BlockPos pos) { }
    private void animate() {
        if (flashFactor>0) flashFactor=Math.max(flashFactor*.92F-.09F,0); else if (rand.nextInt(70)==0) flashFactor=2;
        if (shaderRotationSpeed>0) { boolean negative=shaderRotation<0; shaderRotation+=shaderRotationSpeed;
            if (shaderRotation>=1) shaderRotation=-1; else if (negative && shaderRotation>=0) shaderRotation=shaderRotationSpeed=0;
        } else if (rand.nextInt(40)==0) shaderRotationSpeed=.0625F+rand.nextFloat()*.0625F;
    }
    public float getFlashBrightness() { return flashFactor<=0 || flashFactor>2?0:flashFactor<=1?flashFactor:2-flashFactor; }
    public float getShaderRotation() { return shaderRotation; }
    public boolean isActive() { return ritual!=null; }
    public Collection<VoidMonsterTether> getTethers() { return List.copyOf(tethers.values()); }
    @Override public int getSizeInventory() { return 1; }
    @Override public int getMaxStackSize() { return 64; }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side) { return false; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return false; }
    @Override public ChromaTiles getTile() { return ChromaTiles.VOIDTRAP; }
    @Override public void onToggle(Coordinate wire,boolean active) { if (wire.yCoord() ==worldPosition.getY()-1) activateInnerRing(); else activateOuterRing(); }
    @Override protected void readSyncTag(CompoundTag tag) {
        super.readSyncTag(tag); hasStructure=tag.getBooleanOr("struct",false); link=WorldLocation.load("link",tag);
        Set<UUID> received=new HashSet<>();
        for (Tag value:tag.getListOrEmpty("tethers")) if (value instanceof CompoundTag data) {
            VoidMonsterTether incoming=VoidMonsterTether.read(data); if (incoming==null) continue; received.add(incoming.uuid);
            VoidMonsterTether current=tethers.get(incoming.uuid); if (current==null) tethers.put(incoming.uuid,incoming); else current.distance=incoming.distance;
        }
        tethers.keySet().retainAll(received);
    }
    @Override protected void writeSyncTag(CompoundTag tag) {
        super.writeSyncTag(tag); tag.putBoolean("struct",hasStructure);
        ListTag list=new ListTag(); tethers.values().forEach(t->list.add(t.write())); tag.put("tethers",list);
        if (link!=null) link.saveAdditional("link",tag);
    }
    @Override public int getPacketDelay() { return tethers.isEmpty()?super.getPacketDelay():Math.max(1,super.getPacketDelay()/2); }
    @Override public boolean hasStructure() { return hasStructure; }
    @Override public ChromaStructures getPrimaryStructure() { return isNether()?ChromaStructures.NETHERTRAP:ChromaStructures.VOIDRITUAL; }
    @Override public Coordinate getStructureOffset() { return null; }
    @Override public boolean canStructureBeInspected() { return true; }
    public static boolean handleTNTTrigger(Level world,Entity entity) {
        for (BlockPos pos:List.copyOf(CACHE.getOrDefault(world,Set.of()))) if (world.getBlockEntity(pos) instanceof TileEntityVoidMonsterTrap trap && trap.handleTNTTrigger(entity)) return true;
        return false;
    }
    private boolean handleTNTTrigger(Entity entity) {
        if (!isNether() || !hasValidConnection() || !(hasStructure || explosionSeek.size()<4)) return false;
        Coordinate point=new Coordinate(entity).offset(-worldPosition.getX()+2,-worldPosition.getY()+6,-worldPosition.getZ()+2);
        if (!explosionSeek.remove(point)) return false;
        hasStructure=false; if (explosionSeek.isEmpty()) triggerTeleport(); return true;
    }
    public static void clearCache() { CACHE.clear(); }
    public static void unload(Level world) { CACHE.remove(world); }
    @Override public void reset() { link=null; setChanged(); }
    @Override public void resetOther() { if (link!=null && link.getBlockEntity() instanceof Connectable<?> other) other.reset(); }
    @Override public boolean tryConnect(Level world,BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityVoidMonsterTrap other)) return false;
        other.link=new WorldLocation(this); link=new WorldLocation(other); other.syncAllData(true); syncAllData(true); return true;
    }
    @Override public boolean isEmitting() { return true; }
    @Override public WorldLocation getConnection() { return link; }
    @Override public boolean hasValidConnection() { return link!=null && link.getBlockEntity() instanceof TileEntityVoidMonsterTrap; }
    @Override public int getReceiveRange() { return 24; }
    @Override public boolean isConductingElement(CrystalElement color) { return REQUIRED.contains(color); }
    @Override public int maxThroughput() { return 100; }
    @Override public boolean canConduct() { return hasStructure; }
    @Override public int getMaxStorage(CrystalElement color) { return REQUIRED.getValue(color)*600; }
    public static final class VoidMonsterTether {
        public final int ID;
        public final UUID uuid;
        public final Coordinate location;
        private double distance;
        private final VariableEndpointSpline[] splines=new VariableEndpointSpline[3];
        private VoidMonsterTether(BlockPos pos,Entity entity) { this(new Coordinate(pos),entity.getId(),entity.getUUID(),new DecimalPosition(entity)); }
        private VoidMonsterTether(Coordinate pos,int id,UUID uuid,DecimalPosition endpoint) {
            location=pos; ID=id; this.uuid=uuid;
            for (int i=0;i<3;i++) splines[i]=new VariableEndpointSpline(pos.xCoord() +.5, pos.yCoord() +.5, pos.zCoord() +.5, endpoint.xCoord(), endpoint.yCoord(), endpoint.zCoord(),SplineType.CENTRIPETAL,6,1.2,.125);
        }
        public boolean tick(Level world) {
            Entity entity=getEntity(world); if (entity==null) return true;
            for (var spline:splines) { spline.setEndpoint(entity.getX(),entity.getY()+entity.getBbHeight()/2,entity.getZ()); spline.tick(); } return false;
        }
        public Collection<VariableEndpointSpline> getSplines() { return Arrays.asList(splines); }
        public double getDistance() { return distance; }
        public Entity getEntity(Level world) { Entity entity=world.getEntity(ID); return entity!=null && uuid.equals(entity.getUUID())?entity:null; }
        private CompoundTag write() { CompoundTag data=new CompoundTag(); data.putInt("id",ID); data.putString("uuid",uuid.toString()); data.putDouble("dist",distance); location.writeToTag(data); return data; }
        private static VoidMonsterTether read(CompoundTag data) { if (data.read("uuid",net.minecraft.core.UUIDUtil.STRING_CODEC).isEmpty()) return null; var tether=new VoidMonsterTether(Coordinate.readTag(data),data.getIntOr("id",0),UUID.fromString(data.getStringOr("uuid","")),new DecimalPosition(0,0,0)); tether.distance=data.getDoubleOr("dist",0); return tether; }
    }
}
