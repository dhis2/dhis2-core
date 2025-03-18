/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.dataelement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataElementMergeValidatorTest extends TestBase {

  @Test
  @DisplayName("when all value types match then there are no report errors")
  void whenAllValueTypesMatchThenNoError() {
    // given
    DataElement target = createDataElement('A', ValueType.TEXT, AggregationType.SUM);

    DataElement source1 = createDataElement('B', ValueType.TEXT, AggregationType.SUM);
    DataElement source2 = createDataElement('C', ValueType.TEXT, AggregationType.SUM);
    DataElement source3 = createDataElement('D', ValueType.TEXT, AggregationType.SUM);

    // when
    DataElementMergeValidator validator = new DataElementMergeValidator();
    MergeReport report =
        validator.validateValueType(target, List.of(source1, source2, source3), new MergeReport());

    // then
    assertFalse(report.hasErrorMessages());
  }

  @Test
  @DisplayName("when 1 value type does not match then the report contains the error info")
  void when1ValueTypeDoesNotMatchThenError() {
    // given
    DataElement target = createDataElement('E', ValueType.TEXT, AggregationType.SUM);

    DataElement source1 = createDataElement('F', ValueType.NUMBER, AggregationType.SUM);
    DataElement source2 = createDataElement('G', ValueType.TEXT, AggregationType.SUM);
    DataElement source3 = createDataElement('H', ValueType.TEXT, AggregationType.SUM);

    // when
    DataElementMergeValidator validator = new DataElementMergeValidator();
    MergeReport report =
        validator.validateValueType(target, List.of(source1, source2, source3), new MergeReport());

    // then
    assertTrue(report.hasErrorMessages());
    assertEquals(ErrorCode.E1550, report.getMergeErrors().get(0).getErrorCode());
    assertEquals(
        "All source ValueTypes must match target ValueType: `TEXT`. Other ValueTypes found: `[NUMBER]`",
        report.getMergeErrors().get(0).getMessage());
  }

  @Test
  @DisplayName("when multiple value types do not match then the report contains the error info")
  void whenMultipleValueTypeDoesNotMatchThenError() {
    // given
    DataElement target = createDataElement('I', ValueType.TEXT, AggregationType.SUM);

    DataElement source1 = createDataElement('J', ValueType.NUMBER, AggregationType.SUM);
    DataElement source2 = createDataElement('K', ValueType.NUMBER, AggregationType.SUM);
    DataElement source3 = createDataElement('L', ValueType.DATE, AggregationType.SUM);

    // when
    DataElementMergeValidator validator = new DataElementMergeValidator();
    MergeReport report =
        validator.validateValueType(target, List.of(source1, source2, source3), new MergeReport());

    // then
    assertTrue(report.hasErrorMessages());
    assertEquals(ErrorCode.E1550, report.getMergeErrors().get(0).getErrorCode());
    assertEquals(
        "All source ValueTypes must match target ValueType: `TEXT`. Other ValueTypes found: `[NUMBER, DATE]`",
        report.getMergeErrors().get(0).getMessage());
  }

  @Test
  @DisplayName("when all domain types match then there are no report errors")
  void whenAllDomainTypesMatchThenNoError() {
    // given
    DataElement target =
        createDataElement('A', ValueType.TEXT, AggregationType.SUM, DataElementDomain.TRACKER);

    DataElement source1 =
        createDataElement('B', ValueType.TEXT, AggregationType.SUM, DataElementDomain.TRACKER);
    DataElement source2 =
        createDataElement('C', ValueType.TEXT, AggregationType.SUM, DataElementDomain.TRACKER);
    DataElement source3 =
        createDataElement('D', ValueType.DATE, AggregationType.SUM, DataElementDomain.TRACKER);

    // when
    DataElementMergeValidator validator = new DataElementMergeValidator();
    MergeReport report =
        validator.validateDomainType(target, List.of(source1, source2, source3), new MergeReport());

    // then
    assertFalse(report.hasErrorMessages());
  }

  @Test
  @DisplayName("when domain type do not match then the report contains the error info")
  void whenDomainTypeDoNotMatchThenError() {
    // given
    DataElement target =
        createDataElement('E', ValueType.TEXT, AggregationType.SUM, DataElementDomain.AGGREGATE);

    DataElement source1 =
        createDataElement('F', ValueType.NUMBER, AggregationType.SUM, DataElementDomain.TRACKER);
    DataElement source2 =
        createDataElement('G', ValueType.TEXT, AggregationType.SUM, DataElementDomain.TRACKER);
    DataElement source3 =
        createDataElement('H', ValueType.TEXT, AggregationType.SUM, DataElementDomain.AGGREGATE);

    // when
    DataElementMergeValidator validator = new DataElementMergeValidator();
    MergeReport report =
        validator.validateDomainType(target, List.of(source1, source2, source3), new MergeReport());

    // then
    assertTrue(report.hasErrorMessages());
    assertEquals(ErrorCode.E1551, report.getMergeErrors().get(0).getErrorCode());
    assertEquals(
        "All source DataElementDomains must match target DataElementDomain: `AGGREGATE`. Other DataElementDomains found: `[TRACKER]`",
        report.getMergeErrors().get(0).getMessage());
  }
}
