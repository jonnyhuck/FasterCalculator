package co.uk.winddirecttools.fastercalculator;

import java.awt.geom.AffineTransform;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
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
    public GridCoverage2D process(ArrayList<GridCoverage2D> coverages, int operation)
            throws NoSuchAuthorityCodeException, FactoryException, InvalidGridGeometryException, TransformException {

        //define coord system and 
        final CoordinateReferenceSystem crs = coverages.get(0).getCoordinateReferenceSystem();
        final int resolution = this.getResolution(coverages.get(0))[0];

        //get the dimensions of the output
        final Envelope2D envelope = getCombinedEnvelope(coverages, resolution, crs);

        //get an empty grid coverage at the correct size
        WritableRaster raster = getWritableRaster(envelope, resolution, 0);

        //build a grid coverage for calculations
        final GridCoverageFactory factory = new GridCoverageFactory();
        final GridCoverage2D tmpGc = factory.create("tmp", raster, envelope);

        //apply each input coverage to the writable raster
        raster = applyRasterValues(coverages, raster, tmpGc, operation);

        //convert to grid coverage and return
        return factory.create("output", raster, envelope);
    }

    /**
     * Returns the resolution of a Grid Coverage
     *
     * @param gc
     * @return
     */
    private int[] getResolution(GridCoverage2D gc) {
        //get the resolution
        AffineTransform gridToCRS = (AffineTransform) gc.getGridGeometry().getGridToCRS2D();
        final int[] scale = {(int) Math.round(gridToCRS.getScaleX()), (int) Math.round(gridToCRS.getScaleY())};
        return scale;
    }

    /**
     * Combine two envelopes into one
     *
     * @param envelope1
     * @param envelope2
     * @param crs
     * @return
     */
    private Envelope2D getCombinedEnvelope(ArrayList<GridCoverage2D> coverages,
            int resolution, CoordinateReferenceSystem crs) {

        //get first coverage and set initial dimensions
        GridCoverage2D gc = coverages.remove(0);

        //get corners
        double maxX = gc.getEnvelope2D().getMaxX();
        double maxY = gc.getEnvelope2D().getMaxY();
        double minX = gc.getEnvelope2D().getMinX();
        double minY = gc.getEnvelope2D().getMinY();

        //loop through all coverages and grow envelope as required
        for (GridCoverage2D coverage : coverages) {
            maxX = Math.max(maxX, coverage.getEnvelope2D().getMaxX());
            maxY = Math.max(maxY, coverage.getEnvelope2D().getMaxY());
            minX = Math.min(minX, coverage.getEnvelope2D().getMinX());
            minY = Math.min(minY, coverage.getEnvelope2D().getMinY());
            coverage.dispose(true); //clean up
        }

        //get anchor points (aligns to grid by buffering outwards)
        final DirectPosition2D bl = new DirectPosition2D(crs,
                Math.floor(minX / resolution) * resolution,
                Math.floor(minY / resolution) * resolution);
        final DirectPosition2D tr = new DirectPosition2D(crs,
                Math.ceil(maxX / resolution) * resolution,
                Math.ceil(maxY / resolution) * resolution);

        //clean up
        gc.dispose(true);

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
    private WritableRaster getWritableRaster(Envelope2D envelope, double resolution, double initialValue)
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
    private WritableRaster applyRasterValues(
            ArrayList<GridCoverage2D> coverages,
            WritableRaster raster,
            GridCoverage2D tmpGc,
            int operation)
            throws InvalidGridGeometryException, TransformException,
            NoSuchAuthorityCodeException, FactoryException {

        //details required for converting coordinates to grid positions on the output raster
        final GridGeometry2D rasterGrid = tmpGc.getGridGeometry();

        System.out.println(rasterGrid.getGridRange2D().getMinX());
        System.out.println(rasterGrid.getGridRange2D().getMaxX());
        System.out.println(rasterGrid.getGridRange2D().getMinY());
        System.out.println(rasterGrid.getGridRange2D().getMaxY());

        //loop through each coverage and add to raster
        for (GridCoverage2D coverage : coverages) {

            //get the grid values for the coverage
            final Envelope2D coverageEnvelope = coverage.getEnvelope2D();
            final GridEnvelope2D coverageGridEnvelope = rasterGrid.worldToGrid(coverageEnvelope);

            System.out.println("**");
            System.out.println(coverageGridEnvelope.getMinX());
            System.out.println(coverageGridEnvelope.getMaxX());
            System.out.println(coverageGridEnvelope.getMinY());
            System.out.println(coverageGridEnvelope.getMaxY());

            //get values from coverage
            final int gcValues[] = this.getValuesFromGridCoverage(coverage);

            //get values from raster
            int rasterValues[] = new int[(int) coverageGridEnvelope.getWidth() * (int) coverageGridEnvelope.getHeight()];
            raster.getSamples((int) coverageGridEnvelope.getMinX(), (int) coverageGridEnvelope.getMaxY(),
                    (int) coverageGridEnvelope.getWidth(), (int) coverageGridEnvelope.getHeight(), 0, rasterValues);

            final int values[] = this.operate(operation, gcValues, rasterValues);

            //apply to the raster
            raster.setSamples((int) coverageGridEnvelope.getMinX(), (int) coverageGridEnvelope.getMaxY(),
                    coverageGridEnvelope.width, coverageGridEnvelope.height, 0, values);

            //destroy coverage
            coverage.dispose(true);
        }

        //destroy tmp coverage
        tmpGc.dispose(true);

        //return the finished raster
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
    private int[] getValuesFromGridCoverage(GridCoverage2D gc) {

        //get image and iterator
        final RenderedImage img = gc.getRenderedImage();
        final RectIter iter = RectIterFactory.create(img, null);

        //holder for data
        int[] list = new int[gc.getGridGeometry().getGridRange2D().height
                * gc.getGridGeometry().getGridRange2D().width];

        //get data from each row & column
        int i = 0;
        while (!iter.nextLineDone()) {
            while (!iter.nextPixelDone()) {
                list[i] = iter.getSample(0);
                i++;
            }
        }
        return list;
    }

    /**
     * Execute the selected operation upon two int arrays
     *
     * @param operation
     * @param one
     * @param two
     * @return
     * @throws UnsupportedOperationException
     */
    private int[] operate(int operation, int[] one, int[] two)
            throws UnsupportedOperationException {

        //verify lengths
        if (one.length == two.length) {

            //for output
            int[] out = new int[one.length];

            //apply the required operation
            switch (operation) {
                case 0:
                    out = opAdd(one, two);
                    break;
                case 1:
                    out = opSubtract(one, two);
                    break;
                case 2:
                    out = opMultiply(one, two);
                    break;
                case 3:
                    out = opDivide(one, two);
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "RasterCalculator does not currently support that operation");
            }
            return out;
        } else {
            throw new ArrayIndexOutOfBoundsException(
                    "Arrays used in a RasterCalculator operation must be the same size!");
        }
    }

    /**
     * Operation for Addition
     *
     * @param one
     * @param two
     * @return
     */
    private int[] opAdd(int[] one, int[] two) {
        int[] out = new int[one.length];
        for (int i = 0; i < one.length; i++) {
            out[i] = one[i] + two[i];
        }
        return out;
    }

    /**
     * Operation for Subtraction
     *
     * @param one
     * @param two
     * @return
     */
    private int[] opSubtract(int[] one, int[] two) {
        int[] out = new int[one.length];
        for (int i = 0; i < one.length; i++) {
            out[i] = one[i] - two[i];
        }
        return out;
    }

    /**
     * Operation for Multiplication
     *
     * @param one
     * @param two
     * @return
     */
    private int[] opMultiply(int[] one, int[] two) {
        int[] out = new int[one.length];
        for (int i = 0; i < one.length; i++) {
            out[i] = one[i] * two[i];
        }
        return out;
    }

    /**
     * Operation for Division
     *
     * @param one
     * @param two
     * @return
     */
    private int[] opDivide(int[] one, int[] two) {
        int[] out = new int[one.length];
        for (int i = 0; i < one.length; i++) {
            out[i] = one[i] / two[i];
        }
        return out;
    }
}