/*
 * COMSAT
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.mongodb;

import com.allanbank.mongodb.MongoClient;
import com.allanbank.mongodb.MongoClientConfiguration;
import com.allanbank.mongodb.MongoDbUri;

/**
 *
 * @author circlespainter
 */
public class FiberMongoFactory {
    
    public static MongoClient createClient(MongoClientConfiguration mcc) {
        return new FiberMongoClientImpl(mcc);
    }

    public static MongoClient createClient(MongoDbUri uri) {
        return new FiberMongoClientImpl(new MongoClientConfiguration(uri));
    }

    public static MongoClient createClient(String uri) {
        return new FiberMongoClientImpl(new MongoClientConfiguration(uri));
    }
}
