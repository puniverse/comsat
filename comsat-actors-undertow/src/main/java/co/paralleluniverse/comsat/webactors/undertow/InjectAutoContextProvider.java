package co.paralleluniverse.comsat.webactors.undertow;

import io.undertow.server.HttpServerExchange;

import java.util.List;

/**
 * @author rodedb
 */
class InjectAutoContextProvider extends AutoContextProvider {

    private InjectAutoWebActorHandler.ProvidersWrapper providersWrapper;

    public InjectAutoContextProvider(ClassLoader userClassLoader, List<String> packagePrefixes, InjectAutoWebActorHandler.ProvidersWrapper providersWrapper) {
        super(userClassLoader, packagePrefixes, null);
        this.providersWrapper = providersWrapper;
    }

    @Override
    protected AutoActorContext createContext(HttpServerExchange xch) {
        if (providersWrapper == null)
            throw new IllegalArgumentException("ProvidersWrapper not set");
        return new InjectAutoActorContext(xch, getPackagePrefixes(), getUserClassLoader(), providersWrapper);
    }
}
