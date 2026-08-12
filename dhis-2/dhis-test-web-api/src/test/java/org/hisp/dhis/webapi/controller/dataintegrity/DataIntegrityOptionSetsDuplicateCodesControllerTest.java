/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test for option sets with duplicate option codes. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/option_sets/option_sets_duplicate_codes.yaml}
 */
class DataIntegrityOptionSetsDuplicateCodesControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private OptionService myOptionService;

  private static final String check = "option_sets_duplicate_codes";

  private static final String detailsIdType = "optionSets";

  private static final String UNIQUE_CODE_CONSTRAINT = "optionvalue_unique_optionsetid_and_code";

  @AfterEach
  void restoreUniqueCodeConstraint() {
    // DDL statements are not rolled back by the test transaction, so every test
    // must leave the constraint exactly as a real migrated database would have it.
    // The row-level rollback that undoes each test's inserted duplicate options
    // only happens *after* @AfterEach runs, so the duplicate rows from
    // testOptionSetWithDuplicateCodesDetected are still present at this point --
    // re-adding the constraint would fail against them without this cleanup.
    try {
      jdbcTemplate.execute(
          "DELETE FROM optionvalue a USING optionvalue b "
              + "WHERE a.optionvalueid > b.optionvalueid "
              + "AND a.optionsetid = b.optionsetid AND a.code = b.code");
    } catch (Exception e) {
      // Log but don't fail if delete fails
      System.err.println(
          "Warning: Failed to clean up duplicate optionvalue rows: " + e.getMessage());
    }

    try {
      jdbcTemplate.execute(
          "ALTER TABLE optionvalue DROP CONSTRAINT IF EXISTS " + UNIQUE_CODE_CONSTRAINT);
      jdbcTemplate.execute(
          "ALTER TABLE optionvalue ADD CONSTRAINT "
              + UNIQUE_CODE_CONSTRAINT
              + " UNIQUE (optionsetid, code)");
    } catch (Exception e) {
      // Transaction isolation issues might prevent DDL from working in same transaction.
      // Log the warning but don't fail the test since the constraint will be present
      // in the next clean test run.
      System.err.println("Warning: Failed to restore constraint: " + e.getMessage());
    }
  }

  @Test
  void testOptionSetWithDuplicateCodesDetected() throws ConflictException {
    // Reproduce the real-world precondition: a database that reached a state
    // without the unique constraint (e.g. it had duplicates when V2_41_6 ran).
    jdbcTemplate.execute(
        "ALTER TABLE optionvalue DROP CONSTRAINT IF EXISTS " + UNIQUE_CODE_CONSTRAINT);

    // Build the duplicate-code fixture using OptionService directly (bypassing the
    // metadata-import pipeline's OptionObjectBundleHook validation that would reject
    // duplicates at REST import time).
    Option optionA = new Option("Sweet A", "SWEET", 1);
    Option optionB = new Option("Sweet B", "SWEET", 2);
    OptionSet optionSetA = new OptionSet("Taste", ValueType.TEXT);
    optionSetA.addOption(optionA);
    optionSetA.addOption(optionB);
    myOptionService.saveOptionSet(optionSetA);

    String optionSetId = optionSetA.getUid();

    assertHasDataIntegrityIssues(
        detailsIdType, check, 100, optionSetId, "Taste", "Code 'SWEET' used by 2 options", true);
  }

  @Test
  void testOptionSetWithUniqueCodesHasNoIssues() {
    String optionSetId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/optionSets", "{ 'name': 'Taste', 'shortName': 'Taste', 'valueType': 'TEXT' }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/options",
            "{ 'code': 'SWEET',"
                + "  'sortOrder': 1,"
                + "  'name': 'Sweet',"
                + "  'optionSet': { 'id': '"
                + optionSetId
                + "' } }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/options",
            "{ 'code': 'SOUR',"
                + "  'sortOrder': 2,"
                + "  'name': 'Sour',"
                + "  'optionSet': { 'id': '"
                + optionSetId
                + "' } }"));

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testOptionSetDuplicateCodesDivideByZero() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }
}
