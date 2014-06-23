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
import com.allanbank.mongodb.MongoDatabase;
import com.allanbank.mongodb.client.Client;
import com.allanbank.mongodb.client.ClientImpl;
import com.allanbank.mongodb.client.MongoClientImpl;
import com.allanbank.mongodb.client.SerialClientImpl;

/**
 * @author circlespainter
 */
public class FiberMongoClientImpl extends MongoClientImpl {

    public FiberMongoClientImpl(Client client) {
        super(client);
    }
    
    public FiberMongoClientImpl(MongoClientConfiguration mcc) {
        super(mcc);
    }
    
    @Override
    public MongoDatabase getDatabase(String name) {
        return new FiberMongoDatabaseImpl(this, getClient(), name);
    }
    
    @Override
    public MongoClient asSerializedClient() {
        // TODO Make more robust
        return new FiberMongoClientImpl(new SerialClientImpl((ClientImpl) getClient()));
    }
}
