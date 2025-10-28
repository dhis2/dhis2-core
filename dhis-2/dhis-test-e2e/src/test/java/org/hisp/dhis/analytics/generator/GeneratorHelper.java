/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.generator;

import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.hc.client5.http.utils.Base64.encodeBase64;
import static org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.client.fluent.Request.Get;
import static org.hisp.dhis.test.e2e.helpers.config.TestConfiguration.get;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minidev.json.JSONObject;
import org.apache.http.client.fluent.Content;

/** Helper class that provides auxiliary methods for the test generation. */
public class GeneratorHelper {
  /** Sets the generator implementation to be executed. */
  static final Generator GEN = TestGenerator.get();

  /** Test generation parameters. */
  static final int MAX_TESTS_PER_CLASS = GEN.getMaxTestsPerClass();

  static final String CLASS_NAME_PREFIX = GEN.getClassNamePrefix();
  static final String SCENARIO_FILE = GEN.getScenarioFile();
  static final String SCENARIO_FILE_LOCATION =
      "./src/test/java/org/hisp/dhis/analytics/generator/"
          + GEN.getScenarioFolder()
          + "/"
          + SCENARIO_FILE;
  static final String[] SCENARIOS = GEN.getScenarios();
  static final String ACTION = GEN.getAction();
  static final String ACTION_INSTANCE = GEN.getActionDeclaration();
  static final String PACKAGE = GEN.getPackage();
  static final String TOP_CLASS_COMMENT = GEN.getTopClassComment();
  static final String TEST_OUTPUT_LOCATION =
      "./src/test/java/org/hisp/dhis/analytics/generator/output/";

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

        import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
        import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderExistence;
        import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
        import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
        import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

