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
package org.luaj.debug.j2se;

import java.io.IOException;
import java.net.URL;

import org.luaj.debug.j2se.StandardLuaJVM;
import org.luaj.debug.j2se.StandardLuaJVM.ParseException;

import junit.framework.TestCase;

/**
 * Sanity test for StandardLuaJVM.
 */
public class LuaJVMTest extends TestCase {
	public void testCommandLineParse() {
		// null arguments
		String[] args = null;		
		StandardLuaJVM vm = new StandardLuaJVM();
		try {
			vm.parse(args);
			fail("Bad parsing program. Should never reach this line.");
		} catch (ParseException e) {}
		
		// empty arguments
		args = new String[] {};
		try {
			vm.parse(args);
			fail("Bad parsing program. Should never reach this line.");
		} catch (ParseException e) {}

		// incomplete arguments
		args = new String[] { "-debug" };
		try {
			vm.parse(args);
			fail("Bad parsing program. Should never reach this line.");
		} catch (ParseException e) {}
		
		// incomplete arguments
		args = new String[] { "-debug", "1046" };
		try {
			vm.parse(args);
			fail("Bad parsing program. Should never reach this line.");
		} catch (ParseException e) {}
		
		// missing script name
		args = new String[] { "-debug", "1046", "1047"};
		try {
			vm.parse(args);
			fail("Bad parsing program. Should never reach this line.");
		} catch (ParseException e) {}

		// malformed request port
		args = new String[] { "-debug", "104x", "1046", "dummy.lua"};
		try {
			vm.parse(args);
			fail("Bad parsing program. Should never reach this line.");
		} catch (ParseException e) {}

		// malformed event port
		args = new String[] { "-debug", "1046", "104x", "dummy.lua"};
		try {
			vm.parse(args);
			fail("Bad parsing program. Should never reach this line.");
		} catch (ParseException e) {}
		
		// event port == request port
		args = new String[] { "-debug", "1046", "1046", "dummy.lua"};
		try {
			vm.parse(args);
			fail("Bad parsing program. Should never reach this line.");
		} catch (ParseException e) {}

		// lua script cannot be found
		args = new String[] { "-debug", "1046", "1047", "dummy.lua"};
		try {
			vm.parse(args);
			vm.run();
			fail("Should never reach this line.");
		} catch (ParseException e) {
			fail("Should never reach this line.");
		} catch (IOException e) { 
			//expected
		}
		
		// lua script cannot be found
		args = new String[] {"dummy.lua"};
		try {
			vm.parse(args);
			vm.run();
			fail("Bad parsing program. Should never reach this line.");
		} catch (ParseException e) {
			fail("Should never reach this line.");
		} catch (IOException e) { 
			//expected			
		}
	}
	
    public void testRun() {
        String[] tests = new String[] {
                "autoload",
                "boolean",
                "calls",
                "coercions",
                "compare",
                "math",
                "mathlib",
                "metatables",
                "select",
                "setlist",
                "swingapp",
                "test1",
                "test2",
                "test3",
                "test4",
                "test5",
                "test6",
                "test7",
                "type",
                "upvalues",
                //"strlib"
        };
        
        for (int i = 0; i < tests.length; i++) {
        	String test = tests[i];
            System.out.println("==> running test: " + test + ".lua");
            doTestRun(test + ".lua");            
            System.out.println("==> running test: " + test + ".luac");
            doTestRun(test + ".luac");
            System.out.println();
        }
    }
    
    protected void doTestRun(String testName) {
        String[] args = new String[1];
        URL filePath = getClass().getResource("/"+ testName);
        if (filePath != null) {
            args[0] = filePath.getPath(); 
            try {
                StandardLuaJVM.main(args);
            } catch (Exception e) {
                e.printStackTrace();
                fail("Test " + testName + " failed due to " + e.getMessage());
            }            
        }        
    }    
}
