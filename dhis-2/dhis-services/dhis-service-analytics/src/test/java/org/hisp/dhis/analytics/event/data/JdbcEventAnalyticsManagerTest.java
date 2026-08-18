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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.event.data.JdbcEventAnalyticsManager.ExceptionHandler.handle;
import static org.hisp.dhis.feedback.ErrorCode.E7132;
import static org.hisp.dhis.feedback.ErrorCode.E7133;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.postgresql.util.PSQLState.BAD_DATETIME_FORMAT;
import static org.postgresql.util.PSQLState.DIVISION_BY_ZERO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;

class JdbcEventAnalyticsManagerTest {

  @Test
  void testHandlingDataIntegrityExceptionWhenDivisionByZero() {
    DataIntegrityViolationException aDivisionByZeroException =
        mockDataIntegrityExceptionDivisionByZero();

    assertThrows(
        QueryRuntimeException.class, () -> handle(aDivisionByZeroException), E7132.getMessage());
  }

  @Test
  void testHandlingAnyOtherDataIntegrityException() {
    DataIntegrityViolationException anyDataIntegrityException =
        mockAnyOtherDataIntegrityException();

    assertThrows(
        QueryRuntimeException.class, () -> handle(anyDataIntegrityException), E7133.getMessage());
  }

  @Test
  void testHandlingWhenExceptionIsNull() {
    DataIntegrityViolationException aNullException = null;

    assertThrows(QueryRuntimeException.class, () -> handle(aNullException), E7133.getMessage());
  }

  @Test
  void testHandlingWhenExceptionCauseNull() {
    DataIntegrityViolationException aNullExceptionCause =
        new DataIntegrityViolationException("null", null);

    assertThrows(
        QueryRuntimeException.class, () -> handle(aNullExceptionCause), E7133.getMessage());
  }

  @Test
  void testHandlingWhenExceptionCauseIsNotPSQLException() {
    ArrayIndexOutOfBoundsException aRandomCause = new ArrayIndexOutOfBoundsException();
    DataIntegrityViolationException aNonPSQLExceptionCause =
        new DataIntegrityViolationException("not caused by PSQLException", aRandomCause);

    assertThrows(
        QueryRuntimeException.class, () -> handle(aNonPSQLExceptionCause), E7133.getMessage());
  }

  @Test
  void getOuInConditionEmptyMapReturnsEmptyString() {
    JdbcEventAnalyticsManager jdbcEventAnalyticsManager = getJdbcEventAnalyticsManager();

    assertEquals("", jdbcEventAnalyticsManager.getOuInCondition(Map.of()));
  }

  @Test
  void getOuInConditionSingleLevelSingleUnitReturnsInCondition() {
    JdbcEventAnalyticsManager jdbcEventAnalyticsManager = getJdbcEventAnalyticsManager();

    OrganisationUnit unit = ouWithUid("uid000001A");

    String result =
        jdbcEventAnalyticsManager.getOuInCondition(Map.of("ax.\"uidlevel2\"", List.of(unit)));

    assertEquals("ax.\"uidlevel2\" in ('uid000001A') ", result);
  }

  @Test
  void getOuInConditionSingleLevelMultipleUnitsCombinesUidsInSingleInClause() {
    JdbcEventAnalyticsManager jdbcEventAnalyticsManager = getJdbcEventAnalyticsManager();

    OrganisationUnit unitA = ouWithUid("uid000001A");
    OrganisationUnit unitB = ouWithUid("uid000001B");

    String result =
        jdbcEventAnalyticsManager.getOuInCondition(
            Map.of("ax.\"uidlevel3\"", List.of(unitA, unitB)));

    assertEquals("ax.\"uidlevel3\" in ('uid000001A','uid000001B') ", result);
  }

  @Test
  void getOuInConditionMultipleLevelsJoinsConditionsWithOr() {
    JdbcEventAnalyticsManager jdbcEventAnalyticsManager = getJdbcEventAnalyticsManager();

    Map<String, List<OrganisationUnit>> orgUnitsMap = new LinkedHashMap<>();
    orgUnitsMap.put("ax.\"uidlevel2\"", List.of(ouWithUid("uid000001A")));
    orgUnitsMap.put("ax.\"uidlevel3\"", List.of(ouWithUid("uid000001B")));

    String result = jdbcEventAnalyticsManager.getOuInCondition(orgUnitsMap);

    assertEquals(
        "ax.\"uidlevel2\" in ('uid000001A')  or ax.\"uidlevel3\" in ('uid000001B') ", result);
  }

  @Test
  void getOuInConditionUnitWithNullUidIsExcluded() {
    JdbcEventAnalyticsManager jdbcEventAnalyticsManager = getJdbcEventAnalyticsManager();
    OrganisationUnit organisationUnit = new OrganisationUnit();
    organisationUnit.setUid(null);

    Map<String, List<OrganisationUnit>> orgUnitsMap =
        Map.of("ax.\"uidlevel2\"", List.of(ouWithUid("uid000001A"), organisationUnit));

    String result = jdbcEventAnalyticsManager.getOuInCondition(orgUnitsMap);

    assertEquals("ax.\"uidlevel2\" in ('uid000001A') ", result);
  }

  private OrganisationUnit ouWithUid(String uid) {
    OrganisationUnit unit = new OrganisationUnit();
    unit.setUid(uid);
    return unit;
  }

  private JdbcEventAnalyticsManager getJdbcEventAnalyticsManager() {
    return new JdbcEventAnalyticsManager(
        null, null, null, null, null, null, null, null, null, null, null);
  }

  private DataIntegrityViolationException mockDataIntegrityExceptionDivisionByZero() {
    PSQLException psqlException = new PSQLException("ERROR: division by zero", DIVISION_BY_ZERO);

    return new DataIntegrityViolationException(
        "ERROR: division by zero; nested exception is org.postgresql.util.PSQLException: ERROR: division by zero",
        psqlException);
  }

  private DataIntegrityViolationException mockAnyOtherDataIntegrityException() {
    PSQLException psqlException = new PSQLException("ERROR: bad time format", BAD_DATETIME_FORMAT);

    return new DataIntegrityViolationException(
        "ERROR: bad time format; nested exception is org.postgresql.util.PSQLException: ERROR: bad time format",
        psqlException);
  }
}