        import java.util.List;
        import java.util.Map;
        import java.util.stream.Collectors;
        import org.hisp.dhis.AnalyticsApiTest;
        import org.hisp.dhis.analytics.ValidationHelper;
        import org.json.JSONException;
        import org.json.JSONObject;
        import org.junit.jupiter.api.BeforeAll;
        import org.junit.jupiter.api.Test;
        import org.hisp.dhis.test.e2e.dto.ApiResponse;
        import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
        import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
        import org.apache.commons.lang3.BooleanUtils;
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
   * Generates the test method for the given method "name" and "query". This method now always
   * generates a test structure using name-based validation helpers, incorporating conditional logic
   * (via buildTestConfigurationBlock and buildHeadersAssertion) that adapts at runtime based on the
   * 'expect.postgis' system property.
   *
   * @param name the method's name.
   * @param query the full URL associated with this test.
   * @return the full test method string.
   */
  static String generateTestMethod(String name, String query) {
    if (isBlank(name) || isBlank(query)) {
      return EMPTY;
    }

    String testDeclaration =
        "  @Test\n  public void " + name + "() throws JSONException {\n"; // Indent test method

    // --- Build Params (Common) ---
    StringBuilder paramsBlockBuilder = buildTestParams(query); // Keep as StringBuilder

    String dimensionUid = getTargetDim(query);

    // --- Determine API Call Target (Common) ---
    String testTarget;
    if (isNotBlank(dimensionUid)) {
      testTarget =
          "\n    // When\n    ApiResponse response = actions."
              + ACTION
              + "().get(\""
              + dimensionUid
              + "\", JSON, JSON, params);\n";
    } else {
      testTarget =
          "\n    // When\n    ApiResponse response = actions."
              + ACTION
              + "().get(\"\", JSON, JSON, params);\n";
    }

    // --- Get Live Response (Common) ---
    String responseJson = getResponse(query);

    if (isNotBlank(responseJson)) {
      ReadContext ctx = JsonPath.parse(responseJson);

      // --- Extract Actual Counts and Headers from Live Response ---
      int actualRowCount = 0;
      List<Map<String, Object>> actualHeadersFromCtx = List.of(); // Default to empty list
      boolean rowContextPresent = false;

      try {
        actualRowCount = ctx.read("$.rows.size()");
        actualHeadersFromCtx = ctx.read("$.headers"); // Read headers
      } catch (PathNotFoundException e) {
        return EMPTY;
      }

      // --- Calculate Expected Header Counts based on Live Response ---
      int observedHeaderCount = actualHeadersFromCtx.size();
      long observedPostgisHeaderCount =
          actualHeadersFromCtx.stream()
              .map(h -> (String) h.get("name"))
              .filter(
                  aName ->
                      aName != null && List.of("geometry", "longitude", "latitude").contains(aName))
              .count();

      int expectedHeaderCountWithPostgis;
      int expectedHeaderCountWithoutPostgis;

      // Determine the expected counts based on what was observed
      if (observedPostgisHeaderCount > 0) {
        // Live response included PostGIS headers (e.g., generator run against PostGIS)
        expectedHeaderCountWithPostgis = observedHeaderCount;
        expectedHeaderCountWithoutPostgis = observedHeaderCount - (int) observedPostgisHeaderCount;
      } else {
        // Live response did NOT include PostGIS headers (e.g., generator run against Doris)
        expectedHeaderCountWithoutPostgis = observedHeaderCount;
        // Estimate what the count *would* be with PostGIS headers (assuming only the 3 standard
        // ones are added)
        expectedHeaderCountWithPostgis = observedHeaderCount + 3;
      }
      // --- End Calculate Expected Header Counts ---

      // --- Generate Test Components (Always Name-Based Style) ---

      // 1. Configuration Block
      String testConfigBlock = buildTestConfigurationBlock();

      // 2. Structure Assertion
      String structureAssertions =
          buildStructureAssertion(
              ctx,
              actualRowCount,
              expectedHeaderCountWithPostgis,
              expectedHeaderCountWithoutPostgis);

      // 3. Headers Extraction (Generates 'actualHeaders' variable)
      String headersExtractionBlock = buildHeadersExtractionBlock(ctx);

      // 4. Metadata Assertion
      String metaDataAssertion = buildMetaDataAssertion(ctx);
      // 5. Headers Assertion
      String headersAssertion = buildHeadersAssertion(ctx, actualHeadersFromCtx);
      // 6. Row Context Assertion
      String rowContextAssertion =
          buildRowContextAssertion(ctx, actualHeadersFromCtx, rowContextPresent);
      // 7. Rows Assertion
      String rowsAssertion = buildRowsAssertion(ctx, actualHeadersFromCtx);

      // --- Assemble the Test Method String ---
      return testDeclaration
          + indentBlock(testConfigBlock, 4)
          + indentBlock(paramsBlockBuilder.toString(), 4)
          + indentBlock(testTarget, 4)
          + "\n    // Then\n"
          + indentBlock(structureAssertions, 4) // This now contains the correct call
          + indentBlock(headersExtractionBlock, 4)
          + indentBlock(metaDataAssertion, 4)
          + indentBlock(headersAssertion, 4)
          + indentBlock(rowContextAssertion, 4)
          + indentBlock(rowsAssertion, 4)
          + "  }\n";

    } else {
      return EMPTY;
    }
  }

  /**
   * Indents each line of a given block of text by a specified number of spaces. Handles null or
   * empty input gracefully.
   *
   * @param block The string block to indent.
   * @param spaces The number of spaces to add to the beginning of each line.
   * @return The indented string block.
   */
  private static String indentBlock(String block, int spaces) {
    if (block == null || block.isEmpty()) {
      return "";
    }
    String indent = " ".repeat(spaces);
    // Ensure consistent line endings before splitting
    String normalizedBlock = block.replace("\r\n", "\n").replace("\r", "\n");
    return normalizedBlock
        .lines()
        .map(line -> indent + line)
        .collect(Collectors.joining("\n", "", "\n")); // Add trailing newline
  }

  /** Builds the block defining test-specific configuration like expectPostgis. */
  private static String buildTestConfigurationBlock() {
    return """
           // Read the 'expect.postgis' system property at runtime to adapt assertions.
           boolean expectPostgis = isPostgres();

           """; // Default to false if property not set
  }

  /**
   * Builds the initial structure validation assertions using validateResponseStructure.
   *
   * @param ctx ReadContext for the JSON response.
   * @param actualRowCount The row count observed during generation.
   * @param expectedHeaderCountWithPostgis The expected header count when PostGIS is enabled.
   * @param expectedHeaderCountWithoutPostgis The expected header count when PostGIS is disabled.
   * @return The generated Java code string for the structure assertion.
   */
  private static String buildStructureAssertion(
      ReadContext ctx,
      int actualRowCount,
      int expectedHeaderCountWithPostgis,
      int expectedHeaderCountWithoutPostgis) {
    // Generate the call using the runtime 'expectPostgis' variable and the calculated counts
    return """
             // 1. Validate Response Structure (Counts, Headers, Height/Width)
             //    This helper checks basic counts and dimensions, adapting based on the runtime 'expectPostgis' flag.
             validateResponseStructure(response, expectPostgis, %d, %d, %d); // Pass runtime flag, row count, and expected header counts

             """
        .formatted(
            actualRowCount, expectedHeaderCountWithPostgis, expectedHeaderCountWithoutPostgis);
  }

