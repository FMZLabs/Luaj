/**
 * 
 */
package org.luaj.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.luaj.vm.LClosure;
import org.luaj.vm.LFunction;
import org.luaj.vm.LInteger;
import org.luaj.vm.LNil;
import org.luaj.vm.LNumber;
import org.luaj.vm.LString;
import org.luaj.vm.LTable;
import org.luaj.vm.LValue;
import org.luaj.vm.Lua;
import org.luaj.vm.LuaErrorException;
import org.luaj.vm.LuaState;
import org.luaj.vm.Platform;



public class BaseLib extends LFunction {

	public static InputStream STDIN = null;
	public static PrintStream STDOUT = System.out;
	private static PrintStream stdout = System.out;	
	
	private static final String[] NAMES = { 
		"base",
		"print", 
		"pairs", 
		"getmetatable", 
		"setmetatable", 
		"type", 
		"pcall", 
		"ipairs", 
		"error",
		"assert",
		"loadfile",
		"tonumber",
		"rawget",
		"rawset",
		"getfenv",
		"setfenv",
		"select",
		"collectgarbage",
		"dofile",
		"loadstring",
		"load",
		"tostring",
		"unpack",
		"next",
		};

	private static final int INSTALL        = 0;
	private static final int PRINT          = 1;
	private static final int PAIRS          = 2;
	private static final int GETMETATABLE   = 3;
	private static final int SETMETATABLE   = 4;
	private static final int TYPE           = 5;
	private static final int PCALL          = 6;
	private static final int IPAIRS         = 7;
	private static final int ERROR          = 8;
	private static final int ASSERT         = 9;
	private static final int LOADFILE       = 10;
	private static final int TONUMBER       = 11;
	private static final int RAWGET         = 12;
	private static final int RAWSET         = 13;
	private static final int GETFENV        = 14;
	private static final int SETFENV        = 15;
	private static final int SELECT         = 16;
	private static final int COLLECTGARBAGE = 17;
	private static final int DOFILE         = 18;
	private static final int LOADSTRING     = 19;
	private static final int LOAD           = 20;
	private static final int TOSTRING       = 21;
	private static final int UNPACK         = 22;
	private static final int NEXT           = 23;
	
	public static void install(LTable globals) {
		for ( int i=1; i<NAMES.length; i++ )
			globals.put( NAMES[i], new BaseLib(i) );
		globals.put("_VERSION", new LString("Lua 5.1"));
	}

	private int id;
	
	private BaseLib( int id ) {			
		this.id = id;
	}

	public String toString() {
		return NAMES[id]+"()";
	}
	
	private static void setResult( LuaState vm, LValue value ) {
		vm.resettop();
		vm.pushlvalue( value );
	}
	
	private static void setErrorResult( LuaState vm, String message ) {
		vm.resettop();
		vm.pushnil();
		vm.pushstring( message );
	}
	
