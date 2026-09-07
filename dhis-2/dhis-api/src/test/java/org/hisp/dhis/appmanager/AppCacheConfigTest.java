/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.appmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.appmanager.AppCacheConfig.CacheRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppCacheConfigTest {

  private final ObjectMapper mapper = App.MAPPER;

  @Test
  @DisplayName("Deserializes full dhis2-cache.json with multiple rules")
  void deserializeFullConfig() throws Exception {
    String json =
        """
        {
          "rules": [
            {"pattern": "**/*.html", "maxAgeSeconds": 300, "mustRevalidate": true},
            {"pattern": "**/*.[0-9a-f]{8,}.*", "immutable": true},
            {"pattern": "assets/**", "maxAgeSeconds": 31536000}
          ],
          "defaultMaxAgeSeconds": 7200
        }
        """;

    AppCacheConfig config = mapper.readValue(json, AppCacheConfig.class);

    assertNotNull(config);
    assertEquals(3, config.getRules().size());
    assertEquals(7200, config.getDefaultMaxAgeSeconds());

    CacheRule htmlRule = config.getRules().get(0);
    assertEquals("**/*.html", htmlRule.getPattern());
    assertEquals(300, htmlRule.getMaxAgeSeconds());
    assertTrue(htmlRule.getMustRevalidate());
    assertNull(htmlRule.getImmutable());

    CacheRule hashRule = config.getRules().get(1);
    assertTrue(hashRule.getImmutable());
    assertNull(hashRule.getMaxAgeSeconds());

    CacheRule assetsRule = config.getRules().get(2);
    assertEquals("assets/**", assetsRule.getPattern());
    assertEquals(31536000, assetsRule.getMaxAgeSeconds());
  }

  @Test
  @DisplayName("Deserializes empty JSON object to default values")
  void deserializeEmptyConfig() throws Exception {
    AppCacheConfig config = mapper.readValue("{}", AppCacheConfig.class);

    assertNotNull(config);
    assertNotNull(config.getRules());
    assertTrue(config.getRules().isEmpty());
    assertNull(config.getDefaultMaxAgeSeconds());
  }

  @Test
  @DisplayName("Ignores unknown JSON properties")
  void ignoresUnknownProperties() throws Exception {
    String json =
        """
        {
          "rules": [],
          "unknownField": "should be ignored",
          "anotherUnknown": 42
        }
        """;

    AppCacheConfig config = mapper.readValue(json, AppCacheConfig.class);
    assertNotNull(config);
    assertTrue(config.getRules().isEmpty());
  }

  @Test
  @DisplayName("DEFAULT constant has empty rules and null defaultMaxAge")
  void defaultConstantIsValid() {
    AppCacheConfig def = AppCacheConfig.DEFAULT;
    assertNotNull(def);
    assertTrue(def.getRules().isEmpty());
    assertNull(def.getDefaultMaxAgeSeconds());
  }

  @Test
  @DisplayName("Config with only rules and no defaultMaxAgeSeconds")
  void configWithOnlyRules() throws Exception {
    String json =
        """
        {
          "rules": [
            {"pattern": "**/*.css", "maxAgeSeconds": 86400}
          ]
        }
        """;

    AppCacheConfig config = mapper.readValue(json, AppCacheConfig.class);
    assertEquals(1, config.getRules().size());
    assertNull(config.getDefaultMaxAgeSeconds());
    assertEquals("**/*.css", config.getRules().get(0).getPattern());
  }

  @Test
  @DisplayName("Round-trip serialization preserves values")
  void roundTripSerialization() throws Exception {
    String json =
        """
        {
          "rules": [
            {"pattern": "**/*.js", "maxAgeSeconds": 3600, "immutable": false, "mustRevalidate": false}
          ],
          "defaultMaxAgeSeconds": 1800
        }
        """;

    AppCacheConfig original = mapper.readValue(json, AppCacheConfig.class);
    String serialized = mapper.writeValueAsString(original);
    AppCacheConfig deserialized = mapper.readValue(serialized, AppCacheConfig.class);

    assertEquals(original.getRules().size(), deserialized.getRules().size());
    assertEquals(original.getDefaultMaxAgeSeconds(), deserialized.getDefaultMaxAgeSeconds());
    assertEquals(
        original.getRules().get(0).getPattern(), deserialized.getRules().get(0).getPattern());
    assertEquals(
        original.getRules().get(0).getMaxAgeSeconds(),
        deserialized.getRules().get(0).getMaxAgeSeconds());
  }
}
