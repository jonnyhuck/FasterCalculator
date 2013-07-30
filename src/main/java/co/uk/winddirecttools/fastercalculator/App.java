package co.uk.winddirecttools.fastercalculator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Class for the application itself
 */
public class App {

    /**
     * Main method for the app
     *
     * @param args
     */
    public static void main(String[] args) {
        try {

            //get input directory path
            String inPath;
            JFileChooser inChooser = new JFileChooser();
            inChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int inReturnVal = inChooser.showOpenDialog(inChooser);
            if (inReturnVal == JFileChooser.APPROVE_OPTION) {
                inPath = inChooser.getSelectedFile().getAbsolutePath();
            } else {
                return;
            }

            //get list of files
            String fileName;
            File folder = new File(inPath);
            File[] listOfFiles = folder.listFiles();
            ArrayList<File> filesToProcess = new ArrayList<File>();
            for (int i = 0; i < listOfFiles.length; i++) {
                fileName = listOfFiles[i].getName();
                if (listOfFiles[i].isFile()) {
                    if (fileName.endsWith(".asc") || fileName.endsWith(".ASC")
                            || fileName.endsWith(".tif") || fileName.endsWith(".TIF")) {
                        filesToProcess.add(listOfFiles[i]);
                    }
                }
            }

            //get output file path
            String outPath;
            JFileChooser outChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("GeoTiff Files", "tif");
            outChooser.setFileFilter(filter);
            int returnVal = outChooser.showSaveDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                outPath = outChooser.getSelectedFile().getAbsolutePath();
            } else {
                return;
            }//*/

            //set default crs
            CoordinateReferenceSystem crs = CRS.decode("EPSG:27700", true);
            final Hints hint = new Hints();
            hint.put(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs);

            //read all input laters into ArrayList
            ArrayList<GridCoverage2D> coverages = new ArrayList<GridCoverage2D>();
            for (File file : filesToProcess) {

                //read the next file
                AbstractGridFormat format = GridFormatFinder.findFormat(file);
                AbstractGridCoverage2DReader reader = format.getReader(file, hint);
                coverages.add((GridCoverage2D) reader.read(null));
            }

            //build raster calculator
            RasterCalculator rc = new RasterCalculator();
            GridCoverage2D gc = rc.process(coverages, RasterCalculator.ADD);

            //write result
            writeGeoTiffFile(gc, outPath);  //*/
            System.out.println("Done!");

        } catch (InvalidGridGeometryException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) { //*/
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAuthorityCodeException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FactoryException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Writes a coverage to a GeoTiff file
     *
     * @param gc
     * @param path
     * @throws IOException
     */
    public static void writeGeoTiffFile(GridCoverage2D gc, String path)
            throws IOException {

        //create a geotiff writer
        File file = new File(path);
        GeoTiffWriter gw = new GeoTiffWriter(file);
        try {
            //write the file
            gw.write(gc, null);
        } finally {
            //destroy the writer
            gw.dispose();
        }
    }
}