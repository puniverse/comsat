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
package co.paralleluniverse.comsat.webactors.netty;

import co.paralleluniverse.comsat.webactors.MyWebActor;
import co.paralleluniverse.comsat.webactors.WebActor;

/**
 * @author circlespainter
 */
@WebActor(httpUrlPatterns = {"/*"}, webSocketUrlPatterns = {"/ws"})
public final class NettyWebActor extends MyWebActor {}
