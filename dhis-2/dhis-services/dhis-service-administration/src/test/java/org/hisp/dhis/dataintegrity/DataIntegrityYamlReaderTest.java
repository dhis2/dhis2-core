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
package org.hisp.dhis.dataintegrity;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.dataintegrity.DataIntegrityYamlReader.ResourceLocation.CLASS_PATH;
import static org.hisp.dhis.dataintegrity.DataIntegrityYamlReader.ResourceLocation.FILE_SYSTEM;
import static org.hisp.dhis.dataintegrity.DataIntegrityYamlReader.readDataIntegrityYaml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link DataIntegrityYamlReader}.
 *
 * @author Jan Bernitt
 */
class DataIntegrityYamlReaderTest {
  @Test
  void testReadDataIntegrityYaml() {

    List<DataIntegrityCheck> checks = new ArrayList<>();
    readYaml(checks, "data-integrity-checks.yaml", "data-integrity-checks", CLASS_PATH);
    assertEquals(63, checks.size());

    // Names should be unique
    List<String> allNames = checks.stream().map(DataIntegrityCheck::getName).toList();
    assertEquals(allNames.size(), Set.copyOf(allNames).size());

    // Config checks and Java checks should not have any of the same names
    List<String> nonYamlChecks =
        Stream.of(DataIntegrityCheckType.values())
            .map(e -> e.getName().toLowerCase())
            .collect(toList());
    assertTrue(nonYamlChecks.size() > 0);
    nonYamlChecks.retainAll(allNames);
    assertEquals(0, nonYamlChecks.size());

    // Assert that all "codes" are unique.
    List<String> codeList = checks.stream().map(DataIntegrityCheck::getCode).sorted().toList();
    assertEquals(codeList.size(), Set.copyOf(codeList).size());

    // Assert that codes consist of upper case letter and numbers only
    String regEx = "^[A-Z0-9]+$";
    Predicate<String> IS_NOT_CAPS = Pattern.compile(regEx).asPredicate().negate();
    List<String> badCodes = codeList.stream().filter(IS_NOT_CAPS).toList();
    assertEquals(0, badCodes.size());

    DataIntegrityCheck check = checks.get(0);
    assertEquals("categories_no_options", check.getName());
    assertEquals("Categories with no category options", check.getDescription());
    assertEquals("Categories", check.getSection());
    assertEquals("categories", check.getIssuesIdType());
    assertEquals(DataIntegritySeverity.WARNING, check.getSeverity());
    assertEquals(
        "Categories should always have at least one category option.", check.getIntroduction());
    assertEquals(
        "Any categories without category options should either be removed from the"
            + " system if they are not in use. Otherwise, appropriate category options"
            + " should be added to the category.",
        check.getRecommendation());
    assertFalse(check.isSlow());
    assertTrue(
        check
            .getRunDetailsCheck()
            .apply(check)
            .getIssues()
            .get(0)
            .getComment()
            .startsWith("SELECT uid,name from dataelementcategory"));
  }

  @Test
  void testWithValidChecksFile() {
    List<DataIntegrityCheck> checks = new ArrayList<>();
    readYaml(checks, "test-data-integrity-checks.yaml", "test-data-integrity-checks", CLASS_PATH);
    assertEquals(1, checks.size());
  }

  @Test
  void testWithInvalidChecksFile() {
    List<DataIntegrityCheck> checks = new ArrayList<>();
    readYaml(checks, "invalid-file-checks.yaml", "test-data-integrity-checks", CLASS_PATH);
    assertEquals(0, checks.size());
  }

  @Test
  void testWithInvalidChecksDirectory() {
    List<DataIntegrityCheck> checks = new ArrayList<>();
    readYaml(checks, "test-data-integrity-checks.yaml", "invalid-integrity-checks", CLASS_PATH);
    assertEquals(0, checks.size());
  }

  @Test
  void testWithInvalidChecksDirectoryFromFileSystem() {
    List<DataIntegrityCheck> checks = new ArrayList<>();
    readYaml(checks, "data-integrity-checks.yaml", "invalid-integrity-checks", FILE_SYSTEM);
    assertEquals(0, checks.size());
  }

  @Test
  void testWithInvalidYamlFormat() {
    List<DataIntegrityCheck> checks = new ArrayList<>();
    readYaml(
        checks, "test-data-integrity-checks.yaml", "test-data-integrity-checks.yaml", FILE_SYSTEM);
    assertEquals(0, checks.size());
  }

  private void readYaml(
      List<DataIntegrityCheck> checks,
      String fileChecks,
      String checksDirectory,
      DataIntegrityYamlReader.ResourceLocation resourceLocation) {
    readDataIntegrityYaml(
        new DefaultDataIntegrityService.DataIntegrityRecord(
            resourceLocation,
            fileChecks,
            checksDirectory,
            checks::add,
            (property, defaultValue) -> defaultValue,
            sql -> check -> new DataIntegritySummary(check, new Date(), new Date(), null, 1, 100d),
            sql ->
                check ->
                    new DataIntegrityDetails(
                        check,
                        new Date(),
                        new Date(),
                        null,
                        List.of(new DataIntegrityIssue("id", "name", sql, List.of())))));
  }
}
