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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hisp.dhis.dxf2.Constants.PROGRAM_RULE_VARIABLE_NAME_INVALID_KEYWORDS;
import static org.hisp.dhis.feedback.ErrorCode.E4051;
import static org.hisp.dhis.feedback.ErrorCode.E4052;
import static org.hisp.dhis.feedback.ErrorCode.E4059;
import static org.hisp.dhis.feedback.ErrorCode.E4089;
import static org.hisp.dhis.feedback.ErrorCode.E4090;
import static org.hisp.dhis.feedback.ErrorCode.E4091;
import static org.hisp.dhis.feedback.ErrorCode.E4092;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luca Cambi
 */
@ExtendWith(MockitoExtension.class)
class ProgramRuleVariableObjectBundleHookTest {

  @InjectMocks private ProgramRuleVariableObjectBundleHook programRuleVariableObjectBundleHook;

  @Mock private EntityManager entityManager;

  @Mock private Session session;

  @Mock private ProgramRuleVariable programRuleVariable;

  @Mock private Query<ProgramRuleVariable> query;

  @Mock private Program program;

  @Mock private ObjectBundle objectBundle;

  @Captor private ArgumentCaptor<Class<ProgramRuleVariable>> classArgumentCaptor;

  @BeforeEach
  public void setUp() {
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.createQuery(anyString(), classArgumentCaptor.capture())).thenReturn(query);
    when(program.getUid()).thenReturn("uid");
    // Common defaults; individual tests override what they need.
    lenient().when(programRuleVariable.getProgram()).thenReturn(program);
    lenient().when(programRuleVariable.getName()).thenReturn("someVar");
    lenient().when(objectBundle.getImportMode()).thenReturn(ImportStrategy.CREATE);
    lenient().when(query.getResultList()).thenReturn(Collections.emptyList());
  }

  @Test
  void shouldFailInsertAlreadyExisting() {
    when(query.getResultList()).thenReturn(Collections.singletonList(new ProgramRuleVariable()));
    when(programRuleVariable.getName()).thenReturn("word");
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.CALCULATED_VALUE);
    when(programRuleVariable.getValueType()).thenReturn(ValueType.TEXT);

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(1, errorReports.size());
    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4051)));
  }

  @Test
  void shouldNotFailUpdateExistingSameUid() {
    when(objectBundle.getImportMode()).thenReturn(ImportStrategy.CREATE_AND_UPDATE);
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.CALCULATED_VALUE);
    when(programRuleVariable.getValueType()).thenReturn(ValueType.TEXT);

    ProgramRuleVariable existingProgramRuleVariable = new ProgramRuleVariable();
    existingProgramRuleVariable.setName("word");
    existingProgramRuleVariable.setUid("uid1");

    when(query.getResultList()).thenReturn(Collections.singletonList(existingProgramRuleVariable));
    when(programRuleVariable.getName()).thenReturn("word");
    when(programRuleVariable.getUid()).thenReturn("uid1");

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(0, errorReports.size());
  }

  @Test
  void shouldNotFailUpdateExistingMoreThanOneSameUid() {
    when(objectBundle.getImportMode()).thenReturn(ImportStrategy.CREATE_AND_UPDATE);
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.CALCULATED_VALUE);
    when(programRuleVariable.getValueType()).thenReturn(ValueType.TEXT);

    ProgramRuleVariable existingProgramRuleVariable = new ProgramRuleVariable();
    existingProgramRuleVariable.setName("word");
    existingProgramRuleVariable.setUid("uid1");

    ProgramRuleVariable anotherExistingProgramRuleVariable = new ProgramRuleVariable();
    anotherExistingProgramRuleVariable.setName("word");
    anotherExistingProgramRuleVariable.setUid("uid2");

    when(query.getResultList())
        .thenReturn(List.of(existingProgramRuleVariable, anotherExistingProgramRuleVariable));
    when(programRuleVariable.getName()).thenReturn("word");
    when(programRuleVariable.getUid()).thenReturn("uid1");

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(0, errorReports.size());
  }

  @Test
  void shouldFailUpdateExistingDifferentUid() {
    when(objectBundle.getImportMode()).thenReturn(ImportStrategy.CREATE_AND_UPDATE);
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.CALCULATED_VALUE);
    when(programRuleVariable.getValueType()).thenReturn(ValueType.TEXT);

    ProgramRuleVariable existingProgramRuleVariable = new ProgramRuleVariable();
    existingProgramRuleVariable.setName("word");
    existingProgramRuleVariable.setUid("uid1");

    when(query.getResultList()).thenReturn(Collections.singletonList(existingProgramRuleVariable));
    when(programRuleVariable.getName()).thenReturn("word");
    when(programRuleVariable.getUid()).thenReturn("uid2");

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(1, errorReports.size());
    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4051)));
  }

  @Test
  void shouldFailValidationInvalidCountAndInvalidName() {
    when(query.getResultList()).thenReturn(Collections.singletonList(new ProgramRuleVariable()));
    when(programRuleVariable.getName())
        .thenReturn("Word " + PROGRAM_RULE_VARIABLE_NAME_INVALID_KEYWORDS.get(0) + " Word");
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.CALCULATED_VALUE);
    when(programRuleVariable.getValueType()).thenReturn(ValueType.TEXT);

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(2, errorReports.size());
    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4051)));
    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4052)));
  }

  @Test
  void shouldFailValidationInvalidName() {
    when(objectBundle.getImportMode()).thenReturn(ImportStrategy.CREATE_AND_UPDATE);
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.CALCULATED_VALUE);
    when(programRuleVariable.getValueType()).thenReturn(ValueType.TEXT);
    List<ErrorReport> errorReports;

    for (String invalidKeyWord : PROGRAM_RULE_VARIABLE_NAME_INVALID_KEYWORDS) {
      when(programRuleVariable.getName()).thenReturn("Word " + invalidKeyWord + " Word");
      errorReports =
          programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);
      assertEquals(1, errorReports.size());
      assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4052)));

      when(programRuleVariable.getName()).thenReturn(invalidKeyWord + " Word");
      errorReports =
          programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);
      assertEquals(1, errorReports.size());
      assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4052)));

      when(programRuleVariable.getName()).thenReturn("Word " + invalidKeyWord);
      errorReports =
          programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);
      assertEquals(1, errorReports.size());
      assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4052)));
    }
  }

  @Test
  void shouldPassValidationWithValidName() {
    when(objectBundle.getImportMode()).thenReturn(ImportStrategy.CREATE_AND_UPDATE);
    when(programRuleVariable.getName()).thenReturn("WordAndWord");
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.CALCULATED_VALUE);
    when(programRuleVariable.getValueType()).thenReturn(ValueType.TEXT);

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);
    assertEquals(0, errorReports.size());

    when(programRuleVariable.getName()).thenReturn("Word and_another Word");

    List<ErrorReport> errorReports1 =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);
    assertEquals(0, errorReports1.size());
  }

  static Stream<ProgramRuleVariableSourceType> dataElementSourceTypes() {
    return ProgramRuleVariableSourceType.getDataTypes().stream();
  }

  @ParameterizedTest
  @MethodSource("dataElementSourceTypes")
  void shouldFailForAllDataElementSourceTypesWhenDataElementIsNull(
      ProgramRuleVariableSourceType sourceType) {
    when(programRuleVariable.getSourceType()).thenReturn(sourceType);
    when(programRuleVariable.getDataElement()).thenReturn(null);

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4059)));
  }

  @Test
  void shouldPassWhenDataElementSourceTypeHasDataElement() {
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT);
    when(programRuleVariable.getDataElement()).thenReturn(new DataElement());

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(0, errorReports.size());
  }

  @Test
  void shouldFailWhenTeAttributeSourceTypeHasNoAttribute() {
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.TEI_ATTRIBUTE);
    when(programRuleVariable.getAttribute()).thenReturn(null);

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(1, errorReports.size());
    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4089)));
  }

  @Test
  void shouldPassWhenTeAttributeSourceTypeHasAttribute() {
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.TEI_ATTRIBUTE);
    when(programRuleVariable.getAttribute()).thenReturn(new TrackedEntityAttribute());

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(0, errorReports.size());
  }

  @Test
  void shouldPassWhenCalculatedValueSourceTypeHasValueType() {
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.CALCULATED_VALUE);
    when(programRuleVariable.getValueType()).thenReturn(ValueType.TEXT);

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(0, errorReports.size());
  }

  @Test
  void shouldFailWhenSourceTypeIsMissing() {
    when(programRuleVariable.getSourceType()).thenReturn(null);

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(1, errorReports.size());
    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4090)));
  }

  @Test
  void shouldFailWhenProgramStageMissingForProgramStageDataElementSourceType() {
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE);
    when(programRuleVariable.getDataElement()).thenReturn(new DataElement());
    when(programRuleVariable.getProgramStage()).thenReturn(null);

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(1, errorReports.size());
    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4091)));
  }

  @Test
  void shouldPassWhenDataElementAndProgramStagePresentForProgramStageSourceType() {
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE);
    when(programRuleVariable.getDataElement()).thenReturn(new DataElement());
    when(programRuleVariable.getProgramStage()).thenReturn(new ProgramStage());

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(0, errorReports.size());
  }

  @Test
  void shouldFailWhenCalculatedValueHasNoValueType() {
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.CALCULATED_VALUE);
    when(programRuleVariable.getValueType()).thenReturn(null);

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(1, errorReports.size());
    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4092)));
  }

  @Test
  void shouldFailWhenBothDataElementAndProgramStageMissingForProgramStageSourceType() {
    when(programRuleVariable.getSourceType())
        .thenReturn(ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE);
    when(programRuleVariable.getDataElement()).thenReturn(null);
    when(programRuleVariable.getProgramStage()).thenReturn(null);

    List<ErrorReport> errorReports =
        programRuleVariableObjectBundleHook.validate(programRuleVariable, objectBundle);

    assertEquals(2, errorReports.size());
    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4059)));
    assertTrue(errorReports.stream().anyMatch(e -> e.getErrorCode().equals(E4091)));
  }
}