  /**
   * Builds the block to extract headers into a List<Map<String, Object>> variable named
   * 'actualHeaders'.
   */
  private static String buildHeadersExtractionBlock(ReadContext ctx) {
    return """
            // 2. Extract Headers into a List of Maps for easy access by name
            List<Map<String, Object>> actualHeaders = response.extractList("headers", Map.class).stream()
                .map(obj -> (Map<String, Object>) obj) // Ensure correct type
                .collect(Collectors.toList());

            """;
  }

  /** Builds the assertions for rowContext if present and using name-based validation. */
  private static String buildRowContextAssertion(
      ReadContext ctx, List<Map<String, Object>> actualHeadersFromCtx, boolean rowContextPresent) {
    if (!rowContextPresent) {
      return "\n// rowContext not found or empty in the response, skipping assertions.\n";
    }

    StringBuilder sb = new StringBuilder("\n// 5. Assert rowContext by name.\n");
    try {
      Map<String, Map<String, Map<String, String>>> rowContext = ctx.read("$.rowContext");

      if (actualHeadersFromCtx == null || actualHeadersFromCtx.isEmpty()) {
        sb.append(
            "// Headers not found in response, cannot generate rowContext assertions by name.\n");
        return sb.toString();
      }

      // --- Determine the target header name for rowContext ---
      String targetHeaderName = null;
      for (int i = actualHeadersFromCtx.size() - 1; i >= 0; i--) {
        Map<String, Object> header = actualHeadersFromCtx.get(i);
        Boolean hidden = (Boolean) header.get("hidden");
        Boolean meta = (Boolean) header.get("meta");
        if (Boolean.FALSE.equals(hidden) && Boolean.TRUE.equals(meta)) {
          targetHeaderName = (String) header.get("name");
          break;
        }
      }
      // Fallback to the very last header name if no non-hidden, meta header found
      if (targetHeaderName == null) {
        targetHeaderName =
            (String) actualHeadersFromCtx.get(actualHeadersFromCtx.size() - 1).get("name");
        sb.append(
                "// WARNING: Could not determine specific data header for rowContext, using last header '")
            .append(escapeJava(targetHeaderName))
            .append("'.\n");
      }
      // --- End Determine target header name ---

      // Iterate through the rowContext map (keys are row indices as strings)
      for (Map.Entry<String, Map<String, Map<String, String>>> rowEntry : rowContext.entrySet()) {
        int rowIndex = Integer.parseInt(rowEntry.getKey());
        Map<String, Map<String, String>> colContextMap = rowEntry.getValue();

        // Find the valueStatus for the target column within this row's context
        // We use the *determined targetHeaderName* to call the validation helper.
        boolean foundContextForRow = false;
        for (Map.Entry<String, Map<String, String>> colEntry : colContextMap.entrySet()) {
          // Find the header corresponding to this column index to check if it matches our target
          int colIndex;
          try {
            colIndex = Integer.parseInt(colEntry.getKey());
          } catch (NumberFormatException e) {
            continue; // Skip if key is not a valid index
          }

          if (colIndex >= 0 && colIndex < actualHeadersFromCtx.size()) {
            String currentHeaderName = (String) actualHeadersFromCtx.get(colIndex).get("name");
            if (targetHeaderName.equals(currentHeaderName)) {
              String valueStatus = colEntry.getValue().get("valueStatus");
              if (valueStatus != null) {
                sb.append("ValidationHelper.validateRowContextByName(response, actualHeaders, ")
                    .append(rowIndex)
                    .append(", \"")
                    .append(escapeJava(targetHeaderName))
                    .append("\", \"")
                    .append(escapeJava(valueStatus))
                    .append("\");\n");
                foundContextForRow = true;
                break; // Found the context for the target header in this row
              }
            }
          }
        }
        if (!foundContextForRow) {
          sb.append("// No rowContext found for header '")
              .append(escapeJava(targetHeaderName))
              .append("' in row index ")
              .append(rowIndex)
              .append(".\n");
        }
      }
    } catch (PathNotFoundException e) {
      sb.append("// rowContext path unexpectedly not found during processing.\n");
    } catch (Exception e) {
      sb.append("// Error processing rowContext: ").append(e.getMessage()).append("\n");
      System.err.println("Error generating rowContext assertion: " + e.getMessage());
      e.printStackTrace();
    }
    return sb.toString();
  }

