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
package org.hisp.dhis.test.webapi;

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.CREATED;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.IntegrationH2Test;
import org.hisp.dhis.test.config.H2TestConfig;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base class for all Spring Mock MVC based controller tests which use an H2 in-memory DB.
 *
 * @author Jan Bernitt
 */
@IntegrationH2Test
@ActiveProfiles("test-h2")
@ContextConfiguration(classes = H2TestConfig.class)
public abstract class H2ControllerIntegrationTestBase extends ControllerIntegrationTestBase {

  protected final HttpResponse postCategory(
      String name, DataDimensionType type, List<String> options) {
    List<String> optionIds = postCategoryOptions(options);
    String optionsArray = optionIds.stream().map("{'id':'%s'}"::formatted).collect(joining(","));
    String body =
        """
        {
          'name': '%s',
          'shortName': '%s',
          'dataDimensionType': '%s',
          'categoryOptions': [%s]}
        }"""
            .formatted(name, name, type, optionsArray);
    return POST("/categories", body);
  }

  protected final List<String> postCategoryOptions(List<String> names) {
    List<String> ids = new ArrayList<>(names.size());
    for (String option : names) {
      String body = "{'name': '%s', 'shortName': '%s'}".formatted(option, option);
      ids.add(assertStatus(CREATED, POST("/categoryOptions", body)));
    }
    return ids;
  }

  protected final HttpResponse postCategoryCombo(
      String name, DataDimensionType type, List<String> categories) {
    String catObjects = categories.stream().map("{'id': '%s'}"::formatted).collect(joining(","));
    String body =
        "{'name': '%s', 'dataDimensionType': '%s', 'categories': [%s]}"
            .formatted(name, type, catObjects);
    return POST("/categoryCombos", body);
  }

  protected final List<String> toOrganisationUnitNames(JsonObject response) {
    return response
        .getList("organisationUnits", JsonIdentifiableObject.class)
        .toList(JsonIdentifiableObject::getDisplayName);
  }

  protected final String addOrganisationUnit(String name) {
    return assertStatus(
        CREATED,
        POST(
            "/organisationUnits",
            """
              {
                'name':'%s',
                'shortName':'%s',
                'openingDate':'2021',
                'description':'Org desc',
                'code':'Org code'
              }
            """
                .formatted(name, name)));
  }

  protected final String addOrganisationUnit(String name, String parentId) {
    return assertStatus(
        CREATED,
        POST(
            "/organisationUnits",
            "{'name':'"
                + name
                + "', 'shortName':'"
                + name
                + "', 'openingDate':'2021', 'parent': "
                + "{'id':'"
                + parentId
                + "'}"
                + " }"));
  }
}
