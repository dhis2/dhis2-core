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

import static org.apache.commons.io.FileUtils.lineIterator;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.text.CaseUtils.toCamelCase;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.CLASS_NAME_PREFIX;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.FILE;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.MAX_TESTS_PER_CLASS;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.generateTestMethod;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.getClassDeclaration;
import static org.hisp.dhis.analytics.generator.GeneratorHelper.getPackageAndImports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.LineIterator;

/** Main class, responsible for starting the process of e2e test/code generation. */
class Main {
  /** Token used to separate the test name from its respective URL. */
  static final String SEPARATOR = "@@";

  /** This character is treated as comments. Lines starting with "#" are skipped. */
  static final String COMMENT = "#";

  /** The location of the test that contains the test alias and URLs. */
  static final String FILE_LOCATION = "/src/test/java/org/hisp/dhis/analytics/generator/";

  public static void main(String[] args) throws IOException {
    Map<String, String> urls = loadTestUrls();

    // Creates a list of maps, where each element of the list represents test class that holds a map
    // of test alias/URL.
    List<Map<String, String>> classes = getSubMaps(urls, MAX_TESTS_PER_CLASS);

    for (int i = 0; i < classes.size(); i++) {
      String className = CLASS_NAME_PREFIX + (i + 1) + "AutoTest";
      BufferedWriter writer = new BufferedWriter(new FileWriter(className + ".java"));

      StringBuilder classContents = new StringBuilder(getPackageAndImports());
      classContents.append(getClassDeclaration(className));

      Set<Map.Entry<String, String>> methods = classes.get(i).entrySet();

      for (Map.Entry<String, String> entry : methods) {
        String alias = entry.getKey();
        String url = entry.getValue();

        if (isNotBlank(alias) && isNotBlank(url)) {
          // Aliases that contains ":" separator.
          alias =
              defaultString(trimToNull(substringAfter(alias, ":")), alias.replace(SPACE, EMPTY));

          // Removes all non-numeric and non-alphabet characters and apply camel case.
          alias = toCamelCase(alias, true, ' ').replaceAll("[^a-zA-Z0-9]", EMPTY);

          // Generate the e2e test methods.
          String testMethod = generateTestMethod("query" + alias, url);
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

  private static Map<String, String> loadTestUrls() throws IOException {
    Map<String, String> urls = new LinkedHashMap<>();
    LineIterator it = lineIterator(new File("." + FILE_LOCATION + FILE));

    try {
      while (it.hasNext()) {
        String line = it.nextLine();

        // Ignore blank lines and comments.
        if (isNotBlank(line) && !line.startsWith(COMMENT)) {
          String[] urlDef = line.split(SEPARATOR);

          // Only accept definitions in the format "ALIASES_NAMES@@URL".
          if (urlDef != null && urlDef.length == 2) {
            String alias = trimToNull(urlDef[0]);
            String url = trimToNull(urlDef[1]);

            if (alias != null && url != null) {
              urls.put(alias, url);
            }
          }
        }
      }
    } finally {
      it.close();
    }

    // Should match the total of testing URLs found in the respective text file.
    System.out.println("## TOTAL OF URLs found: " + urls.size());

    return urls;
  }

  private static List<Map<String, String>> getSubMaps(Map<String, String> map, int batchSize) {
    // List of classes.
    List<Map<String, String>> listOfSubMaps = new ArrayList<>();

    // Map of aliases/URLs of a class.
    Map<String, String> aliasesUrls = new LinkedHashMap<>();

    int totalBatches = map.size() / batchSize;
    int currentBatch = 0;

    boolean hasMissingEntries = false;

    for (Map.Entry<String, String> entry : map.entrySet()) {
      aliasesUrls.put(entry.getKey(), entry.getValue());
      if (aliasesUrls.size() == batchSize) {
        listOfSubMaps.add(aliasesUrls);
        aliasesUrls = new LinkedHashMap<>();
        currentBatch++;
      } else if (totalBatches == currentBatch) {
        hasMissingEntries = true;
      }
    }

    // Adds the last sub-map with size smaller than the batch size. ie:
    // {1,2,3}, {4,5,6}, {7,8} -> (this is the last sub-map).
    if (hasMissingEntries) {
      // Adds the missing ones.
      listOfSubMaps.add(aliasesUrls);
    }

    return listOfSubMaps;
  }
}
