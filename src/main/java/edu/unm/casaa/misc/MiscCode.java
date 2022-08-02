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

import java.util.ListIterator;
import java.util.Vector;

// MiscCode associates a label, such as "CR+/-" or "ADP", with a numeric value.
public class MiscCode {

	public static final int 			INVALID			= -1;
	public static final MiscCode		INVALID_CODE	= new MiscCode();

	// List of available codes.  Built when we parse XML file.
	private static Vector< MiscCode >	list	        = new Vector<>();

    public  int 			 value	                    = INVALID;
    public  String		     name	                    = "";
    private MiscCode.Speaker speaker                    = null;

    // possible speakers for MISC codes
	public enum Speaker { Therapist, Client }



	// Class:


    /**
     * Expose list iterator
     * @return
     */
    public static ListIterator<MiscCode> getIterator() {
	    return list.listIterator();
    }

    /**
     * Add new code
     * @param newCode code to be added
     * @throws Exception on duplicates
     */
	public static void addCode( MiscCode newCode ) throws Exception {

		// Check that we're not duplicating an existing value or label.
        for (MiscCode code : list) {
            if (code.value == newCode.value || code.name.equals(newCode.name)) {
                throw new Exception(String.format("New code %s conflicts with existing code %s", newCode.toDisplayString(), code.toDisplayString()));
            }
        }
		list.add( newCode );
	}


	public static int numCodes() {
		return list.size();
	}

    /**
     *
     * @param index code position
     * @return code at index
     * @throws ArrayIndexOutOfBoundsException
     */
	public static MiscCode codeAtIndex( int index ) throws ArrayIndexOutOfBoundsException {
		return list.get( index );
	}


    /**
     * Retrieve code
     * @param value integer code the MISC statistical code
     * @return MiscCode for given id value
     * @throws NullPointerException
     */
	public static MiscCode codeWithValue( int value ) throws NullPointerException {

		// Check known codes.
		if( value == INVALID_CODE.value ) {
			return INVALID_CODE;
		}
		// Check user codes loaded from config for matchs.
        for (MiscCode code : list) {
            if (code.value == value) {
                return code;
            }
        }
        // if we get here, no matching code
		throw new NullPointerException("Code with given value not found: " + value);

	}

	// PRE: code exists with given name.
	public static MiscCode codeWithName( String name ) {
		// Check known codes.
		if( name.equals( INVALID_CODE.name ) ) {
			return INVALID_CODE;
		}
		// Check user codes.
        for (MiscCode code : list) {
            if (code.name.equals(name)) {
                return code;
            }
        }
		assert false : "Code with given name not found: " + name;
		return null;
	}

	// Instance:

	public MiscCode( int value, String name, MiscCode.Speaker speaker) {
		this.value    = value;
		this.name     = name;
        this.speaker  = speaker;
	}

	public MiscCode() {
	}

	public boolean isValid() {
		return value != INVALID;
	}

	// Get string representation for use in user dialogs.
	public String toDisplayString() {
		return "(name: " + name + ", value: " + value + ")";
	}

	public MiscCode.Speaker getSpeaker(){
		return this.speaker;
	}

	public static void clear() {
		list.clear();
	}
}
