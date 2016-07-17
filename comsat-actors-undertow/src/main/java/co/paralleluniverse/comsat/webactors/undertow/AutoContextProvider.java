package co.paralleluniverse.comsat.webactors.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.util.Sessions;

import java.util.List;
import java.util.Map;

import static co.paralleluniverse.comsat.webactors.undertow.WebActorHandler.ACTOR_KEY;

class AutoContextProvider implements WebActorHandler.ContextProvider {
    private final ClassLoader userClassLoader;
    private final Map<Class<?>, Object[]> actorParams;
    private final Long defaultContextValidityMS;
    private final List<String> packagePrefixes;

    public AutoContextProvider(ClassLoader userClassLoader, List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams) {
        this(userClassLoader, packagePrefixes, actorParams, null);
    }

    public AutoContextProvider(ClassLoader userClassLoader, List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams, Long defaultContextValidityMS) {
        this.userClassLoader = userClassLoader;
        this.packagePrefixes = packagePrefixes;
        this.actorParams = actorParams;
        this.defaultContextValidityMS = defaultContextValidityMS;
    }

    @Override
    public final WebActorHandler.Context get(final HttpServerExchange xch) {
        WebActorHandler.Context actorContext;
        Session session = null;
        try {
            session = Sessions.getOrCreateSession(xch);
        } catch (final IllegalStateException ignored) {
        } // No session handler

        if (session != null) {
            actorContext = (WebActorHandler.Context) session.getAttribute(ACTOR_KEY);
            if (actorContext == null || !actorContext.renew()) {
                actorContext = newContext(xch);
                session.setAttribute(ACTOR_KEY, actorContext);
            }
        } else {
            actorContext = newContext(xch);
        }

        return actorContext;
    }

    private WebActorHandler.Context newContext(final HttpServerExchange xch) {
        final AutoActorContext c = createContext(xch);
        c.init(xch);
        if (defaultContextValidityMS != null)
            c.setValidityMS(defaultContextValidityMS);
        return c;
    }

    protected AutoActorContext createContext(HttpServerExchange xch) {
        return new AutoActorContext(xch, packagePrefixes, actorParams, userClassLoader);
    }

    public ClassLoader getUserClassLoader() {
        return userClassLoader;
    }

    public List<String> getPackagePrefixes() {
        return packagePrefixes;
    }
}
