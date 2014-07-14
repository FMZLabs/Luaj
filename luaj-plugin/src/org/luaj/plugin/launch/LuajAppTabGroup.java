package org.luaj.plugin.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaMainTab;

public class LuajAppTabGroup extends AbstractLaunchConfigurationTabGroup
		implements ILaunchConfigurationTabGroup {

	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new LuajMainScriptTab(),
				new JavaMainTab(),  // TODO: replace with LuajMainTab
				new JavaArgumentsTab(), // TODO: replacw with Luaj arguments tab
				new JavaJRETab(),
				new JavaClasspathTab(), new SourceLookupTab(),
				new EnvironmentTab(), new CommonTab() };
		setTabs(tabs);
	}
}
