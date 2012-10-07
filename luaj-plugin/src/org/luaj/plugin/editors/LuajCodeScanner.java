package org.luaj.plugin.editors;

import java.util.Vector;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;

public class LuajCodeScanner extends RuleBasedScanner {

	public LuajCodeScanner(ColorManager manager) {
		IToken string = manager.getToken(ColorConstants.STRING);

		Vector rules = new Vector();
		
		rules.addElement(new SingleLineRule("\"", "\"", string, '\\'));
		rules.addElement(new SingleLineRule("\'", "\'", string, '\\'));

		setRules((IRule[]) rules.toArray(new IRule[rules.size()]));
	}
}

