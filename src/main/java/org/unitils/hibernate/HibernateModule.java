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
package org.unitils.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.unitils.core.Module;
import org.unitils.core.TestListener;
import org.unitils.core.Unitils;
import org.unitils.core.UnitilsException;
import org.unitils.database.DatabaseModule;
import org.unitils.hibernate.annotation.HibernateConfiguration;
import org.unitils.hibernate.annotation.HibernateSession;
import org.unitils.hibernate.annotation.HibernateTest;
import org.unitils.util.AnnotationUtils;
import org.unitils.util.ReflectionUtils;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

/**
 * Module providing support for unit tests for code that uses Hibernate. This involves unit tests for queries to the
 * database and the Hibernate mapping test implemented by {@link HibernateAssert}
 * <p/>
 * This module depends on the {@link DatabaseModule}, for managing connections to a unit test database.
 * <p/>
 * The configuration of Hibernate is performed by loading all configuration files associated with the property key
 * {@link #PROPKEY_HIBERNATE_CONFIGFILES}, using the Hibernate configuration class associated with the property key
 * {@link #PROPKEY_HIBERNATE_CONFIGURATION_CLASS}. Support for programmatic configuration is also foreseen: all methods
 * annotated with {@link HibernateConfiguration} will be invoked with the Hibernate <code>Configuration</code> object
 * as parameters, before the Hibernate <code>SesssionFactory</code> is constructed.
 * <p/>
 * A Hibernate <code>Session</code> is created before each test setup, and closed after eacht test's teardown.
 * This session will connect to the unit test database as configured by the {@link DatabaseModule}. The session will be
 * injected to fields or methods annotated with {@link HibernateSession} before each test setup. This way, the Hibernate
 * session can easily be injected into the tested environment.
 * <p/>
 * It is highly recommended to write a unit test that invokes {@link org.unitils.hibernate.HibernateAssert#assertMappingToDatabase()},
 * This is a very powerful test that verifies if the mapping of all your Hibernate mapped objects with the database is
 * correct.
 *
 * @author Filip Neven
 */
public class HibernateModule implements Module {

    /* Property key for a comma seperated list of Hibernate configuration files, that can be found in the classpath */
    public static final String PROPKEY_HIBERNATE_CONFIGFILES = "HibernateModule.hibernate.configfiles";

    /* Property key for the Hibernate configuration class that is used */
    public static final String PROPKEY_HIBERNATE_CONFIGURATION_CLASS = "HibernateModule.hibernate.configurationclass";

    /* The Hibernate configuration */
    private Configuration hibernateConfiguration;

    /* The Hibernate SessionFactory */
    private SessionFactory hibernateSessionFactory;

    /* The Hibernate Session that is used in unit tests */
    private Session currentHibernateSession;

    /* Fully qualified class name of the Hibernate Configuration class that is used */
    private String configurationClassName;

    /* List of Hibernate configuration files */
    private List<String> configFileNames;

    /**
     * Initializes the module. The given <code>Configuration</code> object should contain values for the properties
     * {@link #PROPKEY_HIBERNATE_CONFIGFILES}, and {@link #PROPKEY_HIBERNATE_CONFIGURATION_CLASS}
     *
     * @param configuration The Unitils configuration, not null
     */
    public void init(org.apache.commons.configuration.Configuration configuration) {

        configFileNames = configuration.getList(PROPKEY_HIBERNATE_CONFIGFILES);
        configurationClassName = configuration.getString(PROPKEY_HIBERNATE_CONFIGURATION_CLASS);
    }


    /**
     * Checks whether the given test instance is a hibernate test, i.e. is annotated with the {@link HibernateTest} annotation.
     *
     * @param testClass the test class, not null
     * @return true if the test class is a hibernate test, false otherwise
     */
    public boolean isHibernateTest(Class<?> testClass) {

        if (testClass == null) {
            return false;
        } else {
            return testClass.getAnnotation(HibernateTest.class) != null;
        }
    }

