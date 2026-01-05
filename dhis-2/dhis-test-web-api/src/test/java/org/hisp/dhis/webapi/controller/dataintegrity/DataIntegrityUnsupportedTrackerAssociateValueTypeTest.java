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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.http.HttpStatus;
import org.junit.jupiter.api.Test;

class DataIntegrityUnsupportedTrackerAssociateValueTypeTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK = "tracker_associate_is_deprecated";

  private static final String DETAILS_ID_TYPE = "dataElements";

  @Test
  void testNoTrackerAssociateValueType() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataElements",
            "{ 'name': 'DE1', 'shortName': 'DE1', 'valueType' : 'TEXT',"
                + "'domainType' : 'TRACKER', 'aggregationType' : 'NONE'  }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/trackedEntityAttributes",
            "{ 'name': 'TEA1', 'shortName': 'TEA1', 'valueType' : 'TEXT', 'aggregationType' : 'NONE' }"));

    dbmsManager.clearSession();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void testDataElementWithTrackerAssociateValueType() {
    String dataElementId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'DE_TRACKER_ASSOCIATE', 'shortName': 'DE_TA', 'valueType' : 'TRACKER_ASSOCIATE',"
                    + "'domainType' : 'TRACKER', 'aggregationType' : 'NONE'  }"));

    dbmsManager.clearSession();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, CHECK, 100, dataElementId, "DE_TRACKER_ASSOCIATE", "Data Element", true);
  }

  @Test
  void testTrackedEntityAttributeWithTrackerAssociateValueType() {
    String teaId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/trackedEntityAttributes",
                "{ 'name': 'TEA_TRACKER_ASSOCIATE', 'shortName': 'TEA_TA', 'valueType' : 'TRACKER_ASSOCIATE', 'aggregationType' : 'NONE' }"));

    dbmsManager.clearSession();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK,
        100,
        teaId,
        "TEA_TRACKER_ASSOCIATE",
        "Tracked Entity Attribute",
        true);
  }

  @Test
  void testBothDataElementAndTrackedEntityAttributeWithTrackerAssociateValueType() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataElements",
            "{ 'name': 'DE_TRACKER_ASSOCIATE', 'shortName': 'DE_TA', 'valueType' : 'TRACKER_ASSOCIATE',"
                + "'domainType' : 'TRACKER', 'aggregationType' : 'NONE'  }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/trackedEntityAttributes",
            "{ 'name': 'TEA_TRACKER_ASSOCIATE', 'shortName': 'TEA_TA', 'valueType' : 'TRACKER_ASSOCIATE', 'aggregationType' : 'NONE' }"));

    dbmsManager.clearSession();

    // Should detect both issues
    checkDataIntegritySummary(CHECK, 2, 100, true);
  }

  @Test
  void testTrackerAssociateCheckRuns() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, false);
  }
}
