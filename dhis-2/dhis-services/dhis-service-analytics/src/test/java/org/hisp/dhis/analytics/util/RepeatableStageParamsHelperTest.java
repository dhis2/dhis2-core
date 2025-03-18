/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.util;

import static org.hisp.dhis.analytics.util.RepeatableStageParamsHelper.getRepeatableStageParams;
import static org.hisp.dhis.analytics.util.RepeatableStageParamsHelper.removeRepeatableStageParams;
import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis.common.RepeatableStageParams;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RepeatableStageParamsHelper}. */
class RepeatableStageParamsHelperTest {

  @Test
  void testGetRepeatableStageParams() {
    // Given
    String dimension1 = "EPEcjy3FWmI[0].fCXKBdc27Bt";
    String dimension2 = "EPEcjy3FWmI[-1].fCXKBdc27Bt";
    String dimension3 = "EPEcjy3FWmI.fCXKBdc27Bt";

    // When
    RepeatableStageParams params1 = getRepeatableStageParams(dimension1);
    RepeatableStageParams params2 = getRepeatableStageParams(dimension2);
    RepeatableStageParams params3 = getRepeatableStageParams(dimension3);

    // Then
    assertFalse(params1.isDefaultObject());
    assertFalse(params2.isDefaultObject());
    assertTrue(params3.isDefaultObject());

    assertEquals(0, params1.getIndex());
    assertEquals(-1, params2.getIndex());
    assertEquals(0, params3.getIndex());

    assertEquals(dimension1, params1.getDimension());
    assertEquals(dimension2, params2.getDimension());
    assertEquals(dimension3, params3.getDimension());
  }

  @Test
  void testRemoveRepeatableStageParams() {
    // Given
    String dimension1 = "EPEcjy3FWmI[0].fCXKBdc27Bt";
    String dimension2 = "EPEcjy3FWmI[-1].fCXKBdc27Bt";
    String dimension3 = "EPEcjy3FWmI.fCXKBdc27Bt";

    // When
    String noRepeatableStages1 = removeRepeatableStageParams(dimension1);
    String noRepeatableStages2 = removeRepeatableStageParams(dimension2);
    String noRepeatableStages3 = removeRepeatableStageParams(dimension3);

    // Then
    assertEquals("EPEcjy3FWmI.fCXKBdc27Bt", noRepeatableStages1);
    assertEquals("EPEcjy3FWmI.fCXKBdc27Bt", noRepeatableStages2);
    assertEquals("EPEcjy3FWmI.fCXKBdc27Bt", noRepeatableStages3);
  }
}
