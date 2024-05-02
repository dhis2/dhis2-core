package org.hisp.dhis.merge.dataelement;

import static org.hisp.dhis.merge.dataelement.DataElementMergeValidator.DataElementPredicate.DOMAIN_TYPE_MISMATCH;
import static org.hisp.dhis.merge.dataelement.DataElementMergeValidator.DataElementPredicate.VALUE_TYPE_MISMATCH;
import static org.hisp.dhis.merge.dataelement.DataElementMergeValidator.DataElementPropertyCheck.DOMAIN_TYPE_CHECK;
import static org.hisp.dhis.merge.dataelement.DataElementMergeValidator.DataElementPropertyCheck.VALUE_TYPE_CHECK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataElementMergeValidatorTest extends DhisConvenienceTest {

  @Test
  @DisplayName("When ValueTypes match, there is no mismatch")
  void valueTypesMatchTest() {
    // given
    DataElement target = createDataElement('a', ValueType.TEXT, AggregationType.NONE);
    DataElement source = createDataElement('b', ValueType.TEXT, AggregationType.NONE);

    // then
    assertFalse(VALUE_TYPE_MISMATCH.test(target, source));
  }

  @Test
  @DisplayName("When ValueTypes do not match, there is a mismatch")
  void valueTypesDoNotMatchTest() {
    // given
    DataElement target = createDataElement('a', ValueType.TEXT, AggregationType.NONE);
    DataElement source = createDataElement('b', ValueType.INTEGER, AggregationType.NONE);

    // then
    assertTrue(VALUE_TYPE_MISMATCH.test(target, source));
  }

  @Test
  @DisplayName("When DomainTypes match, there is no mismatch")
  void domainTypesMatchTest() {
    // given
    DataElement target =
        createDataElement('a', ValueType.TEXT, AggregationType.NONE, DataElementDomain.AGGREGATE);
    DataElement source =
        createDataElement('b', ValueType.TEXT, AggregationType.NONE, DataElementDomain.AGGREGATE);

    // then
    assertFalse(VALUE_TYPE_MISMATCH.test(target, source));
  }

  @Test
  @DisplayName("When DomainTypes do not match, there is a mismatch")
  void domainTypesDoNotMatchTest() {
    // given
    DataElement target =
        createDataElement('a', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER);
    DataElement source =
        createDataElement(
            'b', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.AGGREGATE);

    // then
    assertTrue(DOMAIN_TYPE_MISMATCH.test(target, source));
  }

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
        validator.validateMismatches(
            target,
            List.of(source1, source2, source3),
            VALUE_TYPE_CHECK,
            new MergeReport(MergeType.DATA_ELEMENT));

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
        validator.validateMismatches(
            target,
            List.of(source1, source2, source3),
            VALUE_TYPE_CHECK,
            new MergeReport(MergeType.DATA_ELEMENT));

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
        validator.validateMismatches(
            target,
            List.of(source1, source2, source3),
            VALUE_TYPE_CHECK,
            new MergeReport(MergeType.DATA_ELEMENT));

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
        validator.validateMismatches(
            target,
            List.of(source1, source2, source3),
            DOMAIN_TYPE_CHECK,
            new MergeReport(MergeType.DATA_ELEMENT));

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
        validator.validateMismatches(
            target,
            List.of(source1, source2, source3),
            DOMAIN_TYPE_CHECK,
            new MergeReport(MergeType.DATA_ELEMENT));

    // then
    assertTrue(report.hasErrorMessages());
    assertEquals(ErrorCode.E1551, report.getMergeErrors().get(0).getErrorCode());
    assertEquals(
        "All source DataElementDomains must match target DataElementDomain: `AGGREGATE`. Other DataElementDomains found: `[TRACKER]`",
        report.getMergeErrors().get(0).getMessage());
  }
}
