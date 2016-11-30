/*
 * COMSAT
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
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

import java.util.List;
import java.util.Map;

/**
 * @author circlespainter
 */
public class AutoWebActorHandler extends WebActorHandler {

    private ClassLoader classLoader;
    private List<String> packagePrefixes;
    private Map<Class<?>, Object[]> actorParams;

    public AutoWebActorHandler() {
        this(null, null, null);
    }

    public AutoWebActorHandler(List<String> packagePrefixes) {
        this(null, packagePrefixes, null);
    }

    public AutoWebActorHandler(ClassLoader userClassLoader, List<String> packagePrefixes) {
        this(userClassLoader, packagePrefixes, null);
    }

    public AutoWebActorHandler(List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams) {
        this(null, packagePrefixes, actorParams);
    }

    public AutoWebActorHandler(ClassLoader userClassLoader, List<String> packagePrefixes, Map<Class<?>, Object[]> actorParams) {
        super(null);
        this.classLoader = userClassLoader;
        this.packagePrefixes = packagePrefixes;
        this.actorParams = actorParams;
    }

    protected void initContextProvider() {
        super.contextProvider = new AutoContextProvider(classLoader != null ? classLoader : ClassLoader.getSystemClassLoader(), packagePrefixes, actorParams);
    }

    public AutoWebActorHandler(AutoContextProvider prov) {
        super(prov);
    }
}
