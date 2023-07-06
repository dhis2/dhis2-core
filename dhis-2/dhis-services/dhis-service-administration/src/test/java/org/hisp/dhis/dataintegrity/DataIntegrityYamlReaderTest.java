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

import static org.hisp.dhis.dataintegrity.DataIntegrityYamlReader.readDataIntegrityYaml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    readDataIntegrityYaml(
        "data-integrity-checks.yaml",
        checks::add,
        sql -> check -> new DataIntegritySummary(check, new Date(), null, 1, 100d),
        sql ->
            check ->
                new DataIntegrityDetails(
                    check,
                    new Date(),
                    null,
                    List.of(new DataIntegrityIssue("id", "name", sql, List.of()))));
    assertEquals(6, checks.size());
    DataIntegrityCheck check = checks.get(0);
    assertEquals("categories_no_options", check.getName());
    assertEquals("Categories with no category options", check.getDescription());
    assertEquals("Categories", check.getSection());
    assertEquals(DataIntegritySeverity.WARNING, check.getSeverity());
    assertEquals(
        "Categories should always have at least a single category options.",
        check.getIntroduction());
    assertEquals(
        "Any categories without category options should either be removed from the"
            + " system if they are not in use. Otherwise, appropriate category options"
            + " should be added to the category.",
        check.getRecommendation());
    assertTrue(
        check
            .getRunDetailsCheck()
            .apply(check)
            .getIssues()
            .get(0)
            .getComment()
            .startsWith("SELECT uid,name from dataelementcategory"));
  }
}
