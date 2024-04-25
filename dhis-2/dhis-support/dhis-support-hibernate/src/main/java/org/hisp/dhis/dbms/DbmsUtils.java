/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.dbms;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hisp.dhis.commons.util.DebugUtils;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Slf4j
public class DbmsUtils {
  public static void bindSessionToThread(SessionFactory sessionFactory) {
    Session session = sessionFactory.openSession();

    TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
  }

  /**
   * Method to bind a session to the current thread if there is none open. If there is an open
   * session then no action is taken. If there is no open session then a new session is opened in
   * the catch block.
   *
   * @param sessionFactory session factory
   */
  public static Session bindSessionToThreadIfNoneOpen(SessionFactory sessionFactory) {
    Session currentSession = null;
    try {
      log.info("Checking for an open Hibernate session");
      sessionFactory.getCurrentSession();
      return null;
    } catch (HibernateException he) {
      log.info(
          "No open session found, caught HibernateException {}, opening new Hibernate session now",
          he.getMessage());
      currentSession = sessionFactory.openSession();
      TransactionSynchronizationManager.bindResource(
          sessionFactory, new SessionHolder(currentSession));
    }
    return currentSession;
  }

  public static void unbindSessionFromThread(SessionFactory sessionFactory) {
    log.info("Closing Hibernate session");
    SessionHolder sessionHolder =
        (SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);

    SessionFactoryUtils.closeSession(sessionHolder.getSession());
  }

  public static void closeStatelessSession(StatelessSession session) {
    try {
      session.getTransaction().commit();
    } catch (Exception exception) {
      session.getTransaction().rollback();
      DebugUtils.getStackTrace(exception);
    } finally {
      session.close();
    }
  }
}
