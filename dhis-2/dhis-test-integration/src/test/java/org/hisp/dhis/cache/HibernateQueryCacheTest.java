/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.QueryHints;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class HibernateQueryCacheTest extends HibernateCacheBaseTest {

  private @Autowired EntityManagerFactory entityManagerFactory;
  private @Autowired UserService _userService;

  private SessionFactory sessionFactory;

  @BeforeEach
  public final void before() throws Exception {
    this.entityManager = entityManagerFactory.createEntityManager();
    this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);

    this.entityManager.setProperty(org.hibernate.annotations.QueryHints.FLUSH_MODE, FlushMode.AUTO);
    TransactionSynchronizationManager.bindResource(
        entityManagerFactory, new EntityManagerHolder(entityManager));

    userService = _userService;
    User adminUser = preCreateInjectAdminUser();
    injectSecurityContextUser(adminUser);
    integrationTestBeforeEach();

    this.sessionFactory.getStatistics().setStatisticsEnabled(true);
  }

  @AfterEach
  public final void afterEach() throws Exception {
    unbindSession();
    entityManager.close();
  }

  @Test
  @DisplayName("Hibernate Query cache should be used")
  void testQueryCache() {
    OptionSet optionSet = new OptionSet();
    optionSet.setAutoFields();
    optionSet.setName("OptionSetA");
    optionSet.setCode("OptionSetCodeA");
    optionSet.setValueType(ValueType.TEXT);

    entityManager.getTransaction().begin();
    entityManager.persist(optionSet);
    entityManager.getTransaction().commit();

    for (int i = 0; i < 10; i++) {
      entityManager.getTransaction().begin();
      TypedQuery<OptionSet> query = createQuery(entityManager);
      assertEquals(1, query.getResultList().size());
      entityManager.getTransaction().commit();
    }

    assertEquals(1, sessionFactory.getStatistics().getQueryCacheMissCount());
    assertEquals(9, sessionFactory.getStatistics().getQueryCacheHitCount());
    entityManager.close();
  }

  private TypedQuery<OptionSet> createQuery(EntityManager entityManager) {
    return entityManager
        .createQuery("from OptionSet where code = :code", OptionSet.class)
        .setParameter("code", "OptionSetCodeA")
        .setHint(QueryHints.HINT_CACHE_REGION, "org.hisp.dhis.option.OptionSet")
        .setHint(QueryHints.HINT_CACHEABLE, true);
  }
}
