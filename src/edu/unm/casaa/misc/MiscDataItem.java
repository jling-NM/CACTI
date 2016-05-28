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

package edu.unm.casaa.misc;

import edu.unm.casaa.utterance.Utterance;

/**
 * The MiscDataItem is an object designed to hold all of the
 * relevant information for a MISC utterance.
 * @author Alex Manuel
 *
 */
public class MiscDataItem implements Utterance {

	private int 		orderEnum	= -1;
	private String 		startTime	= null;
	private int 		startBytes	= -1;
	private String 		endTime		= null;
	private int 		endBytes	= -1;
	private MiscCode 	miscCode	= new MiscCode();

	/**
	 * Constructor requires the order from the data queue,
	 * and the start time from the player.
	 * @param orderEnum the enumerated order for this utterance
	 * @param startTime the start time code for this utterance
	 */
	public MiscDataItem( int orderEnum, String startTime, int startBytes ) {
		this.orderEnum 	= orderEnum;
		this.startTime 	= startTime;
		this.startBytes = startBytes;
	}

	/**
	 * Set order number where this particular utterance occurs.
	 * @param index
	 */
	public void setEnum( int index ) {
		orderEnum = index;
	}

	/**
	 * Returns the order number where this particular utterance occurs.
	 * Returns -1 if value wasn't properly set.
	 * @return the order of this utterance in the queue. 
	 */
	public int getEnum() {
		return orderEnum;
	}

	/**
	 * Returns the start time code for this utterance.
	 * Returns -1 if value wasn't properly set.
	 * @return the start time code
	 */
	public String getStartTime() {
		return startTime;
	}
	
	/**
	 * Returns the number of bytes into the audio file 
	 * at the start of this utterance.
	 * Returns -1 if value wasn't properly set.
	 * @return the start time code
	 */
	public int getStartBytes() {
		return startBytes;
	}
	
	/**
	 * Returns the end time code for this utterance.
	 * Returns -1 if value wasn't properly set.
	 * @return the end time code
	 */
	public String getEndTime() {
		//should this throw an exception if the end time 
		//can't be retrieved due to the audio file ending?
		return endTime;
	}

	/**
	 * Returns the number of bytes into the audio file 
	 * at the end of this utterance.
	 * Returns -1 if value wasn't properly set.
	 * @return the end time code
	 */
	public int getEndBytes() {
		//should this throw an exception if the end time 
		//can't be retrieved due to the audio file ending?
		return endBytes;
	}

	/**
	 * Return true if this utterance has been parsed.
	 */
	public boolean isParsed() {
		return endBytes != -1;
	}

	/**
	 * Return true if this utterance has been coded.
	 */
	public boolean isCoded() {
		return miscCode.isValid();
	}

	/**
	 * Strip end data.
	 */
	public void	stripEndData() {
		endBytes 	= -1;
		endTime		= null;
	}

	public void stripMiscCode() {
		setMiscCode( MiscCode.INVALID_CODE );
	}

	/**
	 * Returns the MISC code for this utterance.
	 * Returned misc code will have value MiscCode.INVALID if value wasn't set.
	 * @return the MISC code.
	 */
	public MiscCode getMiscCode() {
		return miscCode;
	}
	
	/**
	 * Sets the end time code for this utterance.
	 * @param end the end time code
	 */
	public void setEndTime( String end ) {
		this.endTime = end;
	}

	/**
	 * Sets the end time byte accumulation for this utterance.
	 * @param bytes end time code
	 */
	public void setEndBytes( int bytes ) {
		this.endBytes = bytes;
	}

	/**
	 * Sets the MISC statistical code for this utterance by integer value.
	 * @param value integer code the MISC statistical code
	 */
	public void setMiscCodeByValue( int value ) {
		miscCode = MiscCode.codeWithValue( value );
	}
	
	/**
	 * Sets the MISC statistical code for this utterance.
	 * @param code name the MISC statistical code
	 */
	public void setMiscCode( MiscCode code ) {
		this.miscCode = code;
	}
	
	/**
	 * Returns the string value for this utterance,
	 * based on whether it has received a MISC code or not.
	 * This is used for writing the utterance to a File.
	 * @return a string representation of this utterance
	 */
	public String toString(){
        // 20160527 - JL - reversed logic as the original didn't make sense to me or work.
		//return isCoded() ? writeParsed() : writeCoded();
        return isCoded() ? writeCoded() : writeParsed();
	}

	public String writeCoded(){
		return ("" + orderEnum 	+ "\t" +
					startTime 	+ "\t" +
					endTime 	+ "\t" +
					startBytes	+ "\t" +
					endBytes	+ "\t" +
					miscCode.value 	+ "\t" +
					miscCode.name);
	}
	
	public String writeParsed(){
		return ("" + orderEnum 	+ "\t" +
					startTime 	+ "\t" +
					endTime 	+ "\t" +
					startBytes	+ "\t" +
					endBytes);
	}

}
