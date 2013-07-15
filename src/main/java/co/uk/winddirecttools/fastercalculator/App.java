package co.uk.winddirecttools.fastercalculator;

import java.io.File;
import java.io.IOException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
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

            //display a data store file chooser dialog
            String[] ext = {"asc", "tif"};
            File file1 = JFileDataStoreChooser.showOpenFile(ext, null);
            if (file1 == null) {
                return;
            }
            File file2 = JFileDataStoreChooser.showOpenFile(ext, null);
            if (file2 == null) {
                return;
            }

            //set default crs
            CoordinateReferenceSystem crs = CRS.decode("EPSG:27700", true);
            final Hints hint = new Hints();
            hint.put(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs);

            //open the raster layer
            AbstractGridFormat format = GridFormatFinder.findFormat(file1);
            AbstractGridCoverage2DReader reader1 = format.getReader(file1, hint);
            GridCoverage2D gc1 = (GridCoverage2D) reader1.read(null);

            //open the raster layer
            AbstractGridCoverage2DReader reader2 = format.getReader(file2, hint);
            GridCoverage2D gc2 = (GridCoverage2D) reader2.read(null);

            //combine
            RasterCalculator rc = new RasterCalculator();
            GridCoverage2D gc = rc.process(gc1, 20000d, gc2, 20000d, RasterCalculator.ADD);

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
            map.setTitle("WDT \"Faster\" Calculator 0.1");
            map.addLayer(gc, rasterStyle);

            // Now display the map
            JMapFrame.showMap(map);

        } catch (Exception e) {
            System.out.println("Sorry, could not open file:");
            System.out.println(e.getMessage());
        }
    }
}
