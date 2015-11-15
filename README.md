# FasterCalculator

GeoTools-based Java program to perform bulk GIS raster calculations without any clipping

Called from the command line:
`java -jar fastercalculator [radius] [input directory] [output file *.tif]`

Whilst it works, there is a lot of work to do in terms of user-friendliness, so watch this space!

###TODO:
*Improve memory efficienty by getting rid of the ArrayList of GridCoverage2D's, instead just keep re-populating a single variable in a loop.
* Rename [radius] - it is actually diameter, so needs to be twice the radius of the Viewshed at any rate.
*Get rid of the requirement for [radius], this is inherited from this project's origins for combining Viewsheds and is unneccessary - just work out the width of a file in m.
* Allow users to specify an SLD file, rather than having it hard-coded
* Allow users to decide whether or not they want a randered GeoTiff or the underlying data.
* Add in some options to dynamically style the GeoTiff - maybe with some pre-loaded ColorBrewer scales, and the ability to calculate value bins based upon the underlying data values
