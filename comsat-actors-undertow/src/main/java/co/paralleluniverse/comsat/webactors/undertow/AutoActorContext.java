package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorSpec;
import co.paralleluniverse.common.reflection.AnnotationUtil;
import co.paralleluniverse.common.reflection.ClassLoaderUtil;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.comsat.webactors.WebActor;
import co.paralleluniverse.comsat.webactors.WebMessage;
import io.undertow.UndertowLogger;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

class AutoActorContext extends WebActorHandler.DefaultContextImpl {

    private static final List<Class<?>> actorClasses = new ArrayList<>(4);
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private String id;

    private final List<String> packagePrefixes;
    private final Map<Class<?>, Object[]> actorParams;
    private final ClassLoader userClassLoader;
    private Class<? extends ActorImpl<? extends WebMessage>> actorClass;
    private ActorRef<? extends WebMessage> actorRef;

    public AutoActorContext(HttpServerExchange xch, List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams, ClassLoader userClassLoader) {
        this.packagePrefixes = packagePrefixes;
        this.actorParams = actorParams;
        this.userClassLoader = userClassLoader;
    }

    private void fillActor(HttpServerExchange xch) {
        final Pair<ActorRef<? extends WebMessage>, Class<? extends ActorImpl<? extends WebMessage>>> p = autoCreateActor(xch);
        if (p != null) {
            actorRef = p.getFirst();
            actorClass = p.getSecond();
        }
    }

    @Override
    public final String getId() {
        return id != null ? id : (id = UUID.randomUUID().toString());
    }

    @Override
    public final void restart(HttpServerExchange xch) {
        renewed = new Date().getTime();
        fillActor(xch);
    }

    @Override
    public final ActorRef<? extends WebMessage> getWebActor() {
        return actorRef;
    }

    @Override
    public final boolean handlesWithWebSocket(String uri) {
        return WebActorHandler.handlesWithWebSocket(uri, actorClass);
    }

    @Override
    public final boolean handlesWithHttp(String uri) {
        return WebActorHandler.handlesWithHttp(uri, actorClass);
    }

    @Override
    public WatchPolicy watch() {
        return WatchPolicy.DIE_IF_EXCEPTION_ELSE_RESTART;
    }

    @SuppressWarnings("unchecked")
    private Pair<ActorRef<? extends WebMessage>, Class<? extends ActorImpl<? extends WebMessage>>> autoCreateActor(HttpServerExchange xch) {
        registerActorClasses();
        final String uri = xch.getRequestURI();
        for (final Class<?> c : actorClasses) {
            if (WebActorHandler.handlesWithHttp(uri, c) || WebActorHandler.handlesWithWebSocket(uri, c))
                return new Pair<ActorRef<? extends WebMessage>, Class<? extends ActorImpl<? extends WebMessage>>>(
                        spawnActor(c), (Class<? extends ActorImpl<? extends WebMessage>>) c
                );
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected ActorRef spawnActor(Class<?> c) {
        return Actor.newActor(new ActorSpec(c, actorParams != null ? actorParams.get(c) : EMPTY_OBJECT_ARRAY)).spawn();
    }

    private synchronized void registerActorClasses() {
        if (actorClasses.isEmpty()) {
            try {
                final ClassLoader classLoader = userClassLoader != null ? userClassLoader : this.getClass().getClassLoader();
                ClassLoaderUtil.accept((URLClassLoader) classLoader, new ClassLoaderUtil.Visitor() {
                    @Override
                    public final void visit(String resource, URL url, ClassLoader cl) {
                        if (packagePrefixes != null) {
                            boolean found = false;
                            for (final String packagePrefix : packagePrefixes) {
                                if (packagePrefix != null && resource.startsWith(packagePrefix.replace('.', '/'))) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found)
                                return;
                        }
                        if (!ClassLoaderUtil.isClassFile(resource))
                            return;
                        final String className = ClassLoaderUtil.resourceToClass(resource);
                        try (final InputStream is = cl.getResourceAsStream(resource)) {
                            if (AnnotationUtil.hasClassAnnotation(WebActor.class, is))
                                registerWebActor(cl.loadClass(className));
                        } catch (final IOException | ClassNotFoundException e) {
                            UndertowLogger.ROOT_LOGGER.error("Exception while scanning class " + className + " for WebActor annotation", e);
                            throw new RuntimeException(e);
                        }
                    }

                    private void registerWebActor(Class<?> c) {
                        actorClasses.add(c);
                    }
                });
            } catch (final IOException e) {
                UndertowLogger.ROOT_LOGGER.error("IOException while scanning classes for WebActor annotation", e);
            }
        }
    }

    public void init(HttpServerExchange xch) {
        fillActor(xch);
    }
}