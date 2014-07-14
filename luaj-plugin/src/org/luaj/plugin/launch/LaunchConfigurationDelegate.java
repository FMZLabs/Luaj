package org.luaj.plugin.launch;

import java.net.URL;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.luaj.plugin.Activator;
import org.osgi.framework.Bundle;

public class LaunchConfigurationDelegate extends
		AbstractJavaLaunchConfigurationDelegate implements
		ILaunchConfigurationDelegate {

	ILaunchConfiguration currentlyLaunchedConfiguration;

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {

		// Verity main type, get the runner.
		String mainTypeName = verifyMainTypeName(configuration);
		IJavaProject javaProject = getJavaProject(configuration);
		// IType type = JavaLaunchConfigurationUtils.getMainType(mainTypeName,
		// javaProject);
		// ITypeHierarchy hierarchy = type
		// .newSupertypeHierarchy(new NullProgressMonitor());
		// IType javaLangApplet = JavaLaunchConfigurationUtils.getMainType(
		// "java.applet.Applet", javaProject);
		// if (!hierarchy.contains(javaLangApplet)) {
		// abort("The applet type is not a subclass of java.applet.Applet.");
		// }
		IVMInstall vm = verifyVMInstall(configuration);
		IVMRunner runner = vm.getVMRunner(mode);

		Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		Path path = new Path("lib/luaj-jse-3.0.jar");
		URL url = FileLocator.find(bundle, path, null);
		String luajpath;
		try {
			final URL fileUrl = FileLocator.toFileURL(url);
			luajpath = fileUrl.getPath();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResourceException(IStatus.ERROR, path, e.getMessage(), e);
			// TODO: throw exception
		}
		System.out.println("Path to jar: "+luajpath);

		final String[] cp = getClasspath(configuration);
		final String[] classpath = { cp[0], luajpath };

		final String programArgs = getProgramArguments(configuration);
		final String vmArgs = getVMArguments(configuration);
		final IPath workingDir = getWorkingDirectoryPath(configuration);
		final String workingDirName = workingDir != null ? workingDir
				.toOSString() : null;

		// Create VM config
		VMRunnerConfiguration runConfig = new VMRunnerConfiguration(
				mainTypeName, classpath);
		runConfig.setProgramArguments(new String[] { programArgs }); // TODO: Only add if not empty, parse?
		runConfig.setVMArguments(new String[] {}); // TODO: only add if not empty, possibly parse ?
		runConfig.setWorkingDirectory(workingDirName);

		// Bootpath - add luaj-jse jar?
		String[] bootpath = getBootpath(configuration);
		runConfig.setBootClassPath(bootpath);

		// Launch the configuration
		this.currentlyLaunchedConfiguration = configuration;
		runner.run(runConfig, launch, monitor);
	}

}
