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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

/**
 * Stores globals data.
 * @author amanuel
 */
public class GlobalDataModel {

	// Map of GlobalCode id (i.e. identifier) to rating.
	private HashMap< Integer, Integer > ratings = new HashMap< Integer, Integer >();
	//
	private File globalsFile = null;
    //
    private String filenameAudio = null;
    //
    private String notes = "";



	public GlobalDataModel(File globalsFile, String filenameAudio) {

        this.globalsFile = globalsFile;
        this.filenameAudio = filenameAudio;

		// Initialize to default ratings as defined by GlobalCode.
	    for( int i = 0; i < GlobalCode.numCodes(); i++ ) {
	        GlobalCode code = GlobalCode.codeAtIndex( i );
	        ratings.put( code.id, code.defaultRating );
		}
	}

	public int getRating( GlobalCode code ) {
		return ratings.get( code.id).intValue();
	}

	public void	setRating( GlobalCode code, int rating ) {
		ratings.put( new Integer( code.id), rating );
	}


	public String toString() {
		String result = new String();
		
        for( int i = 0; i < GlobalCode.numCodes(); i++ ) {
            GlobalCode code = GlobalCode.codeAtIndex( i );
            result += code.name + ":\t" + getRating( code ) + "\n";
        }
		return result;
	}


    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }


    /**
     * Write globals data to file
     * @throws IOException
     */
	public void writeToFile() throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(globalsFile.toPath(), StandardCharsets.UTF_8)) {

            // begin with audio file in header
            writer.write("Global Ratings");
            writer.newLine();
            writer.write("Audio File:\t" + filenameAudio);
            writer.newLine();

            // insert each code line
            for( int i = 0; i < GlobalCode.numCodes(); i++ ) {
                GlobalCode code = GlobalCode.codeAtIndex( i );
                writer.write(String.format("%s:\t%s", code.name, getRating(code) ));
                writer.newLine();
            }

            // insert notes
            if( !"".equals( notes ) ) {
                writer.write( "Notes: " + notes );
                writer.newLine();
            }

        } catch (IOException e) {
            throw e;
        }
	}
}
