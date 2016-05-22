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

import java.util.StringTokenizer;

// Convenience class, converts representation of time between HH:MM::SS string and integer seconds.
public class TimeCode {

	// NOTE: if you change this format you break ability to read existing code files(*.casaa)
	public static String toString( int seconds ) {
		int hours 	= seconds / 3600;
		int minutes = (seconds / 60) - (hours * 60);

		seconds = seconds - (hours * 3600) - (minutes * 60);

		return String.format( "%d:%02d:%02d", hours, minutes, seconds );
	}

	public static int toSeconds( String string ) {
		StringTokenizer st 		= new StringTokenizer( string, ":" );

		assert( st.countTokens() == 3 );

		int	hours 	= Integer.parseInt( st.nextToken() );
		int minutes = Integer.parseInt( st.nextToken() );
		int	seconds	= Integer.parseInt( st.nextToken() );

		return (hours * 3600) + (minutes * 60) + seconds;
	}
}
