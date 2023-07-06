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
package org.hisp.dhis.dto;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

import io.restassured.response.ValidatableResponse;
import java.util.List;
import org.hamcrest.Matchers;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerApiResponse extends ApiResponse {

  public TrackerApiResponse(ApiResponse response) {
    super(response.raw);
  }

  public List<String> extractImportedTeis() {
    return this.extractList("bundleReport.typeReportMap.TRACKED_ENTITY.objectReports.uid");
  }

  public List<String> extractImportedEnrollments() {
    return this.extractList("bundleReport.typeReportMap.ENROLLMENT.objectReports.uid");
  }

  public List<String> extractImportedEvents() {
    return this.extractList("bundleReport.typeReportMap.EVENT.objectReports.uid");
  }

  public List<String> extractImportedRelationships() {
    return this.extractList("bundleReport.typeReportMap.RELATIONSHIP.objectReports.uid");
  }

  public TrackerApiResponse validateSuccessfulImport() {
    return validateSuccessfulImportWithIgnored(0);
  }

  public TrackerApiResponse validateSuccessfulImportWithIgnored(int ignoredCount) {
    validate()
        .statusCode(200)
        .body("status", equalTo("OK"))
        .body("stats.ignored", equalTo(ignoredCount))
        .body("stats.total", greaterThanOrEqualTo(1))
        .body("bundleReport.typeReportMap", notNullValue());

    return this;
  }

  public ValidatableResponse validateErrorReport() {
    return validate()
        .statusCode(Matchers.oneOf(409, 200))
        .body("stats.ignored", greaterThanOrEqualTo(1))
        .body("validationReport.errorReports", Matchers.notNullValue())
        .rootPath("validationReport.errorReports");
  }

  public ValidatableResponse validateWarningReport() {
    return validate()
        .statusCode(Matchers.oneOf(409, 200))
        .body("validationReport.warningReports", Matchers.notNullValue())
        .rootPath("validationReport.warningReports");
  }

  public ValidatableResponse validateTeis() {
    return validate()
        .body("bundleReport.typeReportMap.TRACKED_ENTITY", notNullValue())
        .rootPath("bundleReport.typeReportMap.TRACKED_ENTITY");
  }

  public ValidatableResponse validateEvents() {
    return validate()
        .body("bundleReport.typeReportMap.EVENT", notNullValue())
        .rootPath("bundleReport.typeReportMap.EVENT");
  }

  public ValidatableResponse validateEnrollments() {
    return validate()
        .body("bundleReport.typeReportMap.ENROLLMENT", notNullValue())
        .rootPath("bundleReport.typeReportMap.ENROLLMENT");
  }

  public ValidatableResponse validateRelationships() {
    return validate()
        .body("bundleReport.typeReportMap.RELATIONSHIP", notNullValue())
        .rootPath("bundleReport.typeReportMap.RELATIONSHIP");
  }
}
