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
package org.hisp.dhis.fieldfiltering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen
 */
class FieldFilterParamsTest {
  @Test
  void testBuilderWithObjectAndFilters() {
    FieldFilterParams<String> params =
        FieldFilterParams.<String>builder()
            .objects(Lists.newArrayList("A", "B", "C"))
            .filters("id,name")
            .build();

    assertTrue(params.getObjects().contains("A"));
    assertTrue(params.getObjects().contains("B"));
    assertTrue(params.getObjects().contains("C"));
    assertTrue(params.getFilters().contains("id"));
    assertTrue(params.getFilters().contains("name"));
  }

  @Test
  void testBuilderWithDefault() {
    FieldFilterParams<String> params =
        FieldFilterParams.<String>builder().objects(Lists.newArrayList("A", "B", "C")).build();

    assertTrue(params.getObjects().contains("A"));
    assertTrue(params.getObjects().contains("B"));
    assertTrue(params.getObjects().contains("C"));
    assertEquals("*", params.getFilters());
  }
}
