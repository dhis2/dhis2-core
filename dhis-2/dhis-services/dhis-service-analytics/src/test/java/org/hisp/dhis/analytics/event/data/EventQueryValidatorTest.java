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
package org.hisp.dhis.analytics.event.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultEventQueryValidator}.
 *
 * @author Lars Helge Overland
 */
@ExtendWith(MockitoExtension.class)
class EventQueryValidatorTest extends DhisConvenienceTest {
  private Program prA;

  private Program prB;

  private DataElement deA;

  private DataElement deB;

  private DataElement deC;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private LegendSet lsA;

  private OptionSet osA;

  @Mock private SystemSettingManager systemSettingManager;

  @Mock private QueryValidator queryValidator;

  @InjectMocks private DefaultEventQueryValidator eventQueryValidator;

  @BeforeEach
  public void setUpTest() {
    prA = createProgram('A');
    prB = createProgram('B');

    deA = createDataElement('A', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.TRACKER);
    deB =
        createDataElement(
            'E', ValueType.COORDINATE, AggregationType.NONE, DataElementDomain.TRACKER);
    deC =
        createDataElement('G', ValueType.DATETIME, AggregationType.NONE, DataElementDomain.TRACKER);

    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B', ouA);

    lsA = createLegendSet('A');

    osA = new OptionSet("OptionSetA", ValueType.TEXT);
  }

