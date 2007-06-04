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
package org.unitils.hibernate.util;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

/**
 * A wrapper for a Hibernate session factory that will intercept all opened session factories and
 * offers operations to get those opened session and close or flush them.
 *
 * @author Filip Neven
 * @author Tim Ducheyne
 */
public class SessionInterceptingSessionFactory extends BaseSessionInterceptingSessionFactoryProxy {

    /**
     * The intercepted sessions.
     */
    protected Set<org.hibernate.Session> sessions = new HashSet<org.hibernate.Session>();


    /**
     * Creates a wrapper for the given session factory.
     *
     * @param sessionFactory The factory, not null
     */
    public SessionInterceptingSessionFactory(SessionFactory sessionFactory) {
        super(sessionFactory);
    }


    /**
     * Opens a new hibernate session. Overriden to store the opened session.
     *
     * @return the session, not null
     */
    public Session openSession() throws HibernateException {
        Session session = super.openSession();
        sessions.add(session);
        return session;
    }


    /**
     * Opens a new hibernate session. Overriden to store the opened session.
     *
     * @param connection The connection to use
     * @return the session, not null
     */
    public Session openSession(Connection connection) {
        Session session = super.openSession(connection);
        sessions.add(session);
        return session;
    }


    /**
     * Opens a new hibernate session. Overriden to store the opened session.
     *
     * @param connection  The connection to use
     * @param interceptor The session interceptor to use
     * @return the session, not null
     */
    public Session openSession(Connection connection, Interceptor interceptor) {
        Session session = super.openSession(connection, interceptor);
        sessions.add(session);
        return session;
    }


    /**
     * Opens a new hibernate session. Overriden to store the opened session.
     *
     * @param interceptor The session interceptor to use
     * @return the session, not null
     */
    public Session openSession(Interceptor interceptor) throws HibernateException {
        Session session = super.openSession(interceptor);
        sessions.add(session);
        return session;
    }


    /**
     * Gets the current session if <code>CurrentSessionContext</code> is configured.
     *
     * @return The current session
     */
    public Session getCurrentSession() throws HibernateException {
        Session session = super.getCurrentSession();
        sessions.add(session);
        return session;
    }


    /**
     * Gets all open intercepted sessions.
     *
     * @return The sessions, not null
     */
    public Set<org.hibernate.Session> getOpenedSessions() {
        return sessions;
    }


    /**
     * Closes and clears all open sessions.
     */
    public void closeOpenSessions() {
        for (org.hibernate.Session session : sessions) {
            if (session.isOpen()) {
                session.close();
            }
        }
        sessions.clear();
    }


    /**
     * Flushes all open sessions.
     */
    public void flushOpenSessions() {
        for (org.hibernate.Session session : sessions) {
            if (session.isOpen()) {
                session.flush();
            }
        }
    }
}
