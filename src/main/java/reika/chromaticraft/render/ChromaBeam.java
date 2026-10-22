/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import reika.dragonapi.client.EntityLighting;
import reika.dragonapi.client.ScreenParticleRendering;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.phys.Vec3;

import reika.chromaticraft.registry.ChromaIcons;
import reika.dragonapi.libraries.rendering.ReikaColorAPI;

/**
 * V33a {@code ChromaFX.renderBeam}: a thin full-bright core line from {@code c1} to {@code c2}, then, for every
 * whole block of length and the remaining fraction, three crossed {@code LASER} planes of half-width {@code h}
 * (vertical, across X, across Z), coloured at {@code a/255} brightness and alpha, blending {@code c1} to {@code c2}
 * along the beam. V33a relied on the caller's ADDITIVEDARK, unlit, depth-mask-off state; here the planes use the
 * lightmap-only screen sprite type and the core a lightmapped screen line type. Fully unlit callers may emit planes into their own context.
 */
public final class ChromaBeam {

	private ChromaBeam() {}

	public static void submit(PoseStack poseStack, SubmitNodeCollector collector, Vec3 p1, Vec3 p2, int a, double h,
            int c1, int c2) {
        submit(poseStack,collector,p1,p2,a,h,c1,c2,0,true);
    }

    /** The default ChromaFX caller retains the lightmap; fully unlit callers own their materials via planes(). */
    public static void submit(PoseStack stack,SubmitNodeCollector collector,Vec3 p1,Vec3 p2,int a,double h,
            int c1,int c2,int order,boolean composed) {
        submitGeometry(stack,collector,order,composed,ChromaRenderPipelines.LIGHTMAPPED_SCREEN_LINES_TYPE,(pose,out)-> {
            Vec3 normal=p2.subtract(p1).normalize();if(normal.lengthSqr()==0)normal=new Vec3(0,1,0);
            out.addVertex(pose,(float)p1.x,(float)p1.y,(float)p1.z).setColor(0xff000000|c1).setLight(240)
                .setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z).setLineWidth(1);
            out.addVertex(pose,(float)p2.x,(float)p2.y,(float)p2.z).setColor(0xff000000|c2).setLight(240)
                .setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z).setLineWidth(1);
        });
        var icon=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ChromaIcons.LASER.getIcon());
        submitGeometry(stack,collector,order+1,composed,EntityLighting.LIGHTMAP_ONLY.screen(TextureAtlas.LOCATION_BLOCKS,false),
            (pose,out)->planes(pose,out,icon,p1,p2,a,h,c1,c2,240));
    }
    private static void submitGeometry(PoseStack stack,SubmitNodeCollector collector,int order,boolean composed,
            RenderType type,SubmitNodeCollector.CustomGeometryRenderer geometry) {
        var pose=stack.last().copy();SubmitNodeCollector.CustomGeometryRenderer replay=(ignored,out)->geometry.render(pose,out);
        if(composed)collector.order(order).submitSpecial(ScreenParticleRendering.PHASE,new CustomFeatureRenderer.Submit(pose,type,replay));
        else collector.order(order).submitCustomGeometry(stack,type,replay);
    }

	/** Emit the original whole-block sections and final remainder into a caller-owned material/phase. */
	public static void planes(PoseStack.Pose pose, VertexConsumer v5, TextureAtlasSprite ico, Vec3 p1, Vec3 p2, int a,
            double h,int c1,int c2) {
        planes(pose,v5,ico,p1,p2,a,h,c1,c2,-1);
    }
    private static void planes(PoseStack.Pose pose,VertexConsumer v5,TextureAtlasSprite ico,Vec3 p1,Vec3 p2,int a,
            double h,int c1,int c2,int light) {
        double dd = p1.distanceTo(p2);
		int n = (int)dd;
		double dn = dd-n;
		for (int d = 0; d < n; d++) {
			int ca = c1 != c2 ? ReikaColorAPI.mixColors(c1, c2, 1-(float)(d/dd)) : c1;
			int cb = c1 != c2 ? ReikaColorAPI.mixColors(c1, c2, 1-(float)((d+1)/dd)) : c1;
			section(pose, v5, ico, lerp(p1, p2, d/dd), lerp(p1, p2, (d+1)/dd), a, h, ca, cb, light);
		}
		if (dn > 0) {
			int ca = c1 != c2 ? ReikaColorAPI.mixColors(c1, c2, 1-(float)(n/dd)) : c1;
			int cb = c2;
			section(pose, v5, ico, lerp(p1, p2, n/dd), lerp(p1, p2, (dn+n)/dd), a, h, ca, cb, light);
		}
	}

	private static Vec3 lerp(Vec3 p1, Vec3 p2, double f) {
		return p1.add(p2.subtract(p1).scale(f));
	}

	private static void section(PoseStack.Pose pose, VertexConsumer v5, TextureAtlasSprite ico, Vec3 s, Vec3 e, int a,
			double h, int ca, int cb, int light) {
		int colA = argb(ca, a);
		int colB = argb(cb, a);
		float u = ico.getU0(), v = ico.getV0(), du = ico.getU1(), dv = ico.getV1();
		quad(pose, v5, s.add(0, -h, 0), s.add(0, h, 0), e.add(0, h, 0), e.add(0, -h, 0), colA, colB, u, v, du, dv, light);
		quad(pose, v5, s.add(-h, 0, 0), s.add(h, 0, 0), e.add(h, 0, 0), e.add(-h, 0, 0), colA, colB, u, v, du, dv, light);
		quad(pose, v5, s.add(0, 0, -h), s.add(0, 0, h), e.add(0, 0, h), e.add(0, 0, -h), colA, colB, u, v, du, dv, light);
	}

	/** V33a {@code setColorRGBA_I(getColorWithBrightnessMultiplier(c, a/255F), a)}. */
	private static int argb(int c, int a) {
		return a << 24 | ReikaColorAPI.getColorWithBrightnessMultiplier(c, a/255F) & 0xffffff;
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer v5, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, int colA,
			int colB, float u, float v, float du, float dv, int light) {
		v5.addVertex(pose, (float)p1.x, (float)p1.y, (float)p1.z).setUv(u, v).setColor(colA);
        if(light>=0)v5.setLight(light).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(pose,0,1,0);
		v5.addVertex(pose, (float)p2.x, (float)p2.y, (float)p2.z).setUv(u, dv).setColor(colA);
        if(light>=0)v5.setLight(light).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(pose,0,1,0);
		v5.addVertex(pose, (float)p3.x, (float)p3.y, (float)p3.z).setUv(du, dv).setColor(colB);
        if(light>=0)v5.setLight(light).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(pose,0,1,0);
		v5.addVertex(pose, (float)p4.x, (float)p4.y, (float)p4.z).setUv(du, v).setColor(colB);
        if(light>=0)v5.setLight(light).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(pose,0,1,0);
	}

}
