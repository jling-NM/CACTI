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
import java.util.*;
import edu.unm.casaa.main.Utils;
import edu.unm.casaa.misc.MiscDataItem;
import javafx.util.Duration;


/**
 * A sortedMap of utterances, sorted by id which should be a string representation of start time
 */
public class UtteranceList {

	private static final long serialVersionUID 	   = 1L;
    private SortedMap< String, Utterance > utteranceTreeMap = new TreeMap<>();
    private File storageFile                       = null;
    private String audioFilename                   = null;


    public UtteranceList (File storageFile, String audioFilename) {
        this.storageFile = storageFile;
        this.audioFilename = audioFilename;
    }


	/**
	 * Append utterance.
	 * @param utr new utterance
	 */
	public void	add( Utterance utr ) throws IOException {

        utteranceTreeMap.put( Utils.durationToID(utr.getStartTime()), utr);
        System.out.println("UtteranceList added utr:"+utr.displayCoded());

        try {
            writeToFile();
        } catch (IOException e) {
            throw e;
        }
	}

	/**
	 * Remove last utterance, if list is non-empty.
	 */
	public void removeLast() throws IOException {
        if( !utteranceTreeMap.isEmpty() ) {
            utteranceTreeMap.remove(utteranceTreeMap.lastKey());
        }
	}

	/**
	 * Remove utterance
	 */
	public void remove(Utterance utr) throws IOException {
		utteranceTreeMap.remove(utr.getID());
        System.out.println("UtteranceList remove removed utr:"+utr.getID());
        try {
            writeToFile();
        } catch (IOException e) {
            throw e;
        }
	}


	/**
	 * Return utterance with given id
	 * @return the utterance or null
	 */
	public Utterance get(String utteranceID){
        return utteranceTreeMap.get(utteranceID);
    }

	/**
	 * Get last utterance (coded or not), or null if list is empty.
	 */
	public Utterance last() {
		return utteranceTreeMap.isEmpty() ? null : utteranceTreeMap.get(utteranceTreeMap.lastKey());
	}

	public Collection<Utterance> values() {
        return utteranceTreeMap.values();
    }

	public int size(){
		return utteranceTreeMap.size();
	}

	public boolean isEmpty(){
		return utteranceTreeMap.isEmpty();
	}

	/**
	 * Write to file.
	 */
	public void writeToFile() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(storageFile.toPath(), StandardCharsets.UTF_8)) {

            // begin with audio file in header
            writer.write("Audio File:\t" + audioFilename);
            writer.newLine();

            for (Utterance utr : utteranceTreeMap.values()) {
                writer.write(utr.writeCoded());
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

		utteranceTreeMap.clear(); // Clear existing contents.

		Scanner in;

		try {
			in = new Scanner(file);
		} catch (FileNotFoundException e) {
			throw e;
		}

		storageFile = file;

		// Eat the audio filename line.
		String 			filenameAudio 	= in.nextLine();
		StringTokenizer headReader 		= new StringTokenizer(filenameAudio, "\t");

        // Eat  "Audio Filename:"
		headReader.nextToken();
        // local reference of audiofilename
        audioFilename = headReader.nextToken();

		while( in.hasNextLine() ){

			String 			nextStr 	= in.nextLine();
			StringTokenizer st 			= new StringTokenizer(nextStr, "\t");
			int 			lineSize 	= st.countTokens();

            Duration startTime          = Utils.parseDuration(st.nextToken());
            MiscDataItem 	item 		= new MiscDataItem(Utils.durationToID(startTime), startTime);

            // TODO: also read old format of 7 tokens properly
			if( lineSize == 3 ){

                int codeId = Integer.parseInt( st.nextToken() );
                // look up parsed code in user config codes loaded at init
                try {
                    item.setMiscCodeByValue(codeId);
                } catch (Exception e) {
                    // if lookup failed there is a possible disconnect between codes in casaa file
                    // and codes in user config file
                    throw new Exception( String.format("Code(%d) in casaa file not found in user configuration file", codeId) );
                }
				st.nextToken(); // throw away the code string

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
