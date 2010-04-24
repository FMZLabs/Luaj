/*******************************************************************************
* Copyright (c) 2009 Luaj.org. All rights reserved.
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
package org.luaj.vm2.lib;

import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

abstract public class VarArgFunction extends LibFunction {
	public VarArgFunction() {
	}
	
	public VarArgFunction( LuaValue env ) {
		this.env = env;
	}
	
	public LuaValue call() {
		return invoke(NONE).arg1();
	}

	public LuaValue call(LuaValue arg) {
		return invoke(arg).arg1();
	}

	public LuaValue call(LuaValue arg1, LuaValue arg2) {
		return invoke(varargsOf(arg1,arg2)).arg1();
	}

	public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
		return invoke(varargsOf(arg1,arg2,arg3)).arg1();
	}

	/** 
	 * Override and implement for the best performance. 
	 * May not have expected behavior for tail calls. 
	 * Should not be used if either:
	 * - function needs to be used as a module
	 * - function has a possibility of returning a TailcallVarargs
	 * @param args the arguments to the function call.
	 */
	public Varargs invoke(Varargs args) {
		LuaThread.onCall(this);
		try {
			return this.onInvoke(args).eval();
		} finally {
			LuaThread.onReturn();
		}
	}

	/**
	 * Override to provide a call implementation that runs in an environment
	 * that can participate in setfenv, and behaves as expected 
	 * when returning TailcallVarargs. 
	 * @param args the arguments to the function call.
	 */
	public Varargs onInvoke(Varargs args) {
		return invoke(args);
	}
	
} 
