/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.singleevent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SingleEventFieldsTest {
  private static final String PATH_SEPARATOR = ".";

  @Test
  void shouldIncludeNotesWhenAllFields() {
    assertTrue(SingleEventFields.all().isIncludesNotes());
  }

  @Test
  void shouldNotIncludeNotesWhenNoFields() {
    assertFalse(SingleEventFields.none().isIncludesNotes());
  }

  @Test
  void shouldIncludeNotesWhenBuilderIncludesThem() {
    assertTrue(SingleEventFields.builder().includeNotes().build().isIncludesNotes());
  }

  @Test
  void shouldNotIncludeNotesWhenBuilderDoesNotIncludeThem() {
    assertFalse(SingleEventFields.builder().build().isIncludesNotes());
  }

  @ParameterizedTest
  @ValueSource(strings = {"*", "*,!relationships", "notes", "notes,relationships"})
  void shouldIncludeNotesWhenRequested(String fields) {
    assertTrue(of(fields).isIncludesNotes());
  }

  @ParameterizedTest
  @ValueSource(strings = {"*,!notes", "event", "event,status,orgUnit,occurredAt"})
  void shouldNotIncludeNotesWhenNotRequested(String fields) {
    assertFalse(of(fields).isIncludesNotes());
  }

  @Test
  void shouldIncludeNotesWhenAllPresetIsExpanded() {
    assertTrue(of("*").isIncludesNotes());
  }

  private static SingleEventFields of(String fields) {
    Fields parsed = FieldsParser.parse(fields);
    return SingleEventFields.of(parsed::includes, PATH_SEPARATOR);
  }
}
