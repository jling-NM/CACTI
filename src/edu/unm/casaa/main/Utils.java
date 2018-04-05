/*
This source code file is part of the CASAA Treatment Coding System Utility
    Copyright (C) 2009  UNM CASAA
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
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

    /**
     * Generate a unique GUI id for an utterance which combines the timestamp and code
     * @param startTime
     * @param codeValue
     * @return application guid
     */
    public static int formatID(Duration startTime, int codeValue) {
        return Integer.valueOf(String.format("%s%d", formatDuration(startTime).replaceAll("[^0-9]", ""), codeValue));
    }

    public static String formatIDstring(Duration startTime, int codeValue) {
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
