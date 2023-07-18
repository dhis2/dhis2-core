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
package org.hisp.dhis.fieldfilter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultFieldParser}.
 *
 * @author Volker Schmidt
 */
class DefaultFieldParserTest {

  private final DefaultFieldParser parser = new DefaultFieldParser();

  @Test
  void parseWithTransformer() {
    final FieldMap fieldMap = parser.parse("id,organisationUnits~pluck");
    Assertions.assertEquals(2, fieldMap.size());
    Assertions.assertTrue(fieldMap.containsKey("id"));
    Assertions.assertTrue(fieldMap.containsKey("organisationUnits~pluck"));
  }

  @Test
  void parseWithTransformerArgAndFields() {
    final FieldMap fieldMap = parser.parse("id,organisationUnits~pluck(name)[id,name]");
    Assertions.assertEquals(2, fieldMap.size());
    Assertions.assertTrue(fieldMap.containsKey("id"));
    Assertions.assertTrue(fieldMap.containsKey("organisationUnits~pluck(name)"));
    final FieldMap innerFieldMap = fieldMap.get("organisationUnits~pluck(name)");
    Assertions.assertTrue(innerFieldMap.containsKey("id"));
    Assertions.assertTrue(innerFieldMap.containsKey("name"));
  }
}
