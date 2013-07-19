package co.uk.winddirecttools.fastercalculator;

import java.awt.geom.Point2D;

/**
 * An in-memory patch to be applied to a Writable Raster
 *
 * Being held in-memory allows for faster editing, but means that this class is
 * not ideal for holding large amounts of data. The intended purpose of this
 * class is for small "patches" of data that may be constructed and then written
 * to a WritableRaster on disk.
 *
 * This class is based upon the RasterMap class by Jo Wood in his excellent book
 * Java Programming for Spatial Sciences.
 *
 * @author jonathan.huck
 */
public class MemoryRasterPatch2D {

    /*
     * object variables / raster properties
     */
    private int[][] data;     //raster values
    private int resolution;   //raster resolution
    private int nCols, nRows;    //raster dimensions
    private Point2D origin;      //origin (bottom left)

    /**
     * Constructor for a patch with a uniform initial value
     *
     * @param array1D
     * @param resolution
     * @param nCols
     * @param nRows
     * @param origin
     */
    MemoryRasterPatch2D(int initialValue, int resolution, int nCols,
            int nRows, Point2D origin) {

        //update proberties to class variables
        this.resolution = resolution;
        this.nCols = nCols;
        this.nRows = nRows;
        this.origin = origin;
        this.data = new int[nRows][nCols];

        //create patch with initial value
        for (int row = 0; row < this.nRows; row++) {
            for (int col = 0; col < this.nCols; col++) {
                this.data[row][col] = initialValue;
            }
        }
    }

    /**
     * Constructor for a 1D array patch
     *
     * @param array1D
     * @param resolution
     * @param nCols
     * @param nRows
     * @param origin
     */
    MemoryRasterPatch2D(int array1D[], int resolution, int nCols,
            int nRows, Point2D origin) {

        //update proberties to object variables
        this.resolution = resolution;
        this.nCols = nCols;
        this.nRows = nRows;
        this.origin = origin;
        this.data = new int[nRows][nCols];

        //populate the data from the one dimensional array
        int row, col;
        for (int i = 0; i < array1D.length; i++) {
            row = (int) this.nRows / i;
            col = i % this.nCols;
            this.data[row][col] = array1D[i];
        }
    }

    /**
     * Constructor for a 2D array patch
     *
     * @param array2D
     * @param resolution
     * @param nCols
     * @param nRows
     * @param origin
     */
    MemoryRasterPatch2D(int array2D[][], int resolution,
            int nCols, int nRows, Point2D origin) {

        //update properties and data to object variables
        this.data = array2D;
        this.resolution = resolution;
        this.nCols = nCols;
        this.nRows = nRows;
        this.origin = origin;
        this.data = new int[nRows][nCols];
    }

    /**
     * Return the raster value at a given coordinate location
     *
     * @param x
     * @param y
     * @return
     */
    public int getAttribute(long x, long y) {

        //get array position
        int col = (int) ((x - origin.getX()) / this.resolution);
        int row = (nRows - 1) - (int) ((y - origin.getY()) / this.resolution);
        return this.data[row][col];
    }

    /**
     * Return the raster value at a given coordinate location
     *
     * @param point
     * @return
     */
    public int getAttribute(Point2D point) {

        //get array position
        int col = (int) ((point.getX() - origin.getX()) / this.resolution);
        int row = (nRows - 1) - (int) ((point.getY() - origin.getY()) / this.resolution);
        return this.data[row][col];
    }

    /**
     * Return the raster value at a given coordinate location
     *
     * @param x
     * @param y
     * @return
     */
    public void setAttribute(long x, long y, int value) {

        //get array position
        int col = (int) ((x - origin.getX()) / this.resolution);
        int row = (nRows - 1) - (int) ((y - origin.getY()) / this.resolution);
        this.data[row][col] = value;
    }

    /**
     * Return the raster value at a given coordinate location
     *
     * @param point
     * @return
     */
    public void setAttribute(Point2D point, int value) {

        //get array position
        int col = (int) ((point.getX() - origin.getX()) / this.resolution);
        int row = (nRows - 1) - (int) ((point.getY() - origin.getY()) / this.resolution);
        this.data[row][col] = value;
    }

    /**
     * Outputs the data array as a one dimensional array
     *
     * @return
     */
    public int[] getData1D() {

        //flatten the array and return
        int nCells = this.nRows * this.nCols;
        int[] data1D = new int[nCells];
        int row, col;
        for (int i = 0; i < nCells; i++) {
            row = (int) i / this.nRows;
            col = i % this.nCols;
            data1D[i] = this.data[row][col];
        }
        return data1D;
    }

    /*
     * accessor methods
     */
    /**
     * Return the resolution of the raster
     *
     * @return
     */
    public int getResolution() {
        return this.resolution;
    }

    /**
     * Return the number of rows in the raster
     *
     * @return
     */
    public int getNRows() {
        return this.nRows;
    }

    /**
     * Return the number of columns in the raster
     *
     * @return
     */
    public int getNCols() {
        return this.nCols;
    }

    /**
     * Return the origin of the raster
     *
     * @return
     */
    public Point2D getOrigin() {
        return this.origin;
    }

    /**
     * Return the data as a 2D array
     *
     * @return
     */
    public int[][] getData2D() {
        return this.data;
    }
}