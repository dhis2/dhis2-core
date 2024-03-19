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
package org.hisp.dhis.textpattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestDefaultTextPatternService {
  @InjectMocks private DefaultTextPatternService textPatternService;

  private TextPattern pattern;

  private Map<String, String> values;

  @BeforeEach
  void init() {
    List<TextPatternSegment> segments = new ArrayList<>();
    segments.add(new TextPatternSegment(TextPatternMethod.TEXT, "\"TEXT\""));
    segments.add(new TextPatternSegment(TextPatternMethod.ORG_UNIT_CODE, "ORG_UNIT_CODE(...)"));
    segments.add(new TextPatternSegment(TextPatternMethod.SEQUENTIAL, "SEQUENTIAL(#)"));
    segments.add(new TextPatternSegment(TextPatternMethod.CURRENT_DATE, "CURRENT_DATE(YYYY)"));
    pattern = new TextPattern(segments);
    values =
        Map.of(
            "ORG_UNIT_CODE", "OSLO",
            "SEQUENTIAL", "1");
  }

  @Test
  void testGetRequiredValues() {
    List<String> required = textPatternService.getRequiredValues(pattern).get("REQUIRED");
    assertFalse(required.contains("TEXT"));
    assertFalse(required.contains("CURRENT_DATE"));
    assertTrue(required.contains("ORG_UNIT_CODE"));
    assertFalse(required.contains("SEQUENTIAL"));
    assertEquals(1, required.size());
  }

  @Test
  void testGetOptionalValues() {
    List<String> optional = textPatternService.getRequiredValues(pattern).get("OPTIONAL");
    assertFalse(optional.contains("TEXT"));
    assertFalse(optional.contains("CURRENT_DATE"));
    assertFalse(optional.contains("ORG_UNIT_CODE"));
    assertTrue(optional.contains("SEQUENTIAL"));
    assertEquals(1, optional.size());
  }

  @Test
  void testResolvePattern() throws Exception {
    String result = textPatternService.resolvePattern(pattern, values);
    assertEquals("TEXTOSL1" + (new SimpleDateFormat("YYYY").format(new Date())), result);
  }
}
