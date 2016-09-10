package co.paralleluniverse.comsat.webactors.undertow;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.inject.Provider;

import static org.junit.Assert.assertSame;

/**
 * @author rodedb
 */
public class WebActorInjectionTest {

    @SuppressWarnings("unchecked")
    @Test
    public void guiceWebActorInjection() {
        Injector injector = Guice.createInjector(new GuiceInjectedAutoWebActorHandlerModule());
        InjectAutoWebActorHandler.Settings settings = injector.getInstance(InjectAutoWebActorHandler.Settings.class);
        Provider<UndertowWebActor> webActorProvider = settings.getProvidersWrapper().getProvider(UndertowWebActor.class);
        UndertowWebActorInjected webActor = (UndertowWebActorInjected) webActorProvider.get();
        Object injectedValue = injector.getInstance(Key.get(Object.class, Names.named("webActorInjectedValue")));
        assertSame(webActor.getInjectedValue(), injectedValue);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void springWebActorInjection() {
        ApplicationContext context = new AnnotationConfigApplicationContext(SpringInjectedAutoWebActorHandlerConfig.class);
        InjectAutoWebActorHandler.Settings settings = context.getBean(InjectAutoWebActorHandler.Settings.class);
        Provider<UndertowWebActor> webActorProvider = settings.getProvidersWrapper().getProvider(UndertowWebActor.class);
        UndertowWebActorInjected webActor = (UndertowWebActorInjected) webActorProvider.get();
        Object injectedValue = context.getBean("webActorInjectedValue");
        assertSame(webActor.getInjectedValue(), injectedValue);
    }
}