    /**
     * Configurates Hibernate, i.e. creates a Hibernate configuration object, and instantiates the Hibernate
     * <code>SessionFactory</code>. The class-level JavaDoc explains how Hibernate is to be configured.
     *
     * @param testObject
     */
    protected void configureHibernate(Object testObject) {

        if (hibernateConfiguration == null) {
            hibernateConfiguration = createHibernateConfiguration(testObject);
            createHibernateSessionFactory();
        }
    }

    /**
     * Creates completely configured Hibernate <code>Configuration</code> object. Loads all hibernate configuration files
     * listed in the property {@link #PROPKEY_HIBERNATE_CONFIGFILES} of the Unitils configuration. Also executes all
     * methods in the test class annotated with {@link HibernateConfiguration}.
     * Unitils' own implementation of the Hibernate <code>ConnectionProvider</code>, {@link HibernateConnectionProvider},
     * is set as connection provider. This object makes sure that Hibernate uses connections of Unitils' own
     * <code>DataSource</code>.
     *
     * @param test The test object
     * @return the Hibernate configuration
     */
    private Configuration createHibernateConfiguration(Object test) {

        Configuration hbnConfiguration = createHibernateConfiguration();
        callHibernateConfigurationMethods(test, hbnConfiguration);
        Properties connectionProviderProperty = new Properties();
        connectionProviderProperty.setProperty(Environment.CONNECTION_PROVIDER, HibernateConnectionProvider.class.getName());
        hbnConfiguration.addProperties(connectionProviderProperty);
        return hbnConfiguration;
    }

    /**
     * Creates an unconfigured instance of <code>Configuration</code>
     *
     * @return a Hibernate <code>Configuration</code> object
     */
    protected Configuration createHibernateConfiguration() {

        Configuration hbnConfiguration;
        try {
            hbnConfiguration = (Configuration) Class.forName(configurationClassName).newInstance();
        } catch (ClassNotFoundException e) {
            throw new UnitilsException("Invalid configuration class " + configurationClassName, e);
        } catch (IllegalAccessException e) {
            throw new UnitilsException("Illegal access to configuration class " + configurationClassName, e);
        } catch (InstantiationException e) {
            throw new UnitilsException("Exception while instantiating instance of class " + configurationClassName, e);
        }
        for (String configFileName : configFileNames) {
            if (!StringUtils.isEmpty(configFileName)) {
                hbnConfiguration.configure(configFileName);
            }
        }
        return hbnConfiguration;
    }

    /**
     * Calls all methods annotated with {@link HibernateConfiguration}, so that they can perform extra Hibernate
     * configuration before the <code>SessionFactory</code> is created.
     *
     * @param testObject
     * @param configuration
     */
    private void callHibernateConfigurationMethods(Object testObject, Configuration configuration) {

        List<Method> methods = AnnotationUtils.getMethodsAnnotatedWith(testObject.getClass(), HibernateConfiguration.class);
        for (Method method : methods) {
            try {
                ReflectionUtils.invokeMethod(testObject, method, configuration);
            } catch (UnitilsException e) {
                throw new UnitilsException("Unable to invoke method annotated with @" +
                        HibernateConfiguration.class.getSimpleName() + ". Ensure that this method has following signature: " +
                        "void myMethod(" + Configuration.class.getName() + " configuration)", e);
            }
        }
    }

