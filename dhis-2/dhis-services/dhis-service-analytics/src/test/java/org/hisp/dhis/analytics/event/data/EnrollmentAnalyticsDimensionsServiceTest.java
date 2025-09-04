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

import static org.hisp.dhis.analytics.common.AnalyticsDimensionsTestSupport.allValueTypeDataElements;
import static org.hisp.dhis.analytics.common.AnalyticsDimensionsTestSupport.allValueTypeTEAs;
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
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.security.acl.AclService;
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
  @Mock private AclService aclService;

  @InjectMocks
  private DefaultEnrollmentAnalyticsDimensionsService enrollmentAnalyticsDimensionsService;

  @BeforeEach
  void setup() {
    injectSecurityContextNoSettings(new SystemUser());

    when(programService.getProgram(any())).thenReturn(mock(Program.class));
  }

  @Test
  void testQueryDoesntContainDisallowedValueTypes() {
    // Prepare program with a stage and TEAs
    Program program = mock(Program.class);
    when(programService.getProgram("anUid")).thenReturn(program);
    when(program.isRegistration()).thenReturn(true);
    when(program.getProgramIndicators()).thenReturn(java.util.Collections.emptySet());

    ProgramStage stage = new ProgramStage();
    stage.setProgram(program);
    stage.setProgramStageDataElements(
        allValueTypeDataElements().stream()
            .map(de -> new ProgramStageDataElement(stage, de))
            .collect(java.util.stream.Collectors.toSet()));
    when(program.getProgramStages()).thenReturn(java.util.Set.of(stage));
    when(program.getTrackedEntityAttributes()).thenReturn(allValueTypeTEAs());

    List<BaseIdentifiableObject> analyticsDimensions =
        enrollmentAnalyticsDimensionsService.getQueryDimensionsByProgramId("anUid").stream()
            .map(PrefixedDimension::getItem)
            .toList();

    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof ProgramStageDataElement)
            .map(psde -> ((ProgramStageDataElement) psde).getDataElement().getValueType())
            .noneMatch(queryDisallowedValueTypesPredicate()));
    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof TrackedEntityAttribute)
            .map(tea -> ((TrackedEntityAttribute) tea).getValueType())
            .noneMatch(queryDisallowedValueTypesPredicate()));
  }

  @Test
  void testAggregateOnlyContainsAllowedValueTypes() {
    // Prepare program with stages having all value type data elements
    Program program = mock(Program.class);
    when(programService.getProgram("anUid")).thenReturn(program);

    ProgramStage stage = new ProgramStage();
    stage.setProgram(program);
    stage.setProgramStageDataElements(
        allValueTypeDataElements().stream()
            .map(de -> new ProgramStageDataElement(stage, de))
            .collect(java.util.stream.Collectors.toSet()));
    when(program.getProgramStages()).thenReturn(java.util.Set.of(stage));
    when(program.getTrackedEntityAttributes()).thenReturn(allValueTypeTEAs());

    List<BaseIdentifiableObject> analyticsDimensions =
        enrollmentAnalyticsDimensionsService.getAggregateDimensionsByProgramId("anUid").stream()
            .map(PrefixedDimension::getItem)
            .toList();

    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof ProgramStageDataElement)
            .map(psde -> ((ProgramStageDataElement) psde).getDataElement().getValueType())
            .allMatch(aggregateAllowedValueTypesPredicate()));
    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof TrackedEntityAttribute)
            .map(tea -> ((TrackedEntityAttribute) tea).getValueType())
            .allMatch(aggregateAllowedValueTypesPredicate()));
  }

  @Test
  void testAggregateByProgramReturnsEmptyWhenProgramNotFound() {
    when(programService.getProgram(""))
        .thenReturn(null); // override any() default for this specific case

    List<PrefixedDimension> dims =
        enrollmentAnalyticsDimensionsService.getAggregateDimensionsByProgramId("");
    assertTrue(dims.isEmpty());
  }
}
