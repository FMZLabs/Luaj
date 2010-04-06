/*******************************************************************************
* Copyright (c) 2010 Luaj.org. All rights reserved.
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
package org.luaj.vm2;

public class TailcallVarargs extends Varargs {

	private LuaValue func;
	private Varargs args;
	private Varargs result;
	
	public TailcallVarargs(LuaValue f, Varargs args) {
		this.func = f;
		this.args = args;
	}
	
	private void eval() {
		TailcallVarargs nextcall = this;
		do {
			LuaValue func = nextcall.func;
			Varargs args = nextcall.args;
			nextcall = null;
			Varargs r = func.invoke(args);
			
			if (r instanceof TailcallVarargs)
				nextcall = (TailcallVarargs)r;
			else
				this.result = r;
			
		} while (nextcall != null);
	}
	
	@Override
	public LuaValue arg( int i ) {
		if ( result == null )
			eval();
		return result.arg(i);
	}
	
	@Override
	public LuaValue arg1() {
		if (result == null)
			eval();
		return result.arg1();
	}
	
	@Override
	public int narg() {
		if (result == null)
			eval();
		return result.narg();
	}
}