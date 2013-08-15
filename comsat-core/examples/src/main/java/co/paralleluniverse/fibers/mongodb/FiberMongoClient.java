package co.paralleluniverse.fibers.mongodb;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import java.net.UnknownHostException;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author eitan
 */
public class FiberMongoClient extends MongoClient {
    public FiberMongoClient() throws UnknownHostException {
    }

    @Override
    public DB getDB(String dbname) {
        return new FiberDB(super.getDB(dbname));
    }
}
