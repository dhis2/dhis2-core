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
package org.hisp.dhis.db.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.Test;

class AnalyticsTableNamesTest {

  @Test
  void eventTableLowercasesProgramUid() {
    assertEquals(
        "analytics_event_iphinat79uw", AnalyticsTableNames.eventTable(program("IpHINAT79UW")));
  }

  @Test
  void eventTableIsIdempotentForLowercaseUid() {
    assertEquals(
        "analytics_event_abcdefghijk", AnalyticsTableNames.eventTable(program("abcdefghijk")));
  }

  @Test
  void enrollmentTableLowercasesProgramUid() {
    assertEquals(
        "analytics_enrollment_iphinat79uw",
        AnalyticsTableNames.enrollmentTable(program("IpHINAT79UW")));
  }

  @Test
  void enrollmentTableIsIdempotentForLowercaseUid() {
    assertEquals(
        "analytics_enrollment_abcdefghijk",
        AnalyticsTableNames.enrollmentTable(program("abcdefghijk")));
  }

  @Test
  void eventTablePrefixIsExposedAsConstant() {
    assertEquals("analytics_event_", AnalyticsTableNames.EVENT_PREFIX);
  }

  @Test
  void enrollmentTablePrefixIsExposedAsConstant() {
    assertEquals("analytics_enrollment_", AnalyticsTableNames.ENROLLMENT_PREFIX);
  }

  @Test
  void eventTableUsesLocaleRootSoTurkishLocaleDoesNotChangeOutput() {
    // Java default-locale lowercase of "I" in Turkish yields "ı" (dotless), not "i".
    // Locale.ROOT must guarantee deterministic output regardless of JVM default locale.
    assertEquals(
        "analytics_event_imspshyf6o6", AnalyticsTableNames.eventTable(program("IMSPSHYF6O6")));
  }

  @Test
  void eventTableRejectsNullProgram() {
    assertThrows(NullPointerException.class, () -> AnalyticsTableNames.eventTable(null));
  }

  @Test
  void enrollmentTableRejectsNullProgram() {
    assertThrows(NullPointerException.class, () -> AnalyticsTableNames.enrollmentTable(null));
  }

  private static Program program(String uid) {
    Program program = new Program();
    program.setUid(uid);
    return program;
  }
}
