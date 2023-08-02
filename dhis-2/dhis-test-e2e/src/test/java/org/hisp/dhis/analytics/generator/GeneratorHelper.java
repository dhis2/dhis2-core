/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.generator;

import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.hc.client5.http.utils.Base64.encodeBase64;
import static org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;

/** Helper class that provides auxiliary methods for the test generation. */
public class GeneratorHelper {
  /** Sets the generator implementation to be executed. */
  static final Generator GEN = TestGenerator.get();

  /** Test generation parameters. */
  static final int MAX_TESTS_PER_CLASS = GEN.getMaxTestsPerClass();

  static final String CLASS_NAME_PREFIX = GEN.getClassNamePrefix();
  static final String FILE = GEN.getFile();
  static final boolean ASSERT_META_DATA = GEN.assertMetaData();
  static final boolean ASSERT_ROW_INDEX = GEN.assertRowIndex();
  static final String ACTION = GEN.getAction();
  static final String ACTION_INSTANCE = GEN.getActionDeclaration();
  static final String PACKAGE = GEN.getPackage();
  static final String TOP_CLASS_COMMENT = GEN.getTopClassComment();

  /**
   * Generic header of the class including imports and class declaration. Some imports might be
   * redundant, but they will be cleaned-up during the final code formatting. The main goal is to
   * provide all imports required by all classes.
   *
   * @return the package + imports of the class.
   */
  static String getPackageAndImports() {
    return """
        package _package;

        import static org.hamcrest.Matchers.equalTo;
        import static org.hamcrest.Matchers.hasSize;
        import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
        import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
        import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

        import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
        import org.hisp.dhis.actions.analytics.AnalyticsEnrollmentsActions;
        import java.util.List;
        import java.util.Map;
        import org.hisp.dhis.AnalyticsApiTest;
        import org.hisp.dhis.actions.RestApiActions;
        import org.hisp.dhis.dto.ApiResponse;
        import org.hisp.dhis.helpers.QueryParamsBuilder;
        import org.json.JSONException;
        import org.json.JSONObject;
        import org.junit.jupiter.api.BeforeAll;
        import org.junit.jupiter.api.Test;
        """
        .replace("_package", PACKAGE);
  }

  /**
   * Declares a class based on the given class name. A basic documentation is also generated based
   * on the current internal constants.
   *
   * @param className the class name.
   * @return the code related to the class declaration.
   */
  static String getClassDeclaration(String className) {
    return """
        /**
         * _topComment
         */
        public class _className extends AnalyticsApiTest {
          _actionInstance
        """
        .replace("_topComment", TOP_CLASS_COMMENT)
        .replace("_className", className)
        .replace("_actionInstance", ACTION_INSTANCE);
  }

  /**
   * Generates the test method for the given method "name" and "url".
   *
   * @param name the method's name.
   * @param url the full URL associated with this test.
   * @return the full test method.
   */
  static String generateTestMethod(String name, String url) {
    if (isBlank(name) || isBlank(url)) {
      return EMPTY;
    }

    String testDeclaration = "@Test\n public void " + name + "() throws JSONException {\n";

    StringBuilder params = buildTestParams(url);
    String dimensionUid = getTargetDim(url);

    String testTarget = "\n// When \nApiResponse response = actions.get(params);\n";

    // If a target dimension is present, it should be used as part of the action call.
    if (isNotBlank(dimensionUid)) {
      testTarget =
          "\n// When\n ApiResponse response = actions."
              + ACTION
              + "().get(\""
              + dimensionUid
              + "\", JSON, JSON, params);\n";
    }

    String response = getResponse(url);

    if (isNotBlank(response)) {
      ReadContext ctx = JsonPath.parse(getResponse(url));

      String headersAssertion = buildHeadersAssertion(ctx);
      String responseAssertions = buildResponseAssertion(ctx);
      String metaDataAssertion = buildMetaDataAssertion(ctx);
      String rowsAssertion = buildRowsAssertion(ctx);

      return testDeclaration
          + params
          + testTarget
          + responseAssertions
          + metaDataAssertion
          + headersAssertion
          + rowsAssertion
          + "}";
    } else {
      System.err.println("## No Response. Check the URL requested.");
      return EMPTY;
    }
  }

  /**
   * Builds the assertions for all rows.
   *
   * @param ctx the {@link ReadContext}. The values for the assertions are extracted from it.
   * @return the built assertions.
   */
  private static String buildRowsAssertion(ReadContext ctx) {
    List<Map<String, Object>> rows = ctx.read("$.rows");
    StringBuilder rowsAssertion = new StringBuilder("\n// Assert rows.\n");

    for (int i = 0; i < rows.size(); i++) {
      JSONArray jsonArray = (JSONArray) rows.get(i);

      if (ASSERT_ROW_INDEX) {
        rowsAssertion.append("validateRow(response," + i + ", List.of(");
      } else {
        rowsAssertion.append("validateRow(response, List.of(");
      }

      rowsAssertion
          .append(
              jsonArray.stream()
                  .map(
                      v ->
                          "\""
                              + v.toString().replace(System.getProperty("line.separator"), "\\n")
                              + "\"")
                  .collect(joining(",")))
          .append("));\n");
    }

    return rowsAssertion.toString();
  }

