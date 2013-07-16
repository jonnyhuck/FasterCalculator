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
import org.geotools.referencing.CRS;
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
        final CoordinateReferenceSystem crs = CRS.decode("EPSG:27700", true);
        
        //get the dimensions of the output
        final Envelope2D envelope = getCombinedEnvelope(coverage1.getEnvelope2D(),
                coverage2.getEnvelope2D(), crs);
        
        //get an empty grid coverage at the correct size
        WritableRaster raster = getWritableRaster(envelope, coverage1, coverage2, 0);

        //apply each input coverage to the writable raster
        raster = applyRasterValues(raster, coverage1, radius1, operation);

        raster = applyRasterValues(raster, coverage2, radius2, operation);

        //convert to grid coverage and return
        final GridCoverageFactory factory = new GridCoverageFactory();
        return factory.create("output", raster, envelope);
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
    private WritableRaster getWritableRaster(Envelope2D envelope, 
            GridCoverage2D coverage1, GridCoverage2D coverage2, double initialValue)
            throws NoSuchAuthorityCodeException, FactoryException, ResolutionException {

        //verify resolutions match
        final int resolution = (int) coverage1.getEnvelope2D().width / 
                coverage1.getGridGeometry().getGridRange2D().width;
        if (resolution != (int) coverage2.getEnvelope2D().width / 
                coverage2.getGridGeometry().getGridRange2D().width) {
            throw new ResolutionException();
        }

        //build a raster to base the size upon
        WritableRaster raster = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_INT, (int) (envelope.width / resolution), 
                (int) (envelope.height / resolution), 1, null);

        //populate initial values
        for (int y = 0; y < raster.getHeight(); y++) {
            for (int x = 0; x < raster.getWidth(); x++) {
                raster.setSample(x, y, 0, initialValue);
            }
        }
        return raster;
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
     * Adds a set of values from a grid coverage to a writable raster
     *
     * @param wr
     * @param gc
     * @param radius
     * @return
     */
    private WritableRaster applyRasterValues(WritableRaster raster, GridCoverage2D gc,
            Double radius, int operation) throws InvalidGridGeometryException, TransformException {

        //get patch dimensions
        final int w = gc.getGridGeometry().getGridRange2D().width;
        final int h = gc.getGridGeometry().getGridRange2D().height;
        final int nCells = w * h;

        //get bottom left coords, then convert to top left
        final DirectPosition2D tl = new DirectPosition2D(gc.getEnvelope2D().getCoordinateReferenceSystem(), 
                gc.getEnvelope2D().x, gc.getEnvelope2D().y);
        final GridCoordinates2D tlGrid = gc.getGridGeometry().worldToGrid(tl);
        
        //get the centroid coordinates (from which the radius will be enforced)
        final Point2D centroid = new Point2D.Double(gc.getEnvelope2D().getCenterX(),
                gc.getEnvelope2D().getCenterY());
        
        //get values from the writable raster
        double[] existingData = new double[nCells];
        raster.getSamples(tlGrid.x, tlGrid.y, w, h, 0, existingData);

        //build a patch (enforcing radius)
        final Point2D origin = new Point2D.Double(gc.getEnvelope2D().getMinX(),
                gc.getEnvelope2D().getMaxY());
        final double resolution = gc.getEnvelope2D().width
                / gc.getGridGeometry().getGridRange2D().width;
        final int nCols = gc.getGridGeometry().getGridRange2D().width;
        final int nRows = gc.getGridGeometry().getGridRange2D().height;
        MemoryRasterPatch2D patch = new MemoryRasterPatch2D(0d, resolution, nCols, nRows, origin);
        
        //populate the patch
        for (int row = 0; row < patch.getNRows(); row++) {
            for (int col = 0; col < patch.getNCols(); col++) {

                //get the coordinates of the grid cell
                Point2D cellLocation = new Point2D.Double(patch.getOrigin().getX()
                        + (resolution * patch.getNCols()), patch.getOrigin().getY()
                        + (resolution * patch.getNRows()));

                //verify that the position is within the radius
                if (centroid.distance(cellLocation) < radius) {

                    //get the coverage value for that location
                    double[] gcValue = new double[1];
                    gc.evaluate(cellLocation, gcValue);

                    //perform the necessary operation...
                    switch (operation) {
                        case 0:     //add
                            //get new value and apply to the patch
                            patch.setAttribute(cellLocation, raster.getSampleDouble(
                                    (int) cellLocation.getX(), (int) cellLocation.getY(), 0) + gcValue[0]);
                            break;
                        case 1:     //subtract
                            throw new UnsupportedOperationException("Sorry, that operation has yet to be implemented");
                        case 2:     //multiply
                            throw new UnsupportedOperationException("Sorry, that operation has yet to be implemented");
                        case 3:     //divide
                            throw new UnsupportedOperationException("Sorry, that operation has yet to be implemented");
                        default:    //other number returned...
                            throw new UnsupportedOperationException();
                    }
                }
            }
        }

        //apply the patch to the writable raster
        raster.setPixels(tlGrid.x, tlGrid.y, w, h, patch.getData1D());

        //return it
        return raster;
    }
}