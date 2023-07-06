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
package org.hisp.dhis.tracker.preheat.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class UniqueAttributeSupplierTest extends DhisConvenienceTest {

  private static final String UNIQUE_VALUE = "unique value";

  private static final String TEI_UID = "TEI UID";

  private static final String ANOTHER_TEI_UID = "ANOTHER_TEI_UID";

  @InjectMocks private UniqueAttributesSupplier supplier;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  private TrackerImportParams params;

  private TrackerPreheat preheat;

  private TrackedEntityAttribute uniqueAttribute;

  private TrackedEntityInstance tei;

  private ProgramInstance programInstance;

  private TrackedEntityAttributeValue trackedEntityAttributeValue;

  @BeforeEach
  public void setUp() {
    params = TrackerImportParams.builder().build();
    preheat = new TrackerPreheat();
    uniqueAttribute = createTrackedEntityAttribute('A', ValueType.TEXT);
    OrganisationUnit orgUnit = createOrganisationUnit('A');
    Program program = createProgram('A');
    Attribute attribute = createAttribute('A');
    AttributeValue attributeValue = createAttributeValue(attribute, UNIQUE_VALUE);
    tei = createTrackedEntityInstance('A', orgUnit);
    tei.setUid(TEI_UID);
    tei.setAttributeValues(Collections.singleton(attributeValue));
    programInstance = createProgramInstance(program, tei, orgUnit);
    programInstance.setAttributeValues(Collections.singleton(attributeValue));
    trackedEntityAttributeValue = createTrackedEntityAttributeValue('A', tei, uniqueAttribute);
  }

  @Test
  void verifySupplierWhenNoUniqueAttributeIsPresentInTheSystem() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.emptyList());

    this.supplier.preheatAdd(params, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(0));
  }

  @Test
  void verifySupplierWhenTEIAndEnrollmentHaveTheSameUniqueAttribute() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    TrackerImportParams importParams =
        TrackerImportParams.builder()
            .trackedEntities(Collections.singletonList(trackedEntity()))
            .enrollments(Collections.singletonList(enrollment(TEI_UID)))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(0));
  }

  @Test
  void verifySupplierWhenTwoTEIsHaveAttributeWithSameUniqueValue() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    TrackerImportParams importParams =
        TrackerImportParams.builder().trackedEntities(sameUniqueAttributeTrackedEntities()).build();

    this.supplier.preheatAdd(importParams, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(2));
  }

  @Test
  void verifySupplierWhenTEIAndEnrollmentFromAnotherTEIHaveAttributeWithSameUniqueValue() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    TrackerImportParams importParams =
        TrackerImportParams.builder()
            .trackedEntities(Collections.singletonList(trackedEntity()))
            .enrollments(Collections.singletonList(enrollment(ANOTHER_TEI_UID)))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(2));
  }

  @Test
  void verifySupplierWhenTEIinPayloadAndDBHaveTheSameUniqueAttribute() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    Map<TrackedEntityAttribute, List<String>> trackedEntityAttributeListMap =
        Map.of(uniqueAttribute, List.of(UNIQUE_VALUE));
    List<TrackedEntityAttributeValue> attributeValues = List.of(trackedEntityAttributeValue);
    when(trackedEntityAttributeValueService.getUniqueAttributeByValues(
            trackedEntityAttributeListMap))
        .thenReturn(attributeValues);
    TrackerImportParams importParams =
        TrackerImportParams.builder()
            .trackedEntities(Collections.singletonList(trackedEntity()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(1));
  }

  @Test
  void verifySupplierWhenTEIinPayloadAndAnotherTEIInDBHaveTheSameUniqueAttribute() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    Map<TrackedEntityAttribute, List<String>> trackedEntityAttributeListMap =
        Map.of(uniqueAttribute, List.of(UNIQUE_VALUE));
    List<TrackedEntityAttributeValue> attributeValues = List.of(trackedEntityAttributeValue);
    when(trackedEntityAttributeValueService.getUniqueAttributeByValues(
            trackedEntityAttributeListMap))
        .thenReturn(attributeValues);
    TrackerImportParams importParams =
        TrackerImportParams.builder()
            .trackedEntities(Collections.singletonList(anotherTrackedEntity()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(1));
    assertThat(preheat.getUniqueAttributeValues().get(0).getTeiUid(), is(TEI_UID));
  }

  private List<TrackedEntity> sameUniqueAttributeTrackedEntities() {
    return Lists.newArrayList(
        trackedEntity(),
        TrackedEntity.builder()
            .trackedEntity(ANOTHER_TEI_UID)
            .attributes(Collections.singletonList(uniqueAttribute()))
            .build());
  }

  private TrackedEntity trackedEntity() {

    return TrackedEntity.builder()
        .trackedEntity(TEI_UID)
        .attributes(Collections.singletonList(uniqueAttribute()))
        .build();
  }

  private TrackedEntity anotherTrackedEntity() {

    return TrackedEntity.builder()
        .trackedEntity(ANOTHER_TEI_UID)
        .attributes(Collections.singletonList(uniqueAttribute()))
        .build();
  }

  private Enrollment enrollment(String teiUid) {
    return Enrollment.builder()
        .trackedEntity(teiUid)
        .enrollment("ENROLLMENT")
        .attributes(Collections.singletonList(uniqueAttribute()))
        .build();
  }

  private org.hisp.dhis.tracker.domain.Attribute uniqueAttribute() {
    return org.hisp.dhis.tracker.domain.Attribute.builder()
        .attribute(MetadataIdentifier.ofUid(this.uniqueAttribute))
        .value(UNIQUE_VALUE)
        .build();
  }
}
