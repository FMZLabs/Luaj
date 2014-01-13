import java.applet.Applet;
import java.awt.Graphics;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

/**
 * Simple Applet that forwards Applet lifecycle events to a lua script.
 * 
 * <p>
 * On Applet.init() a script is loaded and executed, with the Applet instance as
 * the first argument. Initialization of the Applet UI can be done here.
 * 
 * <p>
 * Other Applet lifecycle events are invoked when they are recieved, and
 * forwarded to methods in the global environment, if they exist. These are:
 * <ul>
 * <li>start() called when {@link Applet#start} is called.
 * <li>stop() called when {@link Applet#stop} is called.
 * <li>paint(graphics) called when {@link Applet#paint(Graphics)} is called. If
 * this is not defined as a function the superclass method will be called.
 * <li>update(graphics) called when {@link Applet#update(Graphics)} is called.
 * If this is not defined as a function the superclass method will be called. By
 * calling <code>applet:paint(graphics)</code> in the implementation the applet
 * content will not be cleared before painting.
 * </ul>
 * 
 * <p>
 * Note that these lookups are done at runtime, so the paint method or any other
 * method can be changed based on application logic, if desired.
 * 
 * @see Globals
 * @see LuaValue
 * @see Applet
 */
public class SampleApplet extends Applet {
	private static final long serialVersionUID = 1L;

	Globals globals;
	LuaValue pcall;
	LuaValue graphics;
	Graphics coerced;

	// This applet property is searched for the name of script to load.
	static String script_parameter = "script";

	// This must be located relative to the document base to be loaded.
	static String default_script = "swingapplet.lua";

	public void init() {
		// Find the script to load from the applet parameters.
		String script = getParameter(script_parameter);
		if (script == null)
			script = default_script;
		System.out.println("Loading " + script);

		// Construct globals to use.
		globals = JsePlatform.debugGlobals();
		pcall = globals.get("pcall");

		// Load and execute the script, supplying this Applet as the only
		// argument.
		globals.loadfile(script).call(CoerceJavaToLua.coerce(this));
	}

	public void start() {
		pcall.call(globals.get("start"));
	}

	public void stop() {
		pcall.call(globals.get("stop"));
	}

	public void update(Graphics g) {
		LuaValue u = globals.get("update");
		if (!u.isfunction())
			super.update(g);
		else
			pcall.call(
					u,
					coerced == g ? graphics : (graphics = CoerceJavaToLua
							.coerce(coerced = g)));
	}

	public void paint(Graphics g) {
		LuaValue p = globals.get("paint");
		if (!p.isfunction())
			super.paint(g);
		else
			pcall.call(
					p,
					coerced == g ? graphics : (graphics = CoerceJavaToLua
							.coerce(coerced = g)));
	}
}
