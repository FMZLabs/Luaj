package org.luaj.plugin.editors;

import org.eclipse.ui.editors.text.TextEditor;

public class LuajEditor extends TextEditor {

	private ColorManager colorManager;

	public LuajEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new LuajConfiguration(colorManager));
		setDocumentProvider(new LuajDocumentProvider());
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

}
