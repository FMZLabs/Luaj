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
package org.luaj.vm2.lib.jse;


/** LuaJava-like bindings to Java scripting. 
 * 
 * TODO: coerce types on way in and out, pick method base on arg count ant types.
 */
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

public class LuajavaLib extends VarArgFunction {
	
	private static final String LIBNAME = "luajava";
	
	private static final int INIT        = 0;
	private static final int BINDCLASS      = 1;
	private static final int NEWINSTANCE	= 2;
	private static final int NEW			= 3;
	private static final int CREATEPROXY	= 4;
	private static final int LOADLIB		= 5;

	private static final String[] NAMES = {
		LIBNAME,
		"bindClass", 
		"newInstance", 
		"new", 
		"createProxy", 
		"loadLib" };
	
	private static final Map classMetatables = new HashMap(); 

	public static void install(LuaValue globals) {
		globals.set("luajava", new LuajavaLib());
	}
	
	public LuajavaLib() {
		name = LIBNAME;
		opcode = INIT;
	}

	public Varargs invoke(final Varargs args) {
		try {
			switch ( opcode ) {
			case INIT: {
				LuaTable t = new LuaTable(0,8);
				LibFunction.bind( t, this.getClass(), NAMES );
				return t;
			}
			case BINDCLASS: {
				final Class clazz = Class.forName(args.checkString(1));
				return toUserdata( clazz, clazz );
			}
			case NEWINSTANCE:
			case NEW: {
				// get constructor
				final LuaValue c = args.checkvalue(1); 
				final Class clazz = (opcode==NEWINSTANCE? Class.forName(c.toString()): (Class) c.checkuserdata(Class.class));
				final ParamsList params = new ParamsList( args );
				final Constructor con = resolveConstructor( clazz, params );
	
				// coerce args, construct instance 
				Object[] cargs = CoerceLuaToJava.coerceArgs( params.values, con.getParameterTypes() );
				Object o = con.newInstance( cargs );
					
				// return result
				return toUserdata( o, clazz );
			}
				
			case CREATEPROXY: {				
				final int niface = args.narg()-1;
				if ( niface <= 0 )
					throw new LuaError("no interfaces");
				final LuaValue lobj = args.checktable(niface+1);
				
				// get the interfaces
				final Class[] ifaces = new Class[niface];
				for ( int i=0; i<niface; i++ ) 
					ifaces[i] = Class.forName(args.checkString(i+1));
				
				// create the invocation handler
				InvocationHandler handler = new InvocationHandler() {
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						String name = method.getName();
						LuaValue func = lobj.get(name);
						if ( func.isnil() )
							return null;
						int n = args!=null? args.length: 0; 
						LuaValue[] v = new LuaValue[n];
						for ( int i=0; i<n; i++ )
							v[i] = CoerceJavaToLua.coerce(args[i]);
						LuaValue result = func.invoke(v).arg1();
						return CoerceLuaToJava.coerceArg(result, method.getReturnType());
					}
				};
				
				// create the proxy object
				Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), ifaces, handler);
				
				// return the proxy
				return LuaValue.userdataOf( proxy );
			}
			case LOADLIB: {
				// get constructor
				String classname = args.checkString(1);
				String methodname = args.checkString(2);
				Class clazz = Class.forName(classname);
				Method method = clazz.getMethod(methodname, new Class[] {});
				Object result = method.invoke(clazz, new Object[] {});
				if ( result instanceof LuaValue ) {
					return (LuaValue) result;
				} else {
					return NIL;
				}
			}
			default:
				throw new LuaError("not yet supported: "+this);
			}
		} catch (LuaError e) {
			throw e;
		} catch (Exception e) {
			throw new LuaError(e);
		}
	}

	public static class ParamsList {
		public final LuaValue[] values;
		public final Class[] classes;
		public int hash;
		ParamsList( Varargs args ) {
			int n = Math.max(args.narg()-1,0);
			values = new LuaValue[n];
			classes = new Class[n];
			for ( int i=0; i<n; i++ ) {
				values[i] = args.arg(i+2);
				classes[i] = values[i].getClass();
				hash += classes[i].hashCode();
			}
		}
		public int hashCode() {
			return hash;
		}
		public boolean equals( Object o ) {
			return ( o instanceof ParamsList )? 
				Arrays.equals( classes, ((ParamsList) o).classes ):
				false;
		}
	}
		
	static LuaUserdata toUserdata(Object instance, final Class clazz) {
		LuaTable mt = (LuaTable) classMetatables.get(clazz);
		if ( mt == null ) {
			mt = new LuaTable();
			mt.set( LuaValue.INDEX, new TwoArgFunction() {
				public LuaValue call(LuaValue table, LuaValue key) {
					final String s = key.toString();
					try {
						Field f = clazz.getField(s);
						Object o = f.get(table.checkuserdata(Object.class));
						return CoerceJavaToLua.coerce( o );
					} catch (NoSuchFieldException nsfe) {
						return new LMethod(clazz,s);
					} catch (Exception e) {
						throw new LuaError(e);
					}
				}
			});
			mt.set( LuaValue.NEWINDEX, new ThreeArgFunction() {
				public LuaValue call(LuaValue table, LuaValue key, LuaValue val) {
					String s = key.toString();
					try {
						Field f = clazz.getField(s);
						Object v = CoerceLuaToJava.coerceArg(val, f.getType());
						f.set(table.checkuserdata(Object.class),v);
					} catch (Exception e) {
						throw new LuaError(e);
					}
					return NONE;
				}
			});
			classMetatables.put(clazz, mt);
		}
		return LuaValue.userdataOf(instance,mt);
	}
	
	private static final class LMethod extends VarArgFunction {
		private final Class clazz;
		private final String s;
		private LMethod(Class clazz, String s) {
			this.clazz = clazz;
			this.s = s;
		}
		public String toString() {
			return clazz.getName()+"."+s+"()";
		}
		public Varargs invoke(Varargs args) {
			try {
				// find the method 
				Object instance = args.checkuserdata(1,Object.class);
				ParamsList params = new ParamsList( args );
				Method meth = resolveMethod( clazz, s, params );

				// coerce the arguments
				Object[] margs = CoerceLuaToJava.coerceArgs( params.values, meth.getParameterTypes() );
				Object result = meth.invoke( instance, margs );
				
				// coerce the result
				return CoerceJavaToLua.coerce(result);
			} catch (Exception e) {
				throw new LuaError(e);
			}
		}
	}

	private static Map consCache =
		new HashMap();
	
	private static Map consIndex =
		new HashMap();
	
	private static Constructor resolveConstructor(Class clazz, ParamsList params ) {

		// get the cache
		Map cache = (Map) consCache.get( clazz );
		if ( cache == null )
			consCache.put( clazz, cache = new HashMap() );
		
		// look up in the cache
		Constructor c = (Constructor) cache.get( params );
		if ( c != null )
			return c;

		// get index
		Map index = (Map) consIndex.get( clazz );
		if ( index == null ) {
			consIndex.put( clazz, index = new HashMap() );
			Constructor[] cons = clazz.getConstructors();
			for ( int i=0; i<cons.length; i++ ) {
				Constructor con = cons[i];
				Integer n = new Integer( con.getParameterTypes().length );
				List list = (List) index.get(n);
				if ( list == null )
					index.put( n, list = new ArrayList() );
				list.add( con );
			}
		}
		
		// figure out best list of arguments == supplied args
		Integer n = new Integer( params.classes.length );
		List list = (List) index.get(n);
		if ( list == null )
			throw new IllegalArgumentException("no constructor with "+n+" args");

		// find constructor with best score
		int bests = Integer.MAX_VALUE;
		int besti = 0;
		for ( int i=0, size=list.size(); i<size; i++ ) {
			Constructor con = (Constructor) list.get(i);
			int s = CoerceLuaToJava.scoreParamTypes(params.values, con.getParameterTypes());
			if ( s < bests ) {
				 bests = s;
				 besti = i;
			}
		}
		
		// put into cache
		c = (Constructor) list.get(besti);
		cache.put( params, c );
		return c;
	}

	
	private static Map methCache = 
		new HashMap();
	
	private static Map methIndex = 
		new HashMap();

	private static Method resolveMethod(Class clazz, String methodName, ParamsList params ) {

		// get the cache
		Map nameCache = (Map) methCache.get( clazz );
		if ( nameCache == null )
			methCache.put( clazz, nameCache = new HashMap() );
		Map cache = (Map) nameCache.get( methodName );
		if ( cache == null )
			nameCache.put( methodName, cache = new HashMap() );
		
		// look up in the cache
		Method m = (Method) cache.get( params );
		if ( m != null )
			return m;

		// get index
		Map index = (Map) methIndex.get( clazz );
		if ( index == null ) {
			methIndex.put( clazz, index = new HashMap() );
			Method[] meths = clazz.getMethods();
			for ( int i=0; i<meths.length; i++ ) {
				Method meth = meths[i];
				String s = meth.getName();
				Integer n = new Integer(meth.getParameterTypes().length);
				Map map = (Map) index.get(s);
				if ( map == null )
					index.put( s, map = new HashMap() );
				List list = (List) map.get(n);
				if ( list == null )
					map.put( n, list = new ArrayList() );
				list.add( meth );
			}
		}
		
		// figure out best list of arguments == supplied args
		Map map = (Map) index.get(methodName);
		if ( map == null )
			throw new IllegalArgumentException("no method named '"+methodName+"'");
		Integer n = new Integer( params.classes.length );
		List list = (List) map.get(n);
		if ( list == null )
			throw new IllegalArgumentException("no method named '"+methodName+"' with "+n+" args");

		// find constructor with best score
		int bests = Integer.MAX_VALUE;
		int besti = 0;
		for ( int i=0, size=list.size(); i<size; i++ ) {
			Method meth = (Method) list.get(i);
			int s = CoerceLuaToJava.scoreParamTypes(params.values, meth.getParameterTypes());
			if ( s < bests ) {
				 bests = s;
				 besti = i;
			}
		}
		
		// put into cache
		m = (Method) list.get(besti);
		cache.put( params, m );
		return m;
	}
	
}