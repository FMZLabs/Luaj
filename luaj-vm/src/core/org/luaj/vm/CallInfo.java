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
package org.luaj.vm;


public class CallInfo {

    public LClosure closure;
    public int base;
    public int top;
    public int pc;
    public int resultbase;
    public int nresults;

    public CallInfo(LClosure c, int base, int top, int resultoff, int nresults) {
        this.closure = c;
        this.base = base;
        this.top = top;
        this.resultbase = resultoff;
        this.nresults = nresults;
        this.pc = 0;
    }

	public boolean isLua() {
		return true;
	}

	/**
	 * @return current line number, or -1 if no line info found
	 */
	public int currentline() {
		int[] li = closure.p.lineinfo;
		if ( li != null && pc <= li.length )
			return li[currentpc()];
		return -1;
	}

	/**
	 * @param vm
	 * @return current function executing, or null
	 */
	public LFunction currentfunc(LuaState vm) {
		int a = currentfunca(vm);
		if ( a >= 0 ) {
			LValue v = vm.stack[base + a]; 
			if ( v.isFunction() )
				return (LFunction) v;
		}
		return null;
	}

	/**
	 * @param vm
	 * @return register of the current function executing, or null
	 */
	public int currentfunca(LuaState vm) {
		int i = closure.p.code[currentpc()];
		int op = Lua.GET_OPCODE(i);
		if ( op == Lua.OP_CALL || op == Lua.OP_TAILCALL )
			return Lua.GETARG_A(i);
		return -1;
	}

	/** 
	 * Get current program counter or instruction being executed now.
	 */ 
	public int currentpc() {
		return pc>0? pc-1: 0;
	}

}
