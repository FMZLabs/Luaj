package lua.addon.compile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import lua.Lua;
import lua.io.Proto;
import lua.value.LBoolean;
import lua.value.LNil;
import lua.value.LNumber;
import lua.value.LString;
import lua.value.LValue;

public class DumpState {

	/** mark for precompiled code (`<esc>Lua') */
	public static final String LUA_SIGNATURE	= "\033Lua";

	/** for header of binary files -- this is Lua 5.1 */
	public static final int LUAC_VERSION		= 0x51;

	/** for header of binary files -- this is the official format */
	public static final int LUAC_FORMAT		= 0;

	/** size of header of binary files */
	public static final int LUAC_HEADERSIZE		= 12;

	/** expected lua header bytes */
	private static final byte[] LUAC_HEADER_SIGNATURE = { '\033', 'L', 'u', 'a' };

	// header fields
	private boolean IS_LITTLE_ENDIAN = false;
	private boolean IS_NUMBER_INTEGRAL = false;
	private int SIZEOF_LUA_NUMBER = 8;
	private static final int SIZEOF_INT = 4;
	private static final int SIZEOF_SIZET = 4;
	private static final int SIZEOF_INSTRUCTION = 4;

	DataOutputStream writer;
	boolean strip;
	int status;

	public DumpState(OutputStream w, boolean strip) {
		this.writer = new DataOutputStream( w );
		this.strip = strip;
		this.status = 0;
	}

	void dumpBlock(final byte[] b, int size) throws IOException {
		writer.write(b, 0, size);
	}

	void dumpChar(int b) throws IOException {
		writer.write( b );
	}

	void dumpInt(int x) throws IOException {
		if ( IS_LITTLE_ENDIAN ) {
			writer.writeByte(x&0xff);
			writer.writeByte((x>>8)&0xff);
			writer.writeByte((x>>16)&0xff);
			writer.writeByte((x>>24)&0xff);
		} else {
			writer.writeInt(x);
		}
	}
	
	void dumpString(LString s) throws IOException {
		final int len = s.length();
		dumpInt( len+1 );
		s.write( writer, 0, len );
		writer.write( 0 );
	}
	
	void dumpNumber(double d) throws IOException {
		if ( IS_NUMBER_INTEGRAL ) {
			int i = (int) d;
			if ( i != d )
				throw new java.lang.IllegalArgumentException("not an integer: "+d);
			dumpInt( i );
		} else {
			long l = Double.doubleToLongBits(d);
			writer.writeLong(l);
		}
	}

	void dumpCode( final Proto f ) throws IOException {
		int n = f.code.length;
		dumpInt( n );
		for ( int i=0; i<n; i++ )
			dumpInt( f.code[i] );
	}
	
	void dumpConstants(final Proto f) throws IOException {
		int i, n = f.k.length;
		dumpInt(n);
		for (i = 0; i < n; i++) {
			final LValue o = f.k[i];
			if (o == LNil.NIL) {
				writer.write(Lua.LUA_TNIL);
				// do nothing more
			} else if (o instanceof LBoolean) {
				writer.write(Lua.LUA_TBOOLEAN);
				dumpChar(o.toJavaBoolean() ? 1 : 0);
			} else if (o instanceof LNumber) {
				writer.write(Lua.LUA_TNUMBER);
				dumpNumber(o.toJavaDouble());
			} else if (o instanceof LString) {
				writer.write(Lua.LUA_TSTRING);
				dumpString((LString) o);
			} else {
				throw new IllegalArgumentException("bad type for " + o);
			}
		}
		n = f.p.length;
		dumpInt(n);
		for (i = 0; i < n; i++)
			dumpFunction(f.p[i], f.source);
	}
	
	void dumpDebug(final Proto f) throws IOException {
		int i, n;
		n = (strip) ? 0 : f.lineinfo.length;
		dumpInt(n);
		for (i = 0; i < n; i++)
			dumpInt(f.lineinfo[i]);
		n = (strip) ? 0 : f.locvars.length;
		dumpInt(n);
		for (i = 0; i < n; i++) {
			dumpString(f.locvars[i].varname);
			dumpInt(f.locvars[i].startpc);
			dumpInt(f.locvars[i].endpc);
		}
		n = (strip) ? 0 : f.upvalues.length;
		dumpInt(n);
		for (i = 0; i < n; i++)
			dumpString(f.upvalues[i]);
	}
	
	void dumpFunction(final Proto f, final LString string) throws IOException {
		if ( f.source == null || f.source.equals(string) || strip )
			dumpInt(0);
		else
			dumpString(f.source);
		dumpInt(f.linedefined);
		dumpInt(f.lastlinedefined);
		dumpChar(f.nups);
		dumpChar(f.numparams);
		dumpChar(f.is_vararg? 1: 0);
		dumpChar(f.maxstacksize);
		dumpCode(f);
		dumpConstants(f);
		dumpDebug(f);
	}

	void dumpHeader() throws IOException {
		writer.write( LUAC_HEADER_SIGNATURE );
		writer.write( LUAC_VERSION );
		writer.write( LUAC_FORMAT );
		writer.write( IS_LITTLE_ENDIAN? 1: 0 );
		writer.write( SIZEOF_INT );
		writer.write( SIZEOF_SIZET );
		writer.write( SIZEOF_INSTRUCTION );
		writer.write( SIZEOF_LUA_NUMBER );
		writer.write( IS_NUMBER_INTEGRAL? 1: 0 );
	}

	/*
	** dump Lua function as precompiled chunk
	*/
	public static int dump( Proto f, OutputStream w, boolean strip ) throws IOException {
		DumpState D = new DumpState(w,strip);
		D.dumpHeader();
		D.dumpFunction(f,null);
		return D.status;
	}

	public static int dump(Proto f, OutputStream w, boolean strip, boolean intonly, boolean littleendian) throws IOException {
		DumpState D = new DumpState(w,strip);
		D.IS_LITTLE_ENDIAN = littleendian;
		D.IS_NUMBER_INTEGRAL = intonly;
		D.SIZEOF_LUA_NUMBER = (intonly? 4: 8);
		D.dumpHeader();
		D.dumpFunction(f,null);
		return D.status;
	}
}
