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
******************************************************************************/package org.luaj.vm2.lib;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Varargs;

/** 
 * Base library implementation, targeted for JME platforms.
 * 
 * BaseLib instances are typically used as the initial globals table 
 * when creating a new uniqued runtime context.  
 * 
 * Since JME has no file system by default, dofile and loadfile use the 
 * FINDER instance to find resource files.  The default loader chain
 * in PackageLib will use these as well.  
 * 
 * For an implementation that looks in the current directory on JSE, 
 * use org.luaj.lib.j2se.BaseLib instead.
 *  
 * @see org.luaj.vm2.lib.jse.JseBaseLib
 */
public class BaseLib extends LuaTable implements ResourceFinder {
	public static final String VERSION = "Luaj 2.0";
	
	public static InputStream STDIN  = null;
	public static PrintStream STDOUT = System.out;
	public static PrintStream STDERR = System.err;

	/** 
	 * Singleton file opener for this Java ClassLoader realm.
	 * 
	 * Unless set or changed elsewhere, will be set by the BaseLib that is created.
	 */
	public static ResourceFinder FINDER;
	
	/**
	 * Construct a base libarary instance and initialize the functions in it.
	 */
	public BaseLib() {
		this.set( "_G", this );
		this.set( "_VERSION", VERSION );
		LibFunction.bind( this, new BaseFunc1().getClass(), new String[] {
			"getfenv", // ( [f] ) -> env
			"getmetatable", // ( object ) -> table 
			"tostring", // (e) -> value
		} );
		LibFunction.bind( this, new BaseFunc2().getClass(), new String[] {
			"collectgarbage", // ( opt [,arg] ) -> value
			"error", // ( message [,level] ) -> ERR
			"rawequal", // (v1, v2) -> boolean
			"setfenv", // (f, table) -> void
			"tonumber", // (e [,base]) -> value
		} );
		LibFunction.bind( this, new BaseFuncV().getClass(), new String[] {
			"assert", // ( v [,message] ) -> v, message | ERR
			"dofile", // ( filename ) -> result1, ...
			"load", // ( func [,chunkname] ) -> chunk | nil, msg
			"loadfile", // ( [filename] ) -> chunk | nil, msg
			"loadstring", // ( string [,chunkname] ) -> chunk | nil, msg
			"pcall", // (f, arg1, ...) -> status, result1, ...
			"xpcall", // (f, err) -> result1, ...
			"print", // (...) -> void
			"select", // (f, ...) -> value1, ...
			"unpack", // (list [,i [,j]]) -> result1, ...
			"type",  // (v) -> value
			"rawget", // (table, index) -> value
			"rawset", // (table, index, value) -> table
			"setmetatable", // (table, metatable) -> table
		} );
		
		// pairs and ipars need iterator functions
		// "next", // ( table, [index] ) -> next-index, next-value
		// "inext", // not public ( table, [int-index] ) -> next-index, next-value
		// "ipairs", // (t) -> iter-func, t, 0
		// "pairs", // (t) -> iter-func, t, nil
		LuaValue next = new BaseIter(0,"next",null);
		LuaValue inext = new BaseIter(1,"inext",null);
		this.set( "pairs", new BaseIter(2,"pairs",next) );
		this.set( "ipairs", new BaseIter(3,"ipairs",inext) );
		this.set( "next", next );
		
		// set the default resource finder if not set already
		if ( FINDER == null )
			FINDER = this;
	}

	/** ResourceFinder implementation 
	 * 
	 * Tries to open the file as a resource, which can work for . 
	 */
	public InputStream findResource(String filename) {
		Class c = getClass();
		return c.getResourceAsStream(filename.startsWith("/")? filename: "/"+filename);
	}

	public static class BaseFunc1 extends OneArgFunction {
		public LuaValue call(LuaValue arg) {
			switch ( opcode ) {
			case 0: { // "getfenv", // ( [f] ) -> env
				if ( ! arg.isfunction() ) {
					int i = arg.checkint();
					arg = (i==0)? (LuaValue) LuaThread.getRunning(): (LuaValue) LuaThread.getCallstackFunction(i-1);
					if ( arg == null )
						LuaValue.argerror(1, "invalid level");
				}
				return arg.getfenv();
			}
			case 1: // "getmetatable", // ( object ) -> table
				LuaValue mt = arg.getmetatable();
				return mt!=null? mt: NIL;
			case 2: // "tostring", // (e) -> value
				return arg.type() == LuaValue.TSTRING? arg: valueOf(arg.toString());
			}
			return NIL;
		}
	}
	
