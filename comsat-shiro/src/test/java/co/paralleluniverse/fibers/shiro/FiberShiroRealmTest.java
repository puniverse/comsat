/*
 * COMSAT
 * Copyright (C) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.shiro;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

/**
 * @author rodedb
 */
public class FiberShiroRealmTest {

    @Test
    public void testSingleRealm() throws ExecutionException, InterruptedException {
        final IniSecurityManagerFactory factory = new IniSecurityManagerFactory("classpath:shiro-single-realm.ini");
        final SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);

        final Boolean authed = FiberUtil.runInFiber(new SuspendableCallable<Boolean>() {
            @Override
            public Boolean run() throws SuspendExecution, InterruptedException {
                SecurityUtils.getSubject().login(new UsernamePasswordToken("test", "test"));
                return SecurityUtils.getSubject().isAuthenticated()
                    && SecurityUtils.getSubject().hasRole("roleA")
                    && SecurityUtils.getSubject().isPermitted("resource:actionA");
            }
        });
        assertTrue(authed);
    }

    @Test
    public void testMultiRealm() throws ExecutionException, InterruptedException {
        final IniSecurityManagerFactory factory = new IniSecurityManagerFactory("classpath:shiro-multi-realm.ini");
        final SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);

        final Boolean authed = FiberUtil.runInFiber(new SuspendableCallable<Boolean>() {
            @Override
            public Boolean run() throws SuspendExecution, InterruptedException {
                SecurityUtils.getSubject().login(new UsernamePasswordToken("test", "test"));
                return SecurityUtils.getSubject().isAuthenticated()
                    && SecurityUtils.getSubject().hasRole("roleA")
                    && SecurityUtils.getSubject().isPermitted("resource:actionA");
            }
        });
        assertTrue(authed);
    }
}
