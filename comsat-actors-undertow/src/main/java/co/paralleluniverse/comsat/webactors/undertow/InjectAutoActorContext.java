package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import io.undertow.server.HttpServerExchange;

import java.util.List;

/**
 * @author rodedb
 */
class InjectAutoActorContext extends AutoActorContext {

    private InjectAutoWebActorHandler.ProvidersWrapper providersWrapper;

    public InjectAutoActorContext(HttpServerExchange xch,
                                  List<String> packagePrefixes,
                                  ClassLoader userClassLoader,
                                  InjectAutoWebActorHandler.ProvidersWrapper providersWrapper) {
        super(xch, packagePrefixes, null, userClassLoader);
        this.providersWrapper = providersWrapper;
    }

    @Override
    protected ActorRef spawnActor(Class<?> c) {
        BasicActor actor = (BasicActor) providersWrapper.getProvider(c).get();
        return actor.spawn();
    }
}
