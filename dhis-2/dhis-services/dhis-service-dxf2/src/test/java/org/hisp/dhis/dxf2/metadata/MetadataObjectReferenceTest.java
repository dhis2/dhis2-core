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
package org.hisp.dhis.dxf2.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link MetadataObjectReference}.
 *
 * @author David Mackessy
 */
class MetadataObjectReferenceTest {

  @Test
  @DisplayName("A well formed token is split into type and id")
  void parsesTypeAndId() {
    assertEquals(
        List.of(new MetadataObjectReference("dataSet", "lyLU2wR22tC")),
        MetadataObjectReference.parseAll("dataSet:lyLU2wR22tC"));
  }

  @Test
  @DisplayName("One token may name several objects of the same type")
  void parsesCommaSeparatedIds() {
    assertEquals(
        List.of(
            new MetadataObjectReference("optionSet", "aaaaaaaaaaa"),
            new MetadataObjectReference("optionSet", "bbbbbbbbbbb"),
            new MetadataObjectReference("optionSet", "ccccccccccc")),
        MetadataObjectReference.parseAll("optionSet:aaaaaaaaaaa,bbbbbbbbbbb,ccccccccccc"));
  }

  @Test
  @DisplayName("Ids keep the order they were given in")
  void preservesOrder() {
    assertEquals(
        List.of("ccccccccccc", "aaaaaaaaaaa", "bbbbbbbbbbb"),
        MetadataObjectReference.parseAll("optionSet:ccccccccccc,aaaaaaaaaaa,bbbbbbbbbbb").stream()
            .map(MetadataObjectReference::id)
            .toList());
  }

  @Test
  @DisplayName("Surrounding whitespace is ignored, around the type and around each id")
  void parseTrimsWhitespace() {
    assertEquals(
        List.of(
            new MetadataObjectReference("dataSet", "aaaaaaaaaaa"),
            new MetadataObjectReference("dataSet", "bbbbbbbbbbb")),
        MetadataObjectReference.parseAll("  dataSet : aaaaaaaaaaa , bbbbbbbbbbb  "));
  }

  @Test
  @DisplayName("Only the first colon separates, so extra colons stay in the id")
  void parseSplitsOnFirstColonOnly() {
    assertEquals(
        List.of(new MetadataObjectReference("dataSet", "a:b")),
        MetadataObjectReference.parseAll("dataSet:a:b"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "",
        "   ",
        "dataSet",
        "dataSet:",
        ":lyLU2wR22tC",
        ":",
        "  :  ",
        "dataSet:,",
        "dataSet:aaaaaaaaaaa,",
        "dataSet:aaaaaaaaaaa,,bbbbbbbbbbb"
      })
  @DisplayName("A token that is not of the form type:id[,id...] does not parse")
  void malformedTokensDoNotParse(String token) {
    assertTrue(MetadataObjectReference.parseAll(token).isEmpty());
  }

  @Test
  @DisplayName("Identical references are equal, so duplicates can be collapsed")
  void referencesWithSameTypeAndIdAreEqual() {
    assertEquals(
        MetadataObjectReference.parseAll("dataSet:lyLU2wR22tC"),
        MetadataObjectReference.parseAll("dataSet:lyLU2wR22tC"));
  }

  @Test
  @DisplayName("A reference renders back as a single type:id token")
  void toStringRoundTrips() {
    assertEquals(
        "dataSet:lyLU2wR22tC",
        MetadataObjectReference.parseAll("dataSet:lyLU2wR22tC").get(0).toString());
  }
}
