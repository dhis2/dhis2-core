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
package org.hisp.dhis.test.e2e.actions;

import static io.restassured.config.XmlConfig.xmlConfig;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import com.google.gson.JsonArray;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Headers;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.File;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.test.e2e.EndpointTracker;
import org.hisp.dhis.test.e2e.TestRunStorage;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.dto.ImportSummary;
import org.hisp.dhis.test.e2e.dto.ObjectReport;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class RestApiActions {
  protected String endpoint;

  private String baseUri;

  public RestApiActions(final String endpoint) {
    this.baseUri = RestAssured.baseURI;
    this.endpoint = endpoint;
  }

  public RestApiActions setBaseUri(String baseUri) {
    this.baseUri = baseUri;

    return this;
  }

  protected RequestSpecification given() {
    return RestAssured.given()
        .baseUri(this.baseUri)
        .basePath(endpoint)
        .config(
            RestAssured.config().objectMapperConfig(new ObjectMapperConfig(ObjectMapperType.GSON)));
  }

  /**
   * Sends post request to specified endpoint. If post request successful, saves created entity in
   * TestRunStorage
   *
   * @param object Body of request
   * @return Response
   */
  public ApiResponse post(Object object) {
    return post("", object, null);
  }

  public ApiResponse post(String resource, Object object) {
    return post(resource, ContentType.JSON.toString(), object, null);
  }

  public ApiResponse post(String resource, Object object, QueryParamsBuilder queryParams) {
    return post(resource, ContentType.JSON.toString(), object, queryParams);
  }

  public ApiResponse post(Object object, QueryParamsBuilder queryParamsBuilder) {
    return post("", ContentType.JSON.toString(), object, queryParamsBuilder);
  }

  public ApiResponse post(
      String resource, String contentType, Object object, QueryParamsBuilder queryParams) {
    String path = queryParams == null ? "" : queryParams.build();
    addCoverage("POST", resource + path);

    ApiResponse response =
        new ApiResponse(
            this.given().body(object).contentType(contentType).when().post(resource + path));

    saveCreatedObjects(response);

    return response;
  }

  /**
   * Shortcut used in preconditions only. Sends post request to specified endpoint and verifies that
   * request was successful
   *
   * @param object Body of request
   * @return ID of generated entity.
   */
  public String create(Object object) {
    ApiResponse response = post(object);

    response.validate().statusCode(is(oneOf(200, 201)));

    return response.extractUid();
  }

  /**
   * Sends get request with provided path appended to URL.
   *
   * @param path ID of resource
   * @return Response
   */
  public ApiResponse get(String path) {
    return get(path, null);
  }

  /**
   * Sends get request to specified endpoint
   *
   * @return Response
   */
  public ApiResponse get() {
    return get("");
  }

  /**
   * Sends get request with provided path and queryParams appended to URL.
   *
   * @param resourceId Id of resource
   * @param queryParamsBuilder Query params to append to url
   */
  public ApiResponse get(String resourceId, QueryParamsBuilder queryParamsBuilder) {
    String path = queryParamsBuilder == null ? "" : queryParamsBuilder.build();

    addCoverage("GET", resourceId + path);

    Response response = this.given().contentType(ContentType.TEXT).when().get(resourceId + path);

    return new ApiResponse(response);
  }

  /**
   * Sends get request with provided path, headers & queryParams appended to URL.
   *
   * @param resourceId Id of resource
   * @param queryParamsBuilder Query params to append to url
   * @param headers headers to send as part of the request
   */
  public ApiResponse getWithHeaders(
      String resourceId, QueryParamsBuilder queryParamsBuilder, Headers headers) {
    String path = queryParamsBuilder == null ? "" : queryParamsBuilder.build();

    addCoverage("GET", resourceId + path);

    Response response =
        this.given().contentType(ContentType.TEXT).headers(headers).when().get(resourceId + path);

    return new ApiResponse(response);
  }

  public ApiResponse get(QueryParamsBuilder queryParamsBuilder) {
    return this.get("", queryParamsBuilder);
  }

  /**
   * Sends get request with provided path, contentType, accepting content type and queryParams
   * appended to URL.
   *
   * @param resourceId Id of resource
   * @param contentType Content type of the request
   * @param accept Accepted response Content type
   * @param queryParamsBuilder Query params to append to url
   */
  public ApiResponse get(
      String resourceId, String contentType, String accept, QueryParamsBuilder queryParamsBuilder) {
    String path = queryParamsBuilder == null ? "" : queryParamsBuilder.build();

    addCoverage("GET", resourceId + path);

    Response response =
        this.given()
            .config(RestAssured.config().xmlConfig(xmlConfig().namespaceAware(false)))
            .contentType(contentType)
            .accept(accept)
            .when()
            .get(resourceId + path);

    return new ApiResponse(response);
  }

  /**
   * Sends delete request to specified resource. If delete request successful, removes entity from
   * TestRunStorage.
   *
   * @param resourceId Id of resource
   * @param queryParamsBuilder Query params to append to url
   */
  public ApiResponse delete(String resourceId, QueryParamsBuilder queryParamsBuilder) {
    String path = queryParamsBuilder == null ? "" : queryParamsBuilder.build();

    return delete(resourceId + path);
  }

  /**
   * Sends delete request to specified resource. If delete request successful, removes entity from
   * TestRunStorage.
   *
   * @param path Id of resource
   */
  public ApiResponse delete(String path) {
    Response response = this.given().when().delete(path);

    addCoverage("DELETE", path);

    if (response.statusCode() == 200) {
      TestRunStorage.removeEntity(endpoint, path);
    }

    return new ApiResponse(response);
  }

  /**
   * Sends PUT request to specified resource.
   *
   * @param resourceId Id of resource
   * @param object Body of request
   */
  public ApiResponse update(String resourceId, Object object) {
    Response response = this.given().body(object, ObjectMapperType.GSON).when().put(resourceId);

    addCoverage("PUT", resourceId);
    return new ApiResponse(response);
  }

  /**
   * Sends PUT request to specified resource with no body.
   *
   * @param resource resource
   */
  public ApiResponse updateNoBody(String resource) {
    Response response = this.given().when().put(resource);

    addCoverage("PUT", resource);
    return new ApiResponse(response);
  }

  /** Sends PATCH request to specified resource */
  public ApiResponse patch(String resourceId, Object object, QueryParamsBuilder paramsBuilder) {
    Response response =
        this.given()
            .body(object, ObjectMapperType.GSON)
            .when()
            .contentType("application/json-patch+json")
            .patch(resourceId + paramsBuilder.build());

    addCoverage("PATCH", resourceId + paramsBuilder.build());
    return new ApiResponse(response);
  }

  /** Sends PATCH request to specified resource. Uses importReportMode=ERRORS */
  public ApiResponse patch(String resourceId, Object object) {
    return this.patch(
        resourceId, object, new QueryParamsBuilder().add("importReportMode", "ERRORS"));
  }

  public ApiResponse patch(String resourceId, String operation, String path, String value) {
    JsonArray body =
        JsonObjectBuilder.jsonObject()
            .addProperty("op", operation)
            .addProperty("path", path)
            .addProperty("value", value)
            .wrapIntoArray();

    return this.patch(resourceId, body);
  }

  public ApiResponse postFile(File file) {
    return this.postFile(file, null);
  }

  public ApiResponse postFile(File file, QueryParamsBuilder queryParamsBuilder) {
    return this.postFile(file, queryParamsBuilder, null);
  }

  public ApiResponse postFileWithContentType(
      File file, QueryParamsBuilder queryParamsBuilder, String contentType) {
    return this.postFile(file, queryParamsBuilder, contentType);
  }

  public ApiResponse postMultiPartFile(File file) {
    return new ApiResponse(
        given().multiPart("file", file).contentType("multipart/form-data").when().post());
  }

  public ApiResponse postMultiPartFile(
      File file, String fileContentType, String resource, QueryParamsBuilder queryParamsBuilder) {
    String params = queryParamsBuilder == null ? "" : queryParamsBuilder.build();

    ApiResponse response =
        new ApiResponse(
            given()
                .multiPart("file", file, fileContentType)
                .contentType("multipart/form-data")
                .when()
                .post(resource + params));

    return response;
  }

  public ApiResponse postFile(
      File file, QueryParamsBuilder queryParamsBuilder, String contentType) {
    String url = queryParamsBuilder == null ? "" : queryParamsBuilder.build();
    String content = contentType != null ? contentType : "application/json";

    ApiResponse response =
        new ApiResponse(this.given().body(file).contentType(content).when().post(url));

    addCoverage("POST", url);

    saveCreatedObjects(response);

    return response;
  }

  private void saveCreatedObjects(ApiResponse response) {
    if (!response.getContentType().contains("json")) {
      return;
    }

    if (response.containsImportSummaries()) {
      List<ImportSummary> importSummaries = response.getSuccessfulImportSummaries();
      importSummaries.forEach(
          importSummary -> TestRunStorage.addCreatedEntity(endpoint, importSummary.getReference()));
      return;
    }

    if (response.getTypeReports() != null) {
      SchemasActions schemasActions = new SchemasActions();
      response.getTypeReports().stream()
          .filter(
              typeReport ->
                  typeReport.getStats().getCreated() != 0
                      || typeReport.getStats().getImported() != 0)
          .forEach(
              tr -> {
                List<ObjectReport> objectReports = tr.getObjectReports();

                if (!CollectionUtils.isEmpty(objectReports)) {
                  String ep = schemasActions.findSchemaPropertyByKlassName(tr.getKlass(), "plural");

                  objectReports.forEach(
                      or -> {
                        String uid = or.getUid();
                        TestRunStorage.addCreatedEntity(ep, uid);
                      });
                }
              });

      return;
    }
    if (response.isEntityCreated()) {
      this.addCreatedEntity(endpoint, response.extractUid());
    }
  }

  private void addCoverage(String method, String ep) {
    if (Boolean.FALSE.equals(TestConfiguration.get().shouldTrackEndpoints())) {
      return;
    }

    EndpointTracker.add(method, this.endpoint + "/" + ep);
  }

  protected void addCreatedEntity(String ep, String id) {
    TestRunStorage.addCreatedEntity(ep, id);
  }
}
