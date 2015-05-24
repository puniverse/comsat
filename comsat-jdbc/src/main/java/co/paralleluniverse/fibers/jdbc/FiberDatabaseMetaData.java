/*
 * COMSAT
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.jdbc;

import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.fibers.Suspendable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

/**
 * @author crclespainter
 */
class FiberDatabaseMetaData implements DatabaseMetaData {
    private final DatabaseMetaData dbMeta;
    private final ExecutorService executor;

    public FiberDatabaseMetaData(final DatabaseMetaData dbMeta, final ExecutorService executor) {
        this.dbMeta = dbMeta;
        this.executor = executor;
    }

    @Override
    @Suspendable
    public boolean allProceduresAreCallable() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.allProceduresAreCallable();
            }
        });
    }

    @Override
    @Suspendable
    public boolean allTablesAreSelectable() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.allTablesAreSelectable();
            }
        });
    }

    @Override
    @Suspendable
    public String getURL() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getURL();
            }
        });
    }

    @Override
    @Suspendable
    public String getUserName() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getUserName();
            }
        });
    }

    @Override
    @Suspendable
    public boolean isReadOnly() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.isReadOnly();
            }
        });
    }

    @Override
    @Suspendable
    public boolean nullsAreSortedHigh() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.nullsAreSortedHigh();
            }
        });
    }

    @Override
    @Suspendable
    public boolean nullsAreSortedLow() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.nullsAreSortedLow();
            }
        });
    }

    @Override
    @Suspendable
    public boolean nullsAreSortedAtStart() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.nullsAreSortedAtStart();
            }
        });
    }

    @Override
    @Suspendable
    public boolean nullsAreSortedAtEnd() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.nullsAreSortedAtEnd();
            }
        });
    }

    @Override
    @Suspendable
    public String getDatabaseProductName() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getDatabaseProductName();
            }
        });
    }

    @Override
    @Suspendable
    public String getDatabaseProductVersion() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getDatabaseProductVersion();
            }
        });
    }

    @Override
    @Suspendable
    public String getDriverName() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getDriverName();
            }
        });
    }

    @Override
    @Suspendable
    public String getDriverVersion() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getDriverVersion();
            }
        });
    }

    @Override
    public int getDriverMajorVersion() {
        return dbMeta.getDriverMajorVersion();
    }

    @Override
    public int getDriverMinorVersion() {
        return dbMeta.getDriverMinorVersion();
    }

    @Override
    @Suspendable
    public boolean usesLocalFiles() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.usesLocalFiles();
            }
        });
    }

    @Override
    @Suspendable
    public boolean usesLocalFilePerTable() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.usesLocalFilePerTable();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsMixedCaseIdentifiers();
            }
        });
    }

    @Override
    @Suspendable
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.storesUpperCaseIdentifiers();
            }
        });
    }

    @Override
    @Suspendable
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.storesLowerCaseIdentifiers();
            }
        });
    }

    @Override
    @Suspendable
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.storesMixedCaseIdentifiers();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsMixedCaseQuotedIdentifiers();
            }
        });
    }

    @Override
    @Suspendable
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.storesUpperCaseQuotedIdentifiers();
            }
        });
    }

    @Override
    @Suspendable
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.storesLowerCaseQuotedIdentifiers();
            }
        });
    }

    @Override
    @Suspendable
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.storesMixedCaseQuotedIdentifiers();
            }
        });
    }

    @Override
    @Suspendable
    public String getIdentifierQuoteString() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getIdentifierQuoteString();
            }
        });
    }

    @Override
    @Suspendable
    public String getSQLKeywords() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getSQLKeywords();
            }
        });
    }

    @Override
    @Suspendable
    public String getNumericFunctions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getNumericFunctions();
            }
        });
    }

    @Override
    @Suspendable
    public String getStringFunctions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getStringFunctions();
            }
        });
    }

    @Override
    @Suspendable
    public String getSystemFunctions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getSystemFunctions();
            }
        });
    }

    @Override
    @Suspendable
    public String getTimeDateFunctions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getTimeDateFunctions();
            }
        });
    }

    @Override
    @Suspendable
    public String getSearchStringEscape() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getSearchStringEscape();
            }
        });
    }

    @Override
    @Suspendable
    public String getExtraNameCharacters() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getExtraNameCharacters();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsAlterTableWithAddColumn();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsAlterTableWithDropColumn();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsColumnAliasing() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsColumnAliasing();
            }
        });
    }

    @Override
    @Suspendable
    public boolean nullPlusNonNullIsNull() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.nullPlusNonNullIsNull();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsConvert() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsConvert();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsConvert(final int fromType, final int toType) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsConvert(fromType, toType);
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsTableCorrelationNames() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsTableCorrelationNames();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsDifferentTableCorrelationNames();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsExpressionsInOrderBy();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsOrderByUnrelated() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsOrderByUnrelated();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsGroupBy() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsGroupBy();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsGroupByUnrelated() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsGroupByUnrelated();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsGroupByBeyondSelect();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsLikeEscapeClause() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsLikeEscapeClause();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsMultipleResultSets() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsMultipleResultSets();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsMultipleTransactions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsMultipleTransactions();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsNonNullableColumns() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsNonNullableColumns();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsMinimumSQLGrammar();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsCoreSQLGrammar() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsCoreSQLGrammar();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsExtendedSQLGrammar();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsANSI92EntryLevelSQL();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsANSI92IntermediateSQL();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsANSI92FullSQL() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsANSI92FullSQL();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsIntegrityEnhancementFacility();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsOuterJoins() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsOuterJoins();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsFullOuterJoins() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsFullOuterJoins();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsLimitedOuterJoins() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsFullOuterJoins();
            }
        });
    }

    @Override
    @Suspendable
    public String getSchemaTerm() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getSchemaTerm();
            }
        });
    }

    @Override
    @Suspendable
    public String getProcedureTerm() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getProcedureTerm();
            }
        });
    }

    @Override
    @Suspendable
    public String getCatalogTerm() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getCatalogTerm();
            }
        });
    }

    @Override
    @Suspendable
    public boolean isCatalogAtStart() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.isCatalogAtStart();
            }
        });
    }

    @Override
    @Suspendable
    public String getCatalogSeparator() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<String, SQLException>() {
            @Override
            public String call() throws SQLException {
                return dbMeta.getCatalogSeparator();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSchemasInDataManipulation();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSchemasInProcedureCalls();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSchemasInTableDefinitions();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSchemasInIndexDefinitions();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSchemasInPrivilegeDefinitions();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsCatalogsInDataManipulation();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsCatalogsInProcedureCalls();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsCatalogsInTableDefinitions();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsCatalogsInIndexDefinitions();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsCatalogsInPrivilegeDefinitions();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsPositionedDelete() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsPositionedDelete();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsPositionedUpdate() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsPositionedUpdate();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsSelectForUpdate() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSelectForUpdate();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsStoredProcedures() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsStoredProcedures();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSubqueriesInComparisons();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsSubqueriesInExists() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSubqueriesInExists();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsSubqueriesInIns() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSubqueriesInIns();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSubqueriesInQuantifieds();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsCorrelatedSubqueries();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsUnion() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsUnion();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsUnionAll() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsUnionAll();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsOpenCursorsAcrossCommit();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsOpenCursorsAcrossRollback();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsOpenStatementsAcrossCommit();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsOpenStatementsAcrossRollback();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxBinaryLiteralLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxBinaryLiteralLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxCharLiteralLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxCharLiteralLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxColumnNameLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxColumnNameLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxColumnsInGroupBy() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxColumnsInGroupBy();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxColumnsInIndex() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxColumnsInIndex();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxColumnsInOrderBy() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxColumnsInOrderBy();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxColumnsInSelect() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxColumnsInSelect();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxColumnsInTable() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxColumnsInTable();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxConnections() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxConnections();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxCursorNameLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxCursorNameLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxIndexLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxIndexLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxSchemaNameLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxSchemaNameLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxProcedureNameLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxProcedureNameLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxCatalogNameLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxCatalogNameLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxRowSize() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxRowSize();
            }
        });
    }

    @Override
    @Suspendable
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.doesMaxRowSizeIncludeBlobs();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxStatementLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxStatementLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxStatements() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxStatements();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxTableNameLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxTableNameLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxTablesInSelect() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxTablesInSelect();
            }
        });
    }

    @Override
    @Suspendable
    public int getMaxUserNameLength() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getMaxUserNameLength();
            }
        });
    }

    @Override
    @Suspendable
    public int getDefaultTransactionIsolation() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getDefaultTransactionIsolation();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsTransactions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsTransactions();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsTransactionIsolationLevel(final int level) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsTransactionIsolationLevel(level);
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsDataDefinitionAndDataManipulationTransactions();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsDataManipulationTransactionsOnly();
            }
        });
    }

    @Override
    @Suspendable
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.dataDefinitionCausesTransactionCommit();
            }
        });
    }

    @Override
    @Suspendable
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.dataDefinitionIgnoredInTransactions();
            }
        });
    }

    @Override
    @Suspendable
    public FiberResultSet getProcedures(final String catalog, final String schemaPattern, final String procedureNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getProcedures(catalog, schemaPattern, procedureNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getProcedureColumns(final String catalog, final String schemaPattern, final String procedureNamePattern, final String columnNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getTables(final String catalog, final String schemaPattern, final String tableNamePattern, final String[] types) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getTables(catalog, schemaPattern, tableNamePattern, types);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getSchemas() throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getSchemas();
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getCatalogs() throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getCatalogs();
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getTableTypes() throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getTableTypes();
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getColumns(final String catalog, final String schemaPattern, final String tableNamePattern, final String columnNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getColumnPrivileges(final String catalog, final String schema, final String table, final String columnNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getColumnPrivileges(catalog, schema, table, columnNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getTablePrivileges(final String catalog, final String schemaPattern, final String tableNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getTablePrivileges(catalog, schemaPattern, tableNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getBestRowIdentifier(final String catalog, final String schema, final String table, final int scope, final boolean nullable) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getBestRowIdentifier(catalog, schema, table, scope, nullable);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getVersionColumns(final String catalog, final String schema, final String table) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getVersionColumns(catalog, schema, table);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getPrimaryKeys(final String catalog, final String schema, final String table) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getPrimaryKeys(catalog, schema, table);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getImportedKeys(final String catalog, final String schema, final String table) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getImportedKeys(catalog, schema, table);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getExportedKeys(final String catalog, final String schema, final String table) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getExportedKeys(catalog, schema, table);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getCrossReference(final String parentCatalog, final String parentSchema, final String parentTable, final String foreignCatalog, final String foreignSchema, final String foreignTable) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getTypeInfo() throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getTypeInfo();
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getIndexInfo(final String catalog, final String schema, final String table, final boolean unique, final boolean approximate) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getIndexInfo(catalog, schema, table, unique, approximate);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public boolean supportsResultSetType(final int type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsResultSetType(type);
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsResultSetConcurrency(final int type, final int concurrency) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsResultSetConcurrency(type, concurrency);
            }
        });
    }

    @Override
    @Suspendable
    public boolean ownUpdatesAreVisible(final int type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.ownUpdatesAreVisible(type);
            }
        });
    }

    @Override
    @Suspendable
    public boolean ownDeletesAreVisible(final int type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.ownDeletesAreVisible(type);
            }
        });
    }

    @Override
    @Suspendable
    public boolean ownInsertsAreVisible(final int type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.ownInsertsAreVisible(type);
            }
        });
    }

    @Override
    @Suspendable
    public boolean othersUpdatesAreVisible(final int type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.othersUpdatesAreVisible(type);
            }
        });
    }

    @Override
    @Suspendable
    public boolean othersDeletesAreVisible(final int type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.othersDeletesAreVisible(type);
            }
        });
    }

    @Override
    @Suspendable
    public boolean othersInsertsAreVisible(final int type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.othersInsertsAreVisible(type);
            }
        });
    }

    @Override
    @Suspendable
    public boolean updatesAreDetected(final int type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.updatesAreDetected(type);
            }
        });
    }

    @Override
    @Suspendable
    public boolean deletesAreDetected(final int type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.deletesAreDetected(type);
            }
        });
    }

    @Override
    @Suspendable
    public boolean insertsAreDetected(final int type) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.insertsAreDetected(type);
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsBatchUpdates() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsBatchUpdates();
            }
        });
    }

    @Override
    @Suspendable
    public FiberResultSet getUDTs(final String catalog, final String schemaPattern, final String typeNamePattern, final int[] types) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getUDTs(catalog, schemaPattern, typeNamePattern, types);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberConnection getConnection() throws SQLException {
        final Connection conn = JDBCFiberAsync.exec(executor, new CheckedCallable<Connection, SQLException>() {
            @Override
            public Connection call() throws SQLException {
                return dbMeta.getConnection();
            }
        });
        return new FiberConnection(conn, executor);
    }

    @Override
    @Suspendable
    public boolean supportsSavepoints() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsSavepoints();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsNamedParameters() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsNamedParameters();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsMultipleOpenResults() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsMultipleOpenResults();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsGetGeneratedKeys() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsGetGeneratedKeys();
            }
        });
    }

    @Override
    @Suspendable
    public FiberResultSet getSuperTypes(final String catalog, final String schemaPattern, final String typeNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getSuperTypes(catalog, schemaPattern, typeNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getSuperTables(final String catalog, final String schemaPattern, final String tableNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getSuperTables(catalog, schemaPattern, tableNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getAttributes(final String catalog, final String schemaPattern, final String typeNamePattern, final String attributeNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public boolean supportsResultSetHoldability(final int holdability) throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsResultSetHoldability(holdability);
            }
        });
    }

    @Override
    @Suspendable
    public int getResultSetHoldability() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getResultSetHoldability();
            }
        });
    }

    @Override
    @Suspendable
    public int getDatabaseMajorVersion() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getDatabaseMajorVersion();
            }
        });
    }

    @Override
    @Suspendable
    public int getDatabaseMinorVersion() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getDatabaseMinorVersion();
            }
        });
    }

    @Override
    @Suspendable
    public int getJDBCMajorVersion() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getJDBCMajorVersion();
            }
        });
    }

    @Override
    @Suspendable
    public int getJDBCMinorVersion() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getJDBCMinorVersion();
            }
        });
    }

    @Override
    @Suspendable
    public int getSQLStateType() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Integer, SQLException>() {
            @Override
            public Integer call() throws SQLException {
                return dbMeta.getSQLStateType();
            }
        });
    }

    @Override
    @Suspendable
    public boolean locatorsUpdateCopy() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.locatorsUpdateCopy();
            }
        });
    }

    @Override
    @Suspendable
    public boolean supportsStatementPooling() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsStatementPooling();
            }
        });
    }

    @Override
    @Suspendable
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<RowIdLifetime, SQLException>() {
            @Override
            public RowIdLifetime call() throws SQLException {
                return dbMeta.getRowIdLifetime();
            }
        });
    }

    @Override
    @Suspendable
    public FiberResultSet getSchemas(final String catalog, final String schemaPattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getSchemas(catalog, schemaPattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.supportsStoredFunctionsUsingCallSyntax();
            }
        });
    }

    @Override
    @Suspendable
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.autoCommitFailureClosesAllResultSets();
            }
        });
    }

    @Override
    @Suspendable
    public FiberResultSet getClientInfoProperties() throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getClientInfoProperties();
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getFunctions(final String catalog, final String schemaPattern, final String functionNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getFunctions(catalog, schemaPattern, functionNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getFunctionColumns(final String catalog, final String schemaPattern, final String functionNamePattern, final String columnNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public FiberResultSet getPseudoColumns(final String catalog, final String schemaPattern, final String tableNamePattern, final String columnNamePattern) throws SQLException {
        final ResultSet result = JDBCFiberAsync.exec(executor, new CheckedCallable<ResultSet, SQLException>() {
            @Override
            public ResultSet call() throws SQLException {
                return dbMeta.getPseudoColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
            }
        });
        return new FiberResultSet(result, executor);
    }

    @Override
    @Suspendable
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return JDBCFiberAsync.exec(executor, new CheckedCallable<Boolean, SQLException>() {
            @Override
            public Boolean call() throws SQLException {
                return dbMeta.generatedKeyAlwaysReturned();
            }
        });
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        return dbMeta.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return dbMeta.isWrapperFor(iface);
    }
}
