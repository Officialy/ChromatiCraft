/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.models;

import java.util.List;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** V33a ModelFocusCrystalMessy (Techne, 64x32): the disordered ring model of the Flawed and Default tiers. */
public final class ModelFocusCrystalMessy extends ModelFocusCrystalBase {

	private static final List<String> OUTER = List.of("Shape1r", "Shape1s", "Shape1t", "Shape1u", "Shape1v", "Shape1x", "Shape1a", "Shape1b", "Shape1c", "Shape1d", "Shape1e", "Shape1f", "Shape1m", "Shape1n", "Shape1o");
	private static final List<String> INNER = List.of("Shape1g", "Shape1h", "Shape1i", "Shape1j", "Shape1k");
	private static final List<String> LOOSE = List.of("Shape2", "Shape2a", "Shape2b", "Shape2c", "Shape2h", "Shape2i");

	public ModelFocusCrystalMessy(ModelPart root) {
		super(root, OUTER, INNER, LOOSE);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		part(root, "Shape1r", 0, 10, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 0.515163F, 0.4363323F, true);
		part(root, "Shape1s", 0, 0, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 1.304052F, 0.4363323F, true);
		part(root, "Shape1t", 0, 5, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 1.791581F, 0.4363323F, true);
		part(root, "Shape1u", 0, 0, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 2.094395F, 0.4363323F, true);
		part(root, "Shape1v", 9, 11, -4, 2.5F, -4, 2, 2, 2, 0, 23, 0, -0.3246312F, 2.770128F, 0.4363323F, true);
		part(root, "Shape1x", 18, 11, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, -2.866122F, 0.4363323F, true);
		part(root, "Shape1a", 9, 0, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4049164F, -0.5902122F, 0.3839724F, true);
		part(root, "Shape1b", 18, 0, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, -0.9381145F, 0.4363323F, true);
		part(root, "Shape1c", 27, 0, -4, 2, -4, 2, 2, 2, 0, 23, 0, -0.3188135F, -1.451241F, 0.3700098F, true);
		part(root, "Shape1d", 27, 5, -3, 4, -3, 2, 2, 2, 0, 23, 0, -0.3467387F, -2.048144F, 0.9564404F, true);
		part(root, "Shape1e", 0, 15, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 0.1777327F, 0.4363323F, true);
		part(root, "Shape1f", 14, 16, -3.5F, 2.5F, -4.5F, 1, 1, 1, 0, 23, 0, -0.4363323F, -1.844522F, 0.4363323F, true);
		part(root, "Shape1g", 10, 8, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, 1.53589F, 0.4363323F, true);
		part(root, "Shape1h", 10, 5, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, 2.792527F, 0.4363323F, true);
		part(root, "Shape1i", 15, 8, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, -2.77071F, 0.4363323F, true);
		part(root, "Shape1j", 15, 5, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, -1.570796F, 0.4363323F, true);
		part(root, "Shape1k", 20, 5, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, 0.1745329F, 0.4363323F, true);
		part(root, "Shape1m", 9, 16, -4.5F, 2.5F, -4.5F, 1, 1, 1, 0, 23, 0, -0.4363323F, 2.094395F, 0.4363323F, true);
		part(root, "Shape1n", 0, 20, -4.5F, 2.5F, -4.5F, 1, 1, 1, 0, 23, 0, -0.4363323F, 0.1090831F, 0.1419534F, true);
		part(root, "Shape1o", 19, 16, -4.5F, 4.5F, -4.5F, 1, 1, 1, 0, 23, 0, -0.4363323F, 1.156281F, 0.4363323F, true);
		part(root, "Shape2", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, -0.2181662F, 0, true);
		part(root, "Shape2a", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, -1.788962F, 0, true);
		part(root, "Shape2b", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, 0.2181662F, 0, true);
		part(root, "Shape2c", 0, 24, -10, 0, -0.5F, 1, 4, 1, 0, 20, 0, 0, 2.356194F, 0, true);
		part(root, "Shape2h", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, 1.35263F, 0, true);
		part(root, "Shape2i", 0, 24, -10, 0, -0.5F, 1, 4, 1, 0, 20, 0, 0, 0.7853982F, 0, true);
		return LayerDefinition.create(mesh, 64, 32);
	}

}
