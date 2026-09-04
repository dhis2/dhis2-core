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
package org.hisp.dhis.external.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Pins the inverse invariant between {@link ApiCacheEnabledCondition} and {@link
 * ApiCacheDisabledCondition} (profile branches + config-driven multi-node force-off) without a full
 * Spring application context.
 *
 * <p>The conditions re-bootstrap {@link DefaultDhisConfigurationProvider} from {@code dhis2.home};
 * non-profile cases use a temp home with a minimal {@code dhis.conf}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ApiCacheConditionInverseTest {

  private final ApiCacheEnabledCondition enabled = new ApiCacheEnabledCondition();
  private final ApiCacheDisabledCondition disabled = new ApiCacheDisabledCondition();

  @Test
  @DisplayName("profile=test: Enabled is false, Disabled is true (exact inverse)")
  void profileTestForcesDisabled() {
    ConditionContext ctx = contextWithProfiles("test");
    AnnotatedTypeMetadata metadata = null;

    boolean en = enabled.matches(ctx, metadata);
    boolean dis = disabled.matches(ctx, metadata);

    assertFalse(en);
    assertTrue(dis);
    assertEquals(!en, dis);
  }

  @Test
  @DisplayName("profile=test among others still forces Disabled")
  void profileTestAmongOthersForcesDisabled() {
    ConditionContext ctx = contextWithProfiles("production", "test");
    assertFalse(enabled.matches(ctx, null));
    assertTrue(disabled.matches(ctx, null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("configDrivenCases")
  @DisplayName("Enabled.matches is exact inverse of Disabled.matches for non-test profiles")
  void configDrivenInverseInvariant(String caseName, String dhisConfBody, boolean expectEnabled)
      throws IOException {
    Path home = Files.createTempDirectory("etag-activation-home-");
    Files.writeString(home.resolve("dhis.conf"), dhisConfBody);

    String previousHome = System.getProperty("dhis2.home");
    System.setProperty("dhis2.home", home.toAbsolutePath().toString());
    try {
      ConditionContext ctx = contextWithProfiles(); // no "test" profile
      boolean en = enabled.matches(ctx, null);
      boolean dis = disabled.matches(ctx, null);

      assertEquals(expectEnabled, en, caseName + " enabled");
      assertEquals(!expectEnabled, dis, caseName + " disabled");
      assertEquals(!en, dis, caseName + " inverse");
    } finally {
      if (previousHome == null) {
        System.clearProperty("dhis2.home");
      } else {
        System.setProperty("dhis2.home", previousHome);
      }
    }
  }

  static Stream<Arguments> configDrivenCases() {
    return Stream.of(
        Arguments.of(
            "etag on, single-node (no multi-node signals)",
            """
            cache.api.etag.enabled = on
            redis.cache.invalidation.enabled = off
            redis.enabled = off
            """,
            true),
        Arguments.of(
            "etag off",
            """
            cache.api.etag.enabled = off
            redis.cache.invalidation.enabled = off
            """,
            false),
        Arguments.of(
            "etag on + redis cache invalidation",
            """
            cache.api.etag.enabled = on
            redis.cache.invalidation.enabled = on
            """,
            false),
        Arguments.of(
            "etag on + full cluster",
            """
            cache.api.etag.enabled = on
            cluster.members = node1:4001
            cluster.hostname = node1
            redis.cache.invalidation.enabled = off
            """,
            false),
        Arguments.of(
            "etag on + members-only (half cluster fails open for isClusterEnabled)",
            """
            cache.api.etag.enabled = on
            cluster.members = node1:4001
            redis.cache.invalidation.enabled = off
            """,
            true),
        Arguments.of(
            "etag on + hostname-only (half cluster fails open for isClusterEnabled)",
            """
            cache.api.etag.enabled = on
            cluster.hostname = node1
            redis.cache.invalidation.enabled = off
            """,
            true),
        Arguments.of(
            "etag on + redis.enabled only (must stay enabled)",
            """
            cache.api.etag.enabled = on
            redis.enabled = on
            redis.cache.invalidation.enabled = off
            """,
            true));
  }

  private static ConditionContext contextWithProfiles(String... profiles) {
    StandardEnvironment environment = new StandardEnvironment();
    if (profiles.length > 0) {
      environment.setActiveProfiles(profiles);
    }
    return new ConditionContext() {
      @Override
      public BeanDefinitionRegistry getRegistry() {
        return null;
      }

      @Override
      public ConfigurableListableBeanFactory getBeanFactory() {
        return null;
      }

      @Override
      public Environment getEnvironment() {
        return environment;
      }

      @Override
      public ResourceLoader getResourceLoader() {
        return null;
      }

      @Override
      public ClassLoader getClassLoader() {
        return ApiCacheConditionInverseTest.class.getClassLoader();
      }
    };
  }
}
