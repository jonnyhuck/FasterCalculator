package co.uk.winddirecttools.fastercalculator;

/**
 * Exception for if the resolutions don't match
 * @author jonathan.huck
 */
public class ResolutionException extends RuntimeException {
    public ResolutionException(){
        super();
    }
    public ResolutionException(String msg){
        super(msg);
    }
}