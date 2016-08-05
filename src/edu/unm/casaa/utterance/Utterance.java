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

import edu.unm.casaa.misc.MiscCode;

public interface Utterance {

	public int getEnum();
	public String getStartTime();	
	public String getEndTime();
	public int getStartBytes();
	public int getEndBytes();

	public boolean isParsed();
	public boolean isCoded();
	public MiscCode getMiscCode();
	
	// Strip end data, so isParsed() will return false.  Preserves start data.
	public void	stripEndData();
	
	// Strip code, so isCoded() will return false.  Writes MiscCode.INVALID_CODE.
	public void stripMiscCode();

	// Enum and start time will be initialized by derived class constructor.
	public void	setEnum(int index);
	public void setEndTime(String end);	
	public void setEndBytes(int bytes);
	public void setMiscCodeByValue(int value);
	public void setMiscCode(MiscCode code);

	public String displayCoded();
	// Output order should be tab-delimited:
	// order startCode endCode [codeCode codeString] "\r\n"
	// The section in [] is written only if utterance has been coded.
	public String writeParsed();
	public String writeCoded();
}
