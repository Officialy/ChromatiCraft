package reika.chromaticraft.registry;

import reika.chromaticraft.render.isbrh.ArtefactRenderer;
import reika.chromaticraft.render.isbrh.BedrockCrackRenderer;
import reika.chromaticraft.render.isbrh.CaveIndicatorRenderer;
import reika.chromaticraft.render.isbrh.CliffStoneRenderer;
import reika.chromaticraft.render.isbrh.ColorLockRenderer;
import reika.chromaticraft.render.isbrh.ConsoleRenderer;
import reika.chromaticraft.render.isbrh.CrystalEncrustingRenderer;
import reika.chromaticraft.render.isbrh.CrystalFenceRenderer;
import reika.chromaticraft.render.isbrh.CrystalGlassRenderer;
import reika.chromaticraft.render.isbrh.CrystalGlowRenderer;
import reika.chromaticraft.render.isbrh.CrystalRenderer;
import reika.chromaticraft.render.isbrh.CrystallineStoneRenderer;
import reika.chromaticraft.render.isbrh.DecoFlowerRenderer;
import reika.chromaticraft.render.isbrh.DecoPlantRenderer;
import reika.chromaticraft.render.isbrh.DimensionDecoRenderer;
import reika.chromaticraft.render.isbrh.DyeVineRenderer;
import reika.chromaticraft.render.isbrh.EverFluidRenderer;
import reika.chromaticraft.render.isbrh.GlowTreeRenderer;
import reika.chromaticraft.render.isbrh.LampRenderer;
import reika.chromaticraft.render.isbrh.LaserEffectorRenderer;
import reika.chromaticraft.render.isbrh.MetaAlloyRenderer;
import reika.chromaticraft.render.isbrh.PistonTargetRenderer;
import reika.chromaticraft.render.isbrh.PowerTreeRenderer;
import reika.chromaticraft.render.isbrh.RayBlendFloorRenderer;
import reika.chromaticraft.render.isbrh.RelayRenderer;
import reika.chromaticraft.render.isbrh.RuneRenderer;
import reika.chromaticraft.render.isbrh.SelectiveGlassRenderer;
import reika.chromaticraft.render.isbrh.SparklingBlockRender;
import reika.chromaticraft.render.isbrh.SpecialShieldRenderer;
import reika.chromaticraft.render.isbrh.TankBlockRenderer;
import reika.chromaticraft.render.isbrh.TieredOreRenderer;
import reika.chromaticraft.render.isbrh.TieredPlantRenderer;
import reika.chromaticraft.render.isbrh.VoidRiftRenderer;
import reika.dragonapi.base.ISBRH;
import reika.dragonapi.interfaces.registry.ISBRHEnum;

public enum ChromaISBRH implements ISBRHEnum {

	crystal(CrystalRenderer.class),
	rune(RuneRenderer.class),
	crystalStone(CrystallineStoneRenderer.class),
	tank(TankBlockRenderer.class),
	tree(PowerTreeRenderer.class),
	lamp(LampRenderer.class),
	relay(RelayRenderer.class),
	glow(CrystalGlowRenderer.class),
	vrift(VoidRiftRenderer.class),
	dimgen(DimensionDecoRenderer.class),
	glowTree(GlowTreeRenderer.class),
	colorLock(ColorLockRenderer.class),
	specialShield(SpecialShieldRenderer.class),
	glass(CrystalGlassRenderer.class),
	console(ConsoleRenderer.class),
	fence(CrystalFenceRenderer.class),
	selective(SelectiveGlassRenderer.class),
	lasereffect(LaserEffectorRenderer.class),
	rayblendFloor(RayBlendFloorRenderer.class),
	piston(PistonTargetRenderer.class),

	artefact(ArtefactRenderer.class),
	metaAlloy(MetaAlloyRenderer.class),

	encrusted(CrystalEncrustingRenderer.class),

	ore(TieredOreRenderer.class),
	plant(TieredPlantRenderer.class),
	plant2(DecoPlantRenderer.class),
	flower(DecoFlowerRenderer.class),
	sparkle(SparklingBlockRender.class),
	everfluid(EverFluidRenderer.class),
	cliffstone(CliffStoneRenderer.class),
	caveIndicator(CaveIndicatorRenderer.class),
	bedrockCrack(BedrockCrackRenderer.class),
	dyeVine(DyeVineRenderer.class),

	;

	private final Class<? extends ISBRH> renderClass;

	private int renderID;
	private ISBRH renderer;

	private static final ChromaISBRH[] list = values();

	private ChromaISBRH(Class<? extends ISBRH> render) {
		renderClass = render;
	}

	@Override
	public int getRenderID() {
		return renderID;
	}

	@Override
	public ISBRH getRenderer() {
		return renderer;
	}

	@Override
	public void setRenderPass(int pass) {
		renderer.setRenderPass(pass);
	}

	@Override
	public Class<? extends ISBRH> getRenderClass() {
		return renderClass;
	}

	@Override
	public void setRenderID(int id) {
		renderID = id;
	}

	@Override
	public void setRenderer(ISBRH r) {
		renderer = r;
	}

}
