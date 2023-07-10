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

import static org.hisp.dhis.common.CodeGenerator.generateUid;

import org.hisp.dhis.cacheinvalidation.redis.CacheInvalidationEnabledCondition;
import org.hisp.dhis.cacheinvalidation.redis.CacheInvalidationPreStartupRoutine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.session.SessionRegistryImpl;

/**
 * It configures the Redis client and the connection to the Redis server
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Order(10002)
@Configuration
@ComponentScan(basePackages = {"org.hisp.dhis"})
@Profile({"cache-invalidation-test"})
@Conditional(value = CacheInvalidationEnabledCondition.class)
public class TestableCacheInvalidationConfiguration {
  @Bean
  public static SessionRegistryImpl sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Bean(name = "cacheInvalidationServerId")
  public String getCacheInvalidationServerId() {
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
}