  /**
   * Builds the assertions for the metadata if requested.
   *
   * @param ctx the {@link ReadContext}. The values for the assertions are extracted from it.
   * @return the built assertions.
   */
  private static String buildMetaDataAssertion(ReadContext ctx) {
    if (ASSERT_META_DATA) {
      return new StringBuilder("\n// Assert metaData. \n")
          .append("String expectedMetaData = ")
          .append(
              "\""
                  + new JSONObject(ctx.read("$.metaData"))
                      .toString()
                      .replace("\"", "\\\"")
                      .replace("\\/", "/")
                      .replace(System.getProperty("line.separator"), "\\n")
                  + "\";")
          .append("String actualMetaData = ")
          .append("new JSONObject((Map)response.extract(\"metaData\")).toString();\n")
          .append("assertEquals(expectedMetaData, actualMetaData, false);\n")
          .toString();
    }

    return EMPTY;
  }

  /**
   * Builds the assertions for the response body.
   *
   * @param ctx the {@link ReadContext}. The values for the assertions are extracted from it.
   * @return the built assertions.
   */
  private static String buildResponseAssertion(ReadContext ctx) {
    return new StringBuilder("\n// Then \n")
        .append("response.validate().statusCode(200)\n")
        .append(".body(\"headers\", hasSize(equalTo(" + ctx.read("$.headers.size()") + ")))\n")
        .append(".body(\"rows\", hasSize(equalTo(" + ctx.read("$.rows.size()") + ")))\n")
        .append(".body(\"height\", equalTo(" + ctx.read("$.height") + "))\n")
        .append(".body(\"width\", equalTo(" + ctx.read("$.width") + "))\n")
        .append(".body(\"headerWidth\", equalTo(" + ctx.read("$.headerWidth") + "));\n")
        .toString();
  }

  private static String buildHeadersAssertion(ReadContext ctx) {
    List<Map<String, Object>> headers = ctx.read("$.headers");

    StringBuilder headersAssertion = new StringBuilder("\n// Assert headers.\n");

    for (int i = 0; i < headers.size(); i++) {
      Map<String, Object> headerMap = headers.get(i);

      headersAssertion
          .append("validateHeader(response,")
          .append(i)
          .append(",")
          .append("\"" + headerMap.get("name") + "\"")
          .append(",")
          .append("\"" + headerMap.get("column") + "\"")
          .append(",")
          .append("\"" + headerMap.get("valueType") + "\"")
          .append(",")
          .append("\"" + headerMap.get("type") + "\"")
          .append(",")
          .append(headerMap.get("hidden"))
          .append(",")
          .append(headerMap.get("meta"))
          .append(");\n");
    }

    return headersAssertion.toString();
  }

  private static StringBuilder buildTestParams(String url) {
    String testParamsBuilder = "// Given \nQueryParamsBuilder params = new QueryParamsBuilder()";
    StringBuilder params = new StringBuilder(testParamsBuilder);

    Map<String, String> paramsMap = getParamsMap(url);

    paramsMap.forEach((k, v) -> params.append(".add(\"" + k + "=" + v + "\")\n"));
    params.append(";\n");

    return params;
  }

  private static Map<String, String> getParamsMap(String url) {
    Map<String, String> urlMap = new HashMap<>();
    String queryString = substringAfter(url, "?");

    for (String param : queryString.split("&")) {
      String paramName = substringBefore(param, "=");
      String paramValue = decode(substringAfter(param, "="), UTF_8);

      if (!urlMap.containsKey(paramName)) {
        urlMap.put(paramName, paramValue);
      } else {
        // In case of multiple parameters with the same name ("arrays").
        String currentValue = urlMap.get(paramName);
        urlMap.put(paramName, currentValue + "," + paramValue);
      }
    }

    return urlMap;
  }

  private static String getTargetDim(String url) {
    return substringAfterLast(substringBetween(url, "/", ".json"), "/");
  }

  /**
   * This method will authenticate and send a request based on the given "url".
   *
   * @param url the URL to query.
   * @return the HTTP response for the request.
   */
  private static String getResponse(String url) {
    String auth = "admin:district";
    byte[] encodedAuth = encodeBase64(auth.getBytes(ISO_8859_1));
    String authHeader = "Basic " + new String(encodedAuth);

    try {
      Content response =
          Request.Get(url).addHeader(AUTHORIZATION, authHeader).execute().returnContent();

      return response.asString();
    } catch (IOException e) {
      System.err.println("## ERROR: " + e.getLocalizedMessage());
    }

    return EMPTY;
  }
}
