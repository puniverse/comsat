package co.paralleluniverse.comsat.webactors.undertow;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

/**
 * A Spring configuration which prepares bindings for {@link InjectAutoWebActorHandler}.
 *
 * @author rodedb
 */
@Configuration
public class SpringInjectedAutoWebActorHandlerConfig {

    private static final Object INJECTED_VALUE = new Object();

    @Inject
    private Provider<UndertowWebActor> undertowWebActorProvider;

    @Bean(name = "webActorInjectedValue")
    public Object webActorInjectedBean() {
        return INJECTED_VALUE;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public UndertowWebActor undertowWebActor() {
        return new UndertowWebActorInjected();
    }

    @Bean
    public WebActorHandler webActor(InjectAutoWebActorHandler.Settings settings) {
        return new InjectAutoWebActorHandler(settings);
    }

    @Bean
    public InjectAutoWebActorHandler.Settings injectedAutoWebActorHandlerSettings() throws Exception {
        Map<Class<?>, Provider> providers = Maps.newHashMap();
        providers.put(UndertowWebActor.class, undertowWebActorProvider);
        InjectAutoWebActorHandler.ProvidersWrapper providersWrapper = new InjectAutoWebActorHandler.ProvidersWrapper(providers);
        return new InjectAutoWebActorHandler.Settings(providersWrapper);
    }
}