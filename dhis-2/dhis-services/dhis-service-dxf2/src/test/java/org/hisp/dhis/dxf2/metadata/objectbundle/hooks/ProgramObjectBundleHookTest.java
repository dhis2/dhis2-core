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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.EventProgramEnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramCategoryMapping;
import org.hisp.dhis.program.ProgramCategoryOptionMapping;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AclService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class ProgramObjectBundleHookTest {
  private ProgramObjectBundleHook subject;

  @Mock private EventProgramEnrollmentService eventProgramEnrollmentService;

  @Mock private ProgramStageService programStageService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private AclService aclService;

  @Mock private IdentifiableObjectManager identifiableObjectManager;

  private Program programA;

  @BeforeEach
  public void setUp() {
    this.subject =
        new ProgramObjectBundleHook(
            eventProgramEnrollmentService,
            programStageService,
            organisationUnitService,
            aclService,
            identifiableObjectManager);

    programA = createProgram('A');
    programA.setId(100);
  }

  @Test
  void verifyNullObjectIsIgnored() {
    subject.preCreate(null, null);

    verifyNoInteractions(eventProgramEnrollmentService);
  }

  @Test
  void verifyMissingBundleIsIgnored() {
    subject.preCreate(programA, null);

    verifyNoInteractions(eventProgramEnrollmentService);
  }

  @Test
  void verifyProgramInstanceIsSavedForEventProgram() {
    when(organisationUnitService.getRootOrganisationUnits())
        .thenReturn(List.of(createOrganisationUnit('A')));
    ArgumentCaptor<Enrollment> argument = ArgumentCaptor.forClass(Enrollment.class);

    programA.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    subject.postCreate(programA, null);

    verify(identifiableObjectManager).save(argument.capture());

    assertThat(argument.getValue().getEnrollmentDate(), is(notNullValue()));
    assertThat(argument.getValue().getOccurredDate(), is(notNullValue()));
    assertThat(argument.getValue().getProgram(), is(programA));
    assertThat(argument.getValue().getStatus(), is(EnrollmentStatus.ACTIVE));
    assertThat(argument.getValue().getStoredBy(), is("system-process"));
  }

  @Test
  void verifyProgramInstanceIsNotSavedForTrackerProgram() {
    ArgumentCaptor<Enrollment> argument = ArgumentCaptor.forClass(Enrollment.class);

    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    subject.postCreate(programA, null);

    verify(identifiableObjectManager, times(0)).save(argument.capture());
  }

  @Test
  void verifyProgramValidates() {
    assertEquals(0, subject.validate(programA, null).size());
  }

  @Test
  void verifyProgramFailsValidation() {
    when(eventProgramEnrollmentService.getEnrollments(programA, EnrollmentStatus.ACTIVE))
        .thenReturn(Lists.newArrayList(new Enrollment(), new Enrollment()));

    List<ErrorReport> errors = subject.validate(programA, null);

    assertEquals(1, errors.size());
    assertEquals(errors.get(0).getErrorCode(), ErrorCode.E6000);
  }

  @Test
  void verifyValidationIsSkippedWhenObjectIsTransient() {
    Program transientObj = createProgram('A');
    subject.validate(transientObj, null);

    verifyNoInteractions(eventProgramEnrollmentService);
  }

  @Test
  void verifyUpdateProgramStage() {
    ProgramStage programStage = createProgramStage('A', 1);
    programA.getProgramStages().add(programStage);

    assertNull(programA.getProgramStages().iterator().next().getProgram());

    subject.postCreate(programA, null);

    assertNotNull(programA.getProgramStages().iterator().next().getProgram());
  }

  @Test
  void verifyGoodCategoryMappins() {
    Set<ProgramCategoryMapping> goodCategoryMappings =
        Set.of(
            ProgramCategoryMapping.builder()
                .id("Qairi4aiG5p")
                .categoryId("mGeengien2R")
                .mappingName("Mapping 1")
                .optionMappings(
                    List.of(
                        ProgramCategoryOptionMapping.builder()
                            .optionId("sephoo5OWah")
                            .filter("true")
                            .build(),
                        ProgramCategoryOptionMapping.builder()
                            .optionId("iQuash1quuu")
                            .filter("false")
                            .build()))
                .build(),
            ProgramCategoryMapping.builder()
                .id("cNah1phu8ce")
                .categoryId("uweesh3Do7e")
                .mappingName("Mapping 2")
                .optionMappings(
                    List.of(
                        ProgramCategoryOptionMapping.builder()
                            .optionId("hohngoo6aiV")
                            .filter("true")
                            .build(),
                        ProgramCategoryOptionMapping.builder()
                            .optionId("rdie8Sibae0")
                            .filter("false")
                            .build()))
                .build());
    programA.setCategoryMappings(goodCategoryMappings);

    assertEquals(0, subject.validate(programA, null).size());
  }

  @Test
  void verifyBadCategoryOptionMappingUid() {
    Set<ProgramCategoryMapping> badCategoryOptionMappingUid =
        Set.of(
            ProgramCategoryMapping.builder()
                .id("Dve0nohNixu")
                .categoryId("vaiZahCei7P")
                .mappingName("Mapping 1")
                .optionMappings(
                    List.of(
                        ProgramCategoryOptionMapping.builder()
                            .optionId("Bad UID")
                            .filter("true")
                            .build()))
                .build());

    programA.setCategoryMappings(badCategoryOptionMappingUid);

    List<ErrorReport> errors = subject.validate(programA, null);

    assertEquals(1, errors.size());
    assertEquals(ErrorCode.E4080, errors.get(0).getErrorCode());
  }

  @Test
  void verifyDuplicateCategoryOptionMappingUids() {
    Set<ProgramCategoryMapping> duplicateCategoryOptionMappingUids =
        Set.of(
            ProgramCategoryMapping.builder()
                .id("Dve0nohNixu")
                .categoryId("vaiZahCei7P")
                .mappingName("Mapping 1")
                .optionMappings(
                    List.of(
                        ProgramCategoryOptionMapping.builder()
                            .optionId("TheSameUids")
                            .filter("true")
                            .build(),
                        ProgramCategoryOptionMapping.builder()
                            .optionId("TheSameUids")
                            .filter("false")
                            .build()))
                .build());

    programA.setCategoryMappings(duplicateCategoryOptionMappingUids);

    List<ErrorReport> errors = subject.validate(programA, null);

    assertEquals(1, errors.size());
    assertEquals(ErrorCode.E4079, errors.get(0).getErrorCode());
  }

  @Test
  void verifyCategoryMappingsWithInvalidId() {
    Set<ProgramCategoryMapping> categoryMappingsWithInvalidId =
        Set.of(
            ProgramCategoryMapping.builder()
                .id("invalid_UID")
                .categoryId("daihai8Vee4")
                .mappingName("Mapping 1")
                .optionMappings(
                    List.of(
                        ProgramCategoryOptionMapping.builder()
                            .optionId("Ueeth6egaeH")
                            .filter("true")
                            .build()))
                .build());

    programA.setCategoryMappings(categoryMappingsWithInvalidId);

    List<ErrorReport> errors = subject.validate(programA, null);

    assertEquals(1, errors.size());
    assertEquals(ErrorCode.E4075, errors.get(0).getErrorCode());
  }

  @Test
  void verifyCategoryMappingsWithDuplicteId() {
    Set<ProgramCategoryMapping> categoryMappingsWithDuplicateId =
        Set.of(
            ProgramCategoryMapping.builder()
                .id("SameGoodUID")
                .categoryId("Zhoo0oTaej2")
                .mappingName("Mapping 1")
                .optionMappings(
                    List.of(
                        ProgramCategoryOptionMapping.builder()
                            .optionId("gvieJuud0Ro")
                            .filter("true")
                            .build()))
                .build(),
            ProgramCategoryMapping.builder()
                .id("SameGoodUID")
                .categoryId("IaD3eey1wee")
                .mappingName("Mapping 2")
                .optionMappings(
                    List.of(
                        ProgramCategoryOptionMapping.builder()
                            .optionId("prah3dao8Ra")
                            .filter("true")
                            .build()))
                .build());

    programA.setCategoryMappings(categoryMappingsWithDuplicateId);

    List<ErrorReport> errors = subject.validate(programA, null);

    assertEquals(1, errors.size());
    assertEquals(ErrorCode.E4076, errors.get(0).getErrorCode());
  }

  @Test
  void verifyCategoryMappingsWithDuplicteName() {
    Set<ProgramCategoryMapping> categoryMappingsWithDuplicateName =
        Set.of(
            ProgramCategoryMapping.builder()
                .id("eOl1Oof6Ood")
                .categoryId("zceth5Ia2oh")
                .mappingName("Same mapping name for same category")
                .optionMappings(
                    List.of(
                        ProgramCategoryOptionMapping.builder()
                            .optionId("oohX9vageij")
                            .filter("true")
                            .build()))
                .build(),
            ProgramCategoryMapping.builder()
                .id("kXee7cekae7")
                .categoryId("zceth5Ia2oh")
                .mappingName("Same mapping name for same category")
                .optionMappings(
                    List.of(
                        ProgramCategoryOptionMapping.builder()
                            .optionId("oohX9vageij")
                            .filter("false")
                            .build()))
                .build());

    programA.setCategoryMappings(categoryMappingsWithDuplicateName);

    List<ErrorReport> errors = subject.validate(programA, null);

    assertEquals(1, errors.size());
    assertEquals(ErrorCode.E4077, errors.get(0).getErrorCode());
  }
}
