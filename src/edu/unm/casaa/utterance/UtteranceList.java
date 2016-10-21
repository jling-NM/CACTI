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

package edu.unm.casaa.utterance;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Vector;
import java.util.Scanner;
import java.util.StringTokenizer;

import edu.unm.casaa.main.Utils;
import edu.unm.casaa.misc.MiscDataItem;
import javafx.util.Duration;

/**
 * A vector of utterances, stored in their recorded order.
 */
public class UtteranceList {

	private static final long serialVersionUID 	= 1L;
	private Vector< Utterance >	list 			= new Vector<>();

	/**
	 * Append utterance.
	 * @param data new utterance
	 */
	public void	add( Utterance data ){
		list.add( data );
	}

	/**
	 * Remove last utterance, if list is non-empty.
	 */
	public void removeLast(){
		if( !list.isEmpty() ){
			list.remove( list.size() - 1 );
		}
	}

	/**
	 * Return utterance at given index, or null is index is out of bounds.
	 * @param index utterance position
	 * @return the utterance at given index, or null if index is out of bounds
	 */
	public Utterance get(int index){
		if(index < list.size()){
			return list.get(index);
		}
		else{
			return null;
		}
	}

	/**
	 * Get last utterance (coded or not), or null if list is empty.
	 */
	public Utterance last() {
		return list.isEmpty() ? null : list.lastElement();
	}

	public int size(){
		return list.size();
	}

	public boolean isEmpty(){
		return list.isEmpty();
	}

	/**
	 * Write to file.
	 * @param file casaa file
	 * @param filenameAudio audio filename
	 */
	public void writeToFile( File file, String filenameAudio ) throws IOException {
        // TODO: consider append option instead of always writing entire file
		try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {

            // begin with audio file in header
            writer.write("Audio File:\t" + filenameAudio);
            writer.newLine();

            for (int i = 0; i < list.size(); i++) {
                Utterance utterance = list.get(i);
                writer.write(utterance.writeCoded());
                writer.newLine();
            }
        } catch (IOException e) {
            throw e;
        }
	}


    /**
     * Load from file.  Overwrites any existing contents.
     * @param file casaa file
     * @throws Exception
     */
	public void loadFromFile( File file ) throws Exception {

		list.clear(); // Clear existing contents.

		Scanner in;

		try {
			in = new Scanner(file);
		} catch (FileNotFoundException e) {
			throw e;
		}

		// Eat the audio filename line.
		String 			filenameAudio 	= in.nextLine();
		StringTokenizer headReader 		= new StringTokenizer(filenameAudio, "\t");

		headReader.nextToken(); // Eat line heading "Audio Filename:"

		while( in.hasNextLine() ){

			String 			nextStr 	= in.nextLine();
			StringTokenizer st 			= new StringTokenizer(nextStr, "\t");
			int 			lineSize 	= st.countTokens();
			int 			order 		= Integer.parseInt(st.nextToken());

            Duration startTime          = Utils.parseDuration(st.nextToken());
            MiscDataItem 	item 		= new MiscDataItem(order, startTime);

			if( lineSize == 4 ){

                int codeId = Integer.parseInt( st.nextToken() );
                // look up parsed code in user config codes loaded at init
                try {
                    item.setMiscCodeByValue(codeId);
                } catch (Exception e) {
                    // if lookup failed there is a possible disconnect between codes in casaa file
                    // and codes in user config file
                    throw new Exception( String.format("Code(%d) in casaa file not found in user configuration file", codeId) );
                }
				st.nextToken(); //throw away the code string

                add(item);
			}

		}

	}


    /**
     * Separate function for reading audio filename from code file
     * @param file casaa file
     * @return filenameAudio
     */
    public static String getAudioFilename( File file ) throws IOException {

        Scanner in;
        try {
            in = new Scanner(file);
        } catch (FileNotFoundException e) {
            throw e;
        }

        if( !in.hasNext() ){
            throw new IOException("No Audio File Listed in casaa file");
        }

        // Get the audio filename line.
        String 			filenameAudio 	= in.nextLine();
        StringTokenizer headReader 		= new StringTokenizer(filenameAudio, "\t");

        headReader.nextToken(); // Eat line heading "Audio Filename:"
        filenameAudio = headReader.nextToken();
        if( (filenameAudio.trim()).equalsIgnoreCase("") ){
            throw new IOException("No Audio File Listed in casaa file");
        }

        return filenameAudio;
    }
}
