package ru.programpark.mirah.editor.utils;


/**
 * This class represents an error that is thrown when a bug is 
 * recognized inside the runtime. Basically it is thrown when
 * a constraint is not fullfilled that should be fullfiled. 
 * 
 * @author Jochen Theodorou
 */
public class MirahBugError extends AssertionError {
    
    // message string
    private String message;
    // optional exception
    private final Exception exception;

    /**
     * constructs a bug error using the given text
     * @param message the error message text
     */
    public MirahBugError( String message ) {
        this(message, null);
    }
    
    /**
     * Constructs a bug error using the given exception
     * @param exception cause of this error
     */
    public MirahBugError( Exception exception ) {
        this(null, exception);
    }
    
    /**
     * Constructs a bug error using the given exception and
     * a text with additional information about the cause 
     * @param msg additional information about this error
     * @param exception cause of this error
     */
    public MirahBugError( String msg, Exception exception ) {
        this.exception = exception;
        this.message = msg;
    }

    /**
     * Returns a String representation of this class by calling <code>getMessage()</code>.  
     * @see #getMessage()
     */
    public String toString() {
        return getMessage();
    }
    
    /**
     * Returns the detail message string of this error. The message 
     * will consist of the bug text prefixed by "BUG! " if there this
     * instance was created using a message. If this error was
     * constructed without using a bug text the message of the cause 
     * is used prefixed by "BUG! UNCAUGHT EXCEPTION: "
     *  
     * @return the detail message string of this error.
     */
    public String getMessage() {
        if( message != null )
        {
            return "BUG! "+message;
        }
        else
        {
            return "BUG! UNCAUGHT EXCEPTION: " + exception.getMessage();
        }
    }
    
    public Throwable getCause() {
        return this.exception;
    }    
    
    /**
     * Returns the bug text to describe this error
     */
    public String getBugText(){
        if( message != null ){
            return message;
        } else {
            return exception.getMessage();
        }
    }
    
    /**
     * Sets the bug text to describe this error
     */
    public void setBugText(String msg) {
        this.message = msg;
    }
}
