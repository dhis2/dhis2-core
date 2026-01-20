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
package org.hisp.dhis.outlierdetection.util;

import static org.hisp.dhis.feedback.ErrorCode.E2208;
import static org.hisp.dhis.feedback.ErrorCode.E7131;
import static org.hisp.dhis.outlierdetection.util.OutlierDetectionUtils.withExceptionHandling;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.regex.Pattern;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * @author Lars Helge Overland
 */
class OutlierDetectionUtilsTest {

  @Test
  void testGetOrgUnitPathClause() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnit ouB = createOrganisationUnit('B');
    OrganisationUnit ouC = createOrganisationUnit('C');
    List<OrganisationUnit> orgUnits = Lists.newArrayList(ouA, ouB, ouC);
    String expected =
        "(ou.\"path\" like '/ouabcdefghA%' or ou.\"path\" like '/ouabcdefghB%' or ou.\"path\" like '/ouabcdefghC%')";
    assertEquals(expected, OutlierDetectionUtils.getOrgUnitPathClause(orgUnits, "ou"));
  }

  @Test
  void testWithIllegalQueryExceptionHandling() {
    IllegalQueryException illegalQueryException =
        assertThrows(
            IllegalQueryException.class,
            () -> withExceptionHandling(() -> supplyWithErrorCode(E2208)));
    assertEquals(E2208.getMessage(), illegalQueryException.getMessage());
  }

  @Test
  void testWithQueryRuntimeExceptionHandling() {
    QueryRuntimeException queryRuntimeException =
        assertThrows(
            QueryRuntimeException.class,
            () -> withExceptionHandling(() -> supplyWithErrorCode(E7131)));
    assertEquals(E7131.getMessage(), queryRuntimeException.getMessage());
  }

  private Object supplyWithErrorCode(ErrorCode errorCode) {
    if (ErrorCode.E2208 == errorCode) {
      throw new DataIntegrityViolationException(errorCode.getMessage());
    } else if (ErrorCode.E7131 == errorCode) {
      throw new DataAccessResourceFailureException(errorCode.getMessage());
    }

    return null;
  }

  @Test
  void shouldMatchValidNumbers() {
    assertTrue(matches("42"));
    assertTrue(matches("0"));
    assertTrue(matches("-123"));
    assertTrue(matches("3.14"));
    assertTrue(matches("0.0"));
    assertTrue(matches("0001.00"));
    assertTrue(matches("-0.5"));
    assertTrue(matches("+42"));
    assertTrue(matches(".5"));
    assertTrue(matches("42."));
    /* Some extremely large number but still valid */
    String bigNumber = "9".repeat(307);
    assertTrue(matches(bigNumber));
    /* A huge number with a large number of decimal places */
    String bigNumberWithDecimal = "9".repeat(307) + "." + "9".repeat(999);
    assertTrue(matches(bigNumberWithDecimal));
  }

  @Test
  void shouldNotMatchInvalidNumbers() {

    assertFalse(matches("1e5"));
    assertFalse(matches("1,000"));
    assertFalse(matches("abc"));
    assertFalse(matches(""));
    assertFalse(matches(null));
    assertFalse(matches("     "));
    String bigNumber = "9".repeat(308);
    assertFalse(matches(bigNumber));
  }

  /*
   *  This helper function emulates the behavior of the PostgreSQL regex combined with the
   *  length check in the SQL statement.
   */

  private static final Pattern NUMERIC_PATTERN =
      Pattern.compile(OutlierDetectionUtils.PG_DOUBLE_REGEX);

  private boolean matches(String value) {
    if (value == null) {
      return false;
    }
    value = value.trim();

    if (!NUMERIC_PATTERN.matcher(value).matches()) {
      return false;
    }

    String integerPart;
    int dotIndex = value.indexOf('.');

    if (dotIndex >= 0) {
      integerPart = value.substring(0, dotIndex);
    } else {
      integerPart = value;
    }

    if (integerPart.startsWith("+") || integerPart.startsWith("-")) {
      integerPart = integerPart.substring(1);
    }

    return integerPart.length() <= 307;
  }
}
