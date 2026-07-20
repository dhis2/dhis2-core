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
package org.hisp.dhis.analytics.trackedentity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackedEntityRequestParamsMapperTest {
  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private ProgramService programService;

  @InjectMocks private TrackedEntityQueryRequestMapper mapper;

  @Test
  void testOneProgramFailing() {
    testValidateTrackedEntityType(
        "T1", "Program(s) `[nameB (B)]` are not defined on Tracked Entity Type `nameT1 (T1)`");
  }

  @Test
  void testTwoProgramsFailing() {
    testValidateTrackedEntityType(
        "T3",
        "Program(s) `[nameA (A), nameB (B)]` are not defined on Tracked Entity Type `nameT3 (T3)`");
  }

  void testValidateTrackedEntityType(String trackedEntityTypeUid, String expectedMessage) {
    Program programA = stubProgram("A", "T1");
    Program programB = stubProgram("B", "T2");

    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setProgram(Set.of("A", "B"));

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    when(programService.getPrograms(Set.of("A", "B"))).thenReturn(Set.of(programA, programB));

    final IllegalQueryException thrown =
        assertThrows(
            IllegalQueryException.class, () -> mapper.map(trackedEntityTypeUid, requestParams));

    assertEquals(expectedMessage, thrown.getMessage());
  }

  @Test
  void testOK() {
    String trackedEntityTypeUid = "T1";

    Program programA = stubProgram("A", "T1");
    Program programB = stubProgram("B", "T1");

    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setProgram(Set.of("A", "B"));

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    when(programService.getPrograms(Set.of("A", "B"))).thenReturn(Set.of(programA, programB));

    assertDoesNotThrow(() -> mapper.map(trackedEntityTypeUid, requestParams));
  }

  @Test
  void testOKWhenNoPrograms() {
    String trackedEntityTypeUid = "T1";

    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);

    CommonRequestParams requestParams = new CommonRequestParams();

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    assertDoesNotThrow(() -> mapper.map(trackedEntityTypeUid, requestParams));
  }

  @Test
  void testValueResolvesToNumericAttributeAndDefaultsToAverage() {
    String trackedEntityTypeUid = "T1";
    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);
    TrackedEntityAttribute attribute = stubAttribute(trackedEntityType, ValueType.NUMBER);

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setValue(attribute.getUid());

    TrackedEntityQueryParams params = mapper.map(trackedEntityTypeUid, requestParams);

    assertEquals(attribute.getUid(), params.getValue().getUid());
    assertEquals(AggregationType.AVERAGE, params.getAggregationType());
  }

  @Test
  void testExplicitAggregationTypeIsKept() {
    String trackedEntityTypeUid = "T1";
    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);
    TrackedEntityAttribute attribute = stubAttribute(trackedEntityType, ValueType.INTEGER);

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setValue(attribute.getUid());
    requestParams.setAggregationType(AggregationType.SUM);

    TrackedEntityQueryParams params = mapper.map(trackedEntityTypeUid, requestParams);

    assertEquals(attribute.getUid(), params.getValue().getUid());
    assertEquals(AggregationType.SUM, params.getAggregationType());
  }

  @Test
  void testValueResolvesToProgramAttribute() {
    String trackedEntityTypeUid = "T1";
    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);
    Program program = stubProgram("A", trackedEntityTypeUid);
    TrackedEntityAttribute attribute = stubProgramAttribute(program, ValueType.NUMBER);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setProgram(Set.of("A"));
    requestParams.setValue(attribute.getUid());

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);
    when(programService.getPrograms(Set.of("A"))).thenReturn(Set.of(program));

    TrackedEntityQueryParams params = mapper.map(trackedEntityTypeUid, requestParams);

    assertEquals(attribute.getUid(), params.getValue().getUid());
    assertEquals(AggregationType.AVERAGE, params.getAggregationType());
  }

  @Test
  void testValueNotAnAttributeOfTrackedEntityType() {
    String trackedEntityTypeUid = "T1";
    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setValue("PxAttUid001");

    IllegalQueryException thrown =
        assertThrows(
            IllegalQueryException.class, () -> mapper.map(trackedEntityTypeUid, requestParams));

    assertEquals(ErrorCode.E7256, thrown.getErrorCode());
  }

  @Test
  void testValueNotNumeric() {
    String trackedEntityTypeUid = "T1";
    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);
    TrackedEntityAttribute attribute = stubAttribute(trackedEntityType, ValueType.TEXT);

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setValue(attribute.getUid());

    IllegalQueryException thrown =
        assertThrows(
            IllegalQueryException.class, () -> mapper.map(trackedEntityTypeUid, requestParams));

    assertEquals(ErrorCode.E7256, thrown.getErrorCode());
  }

  @Test
  void testUnsupportedAggregationTypeIsRejected() {
    String trackedEntityTypeUid = "T1";
    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);
    TrackedEntityAttribute attribute = stubAttribute(trackedEntityType, ValueType.NUMBER);

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setValue(attribute.getUid());
    requestParams.setAggregationType(AggregationType.FIRST);

    IllegalQueryException thrown =
        assertThrows(
            IllegalQueryException.class, () -> mapper.map(trackedEntityTypeUid, requestParams));

    assertEquals(ErrorCode.E7254, thrown.getErrorCode());
  }

  @Test
  void testNumericAggregationTypeWithoutValueIsRejected() {
    String trackedEntityTypeUid = "T1";
    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setAggregationType(AggregationType.SUM);

    IllegalQueryException thrown =
        assertThrows(
            IllegalQueryException.class, () -> mapper.map(trackedEntityTypeUid, requestParams));

    assertEquals(ErrorCode.E7255, thrown.getErrorCode());
  }

  @Test
  void testCountAggregationTypeWithoutValueIsAllowed() {
    String trackedEntityTypeUid = "T1";
    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    CommonRequestParams requestParams = new CommonRequestParams();
    requestParams.setAggregationType(AggregationType.COUNT);

    assertDoesNotThrow(() -> mapper.map(trackedEntityTypeUid, requestParams));
  }

  @Test
  void testNoValueLeavesAggregationUnset() {
    String trackedEntityTypeUid = "T1";
    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    TrackedEntityQueryParams params = mapper.map(trackedEntityTypeUid, new CommonRequestParams());

    assertNull(params.getValue());
    assertNull(params.getAggregationType());
  }

  private TrackedEntityAttribute stubAttribute(
      TrackedEntityType trackedEntityType, ValueType valueType) {
    TrackedEntityAttribute attribute = new TrackedEntityAttribute();
    attribute.setUid("TeAttrUid01");
    attribute.setValueType(valueType);
    trackedEntityType.setTrackedEntityTypeAttributes(
        List.of(new TrackedEntityTypeAttribute(trackedEntityType, attribute)));
    return attribute;
  }

  private TrackedEntityAttribute stubProgramAttribute(Program program, ValueType valueType) {
    TrackedEntityAttribute attribute = new TrackedEntityAttribute();
    attribute.setUid("ProgAttrU01");
    attribute.setValueType(valueType);
    program.setProgramAttributes(List.of(new ProgramTrackedEntityAttribute(program, attribute)));
    return attribute;
  }

  private Program stubProgram(String uid, String tetUid) {
    Program program = new Program("name" + uid, "description" + uid);
    program.setUid(uid);
    program.setTrackedEntityType(stubTrackedEntityType(tetUid));
    return program;
  }

  private TrackedEntityType stubTrackedEntityType(String tetUid) {
    TrackedEntityType trackedEntityType =
        new TrackedEntityType("name" + tetUid, "description" + tetUid);
    trackedEntityType.setUid(tetUid);
    return trackedEntityType;
  }
}
