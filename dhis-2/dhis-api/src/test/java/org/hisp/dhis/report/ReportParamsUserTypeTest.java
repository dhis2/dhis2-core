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
package org.hisp.dhis.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.visualization.ReportingParams;
import org.junit.jupiter.api.Test;

class ReportParamsUserTypeTest {
  private final ReportParamsUserType type = new ReportParamsUserType();

  @Test
  void testDeepCopyKeepsEveryFlagInANewInstance() {
    ReportingParams params = new ReportingParams(true, true, false, true);

    ReportingParams copy = (ReportingParams) type.deepCopy(params);

    assertNotSame(params, copy);
    assertTrue(copy.isReportingPeriod());
    assertTrue(copy.isGrandParentOrganisationUnit());
    assertFalse(copy.isParentOrganisationUnit());
    assertTrue(copy.isOrganisationUnit());
    assertNull(type.deepCopy(null));
  }

  @Test
  void testEqualsComparesOnlyTheStoredFlags() {
    ReportingParams params = new ReportingParams(true, false, false, false);

    assertTrue(type.equals(params, new ReportingParams(true, true, true, false)));
    assertEquals(
        type.hashCode(params), type.hashCode(new ReportingParams(true, true, true, false)));
    assertFalse(type.equals(params, new ReportingParams(false, false, false, false)));
    assertFalse(type.equals(params, new ReportingParams(true, false, false, true)));
    assertFalse(type.equals(params, null));
    assertTrue(type.equals(null, null));
  }

  @Test
  void testPropertiesAreTheReportingPeriodAndOrganisationUnitFlags() {
    ReportingParams params = new ReportingParams(true, false, false, false);

    assertEquals(List.of("reportingPeriod", "organisationUnit"), List.of(type.getPropertyNames()));
    assertTrue((Boolean) type.getPropertyValue(params, 0));
    assertFalse((Boolean) type.getPropertyValue(params, 1));

    type.setPropertyValue(params, 1, true);
    type.setPropertyValue(params, 0, null);

    assertFalse(params.isReportingPeriod());
    assertTrue(params.isOrganisationUnit());
  }

  @Test
  void testCachedFormIsACopy() {
    ReportingParams params = new ReportingParams(false, false, false, true);

    ReportingParams assembled =
        (ReportingParams) type.assemble(type.disassemble(params, null), null, null);

    assertNotSame(params, assembled);
    assertTrue(type.equals(params, assembled));
  }
}
