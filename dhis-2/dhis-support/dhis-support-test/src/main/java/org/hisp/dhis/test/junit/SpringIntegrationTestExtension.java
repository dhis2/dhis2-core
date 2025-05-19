/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import static org.hisp.dhis.test.TestBase.clearSecurityContext;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.FlushMode;
import org.hibernate.annotations.QueryHints;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.test.TestBase;
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
 * SpringIntegrationTestExtension sets up the pre-requisites for a Spring based integration test.
 *
 * <p>Before a test runs it will
 *
 * <ul>
 *   <li>bind the JPA entity manager to the thread in case the test is not annotated with {@link
 *       Transactional}
 *   <li>set the {@link UserService} in the {@link TestBase} so tests can create users
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
 * <p>What before and after callback is doing the work is decided based on the tests {@link
 * Lifecycle}. If the lifecycle is
 *
 * <ul>
 *   <li>{@link Lifecycle#PER_METHOD} (default) the above setup and tear down is done using {@link
 *       BeforeEachCallback} and {@link AfterEachCallback}
 *   <li>{@link Lifecycle#PER_CLASS} the above setup and tear down is done using {@link
 *       BeforeAllCallback} and {@link AfterAllCallback}
 * </ul>
 */
@Slf4j
public class SpringIntegrationTestExtension
    implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    if (isTestLifecyclePerMethod(context)) {
      return;
    }

    setUp(context);
    log(context, "beforeAll", "ran setUp");
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    if (isTestLifecyclePerClass(context)) {
      return;
    }

    setUp(context);
    log(context, "beforeEach", "ran setUp");
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (isTestLifecyclePerMethod(context)) {
      return;
    }

    tearDown(context);
    log(context, "afterAll", "ran tearDown");
  }

  @Override
  public void afterEach(ExtensionContext context) {
    if (isTestLifecyclePerClass(context)) {
      return;
    }

    tearDown(context);
    log(context, "afterEach", "ran tearDown");
  }

  private void setUp(ExtensionContext context) throws IllegalAccessException {
    boolean hasPerClassLifecycle = isTestLifecyclePerClass(context);
    boolean hasTransactional = isAnnotatedWithTransactional(context);

    if (hasPerClassLifecycle || !hasTransactional) {
      bindSession(context);
    }
    setupAdminUser(context);
    executeStartupRoutines(context);
  }

  private void tearDown(ExtensionContext context) {
    clearSecurityContext();

    // https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/tx.html#testcontext-tx-attribute-support
    // > methods annotated with JUnit @BeforeAll or @AfterAll are not run within a test-managed
    // transaction
    // This means we need to empty the DB even if a test class is annotated with @Transactional if
    // its test lifecycle is set to class
    if (isTestLifecyclePerClass(context) || !isAnnotatedWithTransactional(context)) {
      unbindSession(context);
      emptyDatabase(context);
    }
  }

  private static boolean isTestLifecyclePerMethod(ExtensionContext context) {
    return !isTestLifecyclePerClass(context);
  }

  private static boolean isTestLifecyclePerClass(ExtensionContext context) {
    return context.getTestInstanceLifecycle().orElse(Lifecycle.PER_METHOD) == Lifecycle.PER_CLASS;
  }

  private static boolean isAnnotatedWithTransactional(ExtensionContext context) {
    return findAnnotation(context.getRequiredTestClass(), Transactional.class).isPresent();
  }

  private void bindSession(ExtensionContext context) throws IllegalAccessException {
    EntityManagerFactory entityManagerFactory = getBean(context, EntityManagerFactory.class);
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    entityManager.setProperty(QueryHints.FLUSH_MODE, FlushMode.AUTO);

    Object testInstance = context.getRequiredTestInstance();
    setField(testInstance, "entityManager", entityManager);

    TransactionSynchronizationManager.bindResource(
        entityManagerFactory, new EntityManagerHolder(entityManager));
  }

  private void setupAdminUser(ExtensionContext context) throws IllegalAccessException {
    Object testInstance = context.getRequiredTestInstance();

    UserService userService = getBean(context, UserService.class);
    setField(testInstance, "userService", userService);

    Method preCreateInjectAdminUser =
        ReflectionSupport.findMethod(context.getRequiredTestClass(), "preCreateInjectAdminUser")
            .get();
    User admin = (User) ReflectionSupport.invokeMethod(preCreateInjectAdminUser, testInstance);
    setField(testInstance, "adminUser", admin);
  }

  private static void executeStartupRoutines(ExtensionContext context) {
    TestUtils.executeStartupRoutines(getApplicationContext(context));
  }

  private static <T> T getBean(ExtensionContext context, Class<T> klazz) {
    return getApplicationContext(context).getBean(klazz);
  }

  private static <T> T getBean(ExtensionContext context, Class<T> klazz, String beanName) {
    return klazz.cast(getApplicationContext(context).getBean(beanName));
  }

  private static ApplicationContext getApplicationContext(ExtensionContext context) {
    return SpringExtension.getApplicationContext(context);
  }

  private static void setField(Object testInstance, String fieldName, Object value)
      throws IllegalAccessException {
    Class<?> klazz = testInstance.getClass();
    List<Field> fields =
        ReflectionSupport.findFields(
            klazz, field -> fieldName.equals(field.getName()), HierarchyTraversalMode.TOP_DOWN);
    assert fields.size() == 1;
    fields.get(0).setAccessible(true);
    fields.get(0).set(testInstance, value);
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

  private static void log(ExtensionContext context, String callback, String message) {
    if (!log.isDebugEnabled()) {
      // only pay the annotation lookup cost if needed
      return;
    }
    log.debug(
        "testClass={} testCallback={} @Transactional={} testLifecycle={} message={}",
        context.getRequiredTestClass().getName(),
        callback,
        isAnnotatedWithTransactional(context),
        context.getTestInstanceLifecycle(),
        message);
  }
}
