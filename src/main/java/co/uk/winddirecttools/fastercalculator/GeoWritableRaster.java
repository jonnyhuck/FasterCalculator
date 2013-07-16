package co.uk.winddirecttools.fastercalculator;

import java.awt.image.WritableRaster;
import org.geotools.geometry.Envelope2D;

/**
 * Convenience class to store a WritableRaster and an Envelope2D together at 
 *  the same time.
 * 
 * This class is not robust, and will allow you to resize or swap out the raster or
 *  envelope without affecting the other. As such, it should be used with extreme 
 *  caution
 * 
 * @author jonathan.huck
 */
public class GeoWritableRaster {
    
    /*
     * object variables
     */
    public WritableRaster raster;
    public Envelope2D envelope;
    
    /**
     * Constructor
     * @param raster
     * @param envelope 
     */
    GeoWritableRaster(WritableRaster raster, Envelope2D envelope){
        this.raster = raster;
        this.envelope = envelope;
    }
    
    
}