	public static class BaseFunc2 extends TwoArgFunction {
		public LuaValue call(LuaValue arg1,LuaValue arg2) {
			switch ( opcode ) {
			case 0: // "collectgarbage", // ( opt [,arg] ) -> value
				String s = arg1.optString("collect");
				int result = 0;
				if ( "collect".equals(s) ) {
					System.gc();
					return ZERO;
				}
				else if ( "count".equals(s) ) {
					Runtime rt = Runtime.getRuntime();
					long used = rt.totalMemory() - rt.freeMemory();
					return valueOf(used/1024.);
				} else if ( "step".equals(s) ) {
					System.gc();
					return LuaValue.TRUE;
				}
				return NIL;
			case 1: // "error", // ( message [,level] ) -> ERR
				throw new LuaError( arg1.isnil()? null: arg1.toString() );
			case 2: // "rawequal", // (v1, v2) -> boolean
				return valueOf(arg1 == arg2);
			case 3: { // "setfenv", // (f, table) -> void
				LuaValue f = arg1;
				if ( ! f.isfunction() ) {
					int i = arg1.checkint();
					f = (i==0)? (LuaValue) LuaThread.getRunning(): (LuaValue) LuaThread.getCallstackFunction(i-1);
					if ( f == null )
						LuaValue.argerror(1, "invalid level");
				}
				f.setfenv(arg2);
				return f;
			}
			case 4: // "tonumber", // (e [,base]) -> value
				final int base = arg2.optint(10);
				if (base == 10) {  /* standard conversion */
					return arg1.tonumber();
				} else {
					if ( base < 2 || base > 36 )
						argerror(2, "base out of range");
					final LuaString str = arg1.optstring(null);
					return str!=null? str.tonumber(base): NIL;
				}
			}
			return NIL;
		}
	}

