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
package org.hisp.dhis.configuration;

import org.hisp.dhis.condition.RedisEnabledCondition;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.ConfigurationPropertyFactoryBean;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration registered if {@link RedisEnabledCondition} matches to true. This class deals with
 * the configuration properties for establishing connection to a redis server.
 *
 * @author Ameen Mohamed
 */
@Configuration
@Conditional(RedisEnabledCondition.class)
public class RedisConfiguration {
  @Bean
  public LettuceConnectionFactory lettuceConnectionFactory() {
    LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
    if (DhisConfigurationProvider.isOn((String) redisSslEnabled().getObject())) {
      builder.useSsl();
    }
    LettuceClientConfiguration clientConfiguration = builder.build();
    RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
    standaloneConfig.setHostName((String) redisHost().getObject());
    standaloneConfig.setPassword((String) redisPassword().getObject());
    standaloneConfig.setPort(Integer.parseInt((String) redisPort().getObject()));
    return new LettuceConnectionFactory(standaloneConfig, clientConfiguration);
  }

  @Bean
  public ConfigurationPropertyFactoryBean redisHost() {
    return new ConfigurationPropertyFactoryBean(ConfigurationKey.REDIS_HOST);
  }

  @Bean
  public ConfigurationPropertyFactoryBean redisPort() {
    return new ConfigurationPropertyFactoryBean(ConfigurationKey.REDIS_PORT);
  }

  @Bean
  public ConfigurationPropertyFactoryBean redisPassword() {
    return new ConfigurationPropertyFactoryBean(ConfigurationKey.REDIS_PASSWORD);
  }

  @Bean
  public ConfigurationPropertyFactoryBean redisSslEnabled() {
    return new ConfigurationPropertyFactoryBean(ConfigurationKey.REDIS_USE_SSL);
  }

  @Bean(name = "redisTemplate")
  @Primary
  public RedisTemplate<?, ?> redisTemplate() {
    RedisTemplate<?, ?> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(lettuceConnectionFactory());
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    return redisTemplate;
  }

  @Bean(name = "stringRedisTemplate")
  public StringRedisTemplate stringRedisTemplate() {
    StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
    stringRedisTemplate.setConnectionFactory(lettuceConnectionFactory());
    return stringRedisTemplate;
  }
}
