package co.paralleluniverse.comsat.tomcat;

import co.paralleluniverse.fibers.instrument.Log;
import co.paralleluniverse.fibers.instrument.LogLevel;
import co.paralleluniverse.fibers.instrument.QuasarInstrumentor;
import org.apache.catalina.loader.ResourceEntry;
import org.apache.catalina.loader.WebappClassLoader;

/**
 * See:
 * http://tomcat.apache.org/tomcat-6.0-doc/config/loader.html
 * 
 * @author pron
 */
public class QuasarWebAppClassLoader extends WebappClassLoader {
    private final QuasarInstrumentor instrumentor;

    public QuasarWebAppClassLoader() {
        instrumentor = newInstrumentor();
    }

    public QuasarWebAppClassLoader(ClassLoader parent) {
        super(parent);
        instrumentor = newInstrumentor();
    }

    private QuasarInstrumentor newInstrumentor() {
        QuasarInstrumentor inst = new QuasarInstrumentor(this);
        inst.setLog(new Log() {
            @Override
            public void log(LogLevel level, String msg, Object... args) {
                System.out.println("[quasar] " + level + ": " + String.format(msg, args));
            }

            @Override
            public void error(String msg, Exception exc) {
                System.out.println("[quasar] ERROR: " + msg);
                exc.printStackTrace(System.out);
            }
        });
//        inst.setVerbose(true);
//        inst.setDebug(true);
        return inst;
    }

    @Override
    protected ResourceEntry findResourceInternal(String name, String path) {
        ResourceEntry entry = super.findResourceInternal(name, path);
        if (entry != null && path.endsWith(".class")) {
            if(name.endsWith(".class"))
                name = name.substring(0, name.length() - ".class".length());
            entry.binaryContent = instrumentor.instrumentClass(name, entry.binaryContent);
        }
        return entry;
    }
}
