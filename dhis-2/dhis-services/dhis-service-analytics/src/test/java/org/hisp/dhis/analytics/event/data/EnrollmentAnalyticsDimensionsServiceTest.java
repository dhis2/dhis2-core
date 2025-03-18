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

import static org.hisp.dhis.analytics.common.DimensionServiceCommonTest.aggregateAllowedValueTypesPredicate;
import static org.hisp.dhis.analytics.common.DimensionServiceCommonTest.queryDisallowedValueTypesPredicate;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentAnalyticsDimensionsServiceTest {
  @Mock private ProgramService programService;

  @InjectMocks
  private DefaultEnrollmentAnalyticsDimensionsService enrollmentAnalyticsDimensionsService;

  @BeforeEach
  void setup() {
    injectSecurityContextNoSettings(new SystemUser());

    Program program = mock(Program.class);

    when(programService.getProgram(any())).thenReturn(program);
  }

  @Test
  void testQueryDoesntContainDisallowedValueTypes() {
    List<BaseIdentifiableObject> analyticsDimensions =
        enrollmentAnalyticsDimensionsService.getQueryDimensionsByProgramId("anUid").stream()
            .map(PrefixedDimension::getItem)
            .toList();

    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof DataElement)
            .map(de -> ((DataElement) de).getValueType())
            .noneMatch(queryDisallowedValueTypesPredicate()));
    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof TrackedEntityAttribute)
            .map(tea -> ((TrackedEntityAttribute) tea).getValueType())
            .noneMatch(queryDisallowedValueTypesPredicate()));
  }

  @Test
  void testAggregateOnlyContainsAllowedValueTypes() {
    List<BaseIdentifiableObject> analyticsDimensions =
        enrollmentAnalyticsDimensionsService
            .getAggregateDimensionsByProgramStageId("anUid")
            .stream()
            .map(PrefixedDimension::getItem)
            .toList();

    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof DataElement)
            .map(de -> ((DataElement) de).getValueType())
            .allMatch(aggregateAllowedValueTypesPredicate()));
    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof TrackedEntityAttribute)
            .map(tea -> ((TrackedEntityAttribute) tea).getValueType())
            .allMatch(aggregateAllowedValueTypesPredicate()));
  }
}
