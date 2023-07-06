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
package org.hisp.dhis.leader.election;

import org.hisp.dhis.condition.RedisDisabledCondition;
import org.hisp.dhis.condition.RedisEnabledCondition;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.ConfigurationPropertyFactoryBean;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Configures leaderManager that takes care of node leader elections.
 *
 * @author Ameen Mohamed
 */
@Configuration
public class LeaderElectionConfiguration {
  @Autowired private DhisConfigurationProvider dhisConfigurationProvider;

  @Bean(name = "leaderTimeToLive")
  public ConfigurationPropertyFactoryBean leaderTimeToLive() {
    return new ConfigurationPropertyFactoryBean(ConfigurationKey.LEADER_TIME_TO_LIVE);
  }

  @Bean(name = "leaderManager")
  @Conditional(RedisEnabledCondition.class)
  public LeaderManager redisLeaderManager(
      @Autowired(required = false) @Qualifier("stringRedisTemplate")
          StringRedisTemplate stringRedisTemplate) {
    return new RedisLeaderManager(
        Long.parseLong((String) leaderTimeToLive().getObject()),
        stringRedisTemplate,
        dhisConfigurationProvider);
  }

  @Bean(name = "leaderManager")
  @Conditional(RedisDisabledCondition.class)
  public LeaderManager noOpLeaderManager() {
    return new NoOpLeaderManager(dhisConfigurationProvider);
  }
}
