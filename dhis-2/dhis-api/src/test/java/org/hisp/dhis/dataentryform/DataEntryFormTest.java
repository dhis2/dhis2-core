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
package org.hisp.dhis.dataentryform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.hisp.dhis.common.DisplayDensity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Guards the equality of {@link DataEntryForm}: identifiable properties plus the form content. */
class DataEntryFormTest {

  @Test
  void testFormsWithSamePropertiesAreEqual() {
    DataEntryForm form = form();
    DataEntryForm same = form();

    assertEquals(form, same);
    assertEquals(form.hashCode(), same.hashCode());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("changes")
  void testFormsDifferingInOnePropertyAreNotEqual(String property, Consumer<DataEntryForm> change) {
    DataEntryForm other = form();
    change.accept(other);

    assertNotEquals(form(), other);
  }

  static Stream<Arguments> changes() {
    return Stream.of(
        Arguments.of("uid", (Consumer<DataEntryForm>) f -> f.setUid("FormUid0002")),
        Arguments.of("code", (Consumer<DataEntryForm>) f -> f.setCode("FORM_B")),
        Arguments.of("name", (Consumer<DataEntryForm>) f -> f.setName("Form B")),
        Arguments.of("style", (Consumer<DataEntryForm>) f -> f.setStyle(DisplayDensity.COMPACT)),
        Arguments.of("htmlCode", (Consumer<DataEntryForm>) f -> f.setHtmlCode("<p>B</p>")),
        Arguments.of("format", (Consumer<DataEntryForm>) f -> f.setFormat(1)));
  }

  private static DataEntryForm form() {
    DataEntryForm form = new DataEntryForm("Form A", DisplayDensity.NORMAL, "<p>A</p>");
    form.setUid("FormUid0001");
    form.setCode("FORM_A");
    form.setFormat(DataEntryForm.CURRENT_FORMAT);
    return form;
  }
}
