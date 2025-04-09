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
package org.hisp.dhis.program.function;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.antlr.v4.runtime.Token;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionState;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class D2CountIfValueTest {

  @Mock private CommonExpressionVisitor visitor;
  @Mock private ExprContext ctx;
  @Mock private Token uid0Token;
  @Mock private Token uid1Token;
  @Mock private ExprContext valueExprContext;
  @Mock private ProgramExpressionParams progParams;
  @Mock private ExpressionState expressionState; // Mock state
  @Mock private ExpressionParams mockParams; // Mock params for visitor.getParams()
  @Mock private ExpressionParams.ExpressionParamsBuilder mockParamsBuilder; // Mock builder chain
  @Mock private ProgramIndicator programIndicator;
  @Mock private Program program;
  @Mock private AnalyticsPeriodBoundary boundary1;

  private final String psUid = "PgmStgUid1";
  private final String deUid = "DataElmUid1";
  private final String piUid = "PgmIndUid1";
  private final String pgmUid = "ProgramUid1";
  private final String expectedValueSql =
      "cast(5 as numeric)"; // Expected SQL from internal visitor
  private final Date startDate = new Date();
  private final Date endDate = new Date(startDate.getTime() + 1000000);

  private D2CountIfValue item;

  @BeforeEach
  void setUp() {
    item = new D2CountIfValue();

    // Mock context for UIDs
    ctx.uid0 = uid0Token;
    ctx.uid1 = uid1Token;
    when(uid0Token.getText()).thenReturn(psUid);
    when(uid1Token.getText()).thenReturn(deUid);
    // Mock the value expression part of the context
    when(ctx.expr(0)).thenReturn(valueExprContext);

    // Mock visitor methods needed by getSql to build the internal sqlVisitor
    when(visitor.getProgParams()).thenReturn(progParams);
    when(visitor.getState()).thenReturn(expressionState);
    when(visitor.getParams()).thenReturn(mockParams); // Return mock params
    when(visitor.getItemMap()).thenReturn(new HashMap<>()); // Needed for builder
    when(visitor.getConstantMap()).thenReturn(new HashMap<>()); // Needed for builder
    when(visitor.getSqlBuilder()).thenReturn(new PostgreSqlBuilder()); // Needed for builder

    // Mock the builder chain used internally to create sqlVisitor
    when(mockParams.toBuilder()).thenReturn(mockParamsBuilder);
    when(mockParamsBuilder.dataType(any(DataType.class))).thenReturn(mockParamsBuilder);
    when(mockParamsBuilder.build()).thenReturn(mockParams); // build() returns mock params

    // Mock program/PI structure
    when(progParams.getProgramIndicator()).thenReturn(programIndicator);
    when(progParams.getReportingStartDate()).thenReturn(startDate);
    when(progParams.getReportingEndDate()).thenReturn(endDate);
    when(programIndicator.getUid()).thenReturn(piUid);
    when(programIndicator.getProgram()).thenReturn(program);
    when(program.getUid()).thenReturn(pgmUid);
    // Default to no boundaries, override in specific tests
    when(programIndicator.getAnalyticsPeriodBoundaries()).thenReturn(Collections.emptySet());
  }

  @Test
  @Disabled
  void getSql_shouldReturnRichPlaceholder() {
    // Arrange
    String expectedFunctionName = "countIfValue";
    // Use helper to calculate expected hash (assuming no boundaries in this case)
    String expectedBoundaryHash = generateTestBoundaryHash(programIndicator, startDate, endDate);
    // Base64 encode the expected value SQL
    String expectedEncodedValueSql =
        Base64.getEncoder().encodeToString(expectedValueSql.getBytes(StandardCharsets.UTF_8));

    // Construct the expected rich placeholder string
    String expectedRichPlaceholder =
        String.format(
            "__D2FUNC__(func='%s', ps='%s', de='%s', val64='%s', hash='%s', pi='%s')__",
            expectedFunctionName,
            psUid,
            deUid,
            expectedEncodedValueSql,
            expectedBoundaryHash,
            piUid);

    // Act
    Object result = item.getSql(ctx, visitor);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertInstanceOf(String.class, result, "Result should be a String");
    assertEquals(
        expectedRichPlaceholder,
        (String) result,
        "Result should match the expected rich placeholder format");
  }

  // --- Helper method to calculate boundary hash for test expectations ---
  // Needs to be consistent with the implementation in ProgramCountFunction
  private String generateTestBoundaryHash(
      ProgramIndicator programIndicator, Date reportingStartDate, Date reportingEndDate) {
    Set<AnalyticsPeriodBoundary> boundaries = programIndicator.getAnalyticsPeriodBoundaries();
    if (boundaries == null || boundaries.isEmpty()) {
      return "noboundaries";
    }
    List<String> boundaryInfoList = new ArrayList<>();
    SimpleDateFormat dateFormat = new SimpleDateFormat(Period.DEFAULT_DATE_FORMAT);
    for (AnalyticsPeriodBoundary boundary : boundaries) {
      if (boundary != null) {
        Date boundaryDate = boundary.getBoundaryDate(reportingStartDate, reportingEndDate);
        String dateString = (boundaryDate != null) ? dateFormat.format(boundaryDate) : "null";
        boundaryInfoList.add(boundary.getUid() + ":" + dateString);
      }
    }
    Collections.sort(boundaryInfoList);
    String boundaryConfigString = String.join(";", boundaryInfoList);
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] messageDigest = md.digest(boundaryConfigString.getBytes(StandardCharsets.UTF_8));
      BigInteger no = new BigInteger(1, messageDigest);
      return String.format("%040x", no);
    } catch (NoSuchAlgorithmException e) {
      return "hash_error_" + boundaryConfigString.hashCode();
    }
  }
}
