/*******************************************************************************
* Copyright (c) 2007 LuaJ. All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
******************************************************************************/
package org.luaj.debug.response;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DebugResponseSimple implements DebugResponse {
    protected boolean isSuccessful;
    
    public static final DebugResponseSimple SUCCESS = new DebugResponseSimple(true);
    public static final DebugResponseSimple FAILURE = new DebugResponseSimple(false);
    
    public DebugResponseSimple(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }
    
    public boolean isSuccessful() {
        return this.isSuccessful;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.valueOf(isSuccessful);
    }

    public static void serialize(DataOutputStream out, DebugResponseSimple response) 
    throws IOException {
		out.writeBoolean(response.isSuccessful());		
	}
    
    public static DebugResponseSimple deserialize(DataInputStream in) 
    throws IOException {
		boolean value = in.readBoolean();
		return value ? SUCCESS : FAILURE;
	}
}
