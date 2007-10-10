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
package lua.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lua.CallInfo;
import lua.Lua;
import lua.StackState;
import lua.addon.compile.LexState;
import lua.io.LocVars;
import lua.io.Proto;
import lua.value.LTable;
import lua.value.LValue;

public class DebugStackState extends StackState implements DebugRequestListener {

	private static final boolean DEBUG = false;
    
	protected Map breakpoints = new HashMap();
    protected boolean exiting = false;
    protected boolean suspended = false;
    protected boolean stepping = false;
    protected int lastline = -1;
    protected List debugEventListeners = new ArrayList();
	
	public DebugStackState() {
	}
	
    protected void debugAssert(boolean b) {
    	if ( ! b ) 
    		error( "assert failure" );
    }

    public void addDebugEventListener(DebugEventListener listener) {
        if (!debugEventListeners.contains(listener)) {
            debugEventListeners.add(listener);
        }
    }
    
    public void removeDebugEventListener(DebugEventListener listener) {
        if (debugEventListeners.contains(listener)) {
            debugEventListeners.remove(listener);
        }
    }
    
    protected void notifyDebugEventListeners(DebugEvent event) {
    	for (int i = 0; debugEventListeners != null && i < debugEventListeners.size(); i++) {
    		DebugEventListener listener = (DebugEventListener)debugEventListeners.get(i);
            listener.notifyDebugEvent(event);
    	}
    }
    
	private String getFileLine(int cindex) {
		String func = "?";
		String line = "?";
		String source = "?";
		if ( cindex >= 0 ) {
			CallInfo call = this.calls[cindex];
			Proto p = call.closure.p;
			if ( p != null && p.source != null )
				source = p.source.toJavaString();
			if ( p.lineinfo != null && p.lineinfo.length > call.pc )
				line = String.valueOf( p.lineinfo[call.pc] );
			// TODO: reverse lookup on function name ????
			func = call.closure.luaAsString().toJavaString();
		}
		return source+":"+line+"("+func+")";
	}
	
	
	// override and fill in line number info 
	public void error(String message) {
		super.error( getFileLine(cc)+": "+message );
	}
	
	private void printLuaTrace() {
		System.out.println( "Lua location: "+getFileLine(cc) );
		for ( int cindex=cc-1; cindex>=0; cindex-- )
			System.out.println( "\tin "+getFileLine( cindex ) );
	}
	
	// intercept exceptions and fill in line numbers
	public void exec() {
		try {
			super.exec();
		} catch (AbortException e) {
            // ignored. Client aborts the debugging session.
        } catch ( Exception t ) {        
			t.printStackTrace();
			printLuaTrace();
			System.out.flush();
		}
	}
	
	
	// debug hooks
	public void debugHooks( int pc ) {
        DebugUtils.println("entered debugHook...");
        
		if ( exiting )
			throw new AbortException("exiting");

		synchronized ( this ) {
            
			// anytime the line doesn't change we keep going
			int line = getLineNumber(calls[cc]);
            DebugUtils.println("debugHook - executing line: " + line);
			if ( !stepping && lastline == line ) {
				return;
            }

			// save line in case next op is a step
			lastline = line;
            
            if ( stepping ) {
                DebugUtils.println("suspended by stepping at pc=" + pc);
                notifyDebugEventListeners(new DebugEventStepping());
                suspended = true;
            } else if ( !suspended ) {
                // check for a break point if we aren't suspended already
                Proto p = calls[cc].closure.p;
                String source = DebugUtils.getSourceFileName(p.source);                
                if ( breakpoints.containsKey(constructBreakpointKey(source, line))){
                    DebugUtils.println("hitting breakpoint " + constructBreakpointKey(source, line));
                    notifyDebugEventListeners(
                            new DebugEventBreakpoint(source, line));
                    suspended = true;
                } else {
                    return;
                }                    
			}
			
			// wait for a state change
			while (suspended && !exiting ) {
				try {
					this.wait();
                    DebugUtils.println("resuming execution...");
				} catch ( InterruptedException ie ) {
					ie.printStackTrace();
				}
			}
		}
	}

    /**
     * Get the current line number
     * @param pc program counter
     * @return the line number corresponding to the pc
     */
    private int getLineNumber(CallInfo ci) {
        int[] lineNumbers = ci.closure.p.lineinfo;
        int pc = ci.pc;
        int line = (lineNumbers != null && lineNumbers.length > pc ? lineNumbers[pc] : -1);
        return line;
    }
	
	// ------------------ commands coming from the debugger -------------------   
    
