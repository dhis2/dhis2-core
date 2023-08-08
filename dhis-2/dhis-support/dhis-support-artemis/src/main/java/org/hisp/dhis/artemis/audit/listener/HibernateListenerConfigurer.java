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
package org.hisp.dhis.artemis.audit.listener;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.hisp.dhis.artemis.audit.configuration.AuditMatrix;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * This component configures the Hibernate Auditing listeners. The listeners are responsible for
 * "intercepting" Hibernate-managed objects after a save/update operation and pass them to the
 * Auditing sub-system.
 *
 * <p>This bean is not active during tests.
 *
 * @author Luciano Fiandesio
 */
@Component
@DependsOn("auditMatrix")
@RequiredArgsConstructor
public class HibernateListenerConfigurer implements ApplicationContextAware {
  private ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @PersistenceUnit private EntityManagerFactory emf;

  @Nonnull private final PostInsertAuditListener postInsertAuditListener;

  @Nonnull private final PostUpdateAuditListener postUpdateEventListener;

  @Nonnull private final PostDeleteAuditListener postDeleteEventListener;

  @Nonnull private final PostLoadAuditListener postLoadEventListener;

  @Nonnull private final AuditMatrix auditMatrix;

  @Nonnull private DhisConfigurationProvider config;

  @PostConstruct
  protected void init() {
    boolean auditEnabled = config.isEnabled(ConfigurationKey.AUDIT_ENABLED);

    boolean isTestAndNotAuditTest = isTestRun() && !isAuditTest();

    if (!auditEnabled || isTestAndNotAuditTest) {
      return;
    }

    SessionFactoryImpl sessionFactory = emf.unwrap(SessionFactoryImpl.class);

    EventListenerRegistry registry =
        sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

    registry
        .getEventListenerGroup(EventType.POST_COMMIT_INSERT)
        .appendListener(postInsertAuditListener);

    registry
        .getEventListenerGroup(EventType.POST_COMMIT_UPDATE)
        .appendListener(postUpdateEventListener);

    registry
        .getEventListenerGroup(EventType.POST_COMMIT_DELETE)
        .appendListener(postDeleteEventListener);

    if (auditMatrix.isReadEnabled()) {
      registry.getEventListenerGroup(EventType.POST_LOAD).appendListener(postLoadEventListener);
    }
  }

  protected boolean isTestRun() {
    return SystemUtils.isTestRun(applicationContext.getEnvironment().getActiveProfiles());
  }

  protected boolean isAuditTest() {
    return SystemUtils.isAuditTest(applicationContext.getEnvironment().getActiveProfiles());
  }
}
