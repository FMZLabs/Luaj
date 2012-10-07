package org.luaj.plugin.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

public class LuajConfiguration extends SourceViewerConfiguration {
	private LuajDoubleClickStrategy doubleClickStrategy;
	private RuleBasedScanner commentScanner;
	private RuleBasedScanner longStringScanner;
	private LuajCodeScanner codeScanner;
	private ColorManager colorManager;

	public LuajConfiguration(ColorManager colorManager) {
		this.colorManager = colorManager;
	}
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			LuajPartitionScanner.LUA_COMMENT};
	}
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new LuajDoubleClickStrategy();
		return doubleClickStrategy;
	}
	protected RuleBasedScanner getLuajCommentScanner() {
		if (commentScanner == null) {
			commentScanner = new RuleBasedScanner();
			commentScanner.setDefaultReturnToken(colorManager.getToken(ColorConstants.COMMENT));
			commentScanner.setRules(new IRule[] {
					new EndOfLineRule("@", colorManager.getToken(ColorConstants.LUADOC)),
				});
		}
		return commentScanner;
	}
	protected RuleBasedScanner getLuajLongStringScanner() {
		if (longStringScanner == null) {
			longStringScanner = new RuleBasedScanner();
			longStringScanner.setDefaultReturnToken(colorManager.getToken(ColorConstants.STRING));
		}
		return longStringScanner;
	}
	protected LuajCodeScanner getLuajCodeScanner() {
		if (codeScanner == null) {
			codeScanner = new LuajCodeScanner(colorManager);
			codeScanner.setDefaultReturnToken(colorManager.getToken(ColorConstants.DEFAULT));
		}
		return codeScanner;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr;
		dr = new DefaultDamagerRepairer(getLuajCommentScanner());
		reconciler.setDamager(dr, LuajPartitionScanner.LUA_COMMENT);
		reconciler.setRepairer(dr, LuajPartitionScanner.LUA_COMMENT);

		dr = new DefaultDamagerRepairer(getLuajLongStringScanner());
		reconciler.setDamager(dr, LuajPartitionScanner.LUA_LONGSTRING);
		reconciler.setRepairer(dr, LuajPartitionScanner.LUA_LONGSTRING);

		dr = new DefaultDamagerRepairer(getLuajCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		return reconciler;
	}

}