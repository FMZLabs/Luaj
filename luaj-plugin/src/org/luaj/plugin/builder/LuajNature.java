package org.luaj.plugin.builder;

import java.net.URL;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.luaj.plugin.Activator;
import org.osgi.framework.Bundle;

public class LuajNature implements IProjectNature {

	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = "org.luaj.plugin.luajnature";

	/**
	 * Persistent property name indicating if the project has the luaj nature
	 * applied.
	 */
	public static final String NATURE_ADDED = "true";
	public static final String NATURE_REMOVED = "false";
	public static final QualifiedName HAS_LUAJ_NATURE = new QualifiedName(
			"org.luaj.plugin.luajnature", "hasnature");

	private IProject project;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(LuajBuilder.BUILDER_ID)) {
				return;
			}
		}

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = desc.newCommand();
		command.setBuilderName(LuajBuilder.BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);

		// TODO: Add the luaj jar to the class path 
		// TODO: add a library variable, LUAJ_LIB?
		/*
		Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		Path path = new Path("lib/luaj-jse-3.0.jar");
		URL url = FileLocator.find(bundle, path, null);
		String luajpath;
		try {
			final URL fileUrl = FileLocator.toFileURL(url);
			luajpath = fileUrl.getPath();
			System.out.println("Checking luaj path " +  luajpath);
			Path luajentry = new Path(luajpath);
			
			IJavaProject jProj = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
			IClasspathEntry[] existingEntries = jProj.getRawClasspath();

			// iterate over the class path
			for (IClasspathEntry entry : existingEntries)
			{
				String entryStr = entry.getPath().toString();
				IPath entryPath = entry.getPath();
				System.out.println("-------->  checking entry "+entryPath);
				if (entryPath.equals(luajentry)) {
					System.out.println("-------->  luaj already in class path");
					return;
				}
			}
			System.out.println("-------->  luaj not in class path");
			final int n = existingEntries.length;
			IClasspathEntry[] newEntries = new IClasspathEntry[n+1];
			System.arraycopy(existingEntries,  0,  newEntries,  0, n);			
			// TODO: also add the source path
			newEntries[n+0] = JavaCore.newLibraryEntry(luajentry, null,null);
			jProj.setRawClasspath(newEntries, null);
			System.out.println("-------->  luaj added to class path");
			

			
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResourceException(IStatus.ERROR, path, e.getMessage(), e);
			// TODO: throw exception
		}
		System.out.println("Path to jar: "+luajpath);
		*/

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(LuajBuilder.BUILDER_ID)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i,
						commands.length - i - 1);
				description.setBuildSpec(newCommands);
				project.setDescription(description, null);
				return;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core
	 * .resources.IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

}
