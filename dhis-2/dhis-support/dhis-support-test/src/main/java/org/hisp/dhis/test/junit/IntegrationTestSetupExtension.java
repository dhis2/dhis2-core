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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.FlushMode;
import org.hibernate.annotations.QueryHints;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.test.utils.TestUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * TestSetupExtension sets up the pre-requisites for a Spring based integration test.
 *
 * <p>Before a test runs it will
 *
 * <ul>
 *   <li>bind the JPA entity manager to the thread in case the test is not annotated with {@link
 *       Transactional}
 *   <li>set the {@link UserService} in the {@link org.hisp.dhis.test.DhisConvenienceTest} so tests
 *       can create users
 *   <li>create an admin user and inject it into the Spring security context
 *   <li>execute the startup routines
 * </ul>
 *
 * <p>After a test ran it will
 *
 * <ul>
 *   <li>unbind the JPA entity manager to the thread in case the test is not annotated with {@link
 *       Transactional}
 *   <li>empty DB tables in case the test is not annotated with {@link Transactional}
 * </ul>
 *
 * <p>The exact before and after is decided based on the tests {@link Lifecycle}, if the lifecycle
 * is
 *
 * <ul>
 *   <li>{@link Lifecycle#PER_METHOD} (default) the above setup and tear down is done using {@link
 *       BeforeEachCallback} and {@link AfterEachCallback}
 *   <li>{@link Lifecycle#PER_CLASS} the above setup and tear down is done using {@link
 *       BeforeAllCallback} and {@link AfterAllCallback}
 * </ul>
 */
@Slf4j
public class IntegrationTestSetupExtension implements BeforeEachCallback, AfterEachCallback {

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
    if (hasPerClassLifecycle) {
      return;
    }

    if (!hasTransactional) {
      bindSession(context);
    }
    setupAdminUser(context);
    executeStartupRoutines(context);
  }

  @Override
  public void afterEach(ExtensionContext context) {
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
    if (hasPerClassLifecycle) {
      return;
    }

    clearSecurityContext();
    // TODO(ivo) according to Enrico this is not needed
    //    try {
    //      dbmsManager.clearSession();
    //    } catch (Exception e) {
    //      log.error("Failed to clear hibernate session, reason: {}", e.getMessage(), e);
    //    }

    if (hasTransactional) {
      return;
    }

    unbindSession(context);
    emptyDatabase(context);
  }

  private void bindSession(ExtensionContext context)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    EntityManagerFactory entityManagerFactory = getBean(context, EntityManagerFactory.class);
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.setProperty(QueryHints.FLUSH_MODE, FlushMode.AUTO);

    Object testInstance = context.getRequiredTestInstance();
    testInstance
        .getClass()
        .getMethod("setEntityManager", EntityManager.class)
        .invoke(testInstance, entityManager);

    TransactionSynchronizationManager.bindResource(
        entityManagerFactory, new EntityManagerHolder(entityManager));
  }

  private void setupAdminUser(ExtensionContext context)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // TODO(ivo) decide on field access modifiers and cleanup this reflection mess
    Object testInstance = context.getRequiredTestInstance();
    Class<?> klazz = testInstance.getClass();

    UserService userService = getBean(context, UserService.class);
    List<Field> fields =
        ReflectionSupport.findFields(
            klazz, field -> "userService".equals(field.getName()), HierarchyTraversalMode.TOP_DOWN);
    assert fields.size() == 1;
    fields.get(0).setAccessible(true);
    fields.get(0).set(testInstance, userService);

    Method preCreateInjectAdminUser =
        ReflectionSupport.findMethod(context.getRequiredTestClass(), "preCreateInjectAdminUser")
            .get();
    User admin = (User) ReflectionSupport.invokeMethod(preCreateInjectAdminUser, testInstance);
    testInstance.getClass().getMethod("setAdminUser", User.class).invoke(testInstance, admin);
  }

  private static void executeStartupRoutines(ExtensionContext context) {
    TestUtils.executeStartupRoutines(getApplicationContext(context));
  }

  private static <T> T getBean(ExtensionContext context, Class<T> aClass) {
    return getApplicationContext(context).getBean(aClass);
  }

  private static <T> T getBean(ExtensionContext context, Class<T> aClass, String beanName) {
    return aClass.cast(getApplicationContext(context).getBean(beanName));
  }

  private static ApplicationContext getApplicationContext(ExtensionContext context) {
    return SpringExtension.getApplicationContext(context);
  }

  protected void unbindSession(ExtensionContext context) {
    EntityManagerFactory sessionFactory = getBean(context, EntityManagerFactory.class);
    EntityManagerHolder entityManagerHolder =
        (EntityManagerHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
    EntityManagerFactoryUtils.closeEntityManager(entityManagerHolder.getEntityManager());
  }

  private static void emptyDatabase(ExtensionContext context) {
    TransactionTemplate transactionTemplate = getBean(context, TransactionTemplate.class);
    DbmsManager dbmsManager = getBean(context, DbmsManager.class, "dbmsManager");
    transactionTemplate.execute(
        status -> {
          dbmsManager.emptyDatabase();
          return null;
        });
  }
}
