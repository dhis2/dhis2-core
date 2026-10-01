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
package org.hisp.dhis.common.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.jsontree.JsonAccess;
import org.hisp.dhis.jsontree.JsonAccessException;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link InputUtils}, mostly testing a complex example of mapping with JURL involved.
 *
 * @author Jan Bernitt
 */
class InputUtilsTest {

  public record ImportParams(List<Filter> filter) {}

  /*
  This is just an example of how JURL could be used to model something similar to current filter= parameter
  used in many of our endpoints
   */
  public record Filter(String name, String transform, List<Filter> children) {}

  static {
    JsonAccess.GLOBAL.add(
        Filter.class,
        json -> {
          if (json.isString()) return new Filter(json.to(String.class), null, List.of());
          if (!json.isObject())
            throw new JsonAccessException("Filter must be JSON string or object");

          String name = json.names().get(0);
          JsonMixed value = json.get(name);
          if (value.isString()) return new Filter(name, value.string(), List.of());
          return new Filter(name, null, value.stream().map(e -> e.to(Filter.class)).toList());
        });
  }

  @Test
  void testToJurl() {
    Map<String, String[]> urlParameters =
        Map.of("filter", new String[] {"(name,id,(dataElements:(id,name)),(orgUnits:size))"});

    JsonObject params = InputUtils.decodeInput(ImportParams.class, urlParameters::get);

    assertEquals(
        """
        {"filter":["name","id",{"dataElements":["id","name"]},{"orgUnits":"size"}]}""",
        params.toJson());

    assertEquals(
        new ImportParams(
            List.of(
                new Filter("name", null, List.of()),
                new Filter("id", null, List.of()),
                new Filter(
                    "dataElements",
                    null,
                    List.of(
                        new Filter("id", null, List.of()), new Filter("name", null, List.of()))),
                new Filter("orgUnits", "size", List.of()))),
        params.to(ImportParams.class));
  }
}
