package lua.value;

import lua.StackState;

public class LString extends LValue {

	final String m_string;
	
	public LString(String string) {
		this.m_string = string;
	}

	public boolean equals(Object o) {
		return o != null && o instanceof LString && m_string.equals(((LString)o).m_string);
	}

	public int hashCode() {
		return m_string.hashCode();
	}

	// TODO: what to do with LuaState? 
	public LString(StackState l, String string) {
		this(string);
	}

	public String luaAsString() {
		return m_string;
	}

	/** Built-in opcode LEN, for Strings and Tables */
	public LValue luaLength() {
		return new LInteger( m_string.length() );
	}

}
