package org.luaj.vm;

import java.io.IOException;

/**
 * Test error messages produced by luaj. 
 */
public class ErrorMessageTest extends ScriptDrivenTest {

	private static final String dir = "src/test/errors";
	
	public ErrorMessageTest() {
		super(dir);
	}
	
	public void testBaseLibArgs() throws IOException, InterruptedException {
		runTest("baselibargs");
	}

	

}
