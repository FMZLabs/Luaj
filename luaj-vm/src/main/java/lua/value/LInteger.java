package lua.value;

import lua.Lua;

public class LInteger extends LNumber {

	private final int m_value;
	
	public LInteger(int value) {
		this.m_value = value;
	}
	
	public final int hashCode() {
		return hashCodeOf( m_value );
	}
	
	public static int hashCodeOf( int v ) {
		return v;
	}
	
	public int luaAsInt() {
		return m_value;
	}
	
	public String luaAsString() {
		return String.valueOf(m_value);
	}
	
	public boolean isInteger() {
		return true;
	}
	
	// binary operations on integers, first dispatch
	public LValue luaBinOpUnknown(int opcode, LValue lhs) {
		return lhs.luaBinOpInteger( opcode, this.m_value );
	}
	
	// binary operations on integers
	public LValue luaBinOpInteger(int opcode, int rhs) {
		switch ( opcode ) {
		case Lua.OP_ADD: return new LInteger( m_value + rhs );
		case Lua.OP_SUB: return new LInteger( m_value - rhs );
		case Lua.OP_MUL: return new LInteger( m_value * rhs );
		case Lua.OP_DIV: return new LInteger( m_value / rhs );
		case Lua.OP_MOD: return new LInteger( m_value - ((int) Math.floor(m_value/(double)rhs)) * rhs );
//		case Lua.OP_POW: return new LInteger( (int) Math.pow(m_value, rhs) );
		}
		return luaUnsupportedOperation();
	}
	
	// binary operations on mixed integer, double
	public LValue luaBinOpDouble(int opcode, double rhs) {
		return LDouble.luaBinOpDoubleDouble(opcode, (double) m_value, rhs );
	}
	
	// binary compare for integers, first dispatch
	public boolean luaBinCmpUnknown(int opcode, LValue lhs) {
		return lhs.luaBinCmpInteger( opcode, this.m_value );
	}
	
	// unsupported except for numbers
	public boolean luaBinCmpInteger(int opcode, int rhs) {
		switch ( opcode ) {
		case Lua.OP_EQ: return m_value == rhs;
		case Lua.OP_LT: return m_value < rhs;
		case Lua.OP_LE: return m_value <= rhs;
		}
		luaUnsupportedOperation();
		return false;
	}
	
	// unsupported except for numbers
	public boolean luaBinCmpDouble(int opcode, double rhs) {
		return LDouble.luaBinCmpDoubleDouble(opcode, (double) m_value, rhs );
	}

	/** Arithmetic negative */
	public LValue luaUnaryMinus() {
		return new LInteger( -m_value );
	}


}
