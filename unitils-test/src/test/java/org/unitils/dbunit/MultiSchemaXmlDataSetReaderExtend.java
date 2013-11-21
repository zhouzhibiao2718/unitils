/*
 * CVS file status:
 * 
 * $Id: MultiSchemaXmlDataSetReaderExtend.java,v 1.1 2010-11-22 12:10:09 jeho Exp $
 * 
 * Copyright (c) Smals
 */
package org.unitils.dbunit;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.dbunit.dataset.DataSetException;
import org.unitils.core.UnitilsException;
import org.unitils.dbunit.util.MultiSchemaDataSet;
import org.unitils.dbunit.util.MultiSchemaXmlDataSetReader;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;


/**
 * A simple extension on th {@link } allowing to work with 
 * inputstreams instead of Files.
 * 
 * @author Thomas De Rycke 
 * 
 * @since 3.4
 * 
 * @see <a href="http://unitils.org/summary.html">Unitils</a>
 */
public class MultiSchemaXmlDataSetReaderExtend extends MultiSchemaXmlDataSetReader {

    private String defaultSchemaName;

    
    /**
     * @return the defaultSchemaName
     */
    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }

    
    /**
     * @param defaultSchemaName the defaultSchemaName to set
     */
    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

    /**
     * @param defaultSchemaName
     */
    public MultiSchemaXmlDataSetReaderExtend(String defaultSchemaName) {
        super(defaultSchemaName);
        this.defaultSchemaName = defaultSchemaName;
    }

    /**
     * Parses the datasets from the given inputStreams.
     * Each schema is given its own dataset and each row is given its own table.
     *
     * @param inputStreams The inputstreams , not null
     * @return The read data set, not null
     * @throws UnitilsException 
     */
    public MultiSchemaDataSet readDataSetXml(List<InputStream> inputStreams) throws UnitilsException {
        DataSetContentHandler dataSetContentHandler;
        XMLReader xmlReader;
        dataSetContentHandler = new DataSetContentHandler(defaultSchemaName);
        xmlReader = createXMLReader();
        xmlReader.setContentHandler(dataSetContentHandler);
        xmlReader.setErrorHandler(dataSetContentHandler);


        InputStream dataSetInputStream;

        for (Iterator<InputStream> iterator = inputStreams.iterator(); iterator.hasNext();) {
            dataSetInputStream = iterator.next();

            try {
                xmlReader.parse(new InputSource(dataSetInputStream));
            } catch (Exception e) {
                throw new UnitilsException("Unable to parse data set xml.", e);
            } 
        }


        MultiSchemaDataSet multiSchemaDataSet = null;
        try {
            multiSchemaDataSet = dataSetContentHandler.getMultiSchemaDataSet();
        } catch (DataSetException e) {
            throw new UnitilsException("Unable to load data set xml.", e);

        }

        return multiSchemaDataSet;
    }


}