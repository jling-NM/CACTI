package edu.unm.casaa.main;

import javafx.util.Duration;

/**
 * Created by josef on 5/6/16.
 */
public class Utils {

    public static String formatDuration(Duration duration) {
        double duration_secs = duration.toSeconds();
        return String.format("%02.0f:%02.0f:%02.0f", Math.floor((duration_secs/3600) % 24), Math.floor( (duration_secs/60) % 60), (duration_secs % 60) );
    }

    public static int convertTimeToBytes( int bytesPerSecond, Duration time ) {
        return ((int) time.toSeconds()) * bytesPerSecond;
    }

    /***********************************************************
     *
     * @param filename
     * @param newSuffix Suffixes should be specified without leading period.
     * @return copy of filename with oldSuffix (if present) removed, and newSuffix added.
     */
    public static String changeSuffix( String filename, String newSuffix ) {
        String	result 	= filename;
        int		index 	= filename.lastIndexOf( '.' );

        if( index > 0 ) {
            result = result.substring( 0, index );
        }
        return result + "." + newSuffix;
    }
}
