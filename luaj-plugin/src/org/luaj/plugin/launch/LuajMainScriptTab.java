package org.luaj.plugin.launch;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.debug.ui.IJavaDebugHelpContextIds;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.DebugTypeSelectionDialog;
import org.eclipse.jdt.internal.debug.ui.launcher.SharedJavaMainTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * This tab appears for luaj launch configurations and allows the user to edit
 * attributes such as the main script to launch and its owning project, if any.
 */
public class LuajMainScriptTab extends SharedJavaMainTab {
	
	public static class LauncherMessages {
		public static final String LuajMainTab_3 = null;
		public static String LuajMainTab_1 = "Luaj Main 1";
		public static String LuajMainTab_2 = "Luaj Main 2";
		public static String luajlauncher_maintab_mainclasslabel_name = "Main Script";
		public static String luajlauncher_maintab_name = "Luaj Main Tab Name";
		public static String luajlauncher_maintab_selection_luaj_dialog_title = "Choose luaj main script";
		public static String luajlauncher_maintab_project_error_doesnotexist = "Project does not exist";
		public static String luajlauncher_maintab_type_error_doesnotexist = "Type does not exist";
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite projComp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH); 
		((GridLayout)projComp.getLayout()).verticalSpacing = 0;
		createProjectEditor(projComp);
		createVerticalSpacer(projComp, 1);
		createMainTypeEditor(projComp, LauncherMessages.luajlauncher_maintab_mainclasslabel_name);
		setControl(projComp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), 
				IJavaDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_APPLET_MAIN_TAB);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		// TODO: use luaj image here.
		return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return LauncherMessages.luajlauncher_maintab_name;
	}

	/**
	 * Show a dialog that lists all main types
	 */
	@Override
	protected void handleSearchButtonSelected() {
		IJavaElement[] scope= null;
		IJavaProject project = getJavaProject();
		if (project == null) {
			try {
				scope = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
			}
			catch (JavaModelException e) {
				setErrorMessage(e.getMessage());
				return;
			}
		}
		else {
			scope = new IJavaElement[]{project};
		}
		IType[] types = null;
		try {
			types = LaunchConfigurationUtils.findLuajs(getLaunchConfigurationDialog(), scope);
		} 
		catch (InterruptedException e) {return;} 
		catch (InvocationTargetException e) {
			setErrorMessage(e.getTargetException().getMessage());
			return;
		}
		DebugTypeSelectionDialog dialog = new DebugTypeSelectionDialog(getShell(), types, 
				LauncherMessages.luajlauncher_maintab_selection_luaj_dialog_title); 
		if (dialog.open() == Window.CANCEL) {
			return;
		}
		Object[] results = dialog.getResult();	
		IType type = (IType)results[0];
		if (type != null) {
			fMainText.setText(type.getFullyQualifiedName());
			fProjText.setText(type.getJavaProject().getElementName());
		}
	}

	/**
	 * Initialize the luaj viewer class name attribute.
	 * @param config the configuration
	 */
	private void initializeLuajViewerClass(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_APPLET_APPLETVIEWER_CLASS, (String)null);
	}
	
	/**
	 * Initialize default attribute values based on the
	 * given Java element.
	 * @param javaElement the Java element
	 * @param config the configuration
	 */
	private void initializeDefaults(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		initializeJavaProject(javaElement, config);
		initializeMainTypeAndName(javaElement, config);
		initializeLuajViewerClass(config);
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.debug.ui.launcher.AbstractJavaMainTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		super.initializeFrom(config);
		updateMainTypeFromConfig(config);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);
		String name= fProjText.getText().trim();
		if (name.length() > 0) {
			if (!ResourcesPlugin.getWorkspace().getRoot().getProject(name).exists()) {
				setErrorMessage(LauncherMessages.luajlauncher_maintab_project_error_doesnotexist); 
				return false;
			}
		}
		name = fMainText.getText().trim();
		if (name.length() == 0) {
			setErrorMessage(LauncherMessages.luajlauncher_maintab_type_error_doesnotexist); 
			return false;
		}
		// TODO: check name for .lua extension. 
		// check for existence of source file in source path
		
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText());
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, fMainText.getText());
		mapResources(config);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		IJavaElement je= getContext();
		if (je != null) {
			initializeDefaults(je, config);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 * 
	 * @since 3.3
	 */
	@Override
	public String getId() {
		return "org.eclipse.jdt.debug.ui.luajMainTab"; //$NON-NLS-1$
	}
}
