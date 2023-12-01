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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonDataElement;
import org.hisp.dhis.webapi.json.domain.JsonOption;
import org.hisp.dhis.webapi.json.domain.JsonOptionSet;
import org.junit.jupiter.api.Test;

/**
 * Test for option sets which are not used. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/option_sets/unused_option_sets.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOptionSetsNotUsedControllerTest extends AbstractDataIntegrityIntegrationTest {
  private static final String check = "options_sets_unused";

  private static final String detailsIdType = "optionSets";

  private String goodOptionSet;

  @Test
  void testOptionSetNotUsed() {

    goodOptionSet =
        assertStatus(
            HttpStatus.CREATED,
            POST("/optionSets", "{ 'name': 'Taste', 'shortName': 'Taste', 'valueType' : 'TEXT' }"));

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
            "/options",
            "{ 'code': 'SOUR',"
                + "  'sortOrder': 2,"
                + "  'name': 'Sour',"
                + "  'optionSet': { "
                + "    'id': '"
                + goodOptionSet
                + "'"
                + "  }}"));

    JsonObject content = GET("/optionSets?fields=id,name,options[id]").content();
    JsonList<JsonOptionSet> myOptionSets = content.getList(detailsIdType, JsonOptionSet.class);
    assertEquals(1, myOptionSets.size());
    JsonOptionSet myOptionSet = myOptionSets.get(0);
    assertEquals(goodOptionSet, myOptionSet.getId());
    JsonList<JsonOption> optionSetOptions = myOptionSet.getOptions();
    assertEquals(2, optionSetOptions.size());

    assertHasDataIntegrityIssues(detailsIdType, check, 100, goodOptionSet, "Taste", null, true);
  }

  @Test
  void testOptionSetsUsed() {

    goodOptionSet =
        assertStatus(
            HttpStatus.CREATED,
            POST("/optionSets", "{ 'name': 'Taste', 'shortName': 'Taste', 'valueType' : 'TEXT' }"));

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
            "/options",
            "{ 'code': 'SOUR',"
                + "  'sortOrder': 2,"
                + "  'name': 'Sour',"
                + "  'optionSet': { "
                + "    'id': '"
                + goodOptionSet
                + "'"
                + "  }}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataElements",
            "{ 'name': 'Candy', 'shortName': 'Candy', 'valueType' : 'TEXT',  "
                + "'domainType' : 'AGGREGATE', 'aggregationType' : 'NONE',"
                + "'optionSet' : { 'id' : '"
                + goodOptionSet
                + "'}  }"));

    JsonObject content = GET("/dataElements/?fields=id,name,optionSet").content();
    JsonList<JsonDataElement> testDataElementJSON =
        content.getList("dataElements", JsonDataElement.class);
    assertEquals(1, testDataElementJSON.size());
    assertEquals("Candy", testDataElementJSON.get(0).getName());
    assertEquals(goodOptionSet, testDataElementJSON.get(0).getOptionSet().getId());

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testInvalidCategoriesDivideByZero() {

    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }
}
