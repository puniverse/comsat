package co.paralleluniverse.comsat.webactors.undertow;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import javax.inject.Provider;
import java.util.Map;

/**
 * A Guice module which prepares bindings for {@link InjectAutoWebActorHandler}.
 *
 * @author rodedb
 */
public class GuiceInjectedAutoWebActorHandlerModule extends AbstractModule {

    private static final Object INJECTED_VALUE = new Object();

    @Override
    protected void configure() {
        Map<Class<?>, Provider> providers = Maps.newHashMap();
        providers.put(UndertowWebActor.class, getProvider(UndertowWebActorInjected.class));
        InjectAutoWebActorHandler.ProvidersWrapper providersWrapper = new InjectAutoWebActorHandler.ProvidersWrapper(providers);
        InjectAutoWebActorHandler.Settings settings = new InjectAutoWebActorHandler.Settings(providersWrapper);
        bind(InjectAutoWebActorHandler.Settings.class).toInstance(settings);
        bind(WebActorHandler.class).to(InjectAutoWebActorHandler.class);
        bind(Object.class).annotatedWith(Names.named("webActorInjectedValue")).toInstance(INJECTED_VALUE);
    }
}
