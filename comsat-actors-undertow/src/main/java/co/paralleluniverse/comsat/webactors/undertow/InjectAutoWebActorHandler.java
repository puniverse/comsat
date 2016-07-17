package co.paralleluniverse.comsat.webactors.undertow;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * @author rodedb
 */
@Singleton
public class InjectAutoWebActorHandler extends AutoWebActorHandler {

    private Settings settings;

    @Inject
    public InjectAutoWebActorHandler(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void initContextProvider() {
        this.contextProvider =
                new InjectAutoContextProvider(
                        settings.getClassLoader(),
                        settings.getPackagePrefixes(),
                        settings.getProvidersWrapper());
    }

    /**
     * Wrapper class used to provide all necessary settings to an {@link InjectAutoWebActorHandler} instance.
     * Assuming a DI framework is used to inject the {@link InjectAutoWebActorHandler} instance, its {@link Settings}
     * instance should be configured for injection as well.
     * <p>
     * See {@link AutoWebActorHandler} for details regarding the setting properties.
     */
    public static class Settings {

        public Settings(ProvidersWrapper providersWrapper) {
            this(providersWrapper, null, null);
        }

        public Settings(ProvidersWrapper providersWrapper, List<String> packagePrefixes) {
            this(providersWrapper, null, packagePrefixes);
        }

        public Settings(ProvidersWrapper providersWrapper, ClassLoader classLoader, List<String> packagePrefixes) {
            if (providersWrapper == null)
                throw new IllegalArgumentException("InjectAutoWebActorHandler requires a ProvidersWrapper");
            this.providersWrapper = providersWrapper;
            this.classLoader = classLoader;
            this.packagePrefixes = packagePrefixes;
        }

        private ClassLoader classLoader = null;
        private List<String> packagePrefixes = null;
        private ProvidersWrapper providersWrapper;

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public void setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public List<String> getPackagePrefixes() {
            return packagePrefixes;
        }

        public void setPackagePrefixes(List<String> packagePrefixes) {
            this.packagePrefixes = packagePrefixes;
        }

        public ProvidersWrapper getProvidersWrapper() {
            return providersWrapper;
        }

        public void setProvidersWrapper(ProvidersWrapper providersWrapper) {
            this.providersWrapper = providersWrapper;
        }
    }

    /**
     * Part of the {@link InjectAutoWebActorHandler.Settings} object which wraps actor {@link Provider}s for access by their
     * class. {@link InjectAutoActorContext} will use the appropriate {@link Provider} to instantiate the actor class
     * via the DI framework.
     */
    public static class ProvidersWrapper {

        public ProvidersWrapper(Map<Class<?>, Provider> providers) {
            this.providers = providers;
        }

        private Map<Class<?>, Provider> providers;

        public Provider getProvider(Class<?> clazz) {
            return providers.get(clazz);
        }
    }
}
