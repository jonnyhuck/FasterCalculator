//package co.uk.winddirecttools.fastercalculator;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.swing.JFileChooser;
//import javax.swing.filechooser.FileNameExtensionFilter;
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.coverage.grid.InvalidGridGeometryException;
//import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
//import org.geotools.coverage.grid.io.AbstractGridFormat;
//import org.geotools.coverage.grid.io.GridFormatFinder;
//import org.geotools.factory.CommonFactoryFinder;
//import org.geotools.factory.Hints;
//import org.geotools.gce.geotiff.GeoTiffWriter;
//import org.geotools.map.DefaultMapContext;
//import org.geotools.map.MapContext;
//import org.geotools.referencing.CRS;
//import org.geotools.styling.ChannelSelection;
//import org.geotools.styling.ContrastEnhancement;
//import org.geotools.styling.RasterSymbolizer;
//import org.geotools.styling.SLD;
//import org.geotools.styling.SelectedChannelType;
//import org.geotools.styling.Style;
//import org.geotools.styling.StyleFactory;
//import org.geotools.swing.JMapFrame;
//import org.geotools.swing.data.JFileDataStoreChooser;
//import org.opengis.filter.FilterFactory2;
//import org.opengis.referencing.FactoryException;
//import org.opengis.referencing.NoSuchAuthorityCodeException;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//import org.opengis.referencing.operation.TransformException;
//import org.opengis.style.ContrastMethod;
//
///**
// * Class for the application itself
// */
//public class App_1 {
//
//    /**
//     * Main method for the app
//     *
//     * @param args
//     */
//    public static void main(String[] args) {
//        try {
//
//            //take in files
//            String[] ext = {"asc", "tif"};
//            File file1 = JFileDataStoreChooser.showOpenFile(ext, null);
//            if (file1 == null) {
//                return;
//            }
//            File file2 = JFileDataStoreChooser.showOpenFile(ext, null);
//            if (file2 == null) {
//                return;
//            }
//
//            //get output file path
//            String outPath;
//            JFileChooser outChooser = new JFileChooser();
//            FileNameExtensionFilter filter = new FileNameExtensionFilter("GeoTiff Files", "tif");
//            outChooser.setFileFilter(filter);
//            int returnVal = outChooser.showSaveDialog(null);
//            if (returnVal == JFileChooser.APPROVE_OPTION) {
//                outPath = outChooser.getSelectedFile().getAbsolutePath();
//            } else {
//                return;
//            }
//            
//            //set default crs
//            CoordinateReferenceSystem crs = CRS.decode("EPSG:27700", true);
//            final Hints hint = new Hints();
//            hint.put(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs);
//
//            //open the raster layers
//            AbstractGridFormat format = GridFormatFinder.findFormat(file1);
//            AbstractGridCoverage2DReader reader1 = format.getReader(file1, hint);
//            GridCoverage2D gc1 = (GridCoverage2D) reader1.read(null);
//            AbstractGridCoverage2DReader reader2 = format.getReader(file2, hint);
//            GridCoverage2D gc2 = (GridCoverage2D) reader2.read(null);
//
//            //combine
//            RasterCalculator rc = new RasterCalculator();
//            GridCoverage2D gc = rc.process(gc1, gc2, RasterCalculator.ADD);
//
//            //write result
//            writeGeoTiffFile(gc, outPath);
//
//            //create greyscale style
//            StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
//            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
//            ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.HISTOGRAM);
//            SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(1), ce);
//            RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
//            ChannelSelection sel = sf.channelSelection(sct);
//            sym.setChannelSelection(sel);
//            Style rasterStyle = SLD.wrapSymbolizers(sym);
//
//            //create a map context
//            MapContext map = new DefaultMapContext();
//            map.setTitle("WDT \"Faster\" Calculator 0.1");
//            map.addLayer(gc, rasterStyle);
//
//            // Now display the map
//            JMapFrame.showMap(map);
//
//        } catch (InvalidGridGeometryException ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (TransformException ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalArgumentException ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (NoSuchAuthorityCodeException ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (FactoryException ex) {
//            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    /**
//     * Writes a coverage to a GeoTiff file
//     *
//     * @param gc
//     * @param path
//     * @throws IOException
//     */
//    public static void writeGeoTiffFile(GridCoverage2D gc, String path)
//            throws IOException {
//
//        //create a geotiff writer
//        File file = new File(path);
//        GeoTiffWriter gw = new GeoTiffWriter(file);
//        try {
//            //write the file
//            gw.write(gc, null);
//        } finally {
//            //destroy the writer
//            gw.dispose();
//        }
//    }
//}