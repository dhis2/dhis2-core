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
package org.hisp.dhis.validation;

import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.validation.outlierdetection.ValidationOutlierDetectionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ValidationOutlierDetectionRequestTest {

  @Mock private SystemSettingManager systemSettingManager;

  private ValidationOutlierDetectionRequest subject;

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------

  private DataElement deA;

  private DataElement deB;

  private DataElement deC;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  @BeforeEach
  public void setUp() {
    subject = new ValidationOutlierDetectionRequest(systemSettingManager);
    when(systemSettingManager.getSystemSetting(SettingKey.ANALYTICS_MAX_LIMIT, Integer.class)).thenReturn(500);
    deA = createDataElement('A', ValueType.INTEGER, AggregationType.SUM);
    deB = createDataElement('B', ValueType.INTEGER, AggregationType.SUM);
    deC = createDataElement('C', ValueType.NUMBER, AggregationType.SUM);

    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
  }

  @ParameterizedTest
  @ValueSource(booleans =  {true, false})
  void testSuccessfulValidation(boolean isAnalytics) {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 3, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .build();

    assertDoesNotThrow(() -> subject.validate(request, isAnalytics));
  }
  @ParameterizedTest
  @ValueSource(booleans =  {true, false})
  void testErrorValidation() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 3, 1))
            .build();

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> subject.validate(request, true));
    assertEquals(ErrorCode.E2203, ex.getErrorCode());
  }

  @Test
  void testErrorNoDataElements() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 7, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .build();
    assertRequest(request, ErrorCode.E2200);
  }

  @Test
  void testErrorStartAfterEndDates() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 6, 1), getDate(2020, 3, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .build();

    assertRequest(request, ErrorCode.E2202);
  }

  @Test
  void testErrorNegativeThreshold() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 6, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .withThreshold(-23.4)
            .build();
    assertRequest(request, ErrorCode.E2204);
  }

  @Test
  void testErrorNegativeMaxResults() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 3, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .withMaxResults(-100)
            .build();

    assertRequest(request, ErrorCode.E2205);
  }

  @Test
  void testErrorDataStartDateIsNotAllowed() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 6, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .withDataStartDate(getDate(2020, 5, 1))
            .withDataEndDate(getDate(2020, 6, 1))
            .build();

    assertRequest(request, ErrorCode.E2209);
  }

  @Test
  void testErrorDataEndDateIsNotAllowed() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 6, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .withDataEndDate(getDate(2020, 5, 1))
            .build();

    assertRequest(request, ErrorCode.E2210);
  }

  @Test
  void testMinMaxAlgorithmIsNotAllowed() {
    OutlierDetectionRequest request =
        new OutlierDetectionRequest.Builder()
            .withDataElements(Lists.newArrayList(deA, deB, deC))
            .withStartEndDate(getDate(2020, 1, 1), getDate(2020, 6, 1))
            .withOrgUnits(Lists.newArrayList(ouA, ouB))
            .withAlgorithm(OutlierDetectionAlgorithm.MIN_MAX)
            .build();

    assertRequest(request, ErrorCode.E2211);
  }

  private void assertRequest(OutlierDetectionRequest request, ErrorCode errorCode) {
    assertThrows(IllegalQueryException.class, () -> subject.validate(request, true));
    try {
      subject.validate(request, false);
    } catch (IllegalQueryException e) {
      assertEquals(errorCode, e.getErrorCode());
    }
  }
}
