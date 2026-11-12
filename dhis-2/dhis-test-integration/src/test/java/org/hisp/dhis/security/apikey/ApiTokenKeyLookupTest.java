/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.security.apikey;

import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.generatePersonalAccessToken;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;
import org.hibernate.cfg.AvailableSettings;
import org.hisp.dhis.security.apikey.ApiTokenKeyLookupTest.DhisConfig;
import org.hisp.dhis.test.config.PostgresTestConfigOverride;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

/**
 * The lookup of an API token by its key must follow the stored tokens, also when the query cache of
 * this node still holds the result of an earlier lookup. That is the case when another node of a
 * cluster deleted the token, for example together with its owner: the update timestamps of this
 * node do not see that delete, and the cache invalidation of the other node evicts the token entity
 * but not the query result. A request with the token then failed with HTTP 500 instead of 401. The
 * test deletes the row with plain SQL, which leaves this node's query cache as such a delete does.
 *
 * <p>Not transactional: every service call commits, as on a server. {@link Lifecycle#PER_CLASS}
 * makes the framework empty the database after the class.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@TestInstance(Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {DhisConfig.class})
class ApiTokenKeyLookupTest extends PostgresIntegrationTestBase {

  static class DhisConfig {
    @Bean
    public PostgresTestConfigOverride postgresTestConfigOverride() {
      PostgresTestConfigOverride override = new PostgresTestConfigOverride();
      override.put(AvailableSettings.USE_QUERY_CACHE, "true");
      override.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, "true");
      override.put("cache.ehcache.config.file", "");
      return override;
    }
  }

  @Autowired private ApiTokenService apiTokenService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void tokenDeletedPastTheQueryCacheIsNotFoundByItsKey() {
    injectAdminIntoSecurityContext();
    long expire = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30);
    ApiToken token = generatePersonalAccessToken(null, expire, null).getApiToken();
    apiTokenService.save(token);
    // the first lookup puts the result into the caches of this node
    assertNotNull(apiTokenService.getByKey(token.getKey()));

    jdbcTemplate.update("delete from api_token where apitokenid = ?", token.getId());

    assertNull(apiTokenService.getByKey(token.getKey()));
  }
}
