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
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.jsontree.*;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class AbstractDataIntegrityIntegrationTest extends DhisControllerIntegrationTest {
  final JsonDataIntegrityDetails getDetails(String check) {

    JsonObject content = GET("/dataIntegrity/details?checks={check}&timeout=1000", check).content();
    JsonDataIntegrityDetails details =
        content.get(check.replace('-', '_'), JsonDataIntegrityDetails.class);
    assertTrue(
        details.exists(), "check " + check + " did not complete in time or threw an exception");
    assertTrue(details.isObject());
    return details;
  }

  final void postDetails(String check) {
    HttpResponse trigger = POST("/dataIntegrity/details?checks=" + check);
    assertEquals("http://localhost/dataIntegrity/details?checks=" + check, trigger.location());
    assertTrue(trigger.content().isA(JsonWebMessage.class));
  }

  final JsonDataIntegritySummary getSummary(String check) {
    JsonObject content = GET("/dataIntegrity/summary?checks={check}&timeout=1000", check).content();
    JsonDataIntegritySummary summary =
        content.get(check.replace('-', '_'), JsonDataIntegritySummary.class);
    assertTrue(summary.exists());
    assertTrue(summary.isObject());
    return summary;
  }

  public final void postSummary(String check) {
    HttpResponse trigger = POST("/dataIntegrity/summary?checks=" + check);
    assertEquals("http://localhost/dataIntegrity/summary?checks=" + check, trigger.location());
    assertTrue(trigger.content().isA(JsonWebMessage.class));
  }

  void checkDataIntegritySummary(
      String check, Integer expectedCount, Integer expectedPercentage, Boolean hasPercentage) {

    postSummary(check);

    JsonDataIntegritySummary summary = getSummary(check);
    assertEquals(expectedCount, summary.getCount());

    if (hasPercentage) {
      assertEquals(expectedPercentage, summary.getPercentage().intValue());
    } else {
      assertNull(summary.getPercentage());
    }
  }

  private Boolean hasComments(JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues) {
    return issues.stream()
        .map(issue -> issue.has("comment"))
        .reduce(Boolean.FALSE, Boolean::logicalOr);
  }

  void checkDataIntegrityDetailsIssues(
      String check,
      String expectedDetailsUnits,
      String expectedDetailsNames,
      String expectedDetailsComments,
      String issueType) {
    postDetails(check);

    JsonDataIntegrityDetails details = getDetails(check);
    JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();
    assertTrue(issues.exists());
    assertEquals(1, issues.size());

    if (expectedDetailsUnits != null) {
      assertEquals(expectedDetailsUnits, issues.get(0).getId());
    } else {
      assertNull(issues.get(0).getId());
    }

    if (expectedDetailsNames != null) {
      assertTrue(issues.get(0).getName().startsWith(expectedDetailsNames));
    }

    /* This can be empty if comments do not exist in the JSON response. */

    if (hasComments(issues) && expectedDetailsComments != null) {
      assertTrue(issues.get(0).getComment().toString().contains(expectedDetailsComments));
    }

    assertEquals(issueType, details.getIssuesIdType());
  }

  void checkDataIntegrityDetailsIssues(
      String check,
      Set<String> expectedDetailsUnits,
      Set<String> expectedDetailsNames,
      Set<String> expectedDetailsComments,
      String issueType) {

    postDetails(check);

    JsonDataIntegrityDetails details = getDetails(check);
    JsonList<JsonDataIntegrityDetails.JsonDataIntegrityIssue> issues = details.getIssues();

    assertTrue(issues.exists());
    assertEquals(expectedDetailsUnits.size(), issues.size());

    /* Always check the UIDs */
    Set<String> issueUIDs = issues.stream().map(issue -> issue.getId()).collect(Collectors.toSet());
    assertEquals(issueUIDs, expectedDetailsUnits);

    /*
     * Names can be optionally checked, but should always exist in the
     * response
     */
    if (!expectedDetailsNames.isEmpty()) {
      Set<String> detailsNames =
          issues.stream().map(issue -> issue.getName()).collect(Collectors.toSet());
      assertEquals(expectedDetailsNames, detailsNames);
    }
    /* This can be empty if comments do not exist in the JSON response. */
    if (hasComments(issues) && !expectedDetailsComments.isEmpty()) {
      Set<String> detailsComments =
          issues.stream()
              .map(issue -> issue.getComment().string())
              .collect(Collectors.toUnmodifiableSet());
      assertEquals(expectedDetailsComments, detailsComments);
    }

    assertEquals(issueType, details.getIssuesIdType());
  }

  final void assertHasDataIntegrityIssues(
      String issueType,
      String check,
      Integer expectedPercentage,
      String expectedDetailsUnit,
      String expectedDetailsName,
      String expectedDetailsComment,
      Boolean hasPercentage) {
    checkDataIntegritySummary(check, 1, expectedPercentage, hasPercentage);

    checkDataIntegrityDetailsIssues(
        check, expectedDetailsUnit, expectedDetailsName, expectedDetailsComment, issueType);
  }

  final void assertHasDataIntegrityIssues(
      String issueType,
      String check,
      Integer expectedPercentage,
      Set<String> expectedDetailsUnits,
      Set<String> expectedDetailsNames,
      Set<String> expectedDetailsComments,
      Boolean hasPercentage) {
    checkDataIntegritySummary(
        check, expectedDetailsUnits.size(), expectedPercentage, hasPercentage);
    checkDataIntegrityDetailsIssues(
        check, expectedDetailsUnits, expectedDetailsNames, expectedDetailsComments, issueType);
  }

  final void assertHasNoDataIntegrityIssues(String issueType, String check, Boolean expectPercent) {
    checkDataIntegritySummary(check, 0, 0, expectPercent);
    Set<String> emptyStringSet = Set.of();
    checkDataIntegrityDetailsIssues(
        check, emptyStringSet, emptyStringSet, emptyStringSet, issueType);
  }

  final void assertNamedMetadataObjectExists(String endpoint, String name) {
    JsonObject response = GET("/" + endpoint + "/?filter=name:eq:" + name).content();
    JsonArray dimensions = response.getArray(endpoint);
    assertEquals(1, dimensions.size());
  }

  final void deleteAllMetadataObjects(String endpoint) {
    GET("/" + endpoint + "/gist?fields=id&headless=true")
        .content()
        .stringValues()
        .forEach(id -> DELETE("/" + endpoint + "/" + id));
    JsonObject response = GET("/" + endpoint).content();
    JsonArray dimensions = response.getArray(endpoint);
    assertEquals(0, dimensions.size());
  }

  final void deleteAllOrgUnits() {
    deleteAllMetadataObjects("organisationUnits");
  }

  void deleteMetadataObject(String endpoint, String uid) {

    if (uid != null) {
      assertStatus(HttpStatus.OK, DELETE("/" + endpoint + "/" + uid));
      assertStatus(HttpStatus.NOT_FOUND, GET("/" + endpoint + "/" + uid));
    }
  }

  @Autowired private TransactionTemplate txTemplate;

  protected void doInTransaction(Runnable operation) {

    txTemplate.execute(
        status -> {
          operation.run();
          return null;
        });
  }

  protected final HttpResponse postNewDataValue(
      String period,
      String value,
      String comment,
      boolean followup,
      String dataElementId,
      String orgUnitId) {
    String defaultCOC = getDefaultCOC();
    return POST(
        "/dataValues?de={de}&pe={pe}&ou={ou}&co={coc}&value={val}&comment={comment}&followUp={followup}",
        dataElementId,
        period,
        orgUnitId,
        defaultCOC,
        value,
        comment,
        followup);
  }

  protected final HttpResponse deleteDataValue(
      String period,
      String value,
      String comment,
      boolean followup,
      String dataElementId,
      String orgUnitId) {
    String defaultCOC = getDefaultCOC();
    return DELETE(
        "/dataValues?de={de}&pe={pe}&ou={ou}&co={coc}&value={val}&comment={comment}&followUp={followup}",
        dataElementId,
        period,
        orgUnitId,
        defaultCOC,
        value,
        comment,
        followup);
  }

  protected final String getDefaultCatCombo() {
    JsonObject ccDefault =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0);
    return ccDefault.getString("id").string();
  }

  protected final String getDefaultCOC() {
    JsonObject ccDefault =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0);
    return ccDefault.getArray("categoryOptionCombos").getString(0).string();
  }
}
