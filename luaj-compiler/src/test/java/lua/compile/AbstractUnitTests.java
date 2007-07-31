package lua.compile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URL;

import junit.framework.TestCase;

import lua.Print;
import lua.StackState;
import lua.io.LoadState;
import lua.io.Proto;

abstract 
public class AbstractUnitTests extends TestCase {
	
	private String dir;
	
	public AbstractUnitTests(String dir) {
		this.dir = dir;
	}

	protected void doTest( String file ) {
		try {
			// load source from jar
			String path = "jar:file:" + dir + ".zip!/" + dir + "/" + file;
			byte[] lua = bytesFromJar( path );
			
			// compile in memory
			InputStream is = new ByteArrayInputStream( lua );
	    	Reader r = new InputStreamReader( is );
	    	Proto p = Compiler.compile(r, dir+"/"+file);
	    	String actual = protoToString( p );
			
			// load expected value from jar
			byte[] luac = bytesFromJar( path + "c" );
			Proto e = loadFromBytes( luac, file );
	    	String expected = protoToString( e );

			// compare results
			assertEquals( expected, actual );
			
		} catch (IOException e) {
			fail( e.toString() );
		}
	}
	
	protected byte[] bytesFromJar(String path) throws IOException {
		URL url = new URL(path);
		InputStream is = url.openStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[2048];
		int n;
		while ( (n = is.read(buffer)) >= 0 )
			baos.write( buffer, 0, n );
		is.close();
		return baos.toByteArray();
	}
	
	protected Proto loadFromBytes(byte[] bytes, String script) throws IOException {
		StackState state = new StackState();
		InputStream is = new ByteArrayInputStream( bytes );
		return LoadState.undump(state, is, script);
	}
	
	protected String protoToString(Proto p) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream( baos );
		Print.ps = ps;
		new Print().printFunction(p, true);
		return baos.toString();
	}
	

}