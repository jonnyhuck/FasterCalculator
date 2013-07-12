package co.uk.winddirecttools.fastercalculator;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.referencing.CRS;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.style.ContrastMethod;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws IOException, IllegalArgumentException, NoSuchAuthorityCodeException, FactoryException {

        try {

            //for debugging purposes
            Boolean debug = false;

            //display a data store file chooser dialog for images
            String[] ext = {"asc, tif"};
            File file = JFileDataStoreChooser.showOpenFile(ext, null);
            if (file == null) {
                return;
            }

            //print available file types
            if (debug) {
                Set<GridFormatFactorySpi> formats = GridFormatFinder.getAvailableFormats();
                for (GridFormatFactorySpi format : formats) {
                    System.out.println(format.toString());
                }
            }

            //set default crs
            CoordinateReferenceSystem crs = CRS.decode("EPSG:27700", true);
            final Hints hint = new Hints();
            hint.put(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs);

            //open the raster layer
            AbstractGridFormat format = GridFormatFinder.findFormat(file);
            AbstractGridCoverage2DReader reader = format.getReader(file, hint);
            GridCoverage2D gc = (GridCoverage2D) reader.read(null);

            //create greyscale style
            StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
            ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.HISTOGRAM);
            SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(1), ce);
            RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
            ChannelSelection sel = sf.channelSelection(sct);
            sym.setChannelSelection(sel);
            Style rasterStyle = SLD.wrapSymbolizers(sym);

            //create a map context
            MapContext map = new DefaultMapContext();
            map.setTitle("Faster Calculator 0.1");
            map.addLayer(gc, rasterStyle);

            // Now display the map
            JMapFrame.showMap(map);

        } catch (Exception e) {
            System.out.println("Sorry, could not open file:");
            System.out.println(e.getMessage());
        }
    }
}
