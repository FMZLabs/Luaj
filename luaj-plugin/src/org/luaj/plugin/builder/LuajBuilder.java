package org.luaj.plugin.builder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.LineNumberInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.luajc.JavaGen;
import org.luaj.vm2.luajc.LuaJC;

public class LuajBuilder extends IncrementalProjectBuilder {

	class SampleDeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse
		 * .core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				checkLua(resource);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				checkLua(resource);
				break;
			}
			// return true to continue visiting children.
			return true;
		}
	}

	class SampleResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			checkLua(resource);
			// return true to continue visiting children.
			return true;
		}
	}

	public static final String BUILDER_ID = "org.luaj.plugin.luajbuilder";

	private static final String MARKER_TYPE = "org.luaj.plugin.syntaxerror";

	private final Globals globals;
	private final LuaJC luajc;

	public LuajBuilder() {
		globals = JsePlatform.standardGlobals();
		LuaJC.install(globals);
		luajc = LuaJC.instance;
	}

	private void addMarker(IFile file, String message, int lineNumber,
			int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	protected void clean(IProgressMonitor monitor) throws CoreException {
		// delete markers set and files created
		getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
	}

	void checkLua(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".lua")) {
			IFile file = (IFile) resource;
			deleteMarkers(file);

			try {
				final IProject project = getProject();
				if (!project.hasNature(JavaCore.NATURE_ID)) return;
				final IJavaProject javaProject = JavaCore.create(project);

				// Find the filename relative to the enclosing source folder.
				IPath resourcePath = file.getFullPath();
				final IPath outputDir = javaProject.getOutputLocation();

				// Find the folder for the output files.
				final IPath projectLocation = project.getLocation();
				final IPath binPath = outputDir.makeRelativeTo(projectLocation);

				// Find the enclosing source file.
				for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
					if (entry.getContentKind() != IPackageFragmentRoot.K_SOURCE) continue;
					final IPath sourcePath = entry.getPath();
					if (!sourcePath.isPrefixOf(resourcePath)) continue;
					final IPath relativePath = resourcePath.makeRelativeTo(sourcePath);					
					final String relativeFilename = relativePath.toString();  // ToPortableString()?

					// Compile the lua source file.
					final LineNumberInputStream is = new LineNumberInputStream(file.getContents());
					try {
						final String filename = relativeFilename;
						final String basename = relativeFilename.substring(relativeFilename.lastIndexOf('/')+1);
						final String packages = relativeFilename.substring(0, relativeFilename.length()-basename.length());
						final String namestem = basename.substring(0, basename.lastIndexOf('.'));
	
						final String srcfilename = basename;
						final String luachunkname = namestem;
						final boolean gen_main = true;
						final Hashtable results = luajc.compileAll(is, luachunkname,
								srcfilename, globals, gen_main);
	
						// Persist all the classes generated from this lua source file
						for (Enumeration e = results.keys(); e.hasMoreElements(); ) {
							Object name = e.nextElement();
							Object value = results.get(name); // byte array
							byte[] classbytes = (byte[]) value;
							final String classname = packages + name + ".class";
							final IPath outputPath = binPath.append("/" + classname);	
							final IFile classfile = project.getFile(outputPath);
							createDirs(classfile.getParent());
	
							// overwrite existing file
							final InputStream source = new ByteArrayInputStream(classbytes);
							final int flags = IResource.FORCE | IResource.ALLOW_MISSING_LOCAL;
							if (classfile.exists())
								classfile.setContents(source, flags, null);
							else
								classfile.create(source, flags, null);
							System.out.println("Compiled " + classname);
						}
					} catch (LuaError e) {
						final String msg = e.getMessage();
						final int line = is.getLineNumber();

						System.out.println("parse failed: " + e.getMessage() + "\n"
								+ "line: " + line);

						LuajBuilder.this.addMarker(file, msg, line,
								IMarker.SEVERITY_ERROR);
					}

					// Found the root source, done compiling this lua file.
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void createDirs(IContainer container) throws CoreException {
		if (!(container instanceof IFolder))
			return;
		final IFolder folder = (IFolder) container;
		if (!folder.exists()) {
			createDirs((IFolder) folder.getParent());
			System.out.println("creating folder " + folder);
			folder.create(false, false, null);
		}
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {
			getProject().accept(new SampleResourceVisitor());
		} catch (CoreException e) {
		}
	}

	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new SampleDeltaVisitor());
	}
}
