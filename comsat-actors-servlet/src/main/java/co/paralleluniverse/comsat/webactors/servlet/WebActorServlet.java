/*
 * COMSAT
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorSpec;
import co.paralleluniverse.actors.LocalActorUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import java.io.IOException;
import java.util.Enumeration;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class WebActorServlet extends HttpServlet {
    static final String ACTOR_KEY = "co.paralleluniverse.actor";
    static final String ACTOR_CLASS_PARAM = "actor";
    static final String ACTOR_PARAM_PREFIX = "actorParam";
    private String redirectPath = null;
    private String actorClassName = null;
    private String[] actorParams = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Enumeration<String> initParameterNames = config.getInitParameterNames();
        SortedMap<Integer, String> map = new TreeMap<>();
        while (initParameterNames.hasMoreElements()) {
            String name = initParameterNames.nextElement();
            if (name.equals("redirectNoSessionPath"))
                setRedirectNoSessionPath(config.getInitParameter(name));
            else if (name.equals(ACTOR_CLASS_PARAM))
                setActorClassName(config.getInitParameter(name));
            else if (name.startsWith(ACTOR_PARAM_PREFIX)) {
                try {
                    int num = Integer.parseInt(name.substring("actorParam".length()));
                    map.put(num, config.getInitParameter(name));
                } catch (NumberFormatException nfe) {
                    getServletContext().log("Wrong actor parameter number: ", nfe);
                }

            }
        }
        actorParams = map.values().toArray(new String[0]);
    }

    public WebActorServlet setRedirectNoSessionPath(String path) {
        this.redirectPath = path;
        return this;
    }

    public WebActorServlet setActorClassName(String className) {
        this.actorClassName = className;
        return this;
    }

    static void attachHttpSession(HttpSession session, ActorRef<Object> actor) {
        session.setAttribute(ACTOR_KEY, actor);
    }

    static boolean isHttpAttached(HttpSession session) {
        return (session.getAttribute(ACTOR_KEY) != null);
    }

    static ActorRef<Object> getHttpAttached(HttpSession session) {
        Object actor = session.getAttribute(ACTOR_KEY);
        if ((actor != null) && (actor instanceof ActorRef))
            return (ActorRef<Object>) actor;
        return null;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        sendToActor(req, resp);
    }

    private void sendToActor(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        ActorRef<Object> actor = getHttpAttached(req.getSession());

        if (actor == null) {
            if (actorClassName != null) {
                try {
                    actor = Actor.newActor(new ActorSpec(Class.forName(actorClassName), actorParams)).spawn();
                    attachHttpSession(req.getSession(), actor);
                } catch (ClassNotFoundException ex) {
                    req.getServletContext().log("Unable to load actorClass: ", ex);
                    return;
                }
            } else if (redirectPath != null) {
                resp.sendRedirect(redirectPath);
                return;
            } else {
                resp.sendError(500, "Actor not found");
            }
        }
        if (LocalActorUtil.isDone(actor)) {
            Throwable deathCause = LocalActorUtil.getDeathCause(actor);
            req.getSession().removeAttribute(ACTOR_KEY);
            if (deathCause != null)
                resp.sendError(500, "Actor is dead because of " + deathCause.getMessage());
            else
                resp.sendError(500, "Actor is finised.");
            return;
        }
        req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
        req.startAsync();
        try {
            actor.send(new ServletHttpRequest(req, resp));
        } catch (SuspendExecution ex) {
            req.getServletContext().log("Exception: ", ex);
        }
    }
}
