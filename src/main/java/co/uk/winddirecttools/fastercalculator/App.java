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
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.gridcoverage2d.RasterSymbolizerHelper;
import org.geotools.renderer.lite.gridcoverage2d.SubchainStyleVisitorCoverageProcessingAdapter;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
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
                if (!inDirectory.isDirectory()) {
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
                    String filePath = outFile.getPath();
                    if (filePath.endsWith(".tif") || filePath.endsWith(".TIF")) {
                        System.err.println("Argument 2 must be a *.tif file in a valid directory");
                        System.exit(1);
                    }
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
        final File file = new File(path);
        GeoTiffWriter gw = new GeoTiffWriter(file);
        try {

            //apply style to the GridCoverage2D
            StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
            File sldFile = new File("C:\\Users\\jonathan.huck\\Desktop\\fc.sld");
            SLDParser stylereader = new SLDParser(styleFactory, sldFile.toURI().toURL());
            StyledLayerDescriptor sld = stylereader.parseSLD();
            SubchainStyleVisitorCoverageProcessingAdapter rsh = new RasterSymbolizerHelper(gc, null);
            final NamedLayer ul = (NamedLayer) sld.getStyledLayers()[0];
            final Style style = ul.getStyles()[0]; //ul.getUserStyles()[0];
            final FeatureTypeStyle fts = (FeatureTypeStyle) style.featureTypeStyles().toArray()[0];
            final Rule rule = (Rule) fts.rules().toArray()[0];
            RasterSymbolizer rs = (RasterSymbolizer) rule.getSymbolizers()[0];
            rsh.visit(rs);
            GridCoverage2D gc2 = (GridCoverage2D) rsh.getOutput();

            //default write params (what do these even do??)
            GeoTiffWriteParams wp = new GeoTiffWriteParams();
            GeoTiffFormat format = new GeoTiffFormat();
            ParameterValueGroup paramWrite = format.getWriteParameters();
            paramWrite.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);

            //write the file
            gw.write(gc2, (GeneralParameterValue[]) paramWrite.values().toArray(new GeneralParameterValue[1]));
            //gw.write(gc2, null);

        } catch (IOException e) {
            throw new IOException();
        } finally {
            //destroy the writer
            gw.dispose();
        }
    }

    /**
     * Return the file extension from a file path (very simple and only for the
     * purposes of above, not a robust transferable method).
     *
     * @param path
     * @return
     */
    public static String getFileExtension(String path) {
        int i = path.lastIndexOf('.');
        if (i > 0) {
            return path.substring(i + 1);
        } else {
            return "";
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