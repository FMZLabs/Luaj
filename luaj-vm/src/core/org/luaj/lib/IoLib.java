/*******************************************************************************
* Copyright (c) 2008 LuaJ. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;

import org.luaj.vm.LFunction;
import org.luaj.vm.LNil;
import org.luaj.vm.LString;
import org.luaj.vm.LTable;
import org.luaj.vm.LUserData;
import org.luaj.vm.LValue;
import org.luaj.vm.LuaState;


abstract 
public class IoLib extends LFunction {

	protected interface File {
		public void write( LString string ) throws IOException;
		public void flush() throws IOException;
		public boolean isstdfile();
		public void close() throws IOException;
		public boolean isclosed();
		// returns new position
		public int seek(String option, int bytecount) throws IOException;
		public void setvbuf(String mode, int size);		
		// get length remaining to read
		public int remaining() throws IOException;		
		// peek ahead one character
		public int peek() throws IOException, EOFException;		
		// return char if read, -1 if eof, throw IOException on other exception 
		public int read() throws IOException, EOFException;
		// return number of bytes read if positive, false if eof, throw IOException on other exception
		public int read(byte[] bytes, int offset, int length) throws IOException;
	}


	/** 
	 * Create a function stub with a specific index (factory method)
	 */
	abstract protected IoLib newInstance( int index );

	/** 
	 * Wrap the standard input. 
	 * @return File 
	 * @throws IOException
	 */
	abstract protected File wrapStdin() throws IOException;

	/** 
	 * Wrap the standard output. 
	 * @return File 
	 * @throws IOException
	 */
	abstract protected File wrapStdout() throws IOException;
	
	/**
	 * Open a file in a particular mode. 
	 * @param filename
	 * @param mode
	 * @return File object if successful
	 * @throws IOException if could not be opened
	 */
	abstract protected File openFile( String filename, boolean readMode, boolean appendMode, boolean updateMode, boolean binaryMode ) throws IOException;

	/**
	 * Open a temporary file. 
	 * @return File object if successful
	 * @throws IOException if could not be opened
	 */
	abstract protected File tmpFile() throws IOException;

	/**
	 * Start a new process and return a file for input or output
	 * @param prog the program to execute
	 * @param mode "r" to read, "w" to write
	 * @return File to read to or write from 
	 * @throws IOException if an i/o exception occurs
	 */
	abstract protected File openProgram(String prog, String mode) throws IOException;

	
	public static final String[] NAMES = {
		"io",
		"__index",
		"close",
		"flush",
		"input",
		"lines",
		"open",
		"output",
		"popen",
		"read",
		"tmpfile",
		"type",
		"write",
		"close",
		"flush",
		"lines",
		"read",
		"seek",
		"setvbuf",
		"write",
	};
	
	private static final int INSTALL      = 0;
	private static final int IO_INDEX     = 1;
	private static final int IO_CLOSE     = 2;
	private static final int IO_FLUSH     = 3;
	private static final int IO_INPUT     = 4;
	private static final int IO_LINES     = 5;
	private static final int IO_OPEN      = 6;
	private static final int IO_OUTPUT    = 7;
	private static final int IO_POPEN     = 8;
	private static final int IO_READ      = 9;
	private static final int IO_TMPFILE   = 10;
	private static final int IO_TYPE      = 11;
	private static final int IO_WRITE     = 12;
	private static final int FILE_CLOSE   = 13;
	private static final int FILE_FLUSH   = 14;
	private static final int FILE_LINES   = 15;
	private static final int FILE_READ    = 16;
	private static final int FILE_SEEK    = 17;
	private static final int FILE_SETVBUF = 18;
	private static final int FILE_WRITE   = 19;

	private static File INPUT  = null;
	private static File OUTPUT = null;
	private static File ERROR  = null;
	
	private static LTable FILE_MT;

	protected void initialize( LTable globals ) {
		LTable io = new LTable();
		for ( int i=IO_INDEX; i<=IO_WRITE; i++ )
			io.put(NAMES[i], newInstance(i));
		io.luaSetMetatable(io);
		FILE_MT = new LTable();
		for ( int i=FILE_CLOSE; i<=FILE_WRITE; i++ )
			FILE_MT.put(NAMES[i], newInstance(i));
		FILE_MT.put("__index", FILE_MT);
		INPUT = null;
		OUTPUT = null;
		ERROR = null;
		globals.put( "io", io );
		PackageLib.setIsLoaded("io", io);
	}

	private final int id;

	protected IoLib() {
		id = 0;
	}

	protected IoLib( int id ) {
		this.id = id;
	}

	public LString luaAsString() {
		return new LString(toJavaString());
	}
	
	public String toJavaString() {
		return "io."+toString();
	}

	public String toString() {
		return NAMES[id]+"()";
	}

	private File input(LuaState vm) {
		return INPUT!=null? INPUT: (INPUT=ioopenfile(vm,"-","r"));
	}
	
	private File output(LuaState vm) {
		return OUTPUT!=null? OUTPUT: (OUTPUT=ioopenfile(vm,"-","w"));
	}
	
	private File error(LuaState vm) {
		return ERROR!=null? ERROR: (ERROR=ioopenfile(vm,"-","w"));
	}
	
	public LValue __index(LuaState vm, LValue table, LValue key) {
		String k = key.toJavaString();		
		if ( "stdout".equals(k) )
			return touserdata(output(vm));
		else if ( "stdin".equals(k) )
			return touserdata(input(vm));
		else if ( "stderr".equals(k) )
			return touserdata(error(vm));
		else 
			return LNil.NIL;
	}

	public int invoke( LuaState vm ) {
		File f;
		int n;
		try {
		switch ( id ) {
			/* Load the table library dynamically */
			case INSTALL:
				initialize(vm._G);
				return 0;
			case IO_CLOSE:
				f = vm.isnoneornil(1)? 
						output(vm):
						checkfile(vm,1);
				checkopen(vm, f);
				ioclose(vm,f);
				break;
			case IO_FLUSH:
				checkopen(vm,output(vm));
				OUTPUT.flush();
				vm.pushboolean(true);
				return 1;
			case IO_INPUT:
				INPUT = vm.isnoneornil(1)? 
						input(vm):
						vm.isstring(1)? 
						ioopenfile(vm,vm.checkstring(1),"r"):
						checkfile(vm,1);
				setresult(vm, INPUT);
				break;
			case IO_LINES:
				INPUT = vm.isnoneornil(1)?  
						input(vm):
						ioopenfile(vm,vm.checkstring(1),"r");
				checkopen(vm, INPUT);
				vm.pushlvalue(lines(vm,INPUT));
				return 1;
			case IO_OPEN:
				setresult(vm, rawopenfile(vm.checkstring(1), vm.optstring(2,"r")));
				break;
			case IO_OUTPUT:
				OUTPUT = vm.isnoneornil(1)? 
						output(vm):
						vm.isstring(1)? 
						ioopenfile(vm,vm.checkstring(1),"w"):
						checkfile(vm,1);
				setresult(vm, OUTPUT);
				break;
			case IO_POPEN:
				setresult(vm, openProgram(vm.checkstring(1),vm.optstring(2, "r")));				
				break;
			case IO_READ:
				checkopen(vm, INPUT);
				ioread( vm, INPUT );
				break;
			case IO_TMPFILE:
				setresult(vm, tmpFile());
				break;
			case IO_TYPE:
				f = optfile(vm,1);
				if ( f != null )
					vm.pushstring(f.isclosed()? "closed file": "file");
				else 
					vm.pushnil();
				return 1;
			case IO_WRITE:
				checkopen(vm, output(vm));
				iowrite( vm, OUTPUT );
				break;
			case FILE_CLOSE:
				f = checkfile(vm,1);
				ioclose(vm, f);
				break;
			case FILE_FLUSH:
				f = checkfile(vm,1);
				f.flush();
				vm.pushboolean(true);
				return 1;
			case FILE_LINES:
				f = checkfile(vm,1);
				vm.pushlvalue(lines(vm,f));
				return 1;
			case FILE_READ:
				f = checkfile(vm,1);
				vm.remove(1);
				ioread(vm, f);
				break;
			case FILE_SEEK:
				f = checkfile(vm,1);
				vm.remove(1);
				n = f.seek(vm.optstring(1,"cur"),vm.optint(2, 0));
				vm.pushinteger(n);
				return 1;
			case FILE_SETVBUF:
				f = checkfile(vm,1);
				vm.remove(1);
				f.setvbuf(vm.checkstring(1),vm.optint(2, 1024));
				vm.pushboolean(true);
				return 1;
			case FILE_WRITE:
				f = checkfile(vm,1);
				vm.remove(1);
				iowrite( vm, f );
				break;
			default:
				LuaState.vmerror( "bad io id" );
			}
		} catch ( IOException ioe ) {
			seterrorresult(vm,ioe);
		}
		return -1;
	}
	
	private static void ioclose(LuaState vm, File f) throws IOException {
		if ( f.isstdfile() )
			seterrorresult(vm,"cannot close standard file");
		else {
			f.close();
			setsuccessresult(vm);
		}
	}

	private static void setsuccessresult(LuaState vm) {
		vm.resettop();
		vm.pushboolean(true);
	}

	private static void seterrorresult(LuaState vm, IOException ioe) {
		String s = ioe.getMessage();		
		seterrorresult(vm, "io error: "+(s!=null? s: ioe.toString()));
	}
	
	private static void seterrorresult(LuaState vm, String errortext) {
		vm.resettop();
		vm.pushnil();
		vm.pushstring(errortext);
	}

	private LValue lines(LuaState vm, final File f) {
		return new LFunction() {
			public boolean luaStackCall(LuaState vm) {
				vm.resettop();
				try {
					vm.pushlvalue(freadline(f));
				} catch (IOException e) {
					seterrorresult(vm,e);
				}
				return false;
			}
		};
	}

	private static void iowrite(LuaState vm, File f) throws IOException {
		for ( int i=1, n=vm.gettop(); i<=n; i++ )
			f.write( vm.checklstring(i) );
		vm.resettop();
		vm.pushboolean(true);
	}

	private static void ioread(LuaState vm, File f) throws IOException {
		int i,n=vm.gettop();
		for ( i=1; i<=n; i++ ) {
			if ( vm.isnumber(i) ) {
				vm.pushlvalue(freadbytes(f,vm.tointeger(i)));
			} else {
				String format = vm.checkstring(i);
				if ( "*n".equals(format) ) 
					vm.pushnumber(freadnumber(f));
				else if ( "*a".equals(format) ) 
					vm.pushlvalue(freadall(f));
				else if ( "*l".equals(format) )
					vm.pushlvalue(freadline(f));
				else
					vm.typerror( i, "(invalid format)" );
			}
		}
		for ( i=1; i<=n; i++ )
			vm.remove(1);
	}

	private static File checkfile(LuaState vm, int index) {
		File f = (File) vm.checkudata(index, File.class);
		checkopen( vm, f );
		return f;
	}
	
	private File optfile(LuaState vm, int index) {
		Object u = vm.touserdata(index);
		return (u instanceof File? (File) u: null); 
	}
	
	private static File checkopen(LuaState vm, File file) {
		if ( file.isclosed() )
			vm.error("attempt to use a closed file");
		return file;
	}
	
	private static void setresult(LuaState vm, File file) {
		vm.settop(0);
		vm.pushlvalue(touserdata(file));
	}
	
	private static LUserData touserdata(File file) {
		return new LUserData(file, FILE_MT);		
	}

	private File ioopenfile(LuaState vm, String filename, String mode) {
		try {
			return rawopenfile(filename, mode);
		} catch ( Exception e ) {
			vm.error("io error: "+e.getMessage());
			return null;
		}
	}

	private File rawopenfile(String filename, String mode) throws IOException {
		boolean isstdfile = "-".equals(filename);
		boolean isreadmode = mode.startsWith("r");
		if ( isstdfile ) {
			return isreadmode? 
				wrapStdin():
				wrapStdout();
		}
		boolean isappend = mode.startsWith("a");
		boolean isupdate = mode.indexOf("+") > 0;
		boolean isbinary = mode.endsWith("b");
		return openFile( filename, isreadmode, isappend, isupdate, isbinary );
	}


	// ------------- file reading utilitied ------------------
	
	public static LValue freadbytes(File f, int count) throws IOException {
		byte[] b = new byte[count];
		int r;
		if ( ( r = f.read(b,0,b.length) ) < 0 )
			return LNil.NIL;
		return new LString(b, 0, r);
	}
	public static LValue freaduntil(File f,int delim) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int c;
		try {
			while ( true ) { 
				c = f.read();
				if ( c < 0 || c == delim )
					break;
				baos.write(c);
			}
		} catch ( EOFException e ) {
			c = -1;
		}
		return ( c < 0 && baos.size() == 0 )? 
			(LValue) LNil.NIL:
			(LValue) new LString(baos.toByteArray());
	}
	public static LValue freadline(File f) throws IOException {
		return freaduntil(f,'\n');
	}
	public static LValue freadall(File f) throws IOException {
		int n = f.remaining();
		if ( n >= 0 ) {
			return freadbytes(f, n);
		} else {
			return freaduntil(f,-1);
		}
	}
	public static Double freadnumber(File f) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		freadchars(f," \t\r\n",null);
		freadchars(f,"-+",baos);
		//freadchars(f,"0",baos);
		//freadchars(f,"xX",baos);
		freadchars(f,"0123456789",baos);
		freadchars(f,".",baos);
		freadchars(f,"0123456789",baos);
		//freadchars(f,"eEfFgG",baos);
		// freadchars(f,"+-",baos);
		//freadchars(f,"0123456789",baos);
		String s = baos.toString();
		return s.length()>0? Double.valueOf(s): null;
	}
	private static void freadchars(File f, String chars, ByteArrayOutputStream baos) throws IOException {
		int c;
		while ( true ) {
			c = f.peek();
			if ( chars.indexOf(c) < 0 ) {
				return;
			}
			f.read();
			if ( baos != null )
				baos.write( c );
		}
	}		
	
	
	
}
