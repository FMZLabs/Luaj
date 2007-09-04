
import java.io.IOException;
import java.io.InputStream;

import lua.StackState;
import lua.addon.luacompat.LuaCompat;
import lua.addon.luajava.LuaJava;
import lua.io.Closure;
import lua.io.LoadState;
import lua.io.Proto;
import lua.value.LString;
import lua.value.LValue;

/**
 * Program to run a compiled lua chunk for test purposes, 
 * but with the LuaJava add-ons added in
 * 
 * @author jim_roseborough
 */
public class LuaJavaAppRunner {

	public static void main( String[] args ) throws IOException {

		// add LuaCompat bindings
		LuaCompat.install();
		
		// add LuaJava bindings
		LuaJava.install();
		
		// get script name
		String script = (args.length>0? args[0]: "/swingapp.luac");
		System.out.println("loading '"+script+"'");
		
		// new lua state 
		StackState state = new StackState();

		// load the file
		InputStream is = LuaJavaAppRunner.class.getResourceAsStream( script );
		Proto p = LoadState.undump(state, is, script);
		
		// create closure and execute
		Closure c = new Closure( state, p );
		state.doCall(c, new LValue[0]);
		
	}
}
