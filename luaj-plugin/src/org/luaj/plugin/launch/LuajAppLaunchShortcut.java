package org.luaj.plugin.launch;

import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

public class LuajAppLaunchShortcut implements ILaunchShortcut {

    public void launch(IEditorPart editor, String mode) {
        IEditorInput input = editor.getEditorInput();
        IJavaElement javaElement = 
            (IJavaElement) input.getAdapter(IJavaElement.class);
        if (javaElement != null) {
         searchAndLaunch(new Object[] {javaElement}, mode);
        } 
    }
    
    public void launch(ISelection selection, String mode) {
        if (selection instanceof IStructuredSelection) {
         searchAndLaunch(((IStructuredSelection)selection).toArray(), mode);
        } 
    }


    protected void searchAndLaunch(Object[] search, String mode) {
    	/*
        IType[] types = null;
        if (search != null) {
            try {
             types = LuajAppLaunchConfigurationUtils.findApplets(
                     new ProgressMonitorDialog(getShell()), search);
            } catch (Exception e) {
               // Handle exceptions
            }
            IType type = null;
            
	         if (types.length == 0) {
	                MessageDialog.openInformation(
	                    getShell(), "Luaj App Launch", "No lua scripts found."};
         } else if (types.length > 1) {
                type = chooseType(types, mode);
         } else {
                type = types[0];
            }
            if (type != null) {
             launch(type, mode);
            }
        }
        */
    }

    protected void launch(IType type, String mode) {
    	/*
        try {
            ILaunchConfiguration config = findLaunchConfiguration(type, mode);
            if (config != null) {
             config.launch(mode, null);
            }
        } catch (CoreException e) {
            // Handle exceptions
        }
        */
    }

}