	public boolean luaStackCall(LuaState vm) {
		switch ( id ) {
		case PRINT: {
			int n = vm.gettop();
			for ( int i=2; i<=n; i++ ) {
				if ( i > 2 )
					stdout.print( "\t" );
				stdout.print( vm.tostring(i) );
			}
			stdout.println();
			vm.resettop();
			break;
		}
		case PAIRS:
		case IPAIRS: {
			LValue v = vm.topointer(2);
			LValue r = v.luaPairs(id==PAIRS);
			vm.resettop();
			vm.pushlvalue( r );
			break;
		}
		case GETMETATABLE: {
			if ( 0 == vm.getmetatable(2) ) {
				vm.resettop();
				vm.pushnil();
			} else {
				vm.insert(1);
				vm.settop(1);
			}
			break;
		}
		case SETMETATABLE: {
			if ( ! vm.istable(2) )
				vm.error("bad argument #1 to '?' (table expected, got "+vm.typename(2)+")");
			vm.setmetatable(2);
			vm.remove(1);
			break;
		}		
		case TYPE: {
			// TODO: generalize, compute location insofar as possible
			if ( vm.gettop() < 2 ) 
				vm.error("bad argument #1 to '?' (value expected)");
			LValue v = vm.topointer(2);
			vm.resettop();
			vm.pushlstring( v.luaGetTypeName() );
			break;
		}
		case PCALL: {
			int n = vm.gettop();
			int s = vm.pcall( n-2, Lua.LUA_MULTRET, 0 );
			if ( s == 0 ) { // success, results are on stack above the pcall
				vm.remove( 1 );
				vm.pushboolean( true );
				vm.insert( 1 );
			} else { // error, error message is on the stack
				vm.pushboolean( false );
				vm.insert( 1 );
			}
			break;
		}
		case ERROR: {
			vm.error(vm.tostring(2), vm.gettop()>=3? vm.tointeger(3): 1);
			break;
		}
		case ASSERT: {
			if ( ! vm.toboolean(2) )
				vm.error( vm.gettop()>2? vm.tostring(3): "assertion failed!", 0 );
			vm.remove(1);
			break;
		}
		
		case LOADFILE:
			loadfile(vm, vm.tostring(2));
			break;
			
		case TONUMBER: {
			LValue input = vm.topointer(2);
			vm.resettop();
			if ( input instanceof LNumber ) {
				vm.pushlvalue(input);
			} else if ( input instanceof LString ) {
				int base = 10;
				if ( vm.gettop()>=3 ) {
					base = vm.tointeger(3);
				}
				vm.pushlvalue( ( (LString) input ).luaToNumber( base ) );
			}
			vm.pushnil();
			break;
		}
		case RAWGET: {
			LValue t = vm.topointer(2);
			LValue k = vm.topointer(3);
			vm.resettop();
			if ( t instanceof LTable ) {
				vm.pushlvalue(( (LTable) t ).get( k ));
			}
		}	break;
		case RAWSET: {
			LValue t = vm.topointer(2);
			LValue k = vm.topointer(3);
			LValue v = vm.topointer(4);
			vm.resettop();
			if ( t instanceof LTable ) {
				( (LTable) t ).put( k, v );
			} else {
				vm.error( "expected table" );
			}
		}	break;
		case GETFENV: {
			if ( vm.gettop() <= 1 ) {
				vm.pushlvalue(vm._G);
			} else {
				if ( ! vm.isfunction(2) ) {
					int i = (vm.isnil(2)? 1: vm.tointeger(2));
					vm.pushlvalue( vm.getStackFrame(i).closure );
				}
				vm.getfenv(-1);
			}
			vm.insert(1);
			vm.settop(1);
			break;
		}
		case SETFENV: {
			LTable t = vm.totable(-1);
			if ( vm.setfenv(2) != 0 ) {
				vm.remove(1);
				break;
			}
			int i = vm.tointeger(2);
			if ( i == 0 ) {
				vm._G = t;
				vm.resettop();
			} else {
				LClosure c = vm.getStackFrame(i-1).closure;
				c.luaSetEnv(t);
				vm.resettop();
				vm.pushlvalue(c);
			}
			break;
		}
		case SELECT:
			select( vm );
			break;
		case COLLECTGARBAGE:
			System.gc();
			vm.resettop();
			break;
		case DOFILE:
			dofile(vm);
			break;
		case LOADSTRING:
			loadstring(vm, vm.topointer(2), vm.tostring(3));
			break;
		case LOAD:
			load(vm, vm.topointer(2), vm.tostring(3));
			break;
		case TOSTRING: {
			if ( vm.gettop() < 2 )  
				vm.error( "bad argument #1 to '?' (value expected)" );
			LValue v = vm.topointer(2);
			vm.resettop();
			vm.pushlvalue( v.luaAsString() );			
			break;
		}
		case UNPACK: {
			int n = vm.gettop();
			if ( n < 2 )  
				vm.error( "bad argument #1 to '?' (table expected, got no value)" );
			if ( ! vm.istable(2) )
				vm.error( "bad argument #1 to '?' (table expected, got "+vm.typename(2)+")" );
			LTable list = vm.totable(2);
			int i = vm.tointeger(3);
			int j = vm.tointeger(4);
			if ( n <= 2 )
				i = 1;
			if ( n <= 3 )
				j = list.luaLength();
			vm.resettop();
			vm.checkstack(j+1-i);
			for ( int k=i; k<=j; k++ )
				vm.pushlvalue(list.get(k));
			break;
		}
		case NEXT: {
			setResult( vm, next(vm, vm.topointer(2), vm.tointeger(3)) );
			break;
		}
		default:
			luaUnsupportedOperation();
		}
		return false;
	}
	
