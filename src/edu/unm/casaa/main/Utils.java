package edu.unm.casaa.main;

import javafx.util.Duration;

import java.util.StringTokenizer;

/**
 *
 */
public class Utils {

    /**
     * Newer format using Duration
     * @param duration time
     * @return time converted to string
     */
    public static String formatDuration(Duration duration) {
        double duration_secs = duration.toSeconds();
        return String.format("%02.0f:%02.0f:%04.1f", Math.floor((duration_secs/3600) % 24), Math.floor( (duration_secs/60) % 60), (duration_secs % 60) );
    }

    public static String formatID(Duration startTime, int codeValue) {
        return String.format("%s%d", formatDuration(startTime).replaceAll("[^0-9]", ""), codeValue);
    }

    public static Duration parseDuration( String string ) {
        StringTokenizer st 		= new StringTokenizer( string, ":" );

        assert( st.countTokens() == 3 );

        int	hours 	= Integer.parseInt( st.nextToken() );
        int minutes = Integer.parseInt( st.nextToken() );
        double seconds	= Double.valueOf( st.nextToken() );

        return Duration.seconds( (hours * 3600) + (minutes * 60) + seconds);
    }


    /**
     * @param filename input filename to change
     * @param newSuffix Suffixes should be specified without leading period.
     * @return copy of filename with oldSuffix (if present) removed, and newSuffix added.
     */
    public static String changeSuffix(String filename, String newSuffix) {
        String	result 	= filename;
        int		index 	= filename.lastIndexOf( '.' );

        if( index > 0 ) {
            result = result.substring( 0, index );
        }
        return result + "." + newSuffix;
    }

}
