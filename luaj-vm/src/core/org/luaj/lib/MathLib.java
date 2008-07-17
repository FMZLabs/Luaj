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

import java.util.Random;

import org.luaj.vm.LDouble;
import org.luaj.vm.LFunction;
import org.luaj.vm.LInteger;
import org.luaj.vm.LTable;
import org.luaj.vm.LValue;
import org.luaj.vm.Lua;
import org.luaj.vm.LuaState;


public class MathLib extends LFunction {

	public static final String[] NAMES = {
		"math",
		"abs",
		"cos",
		"max",
		"min",
		"modf",
		"sin",
		"sqrt",
		"ceil",
		"floor",
		"random",
		"randomseed",
	};

	private static final int INSTALL      = 0;
	private static final int ABS          = 1;
	private static final int COS          = 2;
	private static final int MAX          = 3;
	private static final int MIN          = 4;
	private static final int MODF         = 5;
	private static final int SIN          = 6;
	private static final int SQRT         = 7;
	private static final int CEIL         = 8;
	private static final int FLOOR        = 9;
	private static final int RANDOM       = 10;
	private static final int RANDOMSEED   = 11;
	
	public static void install( LTable globals ) {
		LTable math = new LTable();
		for ( int i=1; i<NAMES.length; i++ )
			math.put(NAMES[i], new MathLib(i));
		math.put( "huge", new LDouble( Double.MAX_VALUE ) );
		math.put( "pi", new LDouble( Math.PI ) );		
		globals.put( "math", math );
	}

	private static Random random = null;
	
	private final int id;

	private MathLib( int id ) {
		this.id = id;
	}
	
	public String toString() {
		return NAMES[id]+"()";
	}
	
	private static void setResult( LuaState vm, LValue value ) {
		vm.resettop();
		vm.pushlvalue( value );
	}
	
	private static void setResult( LuaState vm, double d ) {
		vm.resettop();
		vm.pushlvalue( LDouble.valueOf(d) );
	}
	
	private static void setResult( LuaState vm, int i ) {
		vm.resettop();
		vm.pushlvalue( LInteger.valueOf(i) );
	}
	
	public boolean luaStackCall( LuaState vm ) {
		double x;
		switch ( id ) {
		case INSTALL:
			install( vm._G );
			break;
		case ABS:
			setResult( vm, Math.abs (  vm.checkdouble(2) ) );
			break;
		case COS:
			setResult( vm, Math.cos ( vm.checkdouble(2) ) );
			break;
		case MAX: {
			int n = vm.gettop();
			x = vm.checkdouble(2);
			for ( int i=3; i<=n; i++ )
				x = Math.max(x, vm.checkdouble(i));
			setResult( vm, x );
			break;
		}
		case MIN: {
			int n = vm.gettop();
			x = vm.checkdouble(2);
			for ( int i=3; i<=n; i++ )
				x = Math.min(x, vm.checkdouble(i));
			setResult(vm,x);
			break;
		}
		case MODF: {
			double v = vm.checkdouble(2);
			double intPart = ( v > 0 ) ? Math.floor( v ) : Math.ceil( v );
			double fracPart = v - intPart;
			vm.resettop();
			vm.pushnumber( intPart );
			vm.pushnumber( fracPart );
			break;
		}
		case SIN:
			setResult( vm, Math.sin( vm.checkdouble(2) ) );
			break;
		case SQRT:
			setResult( vm, Math.sqrt( vm.checkdouble(2) ) );
			break;
		case CEIL:
			setResult( vm, (int) Math.ceil( vm.checkdouble(2) ) );
			break;
		case FLOOR:
			setResult( vm, (int) Math.floor( vm.checkdouble(2) ) );
			break;
		case RANDOM: {
			if ( random == null )
				random = new Random();
			switch ( vm.gettop() ) {
			case 1:
				vm.resettop();
				vm.pushnumber(random.nextDouble());
				break;
			case 2: {
				int m = vm.checkint(2);
				vm.argcheck(1<=m, 1, "interval is empty");
				vm.resettop();
				vm.pushinteger(1+random.nextInt(m));
				break;
			}
			default: {
				int m = vm.checkint(2);
				int n = vm.checkint(3);
				vm.argcheck(m<=n, 2, "interval is empty");
				vm.resettop();
				vm.pushinteger(m+random.nextInt(n+1-m));
				break;
			}
			}
			break;
		}
		case RANDOMSEED:
			random = new Random( vm.checkint(2) );
			vm.resettop();
			break;
		default:
			LuaState.vmerror( "bad math id" );
		}
		return false;
	}
	
}
