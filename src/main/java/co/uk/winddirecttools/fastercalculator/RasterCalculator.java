package co.uk.winddirecttools.fastercalculator;

import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import javax.media.jai.RasterFactory;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Raster calculator class, performs map algebra without clipping input files
 *
 * @author jonathan.huck
 */
public class RasterCalculator {

    /*
     * Static members to describe the mathematical operations available
     */
    public static final int ADD = 0;
    public static final int SUBTRACT = 1;
    public static final int MULTIPLY = 2;
    public static final int DIVIDE = 3;

    /**
     * Returns the combination of the two grid coverages, performing the
     * specified operation upon them and without clipping them to the overlap
     * region
     *
     * @param coverage1
     * @param coverage2
     * @return
     * @throws NoSuchAuthorityCodeException
     * @throws FactoryException
     */
    public GridCoverage2D process(GridCoverage2D coverage1, Double radius1,
            GridCoverage2D coverage2, Double radius2, int operation)
            throws NoSuchAuthorityCodeException, FactoryException, InvalidGridGeometryException, TransformException {

        //define coord system
        final CoordinateReferenceSystem crs = coverage1.getCoordinateReferenceSystem();

        //get the dimensions of the output
        final Envelope2D envelope = getCombinedEnvelope(coverage1.getEnvelope2D(),
                coverage2.getEnvelope2D(), crs);

        //verify resolutions match
        final int resolution = (int) coverage1.getEnvelope2D().width
                / coverage1.getGridGeometry().getGridRange2D().width;
        if (resolution != (int) coverage2.getEnvelope2D().width
                / coverage2.getGridGeometry().getGridRange2D().width) {
            throw new ResolutionException();
        }

        //get an empty grid coverage at the correct size
        WritableRaster raster = getWritableRaster(envelope, resolution, 0);

        //build a grid coverage for calculations
        final GridCoverageFactory factory = new GridCoverageFactory();
        final GridCoverage2D tmpGc = factory.create("output", raster, envelope);

        //apply each input coverage to the writable raster
        raster = applyRasterValues(raster, tmpGc, coverage1, radius1, operation);
        raster = applyRasterValues(raster, tmpGc, coverage2, radius2, operation);

        //dispose of the temporary grid coverage
        tmpGc.dispose(true);

        //convert to grid coverage and return
        return factory.create("output", raster, envelope);
    }

    
    /**
     * Combine two envelopes into one
     *
     * @param envelope1
     * @param envelope2
     * @param crs
     * @return
     */
    private Envelope2D getCombinedEnvelope(Envelope2D envelope1, Envelope2D envelope2,
            CoordinateReferenceSystem crs) {

        //get corners
        final Double maxX = Math.max(envelope1.getMaxX(), envelope2.getMaxX());
        final Double maxY = Math.max(envelope1.getMaxY(), envelope2.getMaxY());
        final Double minX = Math.min(envelope1.getMinX(), envelope2.getMinX());
        final Double minY = Math.min(envelope1.getMinY(), envelope2.getMinY());

        //get anchor points
        final DirectPosition2D bl = new DirectPosition2D(crs, minX, minY);
        final DirectPosition2D tr = new DirectPosition2D(crs, maxX, maxY);

        //build new envelope
        return new Envelope2D(bl, tr);
    }
    
    
    /**
     * Returns an empty grid coverage upon which to build results
     *
     * @param coverage1
     * @param coverage2
     * @return
     * @throws NoSuchAuthorityCodeException
     * @throws FactoryException
     */
    private WritableRaster getWritableRaster(Envelope2D envelope, double resolution, 
            double initialValue)
            throws NoSuchAuthorityCodeException, FactoryException, ResolutionException {

        //build a raster to base the size upon
        WritableRaster raster = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_INT, (int) Math.ceil(envelope.getWidth() / resolution),
                (int) Math.ceil(envelope.getHeight() / resolution), 1, null);
        