  /** Escapes characters in a string for safe inclusion in Java source code string literals. */
  private static String escapeJava(String str) {
    if (str == null) {
      return "";
    }
    // Order matters: escape backslash first
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private static String generateValidateRowValueByName(
      int rowIndex, List<Object> rowData, List<Map<String, Object>> headers) {
    StringBuilder sb = new StringBuilder();
    if (rowData == null || headers == null || rowData.size() != headers.size()) {
      sb.append("// Skipping row value generation for row ")
          .append(rowIndex)
          .append(" due to data mismatch.\n");
      return sb.toString();
    }

    sb.append("// Validate selected values for row index ").append(rowIndex).append("\n");

    // Function to generate the assertion line safely
    java.util.function.BiConsumer<Integer, String> addAssertion =
        (colIndex, headerName) -> {
          String value = (rowData.get(colIndex) == null) ? "" : rowData.get(colIndex).toString();
          // Special case: If valueType is BOOLEAN, convert "true"/"false" strings to Java booleans
          // for assertion
          String valueType = (String) headers.get(colIndex).get("valueType");
          String expectedValueString;
          if ("BOOLEAN".equalsIgnoreCase(valueType)) {
            expectedValueString =
                Boolean.toString(
                    "true".equalsIgnoreCase(value)); // Convert to "true" or "false" literal
          } else {
            // Default: treat as string, escape for Java code
            expectedValueString = "\"" + escapeJava(value) + "\"";
          }

          sb.append("validateRowValueByName(response, actualHeaders, ")
              .append(rowIndex)
              .append(", \"")
              .append(escapeJava(headerName))
              .append("\", ")
              .append(expectedValueString)
              .append(");\n"); // Use potentially non-string value for booleans
        };

    // Validate first column
    if (!headers.isEmpty()) {
      addAssertion.accept(0, (String) headers.get(0).get("name"));
    }

    // Validate last column
    if (headers.size() > 1) {
      addAssertion.accept(headers.size() - 1, (String) headers.get(headers.size() - 1).get("name"));
    }

    // Validate specific common/identifier columns by name if they exist
    List<String> commonCols =
        List.of("pi", "tei", "enrollmentdate", "incidentdate", "ou", "ouname", "programstatus");
    for (int i = 0; i < headers.size(); i++) {
      String headerName = (String) headers.get(i).get("name");
      if (commonCols.contains(headerName)) {
        // Avoid duplicating first/last column validation if they are in commonCols
        if (i != 0 && i != headers.size() - 1) {
          addAssertion.accept(i, headerName);
        }
      }
    }
    return sb.toString();
  }

  /**
   * Builds the assertions for rows, always generating name-based validation calls for a sample of
   * rows (first and last).
   *
   * @param ctx ReadContext for the JSON response.
   * @param actualHeadersFromCtx The list of headers extracted from the context.
   * @return The generated Java code string for row assertions.
   */
  private static String buildRowsAssertion(
      ReadContext ctx, List<Map<String, Object>> actualHeadersFromCtx) {
    StringBuilder rowsAssertion = new StringBuilder();
    List<List<Object>> rows;

    try {
      rows = ctx.read("$.rows");
    } catch (PathNotFoundException e) {
      // Handle case where the 'rows' path doesn't exist in the JSON
      return "\n// Rows path '$.rows' not found in response, skipping row assertions.\n";
    }

    if (rows == null || rows.isEmpty()) {
      return "\n// No rows found in response, skipping row assertions.\n";
    }

    // --- Always generate NAME-BASED ROW VALIDATION ---
    rowsAssertion.append(
        "\n// 7. Assert row values by name (sample validation: first/last row, key columns).\n");

    // Check if headers are available (needed for name-based validation)
    if (actualHeadersFromCtx == null || actualHeadersFromCtx.isEmpty()) {
      rowsAssertion.append("// Headers not found, cannot generate row assertions by name.\n");
      return rowsAssertion.toString();
    }

    // --- Generate validation calls for the first row (index 0) ---
    rowsAssertion.append(generateValidateRowValueByName(0, rows.get(0), actualHeadersFromCtx));

    // --- Generate validation calls for the last row if there's more than one row ---
    if (rows.size() > 1) {
      rowsAssertion.append("\n");
      rowsAssertion.append(
          generateValidateRowValueByName(
              rows.size() - 1, rows.get(rows.size() - 1), actualHeadersFromCtx));
    }

    return rowsAssertion.toString();
  }

  /**
   * Builds the assertions for the metadata if requested by the Generator. Includes robust error
   * handling and proper escaping.
   *
   * @param ctx the {@link ReadContext}. The values for the assertions are extracted from it.
   * @return the built assertions string, or empty/comment if not applicable.
   */
  private static String buildMetaDataAssertion(ReadContext ctx) {
    // Check the flag from the Generator instance
    if (!GEN.assertMetaData()) {
      return "\n// Metadata assertion skipped by Generator configuration.\n";
    }

    StringBuilder metaDataAssertion = new StringBuilder();

    try {
      Object metaDataObject = ctx.read("$.metaData");

      if (metaDataObject == null
          || (metaDataObject instanceof Map && ((Map<?, ?>) metaDataObject).isEmpty())) {
        metaDataAssertion.append(
            "\n// metaData not found or is empty in response, skipping assertion.\n");
        return metaDataAssertion.toString();
      }

      // Convert the extracted metadata object (likely a Map) to a JSON string
      String expectedMetaDataJsonString;
      if (metaDataObject instanceof Map) {
        expectedMetaDataJsonString = new JSONObject((Map<String, ?>) metaDataObject).toString();
      } else {
        expectedMetaDataJsonString = JsonPath.parse(metaDataObject).jsonString();
      }

      metaDataAssertion.append("\n// 3. Assert metaData.\n");
      metaDataAssertion
          .append("String expectedMetaData = ")
          .append("\"")
          .append(escapeJava(expectedMetaDataJsonString))
          .append("\";\n"); // Append semicolon and newline

      metaDataAssertion
          .append("String actualMetaData = ")
          // Use the ApiResponse method which handles parsing safely
          .append("new JSONObject((Map)response.extract(\"metaData\")).toString();\n");

      metaDataAssertion.append(
          "assertEquals(expectedMetaData, actualMetaData, false);\n"); // Use JSONAssert

      return metaDataAssertion.toString();

    } catch (PathNotFoundException e) {
      metaDataAssertion.append(
          "\n// metaData path '$.metaData' not found in response, skipping assertion.\n");
      return metaDataAssertion.toString();
    } catch (Exception e) {
      // Catch other potential errors during JSON processing or casting
      metaDataAssertion
          .append("\n// Error processing metaData from response: ")
          .append(e.getMessage())
          .append("\n");
      System.err.println("## ERROR generating metaData assertion: " + e.getMessage());
      e.printStackTrace();
      return metaDataAssertion.toString();
    }
  }

  /**
   * Builds the assertions for headers, generating conditional logic based on the 'expectPostgis'
   * flag read at runtime within the test.
   *
   * @param ctx ReadContext for the JSON response (used to get header details).
   * @param actualHeadersFromCtx The list of headers extracted from the context.
   * @return The generated Java code string for header assertions.
   */
  private static String buildHeadersAssertion(
      ReadContext ctx, List<Map<String, Object>> actualHeadersFromCtx) {
    StringBuilder headersAssertion = new StringBuilder();

    if (actualHeadersFromCtx == null || actualHeadersFromCtx.isEmpty()) {
      return "\n// Headers not found in response, skipping header assertions.\n";
    }

    // --- Always generate NAME-BASED HEADER VALIDATION structure ---
    headersAssertion.append(
        "\n// 4. Validate Headers By Name (conditionally checking PostGIS headers).\n");

    for (Map<String, Object> headerMap : actualHeadersFromCtx) {
      // Extract properties safely, providing defaults or handling nulls
      String name = (String) headerMap.get("name");
      String column = (String) headerMap.get("column");
      String valueType = (String) headerMap.get("valueType");
      String type = (String) headerMap.get("type");
      // Use Boolean.TRUE.equals for safe null handling
      boolean hidden = Boolean.TRUE.equals(headerMap.get("hidden"));
      boolean meta = Boolean.TRUE.equals(headerMap.get("meta"));

      // Check if it's a PostGIS-specific header
      boolean isPostGisHeader = List.of("geometry", "longitude", "latitude").contains(name);

      // Start conditional block in generated code if it's a PostGIS header
      if (isPostGisHeader) {
        // Indent conditional validation for readability in generated code
        headersAssertion.append(
            "    if (expectPostgis) {\n      "); // Use 4 spaces for standard Java indentation
      } else {
        // Add indentation for consistency even for non-conditional lines
        headersAssertion.append("    ");
      }

      // Generate the assertion call (always validateHeaderPropertiesByName)
      headersAssertion
          .append("validateHeaderPropertiesByName(response, actualHeaders,")
          .append("\"")
          .append(escapeJava(name))
          .append("\"")
          .append(", \"")
          .append(escapeJava(column))
          .append("\"")
          .append(", \"")
          .append(escapeJava(valueType))
          .append("\"")
          .append(", \"")
          .append(escapeJava(type))
          .append("\"")
          .append(", ")
          .append(hidden) // boolean literal
          .append(", ")
          .append(meta) // boolean literal
          .append(");\n");

      // Close conditional block in generated code if it was opened
      if (isPostGisHeader) {
        headersAssertion.append("    }\n"); // Close if block, add newline
      }
    } // End loop through actualHeadersFromCtx

    // Add assertions for *non-existence* of PostGIS headers if expectPostgis is false at runtime
    headersAssertion.append(
        "\n    // Assert PostGIS-specific headers DO NOT exist if 'expectPostgis' is false\n");
    headersAssertion.append("    if (!expectPostgis) {\n");
    headersAssertion.append("      validateHeaderExistence(actualHeaders, \"geometry\", false);\n");
    headersAssertion.append(
        "      validateHeaderExistence(actualHeaders, \"longitude\", false);\n");
    headersAssertion.append("      validateHeaderExistence(actualHeaders, \"latitude\", false);\n");
    headersAssertion.append("    }\n");

    return headersAssertion.toString();
  }

  private static StringBuilder buildTestParams(String query) {
    String testParamsBuilder = "// Given \nQueryParamsBuilder params = new QueryParamsBuilder()";
    StringBuilder params = new StringBuilder(testParamsBuilder);

    Map<String, String> paramsMap = getParamsMap(query);

    paramsMap.forEach((k, v) -> params.append(".add(\"" + k + "=" + v + "\")\n"));
    params.append(";\n");

    return params;
  }

  private static Map<String, String> getParamsMap(String query) {
    Map<String, String> queryMap = new HashMap<>();
    String queryString = substringAfter(query, "?");

    for (String param : queryString.split("&")) {
      String paramName = substringBefore(param, "=");
      String paramValue = decode(substringAfter(param, "="), UTF_8);

      if (!queryMap.containsKey(paramName)) {
        queryMap.put(paramName, paramValue);
      } else {
        // In case of multiple parameters with the same name ("arrays").
        String currentValue = queryMap.get(paramName);
        queryMap.put(paramName, currentValue + "," + paramValue);
      }
    }

    return queryMap;
  }

  private static String getTargetDim(String query) {
    return substringAfterLast(substringBetween(query, "/", ".json"), "/");
  }

  /**
   * This method will authenticate and send a request based on the given "query". It uses the
   * definitions in "resources/config.properties".
   *
   * @param query the URL to query.
   * @return the HTTP response for the request.
   */
  private static String getResponse(String query) {
    String baseUri = get().baseUrl().replace("/api", "");
    String auth = get().adminUserUsername() + ":" + get().adminUserPassword();
    byte[] encodedAuth = encodeBase64(auth.getBytes(ISO_8859_1));
    String authHeader = "Basic " + new String(encodedAuth);

    try {
      Content response =
          Get(baseUri + query).addHeader(AUTHORIZATION, authHeader).execute().returnContent();

      return response.asString();
    } catch (IOException e) {
      System.err.println("## ERROR: " + e.getLocalizedMessage());
    }

    return EMPTY;
  }
}
