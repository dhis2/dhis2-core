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
package org.hisp.dhis.jsonpatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Jason Pickering
 */
class JsonPatchExcludedPropertyFilterTest {

  /** Getter throws if invoked, so the test fails loudly if the filter ever calls it. */
  static class SampleBean {
    public String getId() {
      return "abc";
    }

    public List<String> getUsers() {
      throw new UnsupportedOperationException("users getter must not be invoked when excluded");
    }
  }

  @Test
  void excludedPropertyGetterIsNeverInvokedAndOmittedFromOutput() {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.setMixInAnnotation(Object.class, JsonPatchFilterMixin.class);
    mapper.registerModule(module);

    SimpleFilterProvider filterProvider =
        new SimpleFilterProvider()
            .addFilter(
                JsonPatchExcludedPropertyFilter.ID,
                new JsonPatchExcludedPropertyFilter(Set.of("users")));

    JsonNode node = mapper.copy().setFilterProvider(filterProvider).valueToTree(new SampleBean());

    assertTrue(node.has("id"), "non-excluded property must still be serialized");
    assertFalse(node.has("users"), "excluded property must not appear in output");
  }

  @Test
  void nonExcludedPropertiesAreUnaffected() {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.setMixInAnnotation(Object.class, JsonPatchFilterMixin.class);
    mapper.registerModule(module);

    SimpleFilterProvider filterProvider =
        new SimpleFilterProvider()
            .addFilter(
                JsonPatchExcludedPropertyFilter.ID, new JsonPatchExcludedPropertyFilter(Set.of()));

    JsonNode node = mapper.copy().setFilterProvider(filterProvider).valueToTree(new NoUsersBean());

    assertTrue(node.has("id"));
  }

  static class NoUsersBean {
    public String getId() {
      return "abc";
    }
  }
}
