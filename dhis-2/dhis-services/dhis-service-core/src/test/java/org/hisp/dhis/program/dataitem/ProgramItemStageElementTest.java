/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.program.dataitem;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.antlr.v4.runtime.Token;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionState;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class ProgramItemStageElementTest extends TestBase {
  @Mock private CommonExpressionVisitor visitor;

  @Mock private ExprContext ctx;

  @Mock private Token uid0Token; // Mock for Program Stage UID Token

  @Mock private Token uid1Token; // M/ Mock for Data Element UID

  @Mock private ProgramExpressionParams progParams;

  @Mock private ExpressionState expressionState;

  private final String psUid = "PgmStgUid1";
  private final String deUid = "DataElmUid1";

  private final SimpleDateFormat dateFormat = new SimpleDateFormat(Period.DEFAULT_DATE_FORMAT);
  private Date startDate;
  private Date endDate;

  private ProgramItemStageElement item;

  @BeforeEach
  void setUp() throws ParseException {
    item = new ProgramItemStageElement(); // class under test

    // Stub the fields on ctx to return the Token mocks
    ctx.uid0 = uid0Token; // Assign the Token mock to the field
    ctx.uid1 = uid1Token; // Assign the Token mock to the field

    // Stub the getText() method on the Token mocks
    when(uid0Token.getText()).thenReturn(psUid);
    when(uid1Token.getText()).thenReturn(deUid);

    when(visitor.getProgParams()).thenReturn(progParams);
    when(visitor.getState()).thenReturn(expressionState);
    when(visitor.isUseExperimentalSqlEngine()).thenReturn(true);

    startDate = dateFormat.parse("2024-01-01");
    endDate = dateFormat.parse("2024-12-31");
    when(progParams.getReportingStartDate()).thenReturn(startDate);
    when(progParams.getReportingEndDate()).thenReturn(endDate);
  }

  @Test
  void getSql_whenNoOffsetAndNoBoundaries_thenReturnsCorrectPlaceholder() {
    when(expressionState.getStageOffset()).thenReturn(Integer.MIN_VALUE); // No explicit offset

    ProgramIndicator programIndicator = createProgramIndicator();
    when(progParams.getProgramIndicator()).thenReturn(programIndicator);

    String expectedPlaceholder =
        String.format(
            "__PSDE_CTE_PLACEHOLDER__(psUid='%s', deUid='%s', offset='0', boundaryHash='noboundaries', piUid='%s')",
            psUid, deUid, programIndicator.getUid());

    Object result = item.getSql(ctx, visitor);

    assertEquals(expectedPlaceholder, result);
  }

  @Test
  void getSql_whenExplicitZeroOffsetAndNoBoundaries_thenReturnsCorrectPlaceholder() {
    when(expressionState.getStageOffset()).thenReturn(0); // Explicit latest offset
    ProgramIndicator programIndicator = createProgramIndicator();
    when(progParams.getProgramIndicator()).thenReturn(programIndicator);

    String expectedPlaceholder =
        String.format(
            "__PSDE_CTE_PLACEHOLDER__(psUid='%s', deUid='%s', offset='0', boundaryHash='noboundaries', piUid='%s')",
            psUid, deUid, programIndicator.getUid());

    // method under test
    Object result = item.getSql(ctx, visitor);

    assertEquals(expectedPlaceholder, result);
  }

  @Test
  void getSql_whenNegativeOffsetAndNoBoundaries_thenReturnsCorrectPlaceholder() {
    int offset = -2; // Third latest
    when(expressionState.getStageOffset()).thenReturn(offset);
    ProgramIndicator programIndicator = createProgramIndicator();
    when(progParams.getProgramIndicator()).thenReturn(programIndicator);

    String expectedPlaceholder =
        String.format(
            "__PSDE_CTE_PLACEHOLDER__(psUid='%s', deUid='%s', offset='%d', boundaryHash='noboundaries', piUid='%s')",
            psUid, deUid, offset, programIndicator.getUid());

    // method under test
    Object result = item.getSql(ctx, visitor);

    assertEquals(expectedPlaceholder, result);
  }

  @Test
  void getSql_whenPositiveOffsetAndNoBoundaries_thenReturnsCorrectPlaceholder() {
    int offset = 3; // Third earliest
    when(expressionState.getStageOffset()).thenReturn(offset);
    ProgramIndicator programIndicator = createProgramIndicator();
    when(progParams.getProgramIndicator()).thenReturn(programIndicator);

    String expectedPlaceholder =
        String.format(
            "__PSDE_CTE_PLACEHOLDER__(psUid='%s', deUid='%s', offset='%d', boundaryHash='noboundaries', piUid='%s')",
            psUid, deUid, offset, programIndicator.getUid());

    // method under test
    Object result = item.getSql(ctx, visitor);

    assertEquals(expectedPlaceholder, result);
  }

  @Test
  void getSql_whenNoOffsetWithBoundaries_thenReturnsCorrectPlaceholderAndHash()
      throws ParseException {
    when(expressionState.getStageOffset()).thenReturn(Integer.MIN_VALUE);
    ProgramIndicator programIndicator = createProgramIndicator();

    AnalyticsPeriodBoundary boundary1 = mock(AnalyticsPeriodBoundary.class);
    AnalyticsPeriodBoundary boundary2 = mock(AnalyticsPeriodBoundary.class);
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(boundary1);
    boundaries.add(boundary2);
    programIndicator.setAnalyticsPeriodBoundaries(boundaries);
    when(progParams.getProgramIndicator()).thenReturn(programIndicator);
    when(boundary1.getUid()).thenReturn("BoundUID1");
    when(boundary2.getUid()).thenReturn("BoundUID2");

    // Mock boundary dates based on reporting period
    Date boundaryDate1 = dateFormat.parse("2024-02-15");
    Date boundaryDate2 = dateFormat.parse("2024-11-30");
    when(boundary1.getBoundaryDate(startDate, endDate)).thenReturn(boundaryDate1);
    when(boundary2.getBoundaryDate(startDate, endDate)).thenReturn(boundaryDate2);

    // Calculate expected hash (sorted concatenation)
    String expectedHash =
        calculateSHA1("BoundUID1:2024-02-15;BoundUID2:2024-11-30"); // Assuming UID1 < UID2

    String expectedPlaceholder =
        String.format(
            "__PSDE_CTE_PLACEHOLDER__(psUid='%s', deUid='%s', offset='0', boundaryHash='%s', piUid='%s')",
            psUid, deUid, expectedHash, programIndicator.getUid());

    // method under test
    Object result = item.getSql(ctx, visitor);

    assertEquals(expectedPlaceholder, result);
  }

  @Test
  void getSql_whenNegativeOffsetWithBoundaries_thenReturnsCorrectPlaceholderAndHash()
      throws ParseException {
    int offset = -1;
    when(expressionState.getStageOffset()).thenReturn(offset);
    ProgramIndicator programIndicator = createProgramIndicator();
    AnalyticsPeriodBoundary boundary1 = mock(AnalyticsPeriodBoundary.class);
    when(boundary1.getUid()).thenReturn("BoundUIDX");
    Set<AnalyticsPeriodBoundary> boundaries = Set.of(boundary1); // Use Set.of for simplicity
    programIndicator.setAnalyticsPeriodBoundaries(boundaries);
    when(progParams.getProgramIndicator()).thenReturn(programIndicator);

    Date boundaryDate1 = dateFormat.parse("2024-06-01");
    when(boundary1.getBoundaryDate(startDate, endDate)).thenReturn(boundaryDate1);

    String expectedHash = calculateSHA1("BoundUIDX:2024-06-01");

    String expectedPlaceholder =
        String.format(
            "__PSDE_CTE_PLACEHOLDER__(psUid='%s', deUid='%s', offset='%d', boundaryHash='%s', piUid='%s')",
            psUid, deUid, offset, expectedHash, programIndicator.getUid());

    // method under test
    Object result = item.getSql(ctx, visitor);

    assertEquals(expectedPlaceholder, result);
  }

  @Test
  void getSql_whenBoundariesAreUnordered_thenHashIsConsistent() throws ParseException {
    // Arrange (Same boundaries as test 5, but added in different order)
    when(expressionState.getStageOffset()).thenReturn(Integer.MIN_VALUE);
    ProgramIndicator programIndicator = createProgramIndicator();
    AnalyticsPeriodBoundary boundary1 = mock(AnalyticsPeriodBoundary.class);
    AnalyticsPeriodBoundary boundary2 = mock(AnalyticsPeriodBoundary.class);
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    // Add in reverse UID order to other test
    boundaries.add(boundary2);
    boundaries.add(boundary1);
    programIndicator.setAnalyticsPeriodBoundaries(boundaries);
    when(progParams.getProgramIndicator()).thenReturn(programIndicator);

    when(boundary1.getUid()).thenReturn("BoundUID1");
    when(boundary2.getUid()).thenReturn("BoundUID2");

    Date boundaryDate1 = dateFormat.parse("2024-02-15");
    Date boundaryDate2 = dateFormat.parse("2024-11-30");
    when(boundary1.getBoundaryDate(startDate, endDate)).thenReturn(boundaryDate1);
    when(boundary2.getBoundaryDate(startDate, endDate)).thenReturn(boundaryDate2);

    // Expected hash should be the same as test 5 due to sorting
    String expectedHash = calculateSHA1("BoundUID1:2024-02-15;BoundUID2:2024-11-30");

    String expectedPlaceholder =
        String.format(
            "__PSDE_CTE_PLACEHOLDER__(psUid='%s', deUid='%s', offset='0', boundaryHash='%s', piUid='%s')",
            psUid, deUid, expectedHash, programIndicator.getUid());

    // Act
    Object result = item.getSql(ctx, visitor);

    // Assert
    assertEquals(expectedPlaceholder, result);
  }

  // Helper to calculate SHA-1 for test expectations
  private String calculateSHA1(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      BigInteger no = new BigInteger(1, messageDigest);
      return String.format("%040x", no);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e); // Fail test if SHA-1 not available
    }
  }

  private ProgramIndicator createProgramIndicator() {
    Program program = createProgram('A');
    ProgramIndicator programIndicator = createProgramIndicator('A', program, "1+1", "1+1");
    programIndicator.setAnalyticsPeriodBoundaries(Collections.emptySet());
    programIndicator.setAnalyticsType(AnalyticsType.ENROLLMENT);
    return programIndicator;
  }
}
