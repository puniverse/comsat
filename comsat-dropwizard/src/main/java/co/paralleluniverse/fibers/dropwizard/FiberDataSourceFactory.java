/*
 * COMSAT
 * Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.dropwizard;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.util.Duration;
import java.util.Map;

public class FiberDataSourceFactory extends DataSourceFactory {
    private final DataSourceFactory dsf;

    public FiberDataSourceFactory(DataSourceFactory dsf) {
        this.dsf = dsf;
    }

    @Override
    public ManagedDataSource build(MetricRegistry metricRegistry, String name) throws ClassNotFoundException {
        ManagedDataSource ds = dsf.build(metricRegistry, name);
        return dsf.getUrl().startsWith("jdbc:fiber:")
                ? ds : FiberManagedDataSource.wrap(ds, 10);
    }

    // Delegations
    @Override
    public boolean isAutoCommentsEnabled() {
        return dsf.isAutoCommentsEnabled();
    }

    @Override
    public void setAutoCommentsEnabled(boolean autoCommentsEnabled) {
        dsf.setAutoCommentsEnabled(autoCommentsEnabled);
    }

    @Override
    public String getDriverClass() {
        return dsf.getDriverClass();
    }

    @Override
    public void setDriverClass(String driverClass) {
        dsf.setDriverClass(driverClass);
    }

    @Override
    public String getUser() {
        return dsf.getUser();
    }

    @Override
    public void setUser(String user) {
        dsf.setUser(user);
    }

    @Override
    public String getPassword() {
        return dsf.getPassword();
    }

    @Override
    public void setPassword(String password) {
        dsf.setPassword(password);
    }

    @Override
    public String getUrl() {
        return dsf.getUrl();
    }

    @Override
    public void setUrl(String url) {
        dsf.setUrl(url);
    }

    @Override
    public Map<String, String> getProperties() {
        return dsf.getProperties();
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        dsf.setProperties(properties);
    }

    @Override
    public Duration getMaxWaitForConnection() {
        return dsf.getMaxWaitForConnection();
    }

    @Override
    public void setMaxWaitForConnection(Duration maxWaitForConnection) {
        dsf.setMaxWaitForConnection(maxWaitForConnection);
    }

    @Override
    public String getValidationQuery() {
        return dsf.getValidationQuery();
    }

    @Override
    public void setValidationQuery(String validationQuery) {
        dsf.setValidationQuery(validationQuery);
    }

    @Override
    public int getMinSize() {
        return dsf.getMinSize();
    }

    @Override
    public void setMinSize(int minSize) {
        dsf.setMinSize(minSize);
    }

    @Override
    public int getMaxSize() {
        return dsf.getMaxSize();
    }

    @Override
    public void setMaxSize(int maxSize) {
        dsf.setMaxSize(maxSize);
    }

    @Override
    public boolean getCheckConnectionWhileIdle() {
        return dsf.getCheckConnectionWhileIdle();
    }

    @Override
    public void setCheckConnectionWhileIdle(boolean checkConnectionWhileIdle) {
        dsf.setCheckConnectionWhileIdle(checkConnectionWhileIdle);
    }

    @Override
    public boolean isDefaultReadOnly() {
        return dsf.isDefaultReadOnly();
    }

    @Override
    public void setDefaultReadOnly(boolean defaultReadOnly) {
        dsf.setDefaultReadOnly(defaultReadOnly);
    }

    @Override
    public boolean isMinSizeLessThanMaxSize() {
        return dsf.isMinSizeLessThanMaxSize();
    }

    @Override
    public boolean isInitialSizeLessThanMaxSize() {
        return dsf.isInitialSizeLessThanMaxSize();
    }

    @Override
    public boolean isInitialSizeGreaterThanMinSize() {
        return dsf.isInitialSizeGreaterThanMinSize();
    }

    @Override
    public int getAbandonWhenPercentageFull() {
        return dsf.getAbandonWhenPercentageFull();
    }

    @Override
    public void setAbandonWhenPercentageFull(int percentage) {
        dsf.setAbandonWhenPercentageFull(percentage);
    }

    @Override
    public boolean isAlternateUsernamesAllowed() {
        return dsf.isAlternateUsernamesAllowed();
    }

    @Override
    public void setAlternateUsernamesAllowed(boolean allow) {
        dsf.setAlternateUsernamesAllowed(allow);
    }

    @Override
    public boolean getCommitOnReturn() {
        return dsf.getCommitOnReturn();
    }

    @Override
    public void setCommitOnReturn(boolean commitOnReturn) {
        dsf.setCommitOnReturn(commitOnReturn);
    }

    @Override
    public Boolean getAutoCommitByDefault() {
        return dsf.getAutoCommitByDefault();
    }

    @Override
    public void setAutoCommitByDefault(Boolean autoCommit) {
        dsf.setAutoCommitByDefault(autoCommit);
    }

    @Override
    public String getDefaultCatalog() {
        return dsf.getDefaultCatalog();
    }

    @Override
    public void setDefaultCatalog(String defaultCatalog) {
        dsf.setDefaultCatalog(defaultCatalog);
    }

    @Override
    public Boolean getReadOnlyByDefault() {
        return dsf.getReadOnlyByDefault();
    }

    @Override
    public void setReadOnlyByDefault(Boolean readOnlyByDefault) {
        dsf.setReadOnlyByDefault(readOnlyByDefault);
    }

    @Override
    public TransactionIsolation getDefaultTransactionIsolation() {
        return dsf.getDefaultTransactionIsolation();
    }

    @Override
    public void setDefaultTransactionIsolation(TransactionIsolation isolation) {
        dsf.setDefaultTransactionIsolation(isolation);
    }

    @Override
    public boolean getUseFairQueue() {
        return dsf.getUseFairQueue();
    }

    @Override
    public void setUseFairQueue(boolean fair) {
        dsf.setUseFairQueue(fair);
    }

    @Override
    public int getInitialSize() {
        return dsf.getInitialSize();
    }

    @Override
    public void setInitialSize(int initialSize) {
        dsf.setInitialSize(initialSize);
    }

    @Override
    public String getInitializationQuery() {
        return dsf.getInitializationQuery();
    }

    @Override
    public void setInitializationQuery(String query) {
        dsf.setInitializationQuery(query);
    }

    @Override
    public boolean getLogAbandonedConnections() {
        return dsf.getLogAbandonedConnections();
    }

    @Override
    public void setLogAbandonedConnections(boolean log) {
        dsf.setLogAbandonedConnections(log);
    }

    @Override
    public boolean getLogValidationErrors() {
        return dsf.getLogValidationErrors();
    }

    @Override
    public void setLogValidationErrors(boolean log) {
        dsf.setLogValidationErrors(log);
    }

    @Override
    public Optional<Duration> getMaxConnectionAge() {
        return dsf.getMaxConnectionAge();
    }

    @Override
    public void setMaxConnectionAge(Duration age) {
        dsf.setMaxConnectionAge(age);
    }

    @Override
    public Duration getMinIdleTime() {
        return dsf.getMinIdleTime();
    }

    @Override
    public void setMinIdleTime(Duration time) {
        dsf.setMinIdleTime(time);
    }

    @Override
    public boolean getCheckConnectionOnBorrow() {
        return dsf.getCheckConnectionOnBorrow();
    }

    @Override
    public void setCheckConnectionOnBorrow(boolean checkConnectionOnBorrow) {
        dsf.setCheckConnectionOnBorrow(checkConnectionOnBorrow);
    }

    @Override
    public boolean getCheckConnectionOnConnect() {
        return dsf.getCheckConnectionOnConnect();
    }

    @Override
    public void setCheckConnectionOnConnect(boolean checkConnectionOnConnect) {
        dsf.setCheckConnectionOnConnect(checkConnectionOnConnect);
    }

    @Override
    public boolean getCheckConnectionOnReturn() {
        return dsf.getCheckConnectionOnReturn();
    }

    @Override
    public void setCheckConnectionOnReturn(boolean checkConnectionOnReturn) {
        dsf.setCheckConnectionOnReturn(checkConnectionOnReturn);
    }

    @Override
    public Duration getEvictionInterval() {
        return dsf.getEvictionInterval();
    }

    @Override
    public void setEvictionInterval(Duration interval) {
        dsf.setEvictionInterval(interval);
    }

    @Override
    public Duration getValidationInterval() {
        return dsf.getValidationInterval();
    }

    @Override
    public void setValidationInterval(Duration validationInterval) {
        dsf.setValidationInterval(validationInterval);
    }
}
