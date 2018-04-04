package edu.unm.casaa.main;

import java.io.IOException;

/**
 * A FileFormatException extends IOException to indicate an invalid file format
 */
public class FileFormatException extends IOException {
    public FileFormatException() {
        super();
    }

    public FileFormatException(String message) {
        super(message);
    }

    public FileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileFormatException(Throwable cause) {
        super(cause);
    }
}
