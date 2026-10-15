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
package org.hisp.dhis.analytics.event.data.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StageQualifiedNameTest {

  @Test
  void parseSplitsPrefixAndSuffixAtTheDot() {
    Optional<StageQualifiedName> name = StageQualifiedName.parse("A03MvHHogjR.ouname");

    assertTrue(name.isPresent());
    assertEquals("A03MvHHogjR", name.get().stagePrefix());
    assertEquals("ouname", name.get().suffix());
  }

  @Test
  void parseKeepsRepeatableStageOffsetInThePrefix() {
    StageQualifiedName name = StageQualifiedName.parse("A03MvHHogjR[-1].ouname").orElseThrow();

    assertEquals("A03MvHHogjR[-1]", name.stagePrefix());
    assertEquals("ouname", name.suffix());
    assertTrue(name.hasRepeatableStageOffset());
  }

  @Test
  void parseSplitsAtTheLastDot() {
    StageQualifiedName name = StageQualifiedName.parse("a.b.c").orElseThrow();

    assertEquals("a.b", name.stagePrefix());
    assertEquals("c", name.suffix());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"ouname", ".ouname", "A03MvHHogjR.", "."})
  void parseRejectsAnythingWithoutAnInteriorDot(String identifier) {
    assertTrue(StageQualifiedName.parse(identifier).isEmpty());
    assertFalse(StageQualifiedName.isStageQualified(identifier));
  }

  @Test
  void isStageQualifiedAcceptsInteriorDot() {
    assertTrue(StageQualifiedName.isStageQualified("A03MvHHogjR.EVENT_DATE"));
  }

  @Test
  void hasRepeatableStageOffsetIsFalseWithoutIndex() {
    assertFalse(
        StageQualifiedName.parse("A03MvHHogjR.ouname").orElseThrow().hasRepeatableStageOffset());
  }

  @Test
  void withSuffixKeepsThePrefixAndRendersTheQualifiedForm() {
    StageQualifiedName name = StageQualifiedName.parse("A03MvHHogjR[-1].ouname").orElseThrow();

    StageQualifiedName canonical = name.withSuffix("ou");

    assertEquals("A03MvHHogjR[-1]", canonical.stagePrefix());
    assertEquals("ou", canonical.suffix());
    assertEquals("A03MvHHogjR[-1].ou", canonical.toString());
  }

  @Test
  void toStringRoundTripsThroughParse() {
    String identifier = "A03MvHHogjR.oucode";

    assertEquals(identifier, StageQualifiedName.parse(identifier).orElseThrow().toString());
  }
}
