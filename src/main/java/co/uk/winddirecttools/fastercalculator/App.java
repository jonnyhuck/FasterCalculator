package co.uk.winddirecttools.fastercalculator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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

            if (args.length == 3) {

                //get radius (verify number)
                int radius = 0;
                try {
                    radius = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.err.println("Argument 1 for radius" + " must be an integer");
                    System.exit(1);
                }

                //get in directory and verify
                File inDirectory = new File(args[1]);
                if (inDirectory.isDirectory()) {
                    System.err.println("Argument 2 must be a valid directory");
                    System.exit(1);
                }

                //get list of files
                String fileName;
                File[] listOfFiles = inDirectory.listFiles();
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

                //get out file and verify
                File outFile = new File(args[2]);
                if (Files.notExists(outFile.getParentFile().toPath())) {
                    System.err.println("Argument 2 must be a *.tif file in a valid directory");
                    System.exit(1);
                }

                //set default crs
                CoordinateReferenceSystem crs = CRS.decode("EPSG:27700", true);
                final Hints hint = new Hints();
                hint.put(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs);

                //read all input layers into an ArrayList of GridCoverage2D's
                ArrayList<GridCoverage2D> coverages = new ArrayList<GridCoverage2D>();
                for (File file : filesToProcess) {
                    AbstractGridFormat format = GridFormatFinder.findFormat(file);
                    AbstractGridCoverage2DReader reader = format.getReader(file, hint);
                    coverages.add((GridCoverage2D) reader.read(null));
                }

                //build raster calculator
                RasterCalculator rc = new RasterCalculator();
                GridCoverage2D gc = rc.process(coverages, radius, RasterCalculator.ADD);

                //write result
                writeGeoTiffFile(gc, outFile.getAbsolutePath());
                System.out.println("Done!");
                
            } else {
                System.out.println("Sorry, I'm a 3 arguments kind of girl");
                System.out.println("Maybe you need to put some speech-marks around your file-paths...?");
                System.out.println("");
                System.out.println("java -jar fastercalculator [radius] [input directory] [output file *.tif]");
                System.exit(0);
            }

        } catch (InvalidGridGeometryException ex) {
            System.err.println("How embarassing...");
            System.err.println(ex.toString());
            System.exit(1);
        } catch (TransformException ex) {
            System.err.println("How embarassing...");
            System.err.println(ex.toString());
            System.exit(1);
        } catch (IllegalArgumentException ex) {
            System.err.println("How embarassing...");
            System.err.println(ex.toString());
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("How embarassing...");
            System.err.println(ex.toString());
            System.exit(1);
        } catch (NoSuchAuthorityCodeException ex) {
            System.err.println("How embarassing...");
            System.err.println(ex.toString());
            System.exit(1);
        } catch (FactoryException ex) {
            System.err.println("How embarassing...");
            System.err.println(ex.toString());
            System.exit(1);
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
/*/get input directory path
 String inPath;
 JFileChooser inChooser = new JFileChooser();
 inChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
 int inReturnVal = inChooser.showOpenDialog(inChooser);
 if (inReturnVal == JFileChooser.APPROVE_OPTION) {
 inPath = inChooser.getSelectedFile().getAbsolutePath();
 } else {
 return;
 }
 //get output file path
 JFileChooser outChooser = new JFileChooser();
 FileNameExtensionFilter filter = new FileNameExtensionFilter("GeoTiff Files", "tif");
 outChooser.setFileFilter(filter);
 int returnVal = outChooser.showSaveDialog(null);
 if (returnVal == JFileChooser.APPROVE_OPTION) {
 outPath = outChooser.getSelectedFile().getAbsolutePath();
 } else {
 return;
 }   //*/