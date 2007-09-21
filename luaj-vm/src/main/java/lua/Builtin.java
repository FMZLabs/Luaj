/**
 * 
 */
package lua;

import java.io.OutputStream;
import java.io.PrintStream;

import lua.value.LBoolean;
import lua.value.LFunction;
import lua.value.LNil;
import lua.value.LTable;
import lua.value.LValue;

final class Builtin extends LFunction {

	static void addBuiltins(LTable table) {
		for ( int i=0; i<NAMES.length; i++ )
			table.put( NAMES[i], new Builtin(i) );
	}

	private static final String[] NAMES = { 
		"print", 
		"pairs", 
		"getmetatable", 
		"setmetatable", 
		"type", 
		"pcall", 
		"ipairs", 
		};
	private static final int PRINT = 0;
	private static final int PAIRS = 1;
	private static final int GETMETATABLE = 2;
	private static final int SETMETATABLE = 3;
	private static final int TYPE = 4;
	private static final int PCALL = 5;
	private static final int IPAIRS = 6;
	
	private static PrintStream stdout = System.out;
	
	private int id;
	private Builtin( int id ) {			
		this.id = id;
	}

	public String toString() {
		return "builtin."+NAMES[id];
	}
	
	// perform a lua call
	public boolean luaStackCall(VM vm) {
		switch ( id ) {
		case PRINT: {
				int n = vm.getArgCount();
				for ( int i=0; i<n; i++ ) {
					if ( i > 0 )
						stdout.print( "\t" );
					stdout.print( vm.getArg(i).luaAsString() );
				}
				stdout.println();
				vm.setResult();
			}
			break;
		case PAIRS:
		case IPAIRS:
			vm.setResult( vm.getArg(0).luaPairs(id==PAIRS) );
			break;
		case GETMETATABLE:
			vm.setResult( vm.getArg(0).luaGetMetatable() );
			break;
		case SETMETATABLE:
			LValue t = vm.getArg(0);
			t.luaSetMetatable(vm.getArg(1));
			vm.setResult( t );
			break;
		case TYPE:
			vm.setResult( vm.getArg(0).luaGetType() );
			break;
		case PCALL: {
				int n = vm.getArgCount();
				int s = vm.lua_pcall( n-1, Lua.LUA_MULTRET );
				if ( s != 0 ) {
					LValue v = vm.lua_tolvalue(-1);
					vm.setResult( LBoolean.FALSE );
					vm.push( v );
				} else {
					vm.setResult( LBoolean.TRUE );
				}
			}
			break;
		default:
			luaUnsupportedOperation();
		}
		return false;
	}
	
	static void redirectOutput( OutputStream newStdOut ) {
		stdout = new PrintStream( newStdOut );
	}
	
	static void restoreStandardOutput() {
		stdout = System.out;
	}

}
