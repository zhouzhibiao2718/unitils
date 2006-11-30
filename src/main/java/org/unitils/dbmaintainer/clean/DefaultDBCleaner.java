/*
 * Copyright 2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.unitils.dbmaintainer.clean;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.unitils.core.UnitilsException;
import org.unitils.dbmaintainer.dbsupport.DatabaseTask;
import org.unitils.dbmaintainer.handler.StatementHandlerException;

import java.sql.SQLException;
import java.util.*;

/**
 * Implementation of {@link DBCleaner}. This implementation will delete all data from a database, except for the tables
 * that are configured as tables to preserve. This includes the tables that are listed in the property
 * {@link #PROPKEY_TABLESTOPRESERVE}, {@link #PROPKEY_DBCLEARER_ITEMSTOPRESERVE}. and the table that is configured as
 * version table using the property {@link #PROPKEY_VERSION_TABLE_NAME}. 
 *
 * @author Filip Neven
 */
public class DefaultDBCleaner extends DatabaseTask implements DBCleaner {

    private static final Logger logger = Logger.getLogger(DefaultDBCleaner.class);

    /* Property key for the tables that should not be cleaned */
    public static final String PROPKEY_TABLESTOPRESERVE = "dbMaintainer.cleanDb.tablesToPreserve";

    /* Property key for the tables that should not be cleared (these tables should also not be cleaned */
    public static final String PROPKEY_DBCLEARER_ITEMSTOPRESERVE = "dbMaintainer.clearDb.itemsToPreserve";

    /* The key of the property that specifies the name of the datase table in which the
* DB version is stored. This table should not be deleted */
    public static final String PROPKEY_VERSION_TABLE_NAME = "dbMaintainer.dbVersionSource.tableName";

    /* The tables that should not be cleaned */
    private Set<String> tablesToPreserve;

    /**
     * Configures this object
     *
     * @param configuration
     */
    protected void doInit(Configuration configuration) {
        tablesToPreserve = new HashSet<String>();
        tablesToPreserve.add(configuration.getString(PROPKEY_VERSION_TABLE_NAME).toUpperCase());
        tablesToPreserve.addAll(toUpperCaseList(Arrays.asList(configuration.getStringArray(PROPKEY_TABLESTOPRESERVE))));
        tablesToPreserve.addAll(toUpperCaseList(Arrays.asList(configuration.getStringArray(PROPKEY_DBCLEARER_ITEMSTOPRESERVE))));
    }

    /**
     * Deletes all data from the database, except for the tables that have been
     * configured as <i>tablesToPreserve</i> , and the table in which the database version is stored
     *
     * @throws StatementHandlerException
     */
    public void cleanDatabase() throws StatementHandlerException {
        try {
            logger.info("Cleaning database tables");
            Set<String> tables = dbSupport.getTableNames();
            tables.removeAll(tablesToPreserve);
            clearTables(tables);
        } catch (SQLException e) {
            throw new UnitilsException("Error while cleaning database", e);
        }
    }

    /**
     * Deletes the data in the database tables with the given table names.
     *
     * @param tableNames
     * @throws StatementHandlerException
     */
    private void clearTables(Set<String> tableNames) throws StatementHandlerException, SQLException {
        for (String tableName : tableNames) {
            if (dbSupport.getRecordCount(tableName) > 0) {
                statementHandler.handle("delete from " + tableName);
            }
        }
    }

    /**
     * Converts the given list of strings to uppercase.
     *
     * @param strings
     * @return the given string list, converted to uppercase
     */
    private List<String> toUpperCaseList(List<String> strings) {
        List<String> toUpperCaseList = new ArrayList<String>();
        for (String string : strings) {
            toUpperCaseList.add(string.toUpperCase());
        }
        return toUpperCaseList;
    }
}
