/*
 * Copyright 2008,  Unitils.org
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
package org.unitils.dbmaintainer.structure;

import static org.apache.commons.lang.StringUtils.deleteWhitespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbmaintain.config.DbMaintainConfigurationLoader;
import org.dbmaintain.config.PropertiesDbMaintainConfigurer;
import org.dbmaintain.dbsupport.DbSupport;
import org.dbmaintain.launch.DbMaintain;
import org.dbmaintain.util.DbMaintainException;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.unitils.UnitilsJUnit4;
import org.unitils.core.ConfigurationLoader;
import org.unitils.core.util.ConfigUtils;
import static org.unitils.database.SQLUnitils.executeUpdate;
import org.unitils.dbmaintainer.structure.impl.XsdDataSetStructureGenerator;
import static org.unitils.database.DatabaseModule.PROPKEY_DATABASE_DIALECT;
import static org.unitils.thirdparty.org.apache.commons.io.FileUtils.deleteDirectory;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;
import static org.unitils.thirdparty.org.apache.commons.io.IOUtils.closeQuietly;
import org.unitils.util.PropertyUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;

/**
 * Test class for the {@link XsdDataSetStructureGenerator} for a single schema.
 * <p/>
 * Currently this is only implemented for HsqlDb.
 *
 * @author Tim Ducheyne
 * @author Filip Neven
 */
public class XsdDataSetStructureGeneratorTest extends UnitilsJUnit4 {

    /* The logger instance for this class */
    private static Log logger = LogFactory.getLog(XsdDataSetStructureGeneratorTest.class);

    /* Tested object */
    private DataSetStructureGenerator dataSetStructureGenerator;

    /* The target directory for the test xsd files */
    private File xsdDirectory;

    /* True if current test is not for the current dialect */
    private boolean disabled;

    DbSupport dbSupport;

    DbMaintain dbMaintain;


    /**
     * Initializes the test by creating following tables in the test database:
     * tableOne(columnA not null, columnB not null, columnC) and
     * tableTwo(column1, column2)
     */
    @Before
    public void setUp() throws Exception {
        Properties configuration = new ConfigurationLoader().loadConfiguration();
        this.disabled = !"hsqldb".equals(PropertyUtils.getString(PROPKEY_DATABASE_DIALECT, configuration));
        if (disabled) {
            return;
        }

        xsdDirectory = new File(System.getProperty("java.io.tmpdir"), "XmlSchemaDatabaseStructureGeneratorTest");
        if (xsdDirectory.exists()) {
            deleteDirectory(xsdDirectory);
        }
        xsdDirectory.mkdirs();

        configuration.setProperty(DataSetStructureGenerator.class.getName() + ".implClassName", XsdDataSetStructureGenerator.class.getName());
        configuration.setProperty(XsdDataSetStructureGenerator.PROPKEY_XSD_DIR_NAME, xsdDirectory.getPath());

        Properties dbMaintainProperties = new DbMaintainConfigurationLoader().loadDefaultConfiguration();
        dbMaintainProperties.putAll(configuration);
        PropertiesDbMaintainConfigurer propertiesDbMaintainConfigurer = new PropertiesDbMaintainConfigurer(dbMaintainProperties, new org.dbmaintain.dbsupport.impl.DefaultSQLHandler());
        dbSupport = propertiesDbMaintainConfigurer.getDefaultDbSupport();
        dbMaintain = new DbMaintain(propertiesDbMaintainConfigurer);

        dataSetStructureGenerator = ConfigUtils.getConfiguredInstanceOf(DataSetStructureGenerator.class, configuration);

        clearDatabase();
        createTestTables();
    }


    /**
     * Clean-up test database.
     */
    @After
    public void tearDown() throws Exception {
        if (disabled) {
            return;
        }
        clearDatabase();
        try {
            deleteDirectory(xsdDirectory);
        } catch (Exception e) {
            // ignore
        }
    }


    /**
     * Tests the generation of the xsd files for 1 database schema.
     */
    @Test
    public void testGenerateDataSetStructure() throws Exception {
        if (disabled) {
            logger.warn("Test is not for current dialect. Skipping test.");
            return;
        }
        dataSetStructureGenerator.generateDataSetStructure();

        // check content of general dataset xsd
        File dataSetXsd = new File(xsdDirectory, "dataset.xsd");
        assertFileContains("<xsd:import namespace=\"PUBLIC\" schemaLocation=\"PUBLIC.xsd\" />", dataSetXsd);
        assertFileContains("xmlns:dflt=\"PUBLIC\"", dataSetXsd);
        assertFileContains("<xsd:element name=\"TABLE_1\" type=\"dflt:TABLE_1__type\" />", dataSetXsd);
        assertFileContains("<xsd:element name=\"TABLE_2\" type=\"dflt:TABLE_2__type\" />", dataSetXsd);
        assertFileContains("<xsd:any namespace=\"PUBLIC\" />", dataSetXsd);

        // check content of PUBLIC schema dataset xsd
        File publicSchemaDataSetXsd = new File(xsdDirectory, "PUBLIC.xsd");
        assertFileContains("xmlns=\"PUBLIC\"", publicSchemaDataSetXsd);
        assertFileContains("targetNamespace=\"PUBLIC\"", publicSchemaDataSetXsd);
        assertFileContains("<xsd:element name=\"TABLE_1\" type=\"TABLE_1__type\" />", publicSchemaDataSetXsd);
        assertFileContains("<xsd:complexType name=\"TABLE_1__type\">", publicSchemaDataSetXsd);
        assertFileContains("<xsd:element name=\"TABLE_2\" type=\"TABLE_2__type\" />", publicSchemaDataSetXsd);
        assertFileContains("<xsd:complexType name=\"TABLE_2__type\">", publicSchemaDataSetXsd);
    }


    /**
     * Creates the test tables.
     */
    private void createTestTables() {
        executeUpdate("create table TABLE_1(columnA int not null identity, columnB varchar(1) not null, columnC varchar(1))", dbSupport.getDataSource());
        executeUpdate("create table TABLE_2(column1 varchar(1), column2 varchar(1))", dbSupport.getDataSource());
    }


    /**
     * Removes the test database tables
     */
    private void clearDatabase() {
        dbMaintain.clearDatabase();
        try {
            dbSupport.dropTable(dbSupport.getDefaultSchemaName(), "DBMAINTAIN_SCRIPTS");
        } catch (DbMaintainException e) {
            // Ignored, thrown if DBMAINTAIN_SCRIPTS doesn't exists, which is not a problem
        }
    }


    /**
     * Asserts that the contents of the given file contains the given string.
     *
     * @param expectedContent The string, not null
     * @param file            The file, not null
     */
    private void assertFileContains(String expectedContent, File file) throws Exception {
        Reader reader = null;
        try {
            assertTrue("Expected file does not exist. File name: " + file.getPath(), file.exists());

            reader = new BufferedReader(new FileReader(file));
            String content = IOUtils.toString(reader);
            assertTrue(content + "\ndid not contain\n" + expectedContent, deleteWhitespace(content).contains(deleteWhitespace(expectedContent)));

        } finally {
            closeQuietly(reader);
        }
    }
}
