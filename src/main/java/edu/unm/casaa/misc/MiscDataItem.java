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

import edu.unm.casaa.main.Utils;
import edu.unm.casaa.utterance.Utterance;
import javafx.util.Duration;

/**
 * The MiscDataItem is an object designed to hold all of the
 * relevant information for a MISC utterance.
 * @author Alex Manuel with changes by josef ling
 *
 */
public class MiscDataItem implements Utterance {

    private String      id;
    private Duration    startTime   = Duration.ZERO;
    private MiscCode    miscCode	= new MiscCode();
    private String      annotation = "";


    /**
     * Constructor requires the order from the data queue,
     * and the start time from the player.
     * @param id sortable; string representation of start time here
     * @param startTime the start time code for this utterance
     */
    public MiscDataItem( String id, double startTime ) {
        this.id 	   = id;
        this.startTime = Duration.seconds(startTime);
    }

    public MiscDataItem( String id, Duration startTime ) {
        this.id 	   = id;
        this.startTime = startTime;
    }

    /**
     * Returns the start time code for this utterance.
     * @return the start time code
     */
    public Duration getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) { this.startTime = Duration.seconds(startTime); }

    /**
     * Returns the MISC code for this utterance.
     * Returned misc code will have value MiscCode.INVALID if value wasn't set.
     * @return the MISC code.
     */
    public MiscCode getMiscCode() {
        return miscCode;
    }

    public void setID(String id) {
        this.id = id;
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

    public String getID() {
        return this.id;
    }

    public String toString(){
        return String.valueOf(this.id);
    }

    public String displayCoded(){
        return ("" + Utils.formatDuration(startTime) + " " + miscCode.name);
    }

    @Override
    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    @Override
    public String getAnnotation() {
        return this.annotation;
    }

    @Override
    public Boolean isAnnotated() {
        return !this.annotation.isEmpty();
    }
    // TODO: rating ids and annotation are bypassing data model for utterance MiscDataItem which really sucks. RESOLVE
}
