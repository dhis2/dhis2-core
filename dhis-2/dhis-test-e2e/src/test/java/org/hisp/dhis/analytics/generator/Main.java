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

import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.CLASS_NAME_PREFIX;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.MAX_TESTS_PER_CLASS;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.SCENARIOS;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.SCENARIO_FILE_LOCATION;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.TEST_OUTPUT_LOCATION;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.generateTestMethod;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.getClassDeclaration;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.getPackageAndImports;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.helpers.file.JsonFileReader;

/** Main class, responsible for starting the process of e2e test/code generation. */
class Main {
  /** Execution args. */
  private static final int VERSION = 43;

  public static void main(String[] args) throws Exception {
    Map<String, String> urls = loadTestQueries();

    // Creates a list of maps, where each element of the list represents test class that holds a map
    // of test name/query.
    List<Map<String, String>> classes = getSubMaps(urls, MAX_TESTS_PER_CLASS);

    for (int i = 0; i < classes.size(); i++) {
      String className = CLASS_NAME_PREFIX + (i + 1) + "AutoTest";
      File file = new File(TEST_OUTPUT_LOCATION + className + ".java");
      file.getParentFile().mkdirs();

      BufferedWriter writer = new BufferedWriter(new FileWriter(file));

      StringBuilder classContents = new StringBuilder(getPackageAndImports());
      classContents.append(getClassDeclaration(className));

      Set<Map.Entry<String, String>> methods = classes.get(i).entrySet();

      for (Map.Entry<String, String> entry : methods) {
        String name = entry.getKey();
        String query = entry.getValue();

        if (isNotBlank(name) && isNotBlank(query)) {
          // Aliases that contains ":" separator.
          name = defaultString(trimToNull(substringAfter(name, ":")), name.replace(SPACE, EMPTY));

          // Removes all non-numeric and non-alphabet characters and apply camel case.
          name = name.replaceAll("[^a-zA-Z0-9]", EMPTY);

          // Generate the e2e test methods.
          String testMethod = generateTestMethod(name, query);
          classContents.append(testMethod);
        }
      }

      // Closing the class.
      classContents.append("}");

      try {
        writer.write(classContents.toString());
        writer.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static Map<String, String> loadTestQueries() throws Exception {
    JsonObject json =
        ((JsonFileReader) new FileReaderUtils().read(new File(SCENARIO_FILE_LOCATION))).get();
    List<JsonElement> scenarios = json.getAsJsonArray("scenarios").asList();

    Map<String, String> queries = new LinkedHashMap<>();

    for (JsonElement scenario : scenarios) {
      JsonObject attrMap = ((JsonObject) scenario);

      String name = trimToNull(attrMap.get("name").getAsString());
      String query = trimToNull(attrMap.get("query").getAsString());
      int minVersion = attrMap.get("version").getAsJsonObject().get("min").getAsInt();

      if (isEmpty(SCENARIOS) || contains(SCENARIOS, name)) {
        // Ignore names and queries that are null/empty.
        if (name != null && query != null && minVersion <= VERSION) {
          queries.put(name, query);
        }
      }
    }

    // Should match the total of testing queries found in the respective file.
    System.out.println("## TOTAL OF QUERIES found: " + queries.size());

    return queries;
  }

  private static List<Map<String, String>> getSubMaps(Map<String, String> map, int batchSize) {
    // List of classes.
    List<Map<String, String>> listOfSubMaps = new ArrayList<>();

    // Map of names/queries of a class.
    Map<String, String> namesQueries = new LinkedHashMap<>();

    int totalBatches = map.size() / batchSize;
    int currentBatch = 0;

    boolean hasMissingEntries = false;

    for (Map.Entry<String, String> entry : map.entrySet()) {
      namesQueries.put(entry.getKey(), entry.getValue());
      if (namesQueries.size() == batchSize) {
        listOfSubMaps.add(namesQueries);
        namesQueries = new LinkedHashMap<>();
        currentBatch++;
      } else if (totalBatches == currentBatch) {
        hasMissingEntries = true;
      }
    }

    // Adds the last sub-map with size smaller than the batch size. ie:
    // {1,2,3}, {4,5,6}, {7,8} -> (this is the last sub-map).
    if (hasMissingEntries) {
      // Adds the missing ones.
      listOfSubMaps.add(namesQueries);
    }

    return listOfSubMaps;
  }
}
