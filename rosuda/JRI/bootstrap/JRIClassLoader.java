import java.net.URLClassLoader;
import java.net.URL;
import java.util.HashMap;
import java.util.Vector;
import java.util.Enumeration;
import java.io.File;

public class JRIClassLoader extends URLClassLoader {
    HashMap libMap;

    Vector children;

    static JRIClassLoader mainLoader;

    public static JRIClassLoader getMainLoader() {
	if (mainLoader == null) mainLoader = new JRIClassLoader();
	return mainLoader;
    }

    public JRIClassLoader() {
	super(new URL[]{});
	children = new Vector();
	libMap = new HashMap();
    }

    public void registerLoader(DelegatedClassLoader cl) {
	if (!children.contains(cl))
	    children.add(cl);
    }

    public void unregisterLoader(DelegatedClassLoader cl) {
	children.removeElement(cl);
    }

    public void registerLibrary(String name, File f) {
	libMap.put(name, f);
    }

    protected String findLibrary(String name) {
	String s = null;
        System.out.println("boot findLibrary(\""+name+"\")");
	try {
	    for (Enumeration e = children.elements() ; e.hasMoreElements() ;) {
		DelegatedClassLoader cl = (DelegatedClassLoader)e.nextElement();
		if (cl != null) {
		    s = cl.delegatedFindLibrary(name);
		    if (s != null) {
			System.out.println(" - found delegated answer "+s+" from "+cl);
			return s;
		    }
		}
	    }
	} catch (Exception ex) {}
	
        File u = (File) libMap.get(name);
        if (u!=null && u.exists()) s=u.getPath();
        System.out.println(" - mapping to "+((s==null)?"<none>":s));
	
        return s;
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        Class cl = null;
        System.out.println("boot findClass(\""+name+"\")");
	for (Enumeration e = children.elements() ; e.hasMoreElements() ;) {
	    DelegatedClassLoader ldr = (DelegatedClassLoader)e.nextElement();
	    if (ldr != null) {
		try {
		    cl = ldr.delegatedFindClass(name);
		    if (cl != null) {
			System.out.println(" - found delegated answer "+cl+" from "+ldr);
			return cl;
		    }
		} catch (Exception ex) {}
	    }
	}
	return super.findClass(name);
    }

    public URL findResource(String name) {
        URL u = null;
        System.out.println("boot findResource(\""+name+"\")");
	for (Enumeration e = children.elements() ; e.hasMoreElements() ;) {
	    DelegatedClassLoader ldr = (DelegatedClassLoader)e.nextElement();
	    if (ldr != null) {
		try {
		    u = ldr.delegatedFindResource(name);
		    if (u != null) {
			System.out.println(" - found delegated answer "+u+" from "+ldr);
			return u;
		    }
		} catch (Exception ex) {}
	    }
	}
	return super.findResource(name);
    }
}
