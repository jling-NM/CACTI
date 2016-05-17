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

package edu.unm.casaa.globals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import edu.unm.casaa.globals.GlobalCode;

/**
 * Stores globals data.
 * @author amanuel
 */
public class GlobalDataModel {

	// Map of GlobalCode value (i.e. identifier) to rating.
	private HashMap< Integer, Integer > ratings = new HashMap< Integer, Integer >();

	public GlobalDataModel() {
		// Initialize to default ratings as defined by GlobalCode.
	    for( int i = 0; i < GlobalCode.numCodes(); i++ ) {
	        GlobalCode code = GlobalCode.codeAtIndex( i );

	        ratings.put( code.value, code.defaultRating );
		}
	}

	public int getRating( GlobalCode code ) {
		return ratings.get( code.value ).intValue();
	}

	public void	setRating( GlobalCode code, int rating ) {
		ratings.put( new Integer( code.value ), rating );
	}

	public String toString() {
		String result = new String();
		
        for( int i = 0; i < GlobalCode.numCodes(); i++ ) {
            GlobalCode code = GlobalCode.codeAtIndex( i );

            result += code.name + ":\t" + getRating( code ) + "\n";
        }
		return result;
	}

	public void writeToFile( File file, String filenameAudio, String notes ) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter( new FileWriter( file, false ) );
			writer.println( "Global Ratings\n" );
			writer.println( "Audio File:\t" + filenameAudio );
			writer.println( toString() );
			if( !"".equals( notes ) ) {
				writer.println( "Notes:\n" + notes );
			}
		} catch( IOException e ) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}
}
