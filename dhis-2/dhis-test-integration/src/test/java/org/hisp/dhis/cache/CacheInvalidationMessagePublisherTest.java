/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.cache;

import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.hisp.dhis.cache.CacheInvalidationMessagePublisherTest.Config;
import org.hisp.dhis.cacheinvalidation.redis.CacheInvalidationMessagePublisher;
import org.hisp.dhis.cacheinvalidation.redis.CacheInvalidationPreStartupRoutine;
import org.hisp.dhis.cacheinvalidation.redis.PostCacheEventPublisher;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ActiveProfiles("cache-invalidation-test")
@ContextConfiguration(
    classes = {
      Config.class,
    })
class CacheInvalidationMessagePublisherTest extends PostgresIntegrationTestBase {

  static class Config {
    @Bean
    public static SessionRegistry sessionRegistry() {
      return new SessionRegistryImpl();
    }

    @Bean
    public String cacheInvalidationServerId() {
      return generateUid();
    }

    @Bean
    public CacheInvalidationPreStartupRoutine redisCacheInvalidationPreStartupRoutine() {
      CacheInvalidationPreStartupRoutine routine = new CacheInvalidationPreStartupRoutine();
      routine.setName("redisPreStartupRoutine");
      routine.setRunlevel(20);
      routine.setSkipInTests(false);
      return routine;
    }

    @Primary
    @Bean
    CacheInvalidationMessagePublisher messagePublisher() {
      return new TestableMessagePublisher();
    }
  }

  static class TestableMessagePublisher implements CacheInvalidationMessagePublisher {
    @Getter private final List<String> messages = new ArrayList<>();

    @Override
    public void publish(String channel, String message) {
      messages.add(message);
    }
  }

  @Autowired private PostCacheEventPublisher postCacheEventPublisher;

  @Autowired private IdentifiableObjectManager manager;

  private TestableMessagePublisher messagePublisher;

  @BeforeEach
  void setUp() {
    messagePublisher = (TestableMessagePublisher) postCacheEventPublisher.getMessagePublisher();
  }

  @Test
  void testReadPublishedMessages() {
    User peter = createUserWithAuth("peter");
    peter.setUsername("peterpan");
    userService.updateUser(peter);

    List<String> messages = messagePublisher.getMessages();

    assertEquals(3, messages.size());

    String messageA = messages.get(0);
    String[] partsA = messageA.split(":");
    assertEquals("insert", partsA[1]);
    assertEquals("org.hisp.dhis.user.UserRole", partsA[2]);
    assertEquals(peter.getUserRoles().stream().toList().get(0).getId(), Long.parseLong(partsA[3]));

    String messageB = messages.get(1);
    String[] partsB = messageB.split(":");
    assertEquals("insert", partsB[1]);
    assertEquals("org.hisp.dhis.user.User", partsB[2]);
    assertEquals(peter.getId(), Long.parseLong(partsB[3]));

    String messageC = messages.get(2);
    String[] partsC = messageC.split(":");
    assertEquals("update", partsC[1]);
    assertEquals("org.hisp.dhis.user.User", partsC[2]);
    assertEquals(peter.getId(), Long.parseLong(partsC[3]));

    OrganisationUnit orgA = createOrganisationUnit("org1");
    manager.save(orgA);

    String messageD = messages.get(3);
    String[] partsD = messageD.split(":");
    assertEquals("insert", partsD[1]);
    assertEquals("org.hisp.dhis.organisationunit.OrganisationUnit", partsD[2]);
    assertEquals(orgA.getId(), Long.parseLong(partsD[3]));

    orgA.setCode("orgA_A");
    manager.update(orgA);

    String messageE = messages.get(4);
    String[] partsE = messageE.split(":");
    assertEquals("update", partsE[1]);
    assertEquals("org.hisp.dhis.organisationunit.OrganisationUnit", partsE[2]);
    assertEquals(orgA.getId(), Long.parseLong(partsE[3]));

    manager.delete(orgA);

    String messageF = messages.get(5);
    String[] partsF = messageF.split(":");
    assertEquals("delete", partsF[1]);
    assertEquals("org.hisp.dhis.organisationunit.OrganisationUnit", partsF[2]);
    assertEquals(orgA.getId(), Long.parseLong(partsF[3]));

    peter.getUserRoles().removeAll(peter.getUserRoles());
    userService.updateUser(peter);

    assertEquals(8, messages.size());

    String messageG = messages.get(6);
    String[] partsG = messageG.split(":");
    assertEquals("collection", partsG[1]);
    assertEquals("org.hisp.dhis.user.User", partsG[2]);
    assertEquals("org.hisp.dhis.user.User.userRoles", partsG[3]);
    assertEquals(peter.getId(), Long.parseLong(partsG[4]));

    String messageH = messages.get(7);
    String[] partsH = messageH.split(":");
    assertEquals("update", partsH[1]);
    assertEquals("org.hisp.dhis.user.User", partsH[2]);
    assertEquals(peter.getId(), Long.parseLong(partsH[3]));
  }
}
