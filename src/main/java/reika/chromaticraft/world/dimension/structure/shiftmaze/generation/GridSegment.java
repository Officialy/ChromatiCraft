/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2017
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.chromaticraft.world.dimension.structure.shiftmaze.generation;

import java.awt.Point;
import java.util.List;

import reika.chromaticraft.world.dimension.structure.shiftmaze.MazeGrid;

public class GridSegment implements SegmentRestriction {

    private final MazeGrid grid;
    public final List<Point> segmentPoints;
    public final int id;

    public GridSegment(MazeGrid grid, List<Point> segmentPoints, int id) {
        this.grid = grid;
        this.segmentPoints = segmentPoints;
        this.id = id;
    }

    @Override
    public boolean isInBounds(int x, int z) {
        return grid.isInBounds(x, z) && segmentPoints.contains(new Point(x, z));
    }

}