	public static void redirectOutput( OutputStream newStdOut ) {
		stdout = new PrintStream( newStdOut );
	}
	
	public static void restoreStandardOutput() {
		stdout = System.out;
	}

	private void select( LuaState vm ) {
		LValue arg = vm.topointer(2);
		if ( arg instanceof LNumber ) {
			final int start = Math.min(arg.toJavaInt(),vm.gettop());
			for ( int i=0; i<=start; i++ )
				vm.remove(1);
			return;
		} else if ( arg.toJavaString().equals( "#" ) ) {
			setResult( vm, LInteger.valueOf( vm.gettop() - 2 ) );
		}
	}
	

	// closes the input stream, provided its not null or System.in
	private static void closeSafely(InputStream is) {
		try {
			if ( is != null && is != STDIN )
				is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// closes the output stream, provided its not null or STDOUT 
	private static void closeSafely(OutputStream os) {
		try {
			if ( os != null && os != STDOUT )
				os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// return true if laoded, false if error put onto the stack
	private static boolean loadis(LuaState vm, InputStream is, String chunkname ) {
		try {
			vm.resettop();
			if ( 0 != vm.load(is, chunkname) ) {
				setErrorResult( vm, "cannot load "+chunkname+": "+vm.topointer(-1) );
				return false;
			} else {
				return true;
			}
		} finally {
			closeSafely( is );
		}
	}
	

	// return true if loaded, false if error put onto stack
	public static boolean loadfile( LuaState vm, String fileName ) {
		InputStream is;
		
		String script;
		if ( ! "".equals(fileName) ) {
			script = fileName;
			is = Platform.getInstance().openFile(fileName);
			if ( is == null ) {
				setErrorResult( vm, "cannot open "+fileName+": No such file or directory" );
				return false;
			}
		} else {
			is = STDIN;
			script = "-";
		}
		
		// use vm to load the script
		return loadis( vm, is, script );
	}
	
	// if load succeeds, return 0 for success, 1 for error (as per lua spec)
	private void dofile( LuaState vm ) {
		String filename = vm.tostring(2);
		if ( loadfile( vm, filename ) ) {
			int s = vm.pcall(1, 0, 0);
			setResult( vm, LInteger.valueOf( s!=0? 1: 0 ) );
		} else {
			vm.error("cannot open "+filename);
		}
	}

	// return true if loaded, false if error put onto stack
	private boolean loadstring(LuaState vm,  LValue string, String chunkname) {
		return loadis( vm, 
				string.luaAsString().toInputStream(), 
				("".equals(chunkname)? "(string)": chunkname) );
	}

	// return true if loaded, false if error put onto stack
	private boolean load(LuaState vm, LValue chunkPartLoader, String chunkname) {
		if ( ! (chunkPartLoader instanceof LClosure) ) {
			vm.error("not a closure: "+chunkPartLoader);
		}
		
		// load all the parts
		LClosure c = (LClosure) chunkPartLoader;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			while ( true ) {
				setResult(vm,c);
				if ( 0 != vm.pcall(0, 1, 0) ) {
					setErrorResult(vm, vm.tostring(2));
					return false;
				}
				LValue v = vm.topointer(2);
				if ( v == LNil.NIL )
					break;
				LString s = v.luaAsString();
				s.write(baos, 0, s.length());
			}

			// load the chunk
			return loadis( vm, 
					new ByteArrayInputStream( baos.toByteArray() ), 
					("".equals(chunkname)? "=(load)": chunkname) );
			
		} catch (IOException ioe) {
			setErrorResult(vm, ioe.getMessage());
			return false;
		} finally {
			closeSafely( baos );
		}
	}

	private LValue next(LuaState vm, LValue table, int index) {
		throw new LuaErrorException("next() not supported yet");
	}

	

}
