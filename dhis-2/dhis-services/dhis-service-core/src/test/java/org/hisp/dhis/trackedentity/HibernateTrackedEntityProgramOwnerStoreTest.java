/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.trackedentity;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.hibernate.HibernateTrackedEntityProgramOwnerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

public class HibernateTrackedEntityProgramOwnerStoreTest {
  private Cache<OrganisationUnit> ownerCache;
  private HibernateTrackedEntityProgramOwnerStore store;

  @BeforeEach
  void setUp() {
    ownerCache = mock(Cache.class);

    CacheProvider cacheProvider = mock(CacheProvider.class);
    doReturn(ownerCache).when(cacheProvider).createProgramOwnerCache();

    Session session = mock(Session.class);
    SessionFactory sessionFactory = mock(SessionFactory.class);
    when(sessionFactory.getCurrentSession()).thenReturn(session);

    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

    store =
        new HibernateTrackedEntityProgramOwnerStore(
            sessionFactory, jdbcTemplate, publisher, cacheProvider);
  }

  @Test
  void saveShouldInvalidateCache() {
    TrackedEntityInstance tei = new TrackedEntityInstance();
    tei.setId(123L);

    Program program = new Program();
    program.setUid("prog1");
    program.setId(456L);

    TrackedEntityProgramOwner owner = new TrackedEntityProgramOwner();
    owner.setEntityInstance(tei);
    owner.setProgram(program);
    owner.setOrganisationUnit(new OrganisationUnit("OU"));

    store.save(owner);

    verify(ownerCache).invalidate(anyString());
  }

  @Test
  void updateShouldInvalidateCache() {
    TrackedEntityInstance tei = new TrackedEntityInstance();
    tei.setId(321L);

    Program program = new Program();
    program.setUid("prog2");
    program.setId(654L);

    TrackedEntityProgramOwner owner = new TrackedEntityProgramOwner();
    owner.setEntityInstance(tei);
    owner.setProgram(program);
    owner.setOrganisationUnit(new OrganisationUnit("OU2"));

    store.update(owner);

    verify(ownerCache).invalidate(anyString());
  }
}
