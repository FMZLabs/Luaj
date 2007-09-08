package lua.value;

import lua.Lua;

public class LString extends LValue {

	public static final LString TYPE_NAME = new LString("string");
	
	final String m_string;
	final int m_hash;
	
	private static LTable s_stringMT;
	
	public LString(String string) {
		this.m_string = string;
		this.m_hash = string.hashCode();
	}

	public boolean equals(Object o) {
		if ( o != null && o instanceof LString ) {
			LString s = (LString) o;
			return m_hash == s.m_hash && m_string.equals(s.m_string);
		}
		return false;
	}

	public int hashCode() {
		return m_hash;
	}

	public boolean luaBinCmpUnknown(int opcode, LValue lhs) {
		return lhs.luaBinCmpString(opcode, m_string);
	}

	public boolean luaBinCmpString(int opcode, String rhs) {
		switch ( opcode ) {
		case Lua.OP_EQ: return m_string.equals(rhs);
		case Lua.OP_LT: return m_string.compareTo(rhs) < 0;
		case Lua.OP_LE: return m_string.compareTo(rhs) <= 0;
		}
		luaUnsupportedOperation();
		return false;
	}
	
	public LValue luaBinOpDouble( int opcode, double m_value ) {
		return luaToNumber().luaBinOpDouble( opcode, m_value );
	}
	
	public LValue luaBinOpInteger( int opcode, int m_value ) {
		return luaToNumber().luaBinOpInteger( opcode, m_value );
	}
	
	public LValue luaBinOpUnknown( int opcode, LValue lhs ) {
		return luaToNumber().luaBinOpUnknown( opcode, lhs );
	}
	
	public LValue luaUnaryMinus() {
		return luaToNumber().luaUnaryMinus();
	}
	
	public LValue luaToNumber() {
		return luaToNumber( 10 );
	}
	
	public LValue luaToNumber( int base ) {
		if ( base >= 2 && base <= 36 ) {
			String str = m_string.trim();
			try {
				return new LInteger( Integer.parseInt( str, base ) );
			} catch ( NumberFormatException nfe ) {
				if ( base == 10 ) {
					try {
						return new LDouble( Double.parseDouble( str ) );
					} catch ( NumberFormatException nfe2 ) {
					}
				}
			}
		}
		
		return LNil.NIL;
	}
	
	public String luaAsString() {
		return m_string;
	}

	/** Built-in opcode LEN, for Strings and Tables */
	public LValue luaLength() {
		return new LInteger( m_string.length() );
	}

	public LString luaGetType() {
		return TYPE_NAME;
	}
	
	public LTable luaGetMetatable() {
		synchronized ( LString.class ) {
			return s_stringMT;
		}
	}
	
	/**
	 * Get the metatable for all string values. Creates the table if it does not
	 * exist yet, and sets its __index entry to point to itself.
	 * 
	 * @return metatable that will be used for all strings
	 */
	public static synchronized LTable getMetatable() {
		if ( s_stringMT == null ) {
			s_stringMT = new LTable();
			s_stringMT.put( TM_INDEX, s_stringMT );
		}
		return s_stringMT;
	}
}
