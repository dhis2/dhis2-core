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
package org.hisp.dhis.dataexchange.aggregate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

class TargetApiSerializerTest {
  /**
   * Asserts that the sensitive {@code accessToken} and {@code password} properties are not
   * serialized for {@link Api}.
   */
  @Test
  void testSerializeTargetApiWithCustomSerializer() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addSerializer(Api.class, new ApiSerializer());
    mapper.registerModule(module);

    Target target =
        new Target()
            .setType(TargetType.EXTERNAL)
            .setApi(
                new Api()
                    .setUrl("https://myserver.org")
                    .setAccessToken("d2pat_abc123")
                    .setUsername("admin")
                    .setPassword("district"));

    String json = mapper.writeValueAsString(target);

    assertTrue(json.contains("https://myserver.org"));
    assertTrue(json.contains("admin"));
    assertFalse(json.contains("d2pat_abc123"));
    assertFalse(json.contains("district"));
  }

  /**
   * Asserts that the sensitive {@code accessToken} and {@code password} properties are serialized
   * for {@link Api}.
   */
  @Test
  void testSerializeTargetApiWithoutCustomSerializer() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    Target target =
        new Target()
            .setType(TargetType.EXTERNAL)
            .setApi(
                new Api()
                    .setUrl("https://myserver.org")
                    .setAccessToken("d2pat_abc123")
                    .setUsername("admin")
                    .setPassword("district"));

    String json = mapper.writeValueAsString(target);

    assertTrue(json.contains("https://myserver.org"));
    assertTrue(json.contains("admin"));
    assertTrue(json.contains("d2pat_abc123"));
    assertTrue(json.contains("district"));
  }
}