        //populate initial values
        for (int y = 0; y < raster.getHeight(); y++) {
            for (int x = 0; x < raster.getWidth(); x++) {
                raster.setSample(x, y, 0, initialValue);
            }
        }
        return raster;
    }


    /**
     * Adds a set of values from a grid coverage to a writable raster
     *
     * @param raster
     * @param gc
     * @param radius
     * @param operation
     * @return
     * @throws InvalidGridGeometryException
     * @throws TransformException
     * @throws NoSuchAuthorityCodeException
     * @throws FactoryException
     */
    private WritableRaster applyRasterValues(WritableRaster raster, GridCoverage2D tmpGc,
            GridCoverage2D gc, Double radius, int operation)
            throws InvalidGridGeometryException, TransformException,
            NoSuchAuthorityCodeException, FactoryException {

        //get coordinate reference system
        CoordinateReferenceSystem crs = gc.getEnvelope2D().getCoordinateReferenceSystem();

        //get patch dimensions (same as the coverage)
        final int w = gc.getGridGeometry().getGridRange2D().width;
        final int h = gc.getGridGeometry().getGridRange2D().height;
        final int nCells = w * h;

        //get top left coord, and transform to grid
        final DirectPosition2D tl = new DirectPosition2D(crs,
                gc.getEnvelope2D().getMinX(), gc.getEnvelope2D().getMaxY());
        final GridCoordinates2D sampleOrigin = tmpGc.getGridGeometry().worldToGrid(tl);

        //get values from the writable raster
        double[] existingData = new double[nCells];
        try {
            raster.getSamples(sampleOrigin.x, sampleOrigin.y, w, h, 0, existingData);
        } catch(ArrayIndexOutOfBoundsException e) {
            System.out.println(sampleOrigin);
        }

        //build a patch (enforcing radius)
        final Point2D origin = new Point2D.Double(gc.getEnvelope2D().getMinX(), gc.getEnvelope2D().getMinY());
        final double resolution = gc.getEnvelope2D().width / gc.getGridGeometry().getGridRange2D().width;
        MemoryRasterPatch2D patch = new MemoryRasterPatch2D(0d, resolution,
                gc.getGridGeometry().getGridRange2D().width,
                gc.getGridGeometry().getGridRange2D().height, origin);

        //get the centroid coordinates (from which the radius will be enforced)
        final Point2D centroid = new Point2D.Double(gc.getEnvelope2D().getCenterX(),
                gc.getEnvelope2D().getCenterY());

        //populate the patch (loop through each cell)
        DirectPosition2D cellLocation;
        GridCoordinates2D cellLocationGrid;
        for (int row = 0; row < patch.getNRows(); row++) {
            for (int col = 0; col < patch.getNCols(); col++) {

                cellLocation = new DirectPosition2D(crs,
                        patch.getOrigin().getX() + (resolution * col),
                        patch.getOrigin().getY() + (resolution * row));

                cellLocationGrid = gc.getGridGeometry().worldToGrid(cellLocation);

                //verify that the position is within the radius
                if (centroid.distance(cellLocation.toPoint2D()) < radius) {

                    //get the raster and coverage values value for that location
                    double gcValue = this.getValueFromGridCoverage(cellLocation, gc);
                    double rasterValue = raster.getSampleDouble((int) cellLocationGrid.x,
                            (int) cellLocationGrid.y, 0);

                    //perform the necessary operation
                    double outValue;
                    switch (operation) {
                        case 0:     //add
                            outValue = rasterValue + gcValue;
                            break;
                        case 1:     //subtract
                            outValue = rasterValue - gcValue;
                            break;
                        case 2:     //multiply
                            outValue = rasterValue * gcValue;
                            break;
                        case 3:     //divide
                            outValue = rasterValue / gcValue;
                            break;
                        default:    //other number returned...
                            throw new UnsupportedOperationException();
                    }
                    //update the raster
                    patch.setAttribute(cellLocation.toPoint2D(), outValue);
                }
            }
        }

        //apply the patch to the writable raster
        /** TODO - NEED TO GET THIS SORTED +1 IS A TEMP FIX FOR A BUG THAT i THINK COMES FROM GRID ALIGNMENT **/
        raster.setPixels(sampleOrigin.x + 1, sampleOrigin.y, w, h, patch.getData1D());

        //return it
        return raster;
    }

    /**
     * Returns a value from a grid coverage
     *
     * @param point
     * @param gc
     * @return
     * @throws NoSuchAuthorityCodeException
     * @throws FactoryException
     */
    private double getValueFromGridCoverage(DirectPosition2D point, GridCoverage2D gc)
            throws NoSuchAuthorityCodeException, FactoryException {

        //get the coverage data
        final DirectPosition position = point;
        double[] bands = new double[1];
        try {
            gc.evaluate(position, bands);
            return bands[0];
        } catch (PointOutsideCoverageException e) {
            //if the point is not withi the weighting data
            System.out.println(e.getOffendingLocation());
            return 0;
        }
    }
}