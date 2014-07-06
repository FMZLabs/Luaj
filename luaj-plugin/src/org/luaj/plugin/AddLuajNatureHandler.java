package org.luaj.plugin;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.luaj.plugin.builder.LuajNature;

public class AddLuajNatureHandler  implements IObjectActionDelegate {

	private ISelection selection;

	/**
	 * Constructor for AddLuajNatureToIProjectAction.
	 */
	public AddLuajNatureHandler() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (selection instanceof IStructuredSelection) {
			for (Iterator it = ((IStructuredSelection) selection).iterator(); it
					.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject) {
					project = (IProject) element;
				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element)
							.getAdapter(IProject.class);
				}
				if (project != null) {
					try {
						addLuajNature(project);
						project.setPersistentProperty(LuajNature.HAS_LUAJ_NATURE, LuajNature.NATURE_ADDED);
						System.out.println("Luaj nature added to "+project);
					} catch (CoreException e) {
						//TODO log something
						throw new RuntimeException("Failed to add luaj nature",
								e);
					}
				}
			}
		}
	}

	/**
	 * Adds luaj nature and Java nature on a project
	 *
	 * @param project
	 *            to have sample nature added
	 */
	private void addLuajNature(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] natures = description.getNatureIds();
		// Add the natures we need, including the JDT.
		includeNature(description, JavaCore.NATURE_ID);
		includeNature(description, LuajNature.NATURE_ID);
		project.setDescription(description, null);
	}
	

	private void includeNature(IProjectDescription description, String nature_id) throws CoreException {
		String[] natures = description.getNatureIds();
		for (int i = 0; i < natures.length; ++i) {
			if (nature_id.equals(natures[i])) {
				System.out.println("Project already has nature id "+nature_id);
				return;
			}
		}
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = nature_id;
		description.setNatureIds(newNatures);
		System.out.println("Added nature id "+nature_id);
	}
	
}
