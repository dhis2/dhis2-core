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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.jupiter.api.Test;

class LegacyRequestParamsTest {

  @Test
  void getIdentifierParamIfTrackedEntityIsSet() {
    LegacyRequestParams legacyRequestParams = new LegacyRequestParams();
    legacyRequestParams.setTrackedEntity("Hq3Kc6HK4OZ");

    assertEquals("Hq3Kc6HK4OZ", legacyRequestParams.getIdentifierParam());
    assertEquals(
        "Hq3Kc6HK4OZ", legacyRequestParams.getIdentifierParam(), "should return cached identifier");
  }

  @Test
  void getIdentifierNameIfTrackedEntityIsSet() {
    LegacyRequestParams legacyRequestParams = new LegacyRequestParams();
    legacyRequestParams.setTrackedEntity("Hq3Kc6HK4OZ");

    assertEquals("trackedEntity", legacyRequestParams.getIdentifierName());
  }

  @Test
  void getIdentifierClassIfTrackedEntityIsSet() {
    LegacyRequestParams legacyRequestParams = new LegacyRequestParams();
    legacyRequestParams.setTrackedEntity("Hq3Kc6HK4OZ");

    assertEquals(TrackedEntity.class, legacyRequestParams.getIdentifierClass());
  }

  @Test
  void getIdentifierParamIfEnrollmentIsSet() {
    LegacyRequestParams legacyRequestParams = new LegacyRequestParams();
    legacyRequestParams.setEnrollment("Hq3Kc6HK4OZ");

    assertEquals("Hq3Kc6HK4OZ", legacyRequestParams.getIdentifierParam());
  }

  @Test
  void getIdentifierNameIfEnrollmentIsSet() {
    LegacyRequestParams legacyRequestParams = new LegacyRequestParams();
    legacyRequestParams.setEnrollment("Hq3Kc6HK4OZ");

    assertEquals("enrollment", legacyRequestParams.getIdentifierName());
  }

  @Test
  void getIdentifierClassIfEnrollmentIsSet() {
    LegacyRequestParams legacyRequestParams = new LegacyRequestParams();
    legacyRequestParams.setEnrollment("Hq3Kc6HK4OZ");

    assertEquals(Enrollment.class, legacyRequestParams.getIdentifierClass());
  }

  @Test
  void getIdentifierParamIfEventIsSet() {
    LegacyRequestParams legacyRequestParams = new LegacyRequestParams();
    legacyRequestParams.setEvent("Hq3Kc6HK4OZ");

    assertEquals("Hq3Kc6HK4OZ", legacyRequestParams.getIdentifierParam());
  }

  @Test
  void getIdentifierNameIfEventIsSet() {

    LegacyRequestParams legacyRequestParams = new LegacyRequestParams();
    legacyRequestParams.setEvent("Hq3Kc6HK4OZ");

    assertEquals("event", legacyRequestParams.getIdentifierName());
  }

  @Test
  void getIdentifierClassIfEventIsSet() {
    LegacyRequestParams legacyRequestParams = new LegacyRequestParams();
    legacyRequestParams.setEvent("Hq3Kc6HK4OZ");

    assertEquals(Event.class, legacyRequestParams.getIdentifierClass());
  }
}
