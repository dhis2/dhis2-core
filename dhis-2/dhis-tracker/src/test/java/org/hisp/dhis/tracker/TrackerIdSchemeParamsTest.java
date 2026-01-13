/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.tracker.TrackerIdSchemeParam.CODE;
import static org.hisp.dhis.tracker.TrackerIdSchemeParam.NAME;
import static org.hisp.dhis.tracker.TrackerIdSchemeParam.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TrackerIdSchemeParamsTest {

  @Test
  void shouldDefaultToUID() {
    TrackerIdSchemeParams params = TrackerIdSchemeParams.builder().build();

    assertEquals(UID, params.getIdScheme(), "idScheme");
    assertEquals(UID, params.getDataElementIdScheme(), "dataElementIdScheme");
    assertEquals(UID, params.getOrgUnitIdScheme(), "orgUnitIdScheme");
    assertEquals(UID, params.getProgramIdScheme(), "programIdScheme");
    assertEquals(UID, params.getProgramStageIdScheme(), "programStageIdScheme");
    assertEquals(UID, params.getCategoryOptionComboIdScheme(), "categoryOptionComboIdScheme");
    assertEquals(UID, params.getCategoryOptionIdScheme(), "categoryOptionIdScheme");
  }

  @Test
  void idSchemeAppliesToAllMetadata() {
    TrackerIdSchemeParams params = TrackerIdSchemeParams.builder().idScheme(CODE).build();

    assertEquals(CODE, params.getIdScheme(), "idScheme");
    assertEquals(CODE, params.getDataElementIdScheme(), "dataElementIdScheme");
    assertEquals(CODE, params.getOrgUnitIdScheme(), "orgUnitIdScheme");
    assertEquals(CODE, params.getProgramIdScheme(), "programIdScheme");
    assertEquals(CODE, params.getProgramStageIdScheme(), "programStageIdScheme");
    assertEquals(CODE, params.getCategoryOptionComboIdScheme(), "categoryOptionComboIdScheme");
    assertEquals(CODE, params.getCategoryOptionIdScheme(), "categoryOptionIdScheme");
  }

  @Test
  void idSchemeIsOverriddenBySpecificField() {
    TrackerIdSchemeParam programIdScheme =
        TrackerIdSchemeParam.ofAttribute(org.hisp.dhis.common.UID.generate().getValue());
    TrackerIdSchemeParams params =
        TrackerIdSchemeParams.builder()
            .idScheme(NAME)
            .programIdScheme(programIdScheme)
            .programStageIdScheme(CODE)
            .build();

    assertEquals(NAME, params.getIdScheme(), "idScheme");
    assertEquals(NAME, params.getDataElementIdScheme(), "dataElementIdScheme");
    assertEquals(NAME, params.getOrgUnitIdScheme(), "orgUnitIdScheme");
    assertEquals(programIdScheme, params.getProgramIdScheme(), "programIdScheme");
    assertEquals(CODE, params.getProgramStageIdScheme(), "programStageIdScheme");
    assertEquals(NAME, params.getCategoryOptionComboIdScheme(), "categoryOptionComboIdScheme");
    assertEquals(NAME, params.getCategoryOptionIdScheme(), "categoryOptionIdScheme");
  }
}
