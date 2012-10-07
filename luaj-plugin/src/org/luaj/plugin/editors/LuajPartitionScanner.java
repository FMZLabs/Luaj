package org.luaj.plugin.editors;

import java.util.Vector;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class LuajPartitionScanner extends RuleBasedPartitionScanner {
	public final static String LUA_COMMENT = "__lua_comment";
	public final static String LUA_LONGSTRING = "__lua_longstring";
	public static final String[] LEGAL_CONTENT_TYPES = 	{
		LUA_COMMENT, 
		LUA_LONGSTRING };

	public LuajPartitionScanner() {

		IToken luaComment = new Token(LUA_COMMENT);
		IToken luaLongString = new Token(LUA_LONGSTRING);

		Vector rules = new Vector();

		String eq = "==========";

		// long comments
		for (int i = eq.length(); i >= 0; --i) {
			String s = eq.substring(0,i);
			rules.addElement(new MultiLineRule("--["+s+"[", "--]"+s+"]", luaComment));
		}

		// plain comments
		rules.add(new EndOfLineRule("--", luaComment));

		// long strings
		for (int i = eq.length(); i >= 0; --i) {
			String s = eq.substring(0,i);
			rules.addElement(new MultiLineRule("["+s+"[", "]"+s+"]", luaLongString));
		}

		setPredicateRules((IPredicateRule[]) rules.toArray(new IPredicateRule[rules.size()]));
	}
}
