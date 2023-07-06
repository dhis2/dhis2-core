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
package org.hisp.dhis.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class QueryKeyTest {
  @Test
  void testAsPlainKey() {
    String key =
        new QueryKey()
            .add("dimension", "dx")
            .add("dimension", "pe")
            .add("filter", "ou")
            .add("aggregationType", AggregationType.SUM)
            .add("skipMeta", true)
            .add("locale", Locale.FRENCH)
            .asPlainKey();

    assertEquals(
        "dimension:dx-dimension:pe-filter:ou-aggregationType:SUM-skipMeta:true-locale:fr", key);
  }

  @Test
  void testAsPlainKeyB() {
    String key =
        new QueryKey()
            .add("dimension", "dx")
            .add("filter", "pe")
            .add("filter", "ou")
            .add("aggregationType", AggregationType.AVERAGE)
            .add("skipMeta", true)
            .asPlainKey();

    assertEquals("dimension:dx-filter:pe-filter:ou-aggregationType:AVERAGE-skipMeta:true", key);
  }

  @Test
  void testAsPlainKeyC() {
    String key = new QueryKey().add("dimension", "dx").add("locale", null).asPlainKey();

    assertEquals("dimension:dx-locale:null", key);
  }

  @Test
  void testAsPlainKeyIgnoreNull() {
    String key =
        new QueryKey()
            .add("dimension", "dx")
            .add("filter", "ou")
            .addIgnoreNull("valueType", null)
            .addIgnoreNull("locale", null)
            .asPlainKey();

    assertEquals("dimension:dx-filter:ou", key);
  }

  @Test
  void testNoCollision() {
    String keyA =
        new QueryKey()
            .add("dimension", "dx")
            .add("dimension", "aZASaK6ebLC")
            .add("filter", "ou")
            .add("aggregationType", AggregationType.SUM)
            .asPlainKey();

    String keyB =
        new QueryKey()
            .add("dimension", "dx")
            .add("dimension", "aZASaK6ebLD")
            .add("filter", "ou")
            .add("aggregationType", AggregationType.SUM)
            .asPlainKey();

    assertNotEquals(keyA, keyB);
  }
}
