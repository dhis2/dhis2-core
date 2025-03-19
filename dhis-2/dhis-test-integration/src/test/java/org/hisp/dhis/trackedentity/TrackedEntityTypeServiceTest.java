/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.trackedentity;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class TrackedEntityTypeServiceTest extends PostgresIntegrationTestBase {

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Test
  void testAddShortNameAndCode() {
    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.setShortName("shortname");
    trackedEntityType.setCode("code");
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);

    TrackedEntityType persisted =
        trackedEntityTypeService.getTrackedEntityType(trackedEntityType.getId());

    Assertions.assertEquals(trackedEntityType.getShortName(), persisted.getShortName());
    Assertions.assertEquals(trackedEntityType.getCode(), persisted.getCode());
  }

  @Test
  void testAddDuplicateShortName() {
    TrackedEntityType trackedEntityType1 = createTrackedEntityType('A');
    trackedEntityType1.setShortName("shortname");
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType1);

    TrackedEntityType trackedEntityType2 = createTrackedEntityType('B');
    trackedEntityType2.setShortName("shortname");
    assertThrows(
        DataIntegrityViolationException.class,
        () -> trackedEntityTypeService.addTrackedEntityType(trackedEntityType2));
  }
}