    /**
     * Injects the Hibernate <code>Session</code> into all fields and methods that are annotated with {@link HibernateSession}
     *
     * @param testObject
     */
    protected void injectHibernateSession(Object testObject) {

        List<Field> fields = AnnotationUtils.getFieldsAnnotatedWith(testObject.getClass(), HibernateSession.class);
        for (Field field : fields) {
            try {
                ReflectionUtils.setFieldValue(testObject, field, getCurrentSession());

            } catch (UnitilsException e) {

                throw new UnitilsException("Unable to assign the hibernate Session to field annotated with @" +
                        HibernateSession.class.getSimpleName() + "Ensure that this field is of type " +
                        Session.class.getName(), e);
            }
        }

        List<Method> methods = AnnotationUtils.getMethodsAnnotatedWith(testObject.getClass(), HibernateSession.class);
        for (Method method : methods) {
            try {
                ReflectionUtils.invokeMethod(testObject, method, getCurrentSession());
            } catch (UnitilsException e) {
                throw new UnitilsException("Unable to invoke method annotated with @" +
                        HibernateSession.class.getSimpleName() + ". Ensure that this method has following signature: " +
                        "void myMethod(" + Session.class.getName() + " session)", e);
            }
        }
    }

    /**
     * Creates the Hibernate <code>SessionFactory</code>
     */
    private void createHibernateSessionFactory() {
        hibernateSessionFactory = hibernateConfiguration.buildSessionFactory();
    }

    /**
     * @return The Hibernate Configuration object
     */
    public Configuration getHibernateConfiguration() {
        return hibernateConfiguration;
    }

    /**
     * Retrieves the current Hibernate <code>Session</code>. If there is no session yet, or if the current session is
     * closed, a new one is created. If the current session is disconnected, it is reconnected with a
     * <code>Connection</code> from the pool.
     *
     * @return An open and connected Hibernate <code>Session</code>
     */
    public Session getCurrentSession() {

        if (currentHibernateSession == null || !currentHibernateSession.isOpen()) {
            currentHibernateSession = hibernateSessionFactory.openSession();
        }
        return currentHibernateSession;
    }

    /**
     * Closes the current Hibernate session.
     */
    public void closeHibernateSession() {

        if (currentHibernateSession != null && currentHibernateSession.isOpen()) {
            currentHibernateSession.close();
        }
    }

    /**
     * Flushes all pending Hibernate updates to the database. This method is useful when the effect of updates needs to
     * be checked directly on the database. For verifying updates using the Hibernate <code>Session</code> provided by
     * the method #getCurrentSession, flushing is not needed.
     */
    public void flushDatabaseUpdates() {

        getCurrentSession().flush();
    }

    /**
     * @return The {@link DatabaseModule} that provides a connection pooled <code>DataSource</code> to the
     *         HibernateModule.
     */
    private DatabaseModule getDatabaseModule() {

        Unitils unitils = Unitils.getInstance();
        DatabaseModule dbModule = unitils.getModulesRepository().getModuleOfType(DatabaseModule.class);
        return dbModule;
    }

    /**
     * @return The TestListener associated with this module
     */
    public TestListener createTestListener() {
        return new HibernateTestListener();
    }

    /**
     * TestListener that makes callbacks to methods of this module while running tests. This TestListener makes sure that
     * <ul>
     * <li>The {@link HibernateTest} annotation is registered as a databasetest annotation in the {@link DatabaseModule}</li>
     * <li>The Hibernate Configuration and SessionFactory are created when running the first Hibernate test</li>
     * <li>A Hibernate Session is created and injected before each test setup and closed after each test teardown</li>
     */
    private class HibernateTestListener extends TestListener {

        @Override
        public void beforeAll() {
            DatabaseModule databaseModule = getDatabaseModule();
            databaseModule.registerDatabaseTestAnnotation(HibernateTest.class);
        }

        @Override
        public void beforeTestSetUp(Object testObject) {
            if (isHibernateTest(testObject.getClass())) {
                configureHibernate(testObject);
            }
        }

        @Override
        public void beforeTestMethod(Object testObject, Method testMethod) {

            if (isHibernateTest(testObject.getClass())) {
                injectHibernateSession(testObject);
            }
        }

        @Override
        public void afterTestMethod(Object testObject, Method testMethod) {

            if (isHibernateTest(testObject.getClass())) {
                closeHibernateSession();
            }
        }

    }
}
