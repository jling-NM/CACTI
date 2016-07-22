package edu.unm.casaa.main;

import javafx.util.Duration;

/**
 *
 */
class Utils {

    /**
     * Newer format using Duration
     * @param duration time
     * @return time converted to string
     */
    static String formatDuration(Duration duration) {
        double duration_secs = duration.toSeconds();
        return String.format("%02.0f:%02.0f:%02.0f", Math.floor((duration_secs/3600) % 24), Math.floor( (duration_secs/60) % 60), (duration_secs % 60) );
    }

    /**
     * @param bytesPerSecond rate
     * @param time time to convert
     * @return byte position
     */
    static int convertTimeToBytes(int bytesPerSecond, Duration time) {
        // truncation cast to int to cut off '.0' introduced by double seconds
        return (int) ( time.toSeconds() * bytesPerSecond );
    }

    /**
     * @param filename input filename to change
     * @param newSuffix Suffixes should be specified without leading period.
     * @return copy of filename with oldSuffix (if present) removed, and newSuffix added.
     */
    static String changeSuffix(String filename, String newSuffix) {
        String	result 	= filename;
        int		index 	= filename.lastIndexOf( '.' );

        if( index > 0 ) {
            result = result.substring( 0, index );
        }
        return result + "." + newSuffix;
    }

}
