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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.http.HttpStatus;
import org.junit.jupiter.api.Test;

class DataIntegrityOptionGroupsWithoutOptions extends AbstractDataIntegrityIntegrationTest {
  private static final String CHECK_NAME = "option_groups_empty";

  private static final String DETAILS_ID_TYPE = "optionGroups";

  @Test
  void testOptionGroupsWithNoOptions() {

    String goodOptionGroup =
        assertStatus(
            HttpStatus.CREATED, POST("/optionGroups", "{ 'name': 'Taste', 'shortName': 'Taste' }"));

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, CHECK_NAME, 100, goodOptionGroup, "Taste", null, true);
  }

  @Test
  void testOptionGroupsWithOptions() {
    String goodOptionSet =
        assertStatus(
            HttpStatus.CREATED,
            POST("/optionSets", "{ 'name': 'Taste', 'shortName': 'Taste', 'valueType' : 'TEXT' }"));

    String sweetOption =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/options",
                "{ 'code': 'SWEET',"
                    + "  'sortOrder': 1,"
                    + "  'name': 'Sweet',"
                    + "  'optionSet': { "
                    + "    'id': '"
                    + goodOptionSet
                    + "'"
                    + "  }}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/optionGroups",
            "{ 'name': 'Taste', 'shortName': 'Taste' , 'optionSet' : { 'id' : '"
                + goodOptionSet
                + "' }, 'options' : [ { 'id' : '"
                + sweetOption
                + "' } ] }"));

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK_NAME, true);
  }
}
