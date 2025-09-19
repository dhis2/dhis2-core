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
package org.hisp.dhis.webapi.controller;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Body;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.annotation.Transactional;

@Transactional
abstract class AbstractDataValueControllerTest extends PostgresControllerIntegrationTestBase {

  protected String dataSetId;
  protected String dataElementId;
  protected String orgUnitId;
  protected String categoryComboId;
  protected String categoryOptionComboId;

  @BeforeEach
  void setUp() {
    orgUnitId = addOrganisationUnit("OU1");
    List<String> orgUnitIds =
        Stream.concat(Stream.of(orgUnitId), setUpAdditionalOrgUnits().stream()).toList();
    // add OU to users hierarchy
    assertStatus(
        HttpStatus.OK,
        POST(
            "/users/{id}/organisationUnits",
            getCurrentUser().getUid(),
            Body("{'additions':[{'id':'" + orgUnitId + "'}]}")));
    JsonObject ccDefault =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0);
    categoryComboId = ccDefault.getString("id").string();
    categoryOptionComboId = ccDefault.getArray("categoryOptionCombos").getString(0).string();
    dataElementId =
        addDataElement("My data element", "DE1", ValueType.INTEGER, null, categoryComboId);

    List<String> dataElementIds =
        Stream.concat(Stream.of(dataElementId), setUpAdditionalDataElements().stream()).toList();
    dataSetId = addDataSet("My data set", "MDS", dataElementIds, orgUnitIds);

    // Add the newly created org unit to the superuser's hierarchy
    User user = userService.getUser(getAdminUser().getUid());
    for (String ou : orgUnitIds) {
      OrganisationUnit unit = manager.get(OrganisationUnit.class, ou);
      user.addOrganisationUnit(unit);
    }
    userService.updateUser(user);

    switchToAdminUser();
  }

  protected List<String> setUpAdditionalDataElements() {
    return List.of();
  }

  protected List<String> setUpAdditionalOrgUnits() {
    return List.of();
  }

  /**
   * @return UID of the created {@link org.hisp.dhis.datavalue.DataValue}
   */
  protected final void addDataValue(String period, String value, String comment, boolean followup) {
    addDataValue(period, value, comment, followup, dataElementId, orgUnitId);
  }

  /**
   * @return UID of the created {@link org.hisp.dhis.datavalue.DataValue}
   */
  protected final void addDataValue(
      String period,
      String value,
      String comment,
      boolean followup,
      String dataElementId,
      String orgUnitId) {
    assertStatus(
        HttpStatus.CREATED,
        postNewDataValue(period, value, comment, followup, dataElementId, orgUnitId));
  }

  protected final HttpResponse postNewDataValue(
      String period,
      String value,
      String comment,
      boolean followup,
      String dataElementId,
      String orgUnitId) {
    return POST(
        "/dataValues?de={de}&pe={pe}&ou={ou}&co={coc}&value={val}&comment={comment}&followUp={followup}",
        dataElementId,
        period,
        orgUnitId,
        categoryOptionComboId,
        value,
        comment,
        followup);
  }

  protected final JsonArray getDataValues(String de, String pe, String ou) {
    return getDataValues(de, categoryOptionComboId, null, null, pe, ou);
  }

  protected final JsonArray getDataValues(
      String de, String co, String cc, String cp, String pe, String ou) {
    List<String> params = new ArrayList<>();
    String url = "/dataValues";
    if (!isEmpty(de)) params.add("de=" + de);
    if (!isEmpty(co)) params.add("co=" + co);
    if (!isEmpty(cc)) params.add("cc=" + cc);
    if (!isEmpty(cp)) params.add("cp=" + cp);
    if (!isEmpty(pe)) params.add("pe=" + pe);
    if (!isEmpty(ou)) params.add("ou=" + ou);
    if (!params.isEmpty()) url += "?" + String.join("&", params);
    return GET(url).content();
  }
}
