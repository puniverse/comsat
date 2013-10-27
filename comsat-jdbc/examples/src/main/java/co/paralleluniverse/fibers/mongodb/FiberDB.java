/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.mongodb;

import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBEncoder;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author eitan
 */
class FiberDB extends DB {
    final DB db;
    final ExecutorService executor;

    public FiberDB(DB db) {
        super(db.getMongo(), db.getName());
        this.db = db;
        this.executor = Executors.newCachedThreadPool();
    }

    public DBCollection getCollection(String name) {
        return db.getCollection(name);
    }

    public DBCollection createCollection(String name, DBObject options) {
        return db.createCollection(name, options);
    }

    public DBCollection getCollectionFromString(String s) {
        return db.getCollectionFromString(s);
    }

    public CommandResult command(DBObject cmd) {
        return db.command(cmd);
    }

    public CommandResult command(DBObject cmd, DBEncoder encoder) {
        return db.command(cmd, encoder);
    }

    public CommandResult command(DBObject cmd, int options, DBEncoder encoder) {
        return db.command(cmd, options, encoder);
    }

    public CommandResult command(DBObject cmd, int options, ReadPreference readPrefs) {
        return db.command(cmd, options, readPrefs);
    }

    public CommandResult command(DBObject cmd, int options, ReadPreference readPrefs, DBEncoder encoder) {
        return db.command(cmd, options, readPrefs, encoder);
    }

    public CommandResult command(DBObject cmd, int options) {
        return db.command(cmd, options);
    }

    public CommandResult command(String cmd) {
        return db.command(cmd);
    }

    public CommandResult command(String cmd, int options) {
        return db.command(cmd, options);
    }

    public CommandResult doEval(String code, Object... args) {
        return db.doEval(code, args);
    }

    public Object eval(String code, Object... args) {
        return db.eval(code, args);
    }

    public CommandResult getStats() {
        return db.getStats();
    }

    public String getName() {
        return db.getName();
    }

    public void setReadOnly(Boolean b) {
        db.setReadOnly(b);
    }

    public Set<String> getCollectionNames() {
        return super.getCollectionNames();
    }

    public boolean collectionExists(String collectionName) {
        return db.collectionExists(collectionName);
    }

    public String toString() {
        return db.toString();
    }

    public CommandResult getLastError() {
        return db.getLastError();
    }

    public CommandResult getLastError(WriteConcern concern) {
        return db.getLastError(concern);
    }

    public CommandResult getLastError(int w, int wtimeout, boolean fsync) {
        return db.getLastError(w, wtimeout, fsync);
    }

    public void setWriteConcern(WriteConcern concern) {
        db.setWriteConcern(concern);
    }

    public WriteConcern getWriteConcern() {
        return db.getWriteConcern();
    }

    public void setReadPreference(ReadPreference preference) {
        db.setReadPreference(preference);
    }

    public ReadPreference getReadPreference() {
        return db.getReadPreference();
    }

    public void dropDatabase() {
        db.dropDatabase();
    }

    public boolean isAuthenticated() {
        return db.isAuthenticated();
    }

    public boolean authenticate(String username, char[] password) {
        return db.authenticate(username, password);
    }

    public synchronized CommandResult authenticateCommand(String username, char[] password) {
        return db.authenticateCommand(username, password);
    }

    public WriteResult addUser(String username, char[] passwd) {
        return db.addUser(username, passwd);
    }

    public WriteResult addUser(String username, char[] passwd, boolean readOnly) {
        return db.addUser(username, passwd, readOnly);
    }

    public WriteResult removeUser(String username) {
        return db.removeUser(username);
    }

    public CommandResult getPreviousError() {
        return db.getPreviousError();
    }

    public void resetError() {
        db.resetError();
    }

    public void forceError() {
        db.forceError();
    }

    public Mongo getMongo() {
        return db.getMongo();
    }

    public DB getSisterDB(String name) {
        return db.getSisterDB(name);
    }

    public void slaveOk() {
        db.slaveOk();
    }

    public void addOption(int option) {
        db.addOption(option);
    }

    public void setOptions(int options) {
        db.setOptions(options);
    }

    public void resetOptions() {
        db.resetOptions();
    }

    @Override
    public int getOptions() {
        return db.getOptions();
    }

    public void requestStart() {
        db.requestStart();
    }

    public void requestDone() {
        db.requestDone();
    }

    public void requestEnsureConnection() {
        db.requestEnsureConnection();
    }

    public void cleanCursors(boolean force) {
        db.cleanCursors(force);
    }

    public int hashCode() {
        return db.hashCode();
    }

    public boolean equals(Object obj) {
        return db.equals(obj);
    }

    @Override
    protected DBCollection doGetCollection(String name) {
        return db.getCollection(name);
    }
}