  @Test
  void validateSuccessA() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouA))
            .build();

    eventQueryValidator.validate(params);
  }

  @Test
  void validateValidTimeField() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouA))
            .withTimeField(TimeField.INCIDENT_DATE.name())
            .build();

    eventQueryValidator.validate(params);
  }

  @Test
  void validateSingleDataElementMultipleProgramsQueryItemSuccess() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouA))
            .addItem(new QueryItem(deA, prA, null, ValueType.TEXT, AggregationType.NONE, null))
            .addItem(new QueryItem(deA, prB, null, ValueType.TEXT, AggregationType.NONE, null))
            .build();

    eventQueryValidator.validate(params);
  }

  @Test
  void validateValidFilterForValueType() {
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "68");
    QueryItem item =
        new QueryItem(
            deA,
            deA.getLegendSet(),
            deA.getValueType(),
            deA.getAggregationType(),
            deA.getOptionSet());
    item.addFilter(filter);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouB))
            .addItem(item)
            .build();

    eventQueryValidator.validate(params);
  }

  @Test
  void validateDuplicateQueryItems() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouA))
            .addItem(new QueryItem(deA, prA, null, ValueType.TEXT, AggregationType.NONE, null))
            .addItem(new QueryItem(deA, prA, null, ValueType.TEXT, AggregationType.NONE, null))
            .build();

    ErrorMessage error = eventQueryValidator.validateForErrorMessage(params);

    assertEquals(ErrorCode.E7202, error.getErrorCode());
  }

  @Test
  void validateFailureNoStartEndDatePeriods() {
    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(prA).withOrganisationUnits(List.of(ouB)).build();

    assertValidationError(ErrorCode.E7205, params);
  }

  @Test
  void validateErrorNoStartEndDatePeriods() {
    EventQueryParams params =
        new EventQueryParams.Builder().withProgram(prA).withOrganisationUnits(List.of(ouB)).build();

    ErrorMessage error = eventQueryValidator.validateForErrorMessage(params);

    assertEquals(ErrorCode.E7205, error.getErrorCode());
  }

  @Test
  void validateInvalidQueryItemBothLegendSetAndOptionSet() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouB))
            .addItem(new QueryItem(deA, lsA, ValueType.TEXT, AggregationType.NONE, osA))
            .build();

    assertValidationError(ErrorCode.E7215, params);
  }

  @Test
  void validateInvalidFilterForIntegerValueType() {
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "male");
    QueryItem item =
        new QueryItem(
            deA,
            deA.getLegendSet(),
            deA.getValueType(),
            deA.getAggregationType(),
            deA.getOptionSet());
    item.addFilter(filter);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouB))
            .addItem(item)
            .build();

    assertValidationError(ErrorCode.E7234, params);
  }

  @Test
  void validateInvalidInFilterForIntegerValueType() {
    QueryFilter filter = new QueryFilter(QueryOperator.IN, "male;1");
    QueryItem item =
        new QueryItem(
            deA,
            deA.getLegendSet(),
            deA.getValueType(),
            deA.getAggregationType(),
            deA.getOptionSet());
    item.addFilter(filter);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouB))
            .addItem(item)
            .build();

    assertValidationError(ErrorCode.E7234, params);
  }

  @Test
  void validateValidInFilterForIntegerValueType() {
    QueryFilter filter = new QueryFilter(QueryOperator.IN, "2;1");
    QueryItem item =
        new QueryItem(
            deA,
            deA.getLegendSet(),
            deA.getValueType(),
            deA.getAggregationType(),
            deA.getOptionSet());
    item.addFilter(filter);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouB))
            .addItem(item)
            .build();

    ErrorMessage error = eventQueryValidator.validateForErrorMessage(params);

    assertNull(error);
  }

  @Test
  void validateInvalidFilterForDateTimeValueType() {
    QueryFilter filter = new QueryFilter(QueryOperator.EQ, "2023-12-01");
    QueryItem item =
        new QueryItem(
            deC,
            deC.getLegendSet(),
            deC.getValueType(),
            deC.getAggregationType(),
            deC.getOptionSet());
    item.addFilter(filter);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouB))
            .addItem(item)
            .build();

    assertValidationError(ErrorCode.E7234, params);
  }

  @Test
  void validateInvalidTimeField() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouA))
            .withTimeField("notAUidOrTimeField")
            .build();

    assertValidationError(ErrorCode.E7210, params);
  }

  @Test
  void validateInvalidOrgUnitField() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouA))
            .withOrgUnitField(new OrgUnitField("notAUid"))
            .build();

    assertValidationError(ErrorCode.E7211, params);
  }

  @Test
  void validateErrorPage() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouB))
            .withPage(-2)
            .build();

    ErrorMessage error = eventQueryValidator.validateForErrorMessage(params);

    assertEquals(ErrorCode.E7207, error.getErrorCode());
  }

  @Test
  void validateErrorPageSize() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouB))
            .withPageSize(-1)
            .build();

    ErrorMessage error = eventQueryValidator.validateForErrorMessage(params);

    assertEquals(ErrorCode.E7208, error.getErrorCode());
  }

  @Test
  void validateErrorMaxLimit() {
    when(systemSettingManager.getIntSetting(SettingKey.ANALYTICS_MAX_LIMIT)).thenReturn(100);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouB))
            .withLimit(200)
            .build();

    ErrorMessage error = eventQueryValidator.validateForErrorMessage(params);

    assertEquals(ErrorCode.E7209, error.getErrorCode());
  }

  @Test
  void validateErrorClusterSize() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withStartDate(new DateTime(2010, 6, 1, 0, 0).toDate())
            .withEndDate(new DateTime(2012, 3, 20, 0, 0).toDate())
            .withOrganisationUnits(List.of(ouB))
            .withCoordinateFields(List.of(deB.getUid()))
            .withClusterSize(-3L)
            .build();

    ErrorMessage error = eventQueryValidator.validateForErrorMessage(params);

    assertEquals(ErrorCode.E7212, error.getErrorCode());
  }

  /**
   * Asserts whether the given error code is thrown by the query validator for the given query.
   *
   * @param errorCode the {@link ErrorCode}.
   * @param params the {@link DataQueryParams}.
   */
  private void assertValidationError(ErrorCode errorCode, EventQueryParams params) {
    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> eventQueryValidator.validate(params));
    assertEquals(errorCode, ex.getErrorCode());
  }
}
