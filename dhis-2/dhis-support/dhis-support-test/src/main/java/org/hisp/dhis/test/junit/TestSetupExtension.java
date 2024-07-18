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
package org.hisp.dhis.test.junit;

import static org.hisp.dhis.test.DhisConvenienceTest.clearSecurityContext;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.InvocationTargetException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.FlushMode;
import org.hibernate.annotations.QueryHints;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.test.utils.TestUtils;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class TestSetupExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    if (context.getTestClass().isEmpty()) {
      return;
    }

    boolean hasPerClassLifecycle =
        context.getTestInstanceLifecycle().orElse(Lifecycle.PER_METHOD) == Lifecycle.PER_CLASS;
    Class<?> testClass = context.getTestClass().get();
    boolean hasTransactional = findAnnotation(testClass, Transactional.class).isPresent();

    log.debug(
        "beforeEachCallback testClass={} @Transactional={} perClassLifecycle={}",
        testClass.getName(),
        hasTransactional,
        hasPerClassLifecycle);
    if (hasTransactional || hasPerClassLifecycle) {
      return;
    }

    bindSession(context);
    // TODO(ivo) how can I create the admin from here and inject it into the tests security context?
    // userService = _userService;
    // adminUser = preCreateInjectAdminUser();
    TestUtils.executeStartupRoutines(getApplicationContext(context));
  }

  private void bindSession(ExtensionContext context)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    ApplicationContext applicationContext = getApplicationContext(context);
    EntityManagerFactory entityManagerFactory =
        (EntityManagerFactory) applicationContext.getBean("entityManagerFactory");
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.setProperty(QueryHints.FLUSH_MODE, FlushMode.AUTO);

    Object testInstance = context.getTestInstance().get();
    testInstance
        .getClass()
        .getMethod("setEntityManager", EntityManager.class)
        .invoke(testInstance, entityManager);

    TransactionSynchronizationManager.bindResource(
        entityManagerFactory, new EntityManagerHolder(entityManager));
  }

  private static ApplicationContext getApplicationContext(ExtensionContext context) {
    return SpringExtension.getApplicationContext(context);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    nonTransactionalAfter(context);
  }

  protected void nonTransactionalAfter(ExtensionContext context) {
    clearSecurityContext();
    // TODO(ivo) according to Enrico this is not needed
    //    try {
    //      dbmsManager.clearSession();
    //    } catch (Exception e) {
    //      log.error("Failed to clear hibernate session, reason: {}", e.getMessage(), e);
    //    }
    unbindSession(context);

    TransactionTemplate transactionTemplate =
        (TransactionTemplate) getApplicationContext(context).getBean("transactionTemplate");
    DbmsManager dbmsManager = (DbmsManager) getApplicationContext(context).getBean("dbmsManager");
    transactionTemplate.execute(
        status -> {
          dbmsManager.emptyDatabase();
          return null;
        });
  }

  protected void unbindSession(ExtensionContext context) {
    EntityManagerFactory sessionFactory =
        (EntityManagerFactory) getApplicationContext(context).getBean("entityManagerFactory");
    EntityManagerHolder entityManagerHolder =
        (EntityManagerHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
    EntityManagerFactoryUtils.closeEntityManager(entityManagerHolder.getEntityManager());
  }
}
