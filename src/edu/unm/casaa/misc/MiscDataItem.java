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
import edu.unm.casaa.main.Utils;
import javafx.util.Duration;

/**
 * The MiscDataItem is an object designed to hold all of the
 * relevant information for a MISC utterance.
 * @author Alex Manuel
 *
 */
public class MiscDataItem implements Utterance {

	private int 		orderEnum	= -1;
	private Duration    startTime   = Duration.ZERO;
	private MiscCode 	miscCode	= new MiscCode();

	/**
	 * Constructor requires the order from the data queue,
	 * and the start time from the player.
	 * @param orderEnum the enumerated order for this utterance
	 * @param startTime the start time code for this utterance
	 */
	public MiscDataItem( int orderEnum, double startTime ) {
		this.orderEnum 	= orderEnum;
		this.startTime 	= Duration.seconds(startTime);
	}

    public MiscDataItem( int orderEnum, Duration startTime ) {
        this.orderEnum 	= orderEnum;
        this.startTime 	= startTime;
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
	public Duration getStartTime() {
		return startTime;
	}

	/**
	 * Return true if this utterance has been coded.
	 */
	public boolean isCoded() {
		return miscCode.isValid();
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
     * Sets the MISC statistical code for this utterance by integer value.
     * @param value integer code the MISC statistical code
     * @throws NullPointerException
     */
	public void setMiscCodeByValue( int value ) throws NullPointerException {
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
        return isCoded() ? writeCoded() : writeParsed();
	}

	public String displayCoded(){
		return ("" + Utils.formatDuration(startTime) + "\t" +
				miscCode.name);
	}
	public String writeCoded(){
		return ("" + orderEnum 	+ "\t" +
                    Utils.formatDuration(startTime)	+ "\t" +
					miscCode.value 	+ "\t" +
					miscCode.name);
	}
	
	public String writeParsed(){
		return ("" + orderEnum 	+ "\t" +
                     startTime.toSeconds() 	+ "s\t");
	}

}
