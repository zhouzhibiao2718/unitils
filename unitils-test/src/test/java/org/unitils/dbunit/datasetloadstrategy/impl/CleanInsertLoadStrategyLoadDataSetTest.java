/*
 * Copyright 2013,  Unitils.org
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
package org.unitils.dbunit.datasetloadstrategy.impl;

import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DeleteAllOperation;
import org.dbunit.operation.InsertOperation;
import org.junit.Before;
import org.junit.Test;
import org.unitils.core.UnitilsException;
import org.unitils.dbunit.connection.DbUnitDatabaseConnection;
import org.unitils.mock.Mock;
import org.unitils.mock.annotation.Dummy;
import org.unitilsnew.UnitilsJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Tim Ducheyne
 */
public class CleanInsertLoadStrategyLoadDataSetTest extends UnitilsJUnit4 {

    /* Tested object */
    private CleanInsertLoadStrategy cleanInsertLoadStrategy;

    private Mock<DeleteAllOperation> deleteAllOperationMock;
    private Mock<InsertOperation> insertOperationMock;

    @Dummy
    private DbUnitDatabaseConnection dbUnitDatabaseConnection;
    @Dummy
    private IDataSet dataSet;


    @Before
    public void initialize() {
        cleanInsertLoadStrategy = new CleanInsertLoadStrategy(deleteAllOperationMock.getMock(), insertOperationMock.getMock());
    }


    @Test
    public void loadDataSet() throws Exception {
        cleanInsertLoadStrategy.loadDataSet(dbUnitDatabaseConnection, dataSet);

        deleteAllOperationMock.assertInvokedInSequence().execute(dbUnitDatabaseConnection, dataSet);
        insertOperationMock.assertInvokedInSequence().execute(dbUnitDatabaseConnection, dataSet);
    }

    @Test
    public void unitilsExceptionWhenDeleteFails() throws Exception {
        deleteAllOperationMock.raises(new NullPointerException("expected")).execute(dbUnitDatabaseConnection, dataSet);
        try {
            cleanInsertLoadStrategy.loadDataSet(dbUnitDatabaseConnection, dataSet);
            fail("UnitilsException expected");
        } catch (UnitilsException e) {
            assertEquals("Unable to clean insert data set.\nReason: NullPointerException: expected", e.getMessage());
        }
    }

    @Test
    public void unitilsExceptionWhenInsertFails() throws Exception {
        insertOperationMock.raises(new NullPointerException("expected")).execute(dbUnitDatabaseConnection, dataSet);
        try {
            cleanInsertLoadStrategy.loadDataSet(dbUnitDatabaseConnection, dataSet);
            fail("UnitilsException expected");
        } catch (UnitilsException e) {
            assertEquals("Unable to clean insert data set.\nReason: NullPointerException: expected", e.getMessage());
        }
    }
}