	public static class BaseFuncV extends VarArgFunction {
		private final BaseLib lib;
		public BaseFuncV() {
			this.lib = null;
		}
		public BaseFuncV(BaseLib lib) {
			this.lib = lib;
		}
		public Varargs invoke(Varargs args) {
			switch ( opcode ) {
			case 0: // "assert", // ( v [,message] ) -> v, message | ERR
				if ( !args.arg1().toboolean() ) error("assertion failed!");
				return args;
			case 1: // "dofile", // ( filename ) -> result1, ...
			{
				LuaValue chunk;
				try {
					String filename = args.checkString(1);
					chunk = loadFile(filename).arg1();
				} catch ( IOException e ) {
					return error(e.getMessage());
				}
				return chunk.invoke();
			}
			case 2: // "load", // ( func [,chunkname] ) -> chunk | nil, msg
				try {
					LuaValue func = args.checkfunction(1);
					String chunkname = args.optString(2, "function");
					return LoadState.load(new StringInputStream(func), chunkname, LuaThread.getRunningEnv(env));
				} catch ( Exception e ) {
					return varargsOf(NIL, valueOf(e.getMessage()));
				} 
			case 3: // "loadfile", // ( [filename] ) -> chunk | nil, msg
			{
				try {
					String filename = args.checkString(1);
					return loadFile(filename);
				} catch ( Exception e ) {
					return varargsOf(NIL, valueOf(e.getMessage()));
				}
			}
			case 4: // "loadstring", // ( string [,chunkname] ) -> chunk | nil, msg
				try {
					LuaString script = args.checkstring(1);
					LuaString chunkname = args.optstring(2, script);
					return LoadState.load(script.toInputStream(), chunkname.toString(),LuaThread.getRunningEnv(env));
				} catch ( Exception e ) {
					return varargsOf(NIL, valueOf(e.getMessage()));
				} 
			case 5: // "pcall", // (f, arg1, ...) -> status, result1, ...
				try {
					LuaThread.onCall(this);
					try {
						return varargsOf(LuaValue.TRUE, args.arg1().invoke(args.subargs(2)));
					} finally {
						LuaThread.onReturn();
					}
				} catch ( LuaError le ) {
					String m = le.getMessage();
					return varargsOf(FALSE, m!=null? valueOf(m): NIL);
				} catch ( Exception e ) {
					String m = e.getMessage();
					return varargsOf(FALSE, valueOf(m!=null? m: e.toString()));
				}
			case 6: // "xpcall", // (f, err) -> result1, ...				
				try {
					LuaThread.onCall(this);
					try {
						LuaThread thread = LuaThread.getRunning();
						LuaValue olderr = thread.err;
						try {
							thread.err = args.arg(2);
							return varargsOf(LuaValue.TRUE, args.arg1().invoke(args.subargs(2)));
						} finally {
							thread.err = olderr;
						}
					} finally {
						LuaThread.onReturn();
					}
				} catch ( Exception e ) {
					try {
						return args.arg(2).invoke(valueOf(e.getMessage()));
					} catch ( Exception f ) {
						return varargsOf(FALSE, valueOf(f.getMessage()));
					}
				}
			case 7: // "print", // (...) -> void
			{
				LuaValue tostring = env.get("tostring"); 
				for ( int i=1, n=args.narg(); i<=n; i++ ) {
					if ( i>1 ) STDOUT.write( '\t' );
					LuaString s = tostring.call( args.arg(i) ).strvalue();
					int z = s.indexOf((byte)0, 0);
					STDOUT.write( s.m_bytes, s.m_offset, z>=0? z: s.m_length );
				}
				STDOUT.println();
				return NONE;
			}
			case 8: // "select", // (f, ...) -> value1, ...
			{
				int n = args.narg()-1; 				
				if ( args.arg1().equals(valueOf("#")) )
					return valueOf(n);
				int i = args.checkint(1);
				if ( i == 0 || i < -n )
					typerror(1,"index out of range");
				return args.subargs(i<0? n+i+2: i+1);
			}
			case 9: // "unpack", // (list [,i [,j]]) -> result1, ...
			{
				int na = args.narg();
				LuaTable t = args.checktable(1);
				int n = t.length();
				int i = na>=2? args.checkint(2): 1;
				int j = na>=3? args.checkint(3): n;
				n = j-i+1;
				if ( n<0 ) return NONE;
				if ( n==1 ) return t.get(i);
				if ( n==2 ) return varargsOf(t.get(i),t.get(j));
				LuaValue[] v = new LuaValue[n];
				for ( int k=0; k<n; k++ )
					v[k] = t.get(i+k);
				return varargsOf(v);
			}
			case 10: // "type",  // (v) -> value
				return valueOf(args.checkvalue(1).typename());
			case 11: // "rawget", // (table, index) -> value
				return args.checktable(1).rawget(args.checkvalue(2));
			case 12: { // "rawset", // (table, index, value) -> table
				LuaTable t = args.checktable(1);
				t.rawset(args.checknotnil(2), args.checkvalue(3));
				return t;
			}
			case 13: { // "setmetatable", // (table, metatable) -> table
				final LuaValue t = args.arg1();
				final LuaValue mt = args.checkvalue(2);
				t.setmetatable(mt.isnil()? null: mt.checktable());
				return t;
			}
			}
			return NONE;
		}

		private Varargs loadFile(String filename) throws IOException {
			InputStream is = FINDER.findResource(filename);
			if ( is == null )
				return varargsOf(NIL, valueOf("not found: "+filename));
			try {
				return LoadState.load(is, filename, LuaThread.getRunningEnv(env));
			} finally {
				is.close();
			}
		}
	}

	public static class BaseIter extends VarArgFunction {
		final LuaValue aux;
		BaseIter(int opcode, String name, LuaValue aux) {
			this.name = name;
			this.opcode = opcode;
			this.aux = aux;
		}
		public Varargs invoke(Varargs args) {
			switch ( opcode ) {
			case 0: // "next"  ( table, [index] ) -> next-index, next-value
				return args.arg1().next(args.arg(2));
			case 1: // "inext" ( table, [int-index] ) -> next-index, next-value
				return args.arg1().inext(args.arg(2));
			case 2: // "pairs" (t) -> iter-func, t, nil
				return varargsOf( aux, args.checktable(1), NIL );
			case 3: // "ipairs", // (t) -> iter-func, t, 0
				return varargsOf( aux, args.checktable(1), ZERO );
			}
			return NONE;
		}
	}
	
	
	private static class StringInputStream extends InputStream {
		LuaValue func;
		byte[] bytes; 
		int offset;
		StringInputStream(LuaValue func) {
			this.func = func;
		}
		public int read() throws IOException {
			if ( func == null ) return -1;
			if ( bytes == null ) {
				LuaValue s = func.call();
				if ( s.isnil() ) {
					func = null;
					bytes = null;
					return -1;
				}
				bytes = s.toString().getBytes();
				offset = 0;
			}
			if ( offset >= bytes.length )
				return -1;
			return bytes[offset++];
			
		}
	}
}
