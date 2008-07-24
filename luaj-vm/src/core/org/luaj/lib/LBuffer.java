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
package org.luaj.lib;

import org.luaj.vm.LString;

public class LBuffer {

	private byte[] bytes;
	private int length;
	
	public LBuffer( int initialCapacity ) {
		bytes = new byte[ initialCapacity ];
		length = 0;
	}
	
	public void append( byte b ) {
		ensureCapacity( length + 1 );
		bytes[ length++ ] = b;
	}
	
	public void append( LString str ) {
		final int alen = str.length();
		ensureCapacity( length + alen );
		str.copyInto( 0, bytes, length, alen );
		length += alen;
	}
	
	public void append( String str ) {
		final int alen = LString.lengthAsUtf8( str );
		ensureCapacity( length + alen );
		LString.encodeToUtf8( str, bytes, length );
		length += alen;
	}
	
	public void setLength( int length ) {
		ensureCapacity( length );
		this.length = length;
	}
	
	public LString toLuaString() {
		return new LString( realloc( bytes, length ) );
	}
	
	public void ensureCapacity( int minSize ) {
		if ( minSize > bytes.length )
			realloc( minSize );
	}
	
	private void realloc( int minSize ) {
		bytes = realloc( bytes, Math.max( bytes.length * 2, minSize ) ); 
	}
	
	private static byte[] realloc( byte[] b, int newSize ) {
		byte[] newBytes = new byte[ newSize ];
		System.arraycopy( b, 0, newBytes, 0, Math.min( b.length, newSize ) );
		return newBytes;
	}
}
