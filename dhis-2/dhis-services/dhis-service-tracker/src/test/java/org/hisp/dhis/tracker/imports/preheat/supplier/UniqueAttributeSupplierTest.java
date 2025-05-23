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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
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
class UniqueAttributeSupplierTest extends TestBase {

  private static final String UNIQUE_VALUE = "unique value";

  private static final UID TE_UID = UID.generate();

  private static final UID ANOTHER_TE_UID = UID.generate();

  @InjectMocks private UniqueAttributesSupplier supplier;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  private TrackerObjects params;

  private TrackerPreheat preheat;

  private TrackedEntityAttribute uniqueAttribute;

  private TrackedEntity trackedEntity;

  private Enrollment enrollment;

  private TrackedEntityAttributeValue trackedEntityAttributeValue;

  @BeforeEach
  public void setUp() {
    params = TrackerObjects.builder().build();
    preheat = new TrackerPreheat();
    uniqueAttribute = createTrackedEntityAttribute('A', ValueType.TEXT);
    OrganisationUnit orgUnit = createOrganisationUnit('A');
    Program program = createProgram('A');
    Attribute attribute = createAttribute('A');
    trackedEntity = createTrackedEntity('A', orgUnit, createTrackedEntityType('U'));
    trackedEntity.setUid(TE_UID.getValue());
    trackedEntity.setAttributeValues(AttributeValues.of(Map.of(attribute.getUid(), UNIQUE_VALUE)));
    enrollment = createEnrollment(program, trackedEntity, orgUnit);
    enrollment.setAttributeValues(AttributeValues.of(Map.of(attribute.getUid(), UNIQUE_VALUE)));
    trackedEntityAttributeValue =
        createTrackedEntityAttributeValue('A', trackedEntity, uniqueAttribute);
  }

  @Test
  void verifySupplierWhenNoUniqueAttributeIsPresentInTheSystem() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.emptyList());

    this.supplier.preheatAdd(params, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(0));
  }

  @Test
  void verifySupplierWhenTeAndEnrollmentHaveTheSameUniqueAttribute() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(Collections.singletonList(trackedEntity()))
            .enrollments(Collections.singletonList(enrollment(TE_UID)))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(0));
  }

  @Test
  void verifySupplierWhenTwoTesHaveAttributeWithSameUniqueValue() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    TrackerObjects importParams =
        TrackerObjects.builder().trackedEntities(sameUniqueAttributeTrackedEntities()).build();

    this.supplier.preheatAdd(importParams, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(2));
  }

  @Test
  void verifySupplierWhenTeAndEnrollmentFromAnotherTeHaveAttributeWithSameUniqueValue() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(Collections.singletonList(trackedEntity()))
            .enrollments(Collections.singletonList(enrollment(ANOTHER_TE_UID)))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(2));
  }

  @Test
  void verifySupplierWhenTeinPayloadAndDBHaveTheSameUniqueAttribute() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    Map<TrackedEntityAttribute, List<String>> trackedEntityAttributeListMap =
        Map.of(uniqueAttribute, List.of(UNIQUE_VALUE));
    List<TrackedEntityAttributeValue> attributeValues = List.of(trackedEntityAttributeValue);
    when(trackedEntityAttributeValueService.getUniqueAttributeByValues(
            trackedEntityAttributeListMap))
        .thenReturn(attributeValues);
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(Collections.singletonList(trackedEntity()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(1));
  }

  @Test
  void verifySupplierWhenTeinPayloadAndAnotherTeInDBHaveTheSameUniqueAttribute() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    Map<TrackedEntityAttribute, List<String>> trackedEntityAttributeListMap =
        Map.of(uniqueAttribute, List.of(UNIQUE_VALUE));
    List<TrackedEntityAttributeValue> attributeValues = List.of(trackedEntityAttributeValue);
    when(trackedEntityAttributeValueService.getUniqueAttributeByValues(
            trackedEntityAttributeListMap))
        .thenReturn(attributeValues);
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(Collections.singletonList(anotherTrackedEntity()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertThat(preheat.getUniqueAttributeValues(), hasSize(1));
    assertEquals(TE_UID, preheat.getUniqueAttributeValues().get(0).getTe());
  }

  private List<org.hisp.dhis.tracker.imports.domain.TrackedEntity>
      sameUniqueAttributeTrackedEntities() {
    return Lists.newArrayList(
        trackedEntity(),
        org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
            .trackedEntity(ANOTHER_TE_UID)
            .attributes(Collections.singletonList(uniqueAttribute()))
            .build());
  }

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity() {

    return org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
        .trackedEntity(TE_UID)
        .attributes(Collections.singletonList(uniqueAttribute()))
        .build();
  }

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity anotherTrackedEntity() {

    return org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
        .trackedEntity(ANOTHER_TE_UID)
        .attributes(Collections.singletonList(uniqueAttribute()))
        .build();
  }

  private org.hisp.dhis.tracker.imports.domain.Enrollment enrollment(UID teUid) {
    return org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
        .trackedEntity(teUid)
        .enrollment(UID.generate())
        .attributes(Collections.singletonList(uniqueAttribute()))
        .build();
  }

  private org.hisp.dhis.tracker.imports.domain.Attribute uniqueAttribute() {
    return org.hisp.dhis.tracker.imports.domain.Attribute.builder()
        .attribute(MetadataIdentifier.ofUid(this.uniqueAttribute))
        .value(UNIQUE_VALUE)
        .build();
  }
}
