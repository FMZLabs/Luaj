package org.luaj.plugin.launch;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.internal.resources.Folder;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.debug.core.JavaDebugUtils;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;

public class LaunchConfigurationUtils {

	public static class LauncherMessages {

		public static String luajlauncher_utils_error_main_type_not_specified = "Main type not specified.";
		public static String luajlauncher_utils_error_main_type_does_not_exist = "Main type does not exist.";
		protected static String luajlauncher_search_task_inprogress = "Search in progress.";
	}

	/**
	 * Throws a core exception with an error status object built from the given
	 * message, lower level exception, and error code.
	 * 
	 * @param message
	 *            the status message
	 * @param exception
	 *            lower level exception associated with the error, or
	 *            <code>null</code> if none
	 * @param code
	 *            error code
	 */
	public static void abort(String message, Throwable exception, int code)
			throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR,
				JDIDebugUIPlugin.getUniqueIdentifier(), code, message,
				exception));
	}

	/**
	 * Return the <code>IType</code> referenced by the specified name and
	 * contained in the specified project or throw a <code>CoreException</code>
	 * whose message explains why this couldn't be done.
	 */
	public static IType getMainType(String mainTypeName,
			IJavaProject javaProject) throws CoreException {
		if ((mainTypeName == null) || (mainTypeName.trim().length() < 1)) {
			abort(LauncherMessages.luajlauncher_utils_error_main_type_not_specified,
					null,
					IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
		}
		IType mainType = null;
		try {
			mainType = findType(javaProject, mainTypeName);
		} catch (JavaModelException jme) {
		}
		if (mainType == null) {
			abort(NLS
					.bind(LauncherMessages.luajlauncher_utils_error_main_type_does_not_exist,
							new String[] { mainTypeName,
									javaProject.getElementName() }),
					null,
					IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
		}
		return mainType;
	}

	/**
	 * Find the specified (fully-qualified) type name in the specified java
	 * project.
	 */
	public static IType findType(IJavaProject javaProject, String mainTypeName)
			throws CoreException {
		IJavaElement javaElement = JavaDebugUtils.findElement(mainTypeName,
				javaProject);
		if (javaElement == null) {
			return null;
		} else if (javaElement instanceof IType) {
			return (IType) javaElement;
		} else if (javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
			String simpleName = Signature.getSimpleName(mainTypeName);
			return ((ICompilationUnit) javaElement).getType(simpleName);
		} else if (javaElement.getElementType() == IJavaElement.CLASS_FILE) {
			return ((IClassFile) javaElement).getType();
		}
		return null;
	}

	/**
	 * 
	 */
	public static Set<IType> collectLuajTypesInProject(
			IProgressMonitor monitor, IJavaProject javaProject) {
		IType[] types;
		HashSet<IType> result = new HashSet<IType>(5);
		try {
			IType luaValueType = LaunchConfigurationUtils.getMainType(
					"org.luaj.vm2.LuaValue", javaProject); //$NON-NLS-1$
			ITypeHierarchy hierarchy = luaValueType.newTypeHierarchy(
					javaProject, new SubProgressMonitor(monitor, 1));
			types = hierarchy.getAllSubtypes(luaValueType);
			int length = types.length;
			if (length != 0) {
				for (int i = 0; i < length; i++) {
					System.out.println("type[i]: " + types[i]);
					 if (!types[i].isBinary())
					{
						result.add(types[i]);
					}
				}
			}
		} catch (JavaModelException jme) {
			jme.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

		// Find classpath entries
		// Find those that have IPackageFragmentRoot.K_SOURCE
		// Find sources in those paths
		// Fond sources that have .lua extensions
		try {
			for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {

				if (entry.getContentKind() == IPackageFragmentRoot.K_SOURCE) {
					IPath path = entry.getPath();
					IPath projectLocation = javaProject.getProject().getLocation();
					final IPath relativePath = path.makeRelativeTo(
							projectLocation);
					
					// TODO: find all .lua files, add to types.
					IFolder folder = javaProject.getProject().getFolder(relativePath);
					for (IResource resource : folder.members()) {
						System.out.println("======= examining " + resource);
						if (resource.getType() != IResource.FILE) continue;
						String name = resource.getName();
						if (name.endsWith(".lua")) {
							System.out.println("Adding "+name);

							// Find the path segments relative to the source entry.
							String[] segments = resource.getFullPath().segments();
							String[] relative = new String[segments.length - path.segmentCount()];
							System.arraycopy(segments,  path.segmentCount(), relative, 0, relative.length);
							String s = "";
							for (String r : relative)
								s = s + "." + r;
							s = s.substring(1);
							s = s.substring(0,s.length()-4);
							System.out.println("File segments: "+s);

							IType type = LaunchConfigurationUtils.getMainType(
									s, javaProject); //$NON-NLS-1$
							System.out.println("Here is the type: " + type);

						}
						
					}
				}

			}
		} catch (JavaModelException jme) {
			jme.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

		monitor.done();
		return result;
	}

	public static void collectTypes(Object element, IProgressMonitor monitor,
			Set<Object> result) throws JavaModelException/*
														 * ,
														 * InvocationTargetException
														 */{
		element = computeScope(element);
		System.out.println("--------- collecting froim " + element);
		while (element instanceof IMember) {
			if (element instanceof IType) {
				if (isSubclassOfLuaj(monitor, (IType) element)) {
					result.add(element);
					monitor.done();
					return;
				}
			}
			element = ((IJavaElement) element).getParent();
		}
		if (element instanceof ICompilationUnit) {
			ICompilationUnit cu = (ICompilationUnit) element;
			IType[] types = cu.getAllTypes();
			for (int i = 0; i < types.length; i++) {
				if (isSubclassOfLuaj(monitor, types[i])) {
					result.add(types[i]);
				}
			}
		} else if (element instanceof IClassFile) {
			IType type = ((IClassFile) element).getType();
			if (isSubclassOfLuaj(monitor, type)) {
				result.add(type);
			}
		} else if (element instanceof IJavaElement) {
			IJavaElement parent = (IJavaElement) element;
			List<IType> found = searchSubclassesOfLuaj(monitor,
					(IJavaElement) element);
			// filter within the parent element
			Iterator<IType> iterator = found.iterator();
			while (iterator.hasNext()) {
				IJavaElement target = iterator.next();
				IJavaElement child = target;
				while (child != null) {
					if (child.equals(parent)) {
						result.add(target);
						break;
					}
					child = child.getParent();
				}
			}
		} else {
			// TODO: add soure, and is a lua file!
			System.out.println("Skipping over " + element);
		}

		monitor.done();
	}

	private static List<IType> searchSubclassesOfLuaj(IProgressMonitor pm,
			IJavaElement javaElement) {
		return new ArrayList<IType>(collectLuajTypesInProject(pm,
				javaElement.getJavaProject()));
	}

	private static boolean isSubclassOfLuaj(IProgressMonitor pm, IType type) {
		return collectLuajTypesInProject(pm, type.getJavaProject()).contains(
				type);
	}

	private static Object computeScope(Object element) {
		if (element instanceof IJavaElement) {
			return element;
		}
		if (element instanceof IAdaptable) {
			element = ((IAdaptable) element).getAdapter(IResource.class);
		}
		if (element instanceof IResource) {
			IJavaElement javaElement = JavaCore.create((IResource) element);
			if (javaElement != null && !javaElement.exists()) {
				// do not consider the resource - corresponding java element
				// does not exist
				element = null;
			} else {
				element = javaElement;
			}

		}
		return element;
	}

	/**
	 * Searches for luajs from within the given scope of elements
	 * 
	 * @param context
	 * @param elements
	 *            the search scope
	 * @return and array of <code>IType</code>s of matches for java types that
	 *         extend <code>Luaj</code> (directly or indirectly)
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public static IType[] findLuajs(IRunnableContext context,
			final Object[] elements) throws InvocationTargetException,
			InterruptedException {
		final Set<Object> result = new HashSet<Object>();

		if (elements.length > 0) {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor pm)
						throws InterruptedException {
					int nElements = elements.length;
					pm.beginTask(
							LauncherMessages.luajlauncher_search_task_inprogress,
							nElements);
					try {
						for (int i = 0; i < nElements; i++) {
							try {
								collectTypes(elements[i],
										new SubProgressMonitor(pm, 1), result);
							} catch (JavaModelException jme) {
								JDIDebugUIPlugin.log(jme.getStatus());
							}
							if (pm.isCanceled()) {
								throw new InterruptedException();
							}
						}
					} finally {
						pm.done();
					}
				}
			};
			context.run(true, true, runnable);
		}
		return result.toArray(new IType[result.size()]);
	}
}
