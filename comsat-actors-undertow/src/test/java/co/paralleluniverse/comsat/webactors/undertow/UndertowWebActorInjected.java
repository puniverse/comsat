/*
 * COMSAT
 * Copyright (C) 2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.comsat.webactors.undertow;

import co.paralleluniverse.comsat.webactors.WebActor;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Used to test injection into WebActors.
 *
 * @author rodedb
 */
@WebActor(httpUrlPatterns = {"/*"}, webSocketUrlPatterns = {"/ws"})
public final class UndertowWebActorInjected extends UndertowWebActor {

    private Object injectedValue;

    @Inject
    public void setInjectedValue(@Named("webActorInjectedValue") Object injectedValue) {
        this.injectedValue = injectedValue;
    }

    public Object getInjectedValue() {
        return injectedValue;
    }
}