	public DebugResponse handleRequest(DebugRequest request) {
        DebugUtils.println("DebugStackState is handling request: " + request.toString());
        DebugRequestType requestType = request.getType();    	
    	if (DebugRequestType.suspend == requestType) { 
            suspend();             
            return DebugResponseSimple.SUCCESS;
    	} else if (DebugRequestType.resume == requestType) { 
            resume(); 
            return DebugResponseSimple.SUCCESS;
    	} else if (DebugRequestType.exit == requestType) { 
            exit(); 
            return DebugResponseSimple.SUCCESS;
    	} else if (DebugRequestType.lineBreakpointSet == requestType) {
            DebugRequestLineBreakpointToggle setBreakpointRequest 
                = (DebugRequestLineBreakpointToggle)request;
            setBreakpoint(setBreakpointRequest.getSource(), setBreakpointRequest.getLineNumber()); 
            return DebugResponseSimple.SUCCESS;
    	} else if (DebugRequestType.lineBreakpointClear == requestType) {
            DebugRequestLineBreakpointToggle clearBreakpointRequest 
                = (DebugRequestLineBreakpointToggle)request;
            clearBreakpoint(clearBreakpointRequest.getSource(), clearBreakpointRequest.getLineNumber()); 
            return DebugResponseSimple.SUCCESS;
    	} else if (DebugRequestType.callgraph == requestType) { 
            return new DebugResponseCallgraph(getCallgraph());
    	} else if (DebugRequestType.stack == requestType) { 
            DebugRequestStack stackRequest = (DebugRequestStack) request;
            int index = stackRequest.getIndex();
            return new DebugResponseStack(getStack(index));
    	} else if (DebugRequestType.step == requestType) { 
            step(); 
            return DebugResponseSimple.SUCCESS;
        }
    	
    	throw new java.lang.IllegalArgumentException( "unkown request type: "+ request.getType());
	}

    /**
     * suspend the execution
     */
	public void suspend() {
		synchronized ( this ) {
			suspended = true;
			stepping = false;
			lastline = -1;
			this.notify();
		}
	}
    
	/** 
	 * resume the execution
	 */
	public void resume() {
		synchronized ( this ) {
			suspended = false;
            stepping = false;
			this.notify();
		}
	}
	
    /** 
     * terminate the execution
     */
	public void exit() {
		synchronized ( this ) {
			exiting = true;
			this.notify();
		}
	}
	
    /**
     * set breakpoint at line N
     * @param N the line to set the breakpoint at
     */
	public void setBreakpoint(String source, int lineNumber) {
        DebugUtils.println("adding breakpoint " + constructBreakpointKey(source, lineNumber));
		synchronized ( this ) {
			breakpoints.put(constructBreakpointKey(source, lineNumber), Boolean.TRUE );
		}
	}
	
    protected String constructBreakpointKey(String source, int lineNumber) {
        return source + ":" + lineNumber;
    }

    /**
     * clear breakpoint at line lineNumber of source source
     */
	public void clearBreakpoint(String source, int lineNumber) {
        DebugUtils.println("removing breakpoint " + constructBreakpointKey(source, lineNumber));
		synchronized ( this ) {
			breakpoints.remove(constructBreakpointKey(source, lineNumber));
		}
	}
	
    /** 
     * return the current call graph (i.e. stack frames from
     * old to new, include information about file, method, etc.)
     */
	public StackFrame[] getCallgraph() {
		int n = cc;
        
		if ( n < 0 || n >= calls.length )
			return new StackFrame[0];
        
		StackFrame[] frames = new StackFrame[n+1];        
		for ( int i = 0; i <= n; i++ ) {
			CallInfo ci = calls[i];
            frames[i] = new StackFrame(ci, getLineNumber(ci));
		}
		return frames;
	}

	public Variable[] getStack(int index) {
        if (index < 0 || index >= calls.length) {
            //TODO: this is an error, handle it differently
            return new Variable[0];
        }
        
        CallInfo callInfo = calls[index];
        DebugUtils.println("Stack Frame: " + index + "[" + callInfo.base + "," + callInfo.top + "]");
        int top = callInfo.top < callInfo.base ? callInfo.base : callInfo.top;
        Proto prototype = callInfo.closure.p;
        LocVars[] localVariables = prototype.locvars;
        List variables = new ArrayList();
        int localVariableCount = 0;
        Set variablesSeen = new HashSet();
        for (int i = 0; localVariables != null && i < localVariables.length && i <= top; i++) {
            String varName = localVariables[i].varname.toString();
            DebugUtils.print("\tVariable: " + varName); 
            DebugUtils.print("\tValue: " + stack[callInfo.base + i]);
            if (!variablesSeen.contains(varName) &&
                !LexState.isReservedKeyword(varName)) {
                variablesSeen.add(varName);
                LValue value = stack[callInfo.base + i];                
                if (value != null) {
                    int type = value.luaGetType();
                    DebugUtils.print("\tType: " + type);
                    if (type == Lua.LUA_TTABLE) {
                        DebugUtils.println(" (selected)");
                        variables.add(
                                new TableVariable(localVariableCount++, 
                                             varName, 
                                             type, 
                                             (LTable) value));                        
                    } else if (type != Lua.LUA_TFUNCTION &&
                               type != LUA_TTHREAD) {
                        DebugUtils.println(" (selected)");
                        variables.add(
                                new Variable(localVariableCount++, 
                                             varName, 
                                             type, 
                                             value.toString()));
                    } else {
                        DebugUtils.println("");
                    }
                } else {
                    DebugUtils.println("");
                }
            } else {
                DebugUtils.println("");
            }
        }            
        return (Variable[])variables.toArray(new Variable[0]);
	}
	
	
    /**
     * single step forward (go to next statement)
     */
	public void step() {
		synchronized ( this ) {
            suspended = false;
			stepping = true;
			this.notify();
		}
	}
}
