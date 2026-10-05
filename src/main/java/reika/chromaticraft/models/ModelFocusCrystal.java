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

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.util.List;

/** V33a ModelFocusCrystal (Techne, 64x32): the ordered ring model of the Refined and better tiers. */
public final class ModelFocusCrystal extends ModelFocusCrystalBase {

	private static final List<String> OUTER = List.of("Shape1", "Shape1r", "Shape1s", "Shape1t", "Shape1u", "Shape1v", "Shape1x", "Shape1a", "Shape1b", "Shape1c", "Shape1d", "Shape1e", "Shape1f", "Shape1l", "Shape1m", "Shape1n", "Shape1o", "Shape1p");
	private static final List<String> INNER = List.of("Shape1g", "Shape1h", "Shape1i", "Shape1j", "Shape1k", "Shape1q");
	private static final List<String> LOOSE = List.of("Shape2", "Shape2a", "Shape2b", "Shape2c", "Shape2d", "Shape2e", "Shape2f", "Shape2g", "Shape2h", "Shape2i", "Shape2j", "Shape2k");

	public ModelFocusCrystal(ModelPart root) {
		super(root, OUTER, INNER, LOOSE);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		part(root, "Shape1", 27, 10, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, -2.508911F, 0.4363323F, true);
		part(root, "Shape1q", 20, 8, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, -0.6763151F, 0.4363323F, true);
		part(root, "Shape1r", 0, 10, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 0.6326819F, 0.4363323F, true);
		part(root, "Shape1s", 0, 0, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 1.156281F, 0.4363323F, true);
		part(root, "Shape1t", 0, 5, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 1.679879F, 0.4363323F, true);
		part(root, "Shape1u", 0, 0, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 2.203478F, 0.4363323F, true);
		part(root, "Shape1v", 9, 11, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 2.74453F, 0.4363323F, true);
		part(root, "Shape1x", 18, 11, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, -3.03251F, 0.4363323F, true);
		part(root, "Shape1a", 9, 0, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, -0.4145157F, 0.4363323F, true);
		part(root, "Shape1b", 18, 0, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, -0.9381145F, 0.4363323F, true);
		part(root, "Shape1c", 27, 0, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, -1.461713F, 0.4363323F, true);
		part(root, "Shape1d", 27, 5, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, -1.985312F, 0.4363323F, true);
		part(root, "Shape1e", 0, 15, -4, 3, -4, 2, 2, 2, 0, 23, 0, -0.4363323F, 0.1090831F, 0.4363323F, true);
		part(root, "Shape1f", 14, 16, -4.5F, 2.5F, -4.5F, 1, 1, 1, 0, 23, 0, -0.4363323F, -1.985312F, 0.4363323F, true);
		part(root, "Shape1g", 10, 8, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, 1.41808F, 0.4363323F, true);
		part(root, "Shape1h", 10, 5, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, 2.465278F, 0.4363323F, true);
		part(root, "Shape1i", 15, 8, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, -2.77071F, 0.4363323F, true);
		part(root, "Shape1j", 15, 5, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, -1.723513F, 0.4363323F, true);
		part(root, "Shape1k", 20, 5, -1.8F, 1.8F, -1.8F, 1, 1, 1, 0, 23, 0, -0.4363323F, 0.3708825F, 0.4363323F, true);
		part(root, "Shape1l", 14, 19, -4.5F, 4.5F, -4.5F, 1, 1, 1, 0, 23, 0, -0.4363323F, -0.9381145F, 0.4363323F, true);
		part(root, "Shape1m", 9, 16, -4.5F, 2.5F, -4.5F, 1, 1, 1, 0, 23, 0, -0.4363323F, 2.203478F, 0.4363323F, true);
		part(root, "Shape1n", 0, 20, -4.5F, 2.5F, -4.5F, 1, 1, 1, 0, 23, 0, -0.4363323F, 0.1090831F, 0.4363323F, true);
		part(root, "Shape1o", 19, 16, -4.5F, 4.5F, -4.5F, 1, 1, 1, 0, 23, 0, -0.4363323F, 1.156281F, 0.4363323F, true);
		part(root, "Shape1p", 9, 19, -4.5F, 4.5F, -4.5F, 1, 1, 1, 0, 23, 0, -0.4363323F, -3.03251F, 0.4363323F, true);
		part(root, "Shape2", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, -0.2181662F, 0, true);
		part(root, "Shape2a", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, -1.788962F, 0, true);
		part(root, "Shape2b", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, 0.2181662F, 0, true);
		part(root, "Shape2c", 0, 24, -10, 0, -0.5F, 1, 4, 1, 0, 20, 0, 0, 2.356194F, 0, true);
		part(root, "Shape2d", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, -2.923426F, 0, true);
		part(root, "Shape2e", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, 2.923426F, 0, true);
		part(root, "Shape2f", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, -1.35263F, 0, true);
		part(root, "Shape2g", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, 1.788962F, 0, true);
		part(root, "Shape2h", 8, 24, -6, 0, -6, 1, 3, 1, 0, 21, 0, 0, 1.35263F, 0, true);
		part(root, "Shape2i", 0, 24, -10, 0, -0.5F, 1, 4, 1, 0, 20, 0, 0, 0.7853982F, 0, true);
		part(root, "Shape2j", 0, 24, -10, 0, -0.5F, 1, 4, 1, 0, 20, 0, 0, -0.7853982F, 0, true);
		part(root, "Shape2k", 0, 24, -10, 0, -0.5F, 1, 4, 1, 0, 20, 0, 0, -2.356194F, 0, true);
		return LayerDefinition.create(mesh, 64, 32);
	}

}
