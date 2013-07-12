package co.uk.winddirecttools.fastercalculator;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import javax.media.jai.RasterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Raster calculator class, performs map algebra without clipping input files
 * @author jonathan.huck
 */
public class RasterCalculator {

    /**
     * Returns the sum of two grid coverages, without clipping them to the overlap region
     * @param coverage1
     * @param coverage2
     * @return
     * @throws NoSuchAuthorityCodeException
     * @throws FactoryException 
     */
    public GridCoverage2D sum(GridCoverage2D coverage1, GridCoverage2D coverage2){
        return null;
    }
    
    
    /**
     * Returns an empty grid coverage upon which to build results
     * @param coverage1
     * @param coverage2
     * @return
     * @throws NoSuchAuthorityCodeException
     * @throws FactoryException 
     */
    private GridCoverage2D getEmptyGridCoverage(GridCoverage2D coverage1, GridCoverage2D coverage2) 
            throws NoSuchAuthorityCodeException, FactoryException {

        //define coord system
        final CoordinateReferenceSystem crs = CRS.decode("EPSG:27700", true);
        
        //get the dimensions of the output
        final Envelope2D envelope = getCombinedEnvelope(coverage1.getEnvelope2D(), 
                coverage2.getEnvelope2D(), crs);
        
        //verify resolutions match
        final int pxWidth = (int) coverage1.getGridGeometry().getGridRange2D().getSpan(0);
        if (pxWidth != coverage1.getGridGeometry().getGridRange2D().getSpan(0)) {
            throw new ResolutionException();
        }
        
        //build a raster to bas the size on
        final WritableRaster raster = RasterFactory.createBandedRaster(
                DataBuffer.TYPE_INT, pxWidth, pxWidth, 1, null);
        
        //convert to grid coverage
        final GridCoverageFactory factory = new GridCoverageFactory();
        return factory.create("output", raster, envelope);
    }
    
    /**
     * Combine two envelopes into one
     * @param envelope1
     * @param envelope2
     * @param crs
     * @return 
     */
    private Envelope2D getCombinedEnvelope(Envelope2D envelope1, Envelope2D envelope2, CoordinateReferenceSystem crs){
        
        //get corners
        final Double maxX = Math.max(envelope1.getMaxX(), envelope2.getMaxX());
        final Double maxY = Math.max(envelope1.getMaxY(), envelope2.getMaxY());
        final Double minX = Math.min(envelope1.getMinX(), envelope2.getMinX());
        final Double minY = Math.min(envelope1.getMinY(), envelope2.getMinY());
        
        //get dimensions
        final Double width = Math.sqrt(Math.pow(minX, 2) + Math.pow(maxX, 2));
        final Double height = Math.sqrt(Math.pow(minY, 2) + Math.pow(maxY, 2));
        
        //build new envelope
        return new Envelope2D(crs, minX, minY, width, height);
    }
}