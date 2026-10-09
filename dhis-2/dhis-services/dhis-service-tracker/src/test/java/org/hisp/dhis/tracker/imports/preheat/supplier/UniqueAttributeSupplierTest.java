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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.trackedentityattributevalue.UniqueAttributeValueMatch;
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

  private OrganisationUnit orgUnit;

  @BeforeEach
  void setUp() {
    params = TrackerObjects.builder().build();
    preheat = new TrackerPreheat();
    uniqueAttribute = createTrackedEntityAttribute('A', ValueType.TEXT);
    orgUnit = createOrganisationUnit('A');
    Program program = createProgram('A');
    Attribute attribute = createAttribute('A');
    trackedEntity = createTrackedEntity('A', orgUnit, createTrackedEntityType('U'));
    trackedEntity.setUid(TE_UID.getValue());
    trackedEntity.setAttributeValues(AttributeValues.of(Map.of(attribute.getUid(), UNIQUE_VALUE)));
    enrollment = createEnrollment(program, trackedEntity, orgUnit);
    enrollment.setAttributeValues(AttributeValues.of(Map.of(attribute.getUid(), UNIQUE_VALUE)));
  }

  @Test
  void verifySupplierWhenNoUniqueAttributeIsPresentInTheSystem() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.emptyList());

    this.supplier.preheatAdd(params, preheat);

    assertFalse(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, UID.generate()));
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

    assertFalse(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, UID.generate()));
  }

  @Test
  void verifySupplierWhenTwoTesHaveAttributeWithSameUniqueValue() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(sameUniqueAttributeTrackedEntities(UNIQUE_VALUE))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, TE_UID));
    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, ANOTHER_TE_UID));
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

    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, TE_UID));
    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, ANOTHER_TE_UID));
  }

  @Test
  void shouldNotFlagAsDuplicateWhenUniqueAttributeValueIsNullForBothTrackedEntities() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    TrackerObjects importParams =
        TrackerObjects.builder().trackedEntities(sameUniqueAttributeTrackedEntities(null)).build();

    this.supplier.preheatAdd(importParams, preheat);

    assertFalse(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, UID.generate()));
  }

  @Test
  void shouldFlagAsDuplicateWhenUniqueAttributeValuesHaveDifferentCasing() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(
                List.of(
                    trackedEntityWithAttributeValue(TE_UID, UNIQUE_VALUE.toUpperCase()),
                    trackedEntityWithAttributeValue(ANOTHER_TE_UID, UNIQUE_VALUE.toLowerCase())))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, TE_UID));
    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, ANOTHER_TE_UID));
  }

  @Test
  void shouldFlagAsDuplicateWhenPayloadHasManyTrackedEntitiesWithDistinctUniqueValues() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    List<org.hisp.dhis.tracker.imports.domain.TrackedEntity> trackedEntities =
        new ArrayList<>(sameUniqueAttributeTrackedEntities(UNIQUE_VALUE));
    for (int i = 0; i < 20; i++) {
      trackedEntities.add(trackedEntityWithAttributeValue(UID.generate(), "value " + i));
    }
    TrackerObjects importParams = TrackerObjects.builder().trackedEntities(trackedEntities).build();

    this.supplier.preheatAdd(importParams, preheat);

    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, TE_UID));
    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, ANOTHER_TE_UID));
    assertFalse(preheat.hasSystemWideDuplicate(uniqueAttribute, "value 0", UID.generate()));
  }

  @Test
  void shouldNotFlagAsDuplicateWhenTeAndItsEnrollmentHaveUniqueValueWithDifferentCasing() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(
                List.of(trackedEntityWithAttributeValue(TE_UID, UNIQUE_VALUE.toUpperCase())))
            .enrollments(Collections.singletonList(enrollment(TE_UID)))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertFalse(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, UID.generate()));
  }

  @Test
  void verifySupplierWhenTeinPayloadAndDBHaveTheSameUniqueAttribute() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    when(trackedEntityAttributeValueService.getUniqueAttributeValues(
            uniqueAttribute, Set.of(UNIQUE_VALUE)))
        .thenReturn(List.of(new UniqueAttributeValueMatch(TE_UID, UNIQUE_VALUE, null)));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(Collections.singletonList(trackedEntity()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    // the value in the DB is the one of the tracked entity itself
    assertFalse(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, TE_UID));
    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, UID.generate()));
  }

  @Test
  void verifySupplierWhenTeinPayloadAndAnotherTeInDBHaveTheSameUniqueAttribute() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    when(trackedEntityAttributeValueService.getUniqueAttributeValues(
            uniqueAttribute, Set.of(UNIQUE_VALUE)))
        .thenReturn(List.of(new UniqueAttributeValueMatch(TE_UID, UNIQUE_VALUE, null)));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(Collections.singletonList(anotherTrackedEntity()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, ANOTHER_TE_UID));
    assertFalse(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, TE_UID));
  }

  @Test
  void shouldMapOrgUnitScopedValuesFoundInDbUsingTheImportIdSchemes() {
    TrackedEntityAttribute scopedAttribute = scopedUniqueAttribute();
    orgUnit.setId(1);
    orgUnit.setCode("OU_CODE");
    preheat.setIdSchemes(
        TrackerIdSchemeParams.builder().orgUnitIdScheme(TrackerIdSchemeParam.CODE).build());
    preheat.put(TrackerIdSchemeParam.CODE, orgUnit);
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(List.of(scopedAttribute));
    when(trackedEntityAttributeValueService.getUniqueAttributeValues(
            scopedAttribute, Map.of(1L, Set.of(UNIQUE_VALUE))))
        .thenReturn(List.of(new UniqueAttributeValueMatch(TE_UID, "Unique Value", 1L)));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(
                List.of(
                    org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
                        .trackedEntity(ANOTHER_TE_UID)
                        .orgUnit(MetadataIdentifier.ofCode("OU_CODE"))
                        .attributes(List.of(value(scopedAttribute, UNIQUE_VALUE)))
                        .build()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertTrue(
        preheat.hasDuplicateInOrgUnit(scopedAttribute, UNIQUE_VALUE, orgUnit, ANOTHER_TE_UID));
  }

  @Test
  void shouldLookUpOrgUnitScopedValuesOnlyInTheOrgUnitOfTheTrackedEntitiesSendingThem() {
    TrackedEntityAttribute scopedAttribute = scopedUniqueAttribute();
    OrganisationUnit orgUnit1 = orgUnitInPreheat('1', 1);
    OrganisationUnit orgUnit2 = orgUnitInPreheat('2', 2);
    OrganisationUnit orgUnit3 = orgUnitInPreheat('3', 3);
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(List.of(uniqueAttribute, scopedAttribute));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(
                List.of(
                    trackedEntity(
                        orgUnit1, value(uniqueAttribute, "g1"), value(scopedAttribute, "s1")),
                    trackedEntity(
                        orgUnit1, value(uniqueAttribute, "g2"), value(scopedAttribute, "s2")),
                    trackedEntity(
                        orgUnit1, value(uniqueAttribute, "g3"), value(scopedAttribute, "s3")),
                    // same scoped value as in orgUnit1, which is allowed in another org unit
                    trackedEntity(
                        orgUnit2, value(uniqueAttribute, "g4"), value(scopedAttribute, "s1")),
                    // no scoped value, so orgUnit3 must not be part of the scoped lookup
                    trackedEntity(orgUnit3, value(uniqueAttribute, "g5"))))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    verify(trackedEntityAttributeValueService)
        .getUniqueAttributeValues(uniqueAttribute, Set.of("g1", "g2", "g3", "g4", "g5"));
    verify(trackedEntityAttributeValueService)
        .getUniqueAttributeValues(
            scopedAttribute,
            Map.of(orgUnit1.getId(), Set.of("s1", "s2", "s3"), orgUnit2.getId(), Set.of("s1")));
  }

  @Test
  void shouldLookUpOrgUnitScopedValueOfEnrollmentInTheOrgUnitOfItsTrackedEntityInDb() {
    TrackedEntityAttribute scopedAttribute = scopedUniqueAttribute();
    OrganisationUnit dbOrgUnit = orgUnitInPreheat('D', 4);
    trackedEntity.setOrganisationUnit(dbOrgUnit);
    preheat.putTrackedEntities(List.of(trackedEntity));
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(List.of(scopedAttribute));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .enrollments(
                List.of(
                    org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
                        .enrollment(UID.generate())
                        .trackedEntity(TE_UID)
                        .attributes(List.of(value(scopedAttribute, "s1")))
                        .build()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    verify(trackedEntityAttributeValueService)
        .getUniqueAttributeValues(scopedAttribute, Map.of(dbOrgUnit.getId(), Set.of("s1")));
  }

  @Test
  void shouldLookUpOrgUnitScopedValueOfEnrollmentInTheOrgUnitOfItsTrackedEntityInDbNotInPreheat() {
    TrackedEntityAttribute scopedAttribute = scopedUniqueAttribute();
    // only org units referenced by the payload are preheated, so not the one of a tracked entity
    // that only an enrollment refers to
    OrganisationUnit dbOrgUnit = createOrganisationUnit('D');
    dbOrgUnit.setId(4);
    trackedEntity.setOrganisationUnit(dbOrgUnit);
    preheat.putTrackedEntities(List.of(trackedEntity));
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(List.of(scopedAttribute));
    when(trackedEntityAttributeValueService.getUniqueAttributeValues(
            scopedAttribute, Map.of(dbOrgUnit.getId(), Set.of("s1"))))
        .thenReturn(List.of(new UniqueAttributeValueMatch(ANOTHER_TE_UID, "s1", 4L)));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .enrollments(
                List.of(
                    org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
                        .enrollment(UID.generate())
                        .trackedEntity(TE_UID)
                        .attributes(List.of(value(scopedAttribute, "s1")))
                        .build()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertTrue(preheat.hasDuplicateInOrgUnit(scopedAttribute, "s1", dbOrgUnit, TE_UID));
  }

  @Test
  void shouldNotLookUpOrgUnitScopedValueWhenOrgUnitCannotBeResolved() {
    TrackedEntityAttribute scopedAttribute = scopedUniqueAttribute();
    OrganisationUnit notInPreheat = createOrganisationUnit('N');
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(List.of(scopedAttribute));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(List.of(trackedEntity(notInPreheat, value(scopedAttribute, "s1"))))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    verifyNoInteractions(trackedEntityAttributeValueService);
    assertFalse(preheat.hasDuplicateInOrgUnit(scopedAttribute, "s1", notInPreheat, UID.generate()));
  }

  @Test
  void shouldAddOrgUnitScopedValuesDuplicatedInPayloadInTheOrgUnitOfTheirTrackedEntity() {
    TrackedEntityAttribute scopedAttribute = scopedUniqueAttribute();
    OrganisationUnit orgUnit1 = orgUnitInPreheat('1', 1);
    OrganisationUnit orgUnit2 = orgUnitInPreheat('2', 2);
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(List.of(scopedAttribute));
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity1 =
        trackedEntity(orgUnit1, value(scopedAttribute, "s1"));
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity2 =
        trackedEntity(orgUnit2, value(scopedAttribute, "S1"));
    TrackerObjects importParams =
        TrackerObjects.builder().trackedEntities(List.of(trackedEntity1, trackedEntity2)).build();

    this.supplier.preheatAdd(importParams, preheat);

    // each value is only a duplicate in the org unit of its tracked entity
    assertFalse(
        preheat.hasDuplicateInOrgUnit(scopedAttribute, "s1", orgUnit1, trackedEntity1.getUid()));
    assertTrue(preheat.hasDuplicateInOrgUnit(scopedAttribute, "s1", orgUnit1, UID.generate()));
    assertFalse(
        preheat.hasDuplicateInOrgUnit(scopedAttribute, "s1", orgUnit2, trackedEntity2.getUid()));
    assertTrue(preheat.hasDuplicateInOrgUnit(scopedAttribute, "s1", orgUnit2, UID.generate()));
    assertFalse(preheat.hasSystemWideDuplicate(scopedAttribute, "s1", UID.generate()));
  }

  @Test
  void shouldSkipOrgUnitScopedValueWhenEnrollmentTrackedEntityDoesNotExist() {
    TrackedEntityAttribute scopedAttribute = scopedUniqueAttribute();
    OrganisationUnit orgUnit1 = orgUnitInPreheat('1', 1);
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(List.of(scopedAttribute));
    org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity =
        trackedEntity(orgUnit1, value(scopedAttribute, "s1"));
    UID unknownTrackedEntity = UID.generate();
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(List.of(trackedEntity))
            .enrollments(
                List.of(
                    org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
                        .enrollment(UID.generate())
                        .trackedEntity(unknownTrackedEntity)
                        .attributes(List.of(value(scopedAttribute, "s1")))
                        .build()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    // the tracked entity of the enrollment is in neither the payload nor the DB, so its org unit
    // is unknown and its value can't conflict with any other
    assertFalse(
        preheat.hasDuplicateInOrgUnit(scopedAttribute, "s1", orgUnit1, trackedEntity.getUid()));
  }

  @Test
  void shouldAddValuesFoundInDbAsSystemWideWhenAttributeIsUniqueInTheSystem() {
    when(trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes())
        .thenReturn(Collections.singletonList(uniqueAttribute));
    when(trackedEntityAttributeValueService.getUniqueAttributeValues(
            uniqueAttribute, Set.of(UNIQUE_VALUE)))
        .thenReturn(List.of(new UniqueAttributeValueMatch(TE_UID, UNIQUE_VALUE, 42L)));
    TrackerObjects importParams =
        TrackerObjects.builder()
            .trackedEntities(Collections.singletonList(anotherTrackedEntity()))
            .build();

    this.supplier.preheatAdd(importParams, preheat);

    assertTrue(preheat.hasSystemWideDuplicate(uniqueAttribute, UNIQUE_VALUE, ANOTHER_TE_UID));
  }

  private TrackedEntityAttribute scopedUniqueAttribute() {
    TrackedEntityAttribute attribute = createTrackedEntityAttribute('S', ValueType.TEXT);
    attribute.setUnique(true);
    attribute.setOrgunitScope(true);
    return attribute;
  }

  private OrganisationUnit orgUnitInPreheat(char uniqueChar, long id) {
    OrganisationUnit ou = createOrganisationUnit(uniqueChar);
    ou.setId(id);
    preheat.put(TrackerIdSchemeParam.UID, ou);
    return ou;
  }

  private static org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity(
      OrganisationUnit orgUnit, org.hisp.dhis.tracker.imports.domain.Attribute... attributes) {
    return org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
        .trackedEntity(UID.generate())
        .orgUnit(MetadataIdentifier.ofUid(orgUnit))
        .attributes(List.of(attributes))
        .build();
  }

  private static org.hisp.dhis.tracker.imports.domain.Attribute value(
      TrackedEntityAttribute attribute, String value) {
    return org.hisp.dhis.tracker.imports.domain.Attribute.builder()
        .attribute(MetadataIdentifier.ofUid(attribute))
        .value(value)
        .build();
  }

  private List<org.hisp.dhis.tracker.imports.domain.TrackedEntity>
      sameUniqueAttributeTrackedEntities(String value) {
    return Lists.newArrayList(
        trackedEntityWithAttributeValue(TE_UID, value),
        trackedEntityWithAttributeValue(ANOTHER_TE_UID, value));
  }

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntityWithAttributeValue(
      UID teUid, String value) {
    return org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
        .trackedEntity(teUid)
        .attributes(Collections.singletonList(attributeWithValue(value)))
        .build();
  }

  private org.hisp.dhis.tracker.imports.domain.Attribute attributeWithValue(String value) {
    return org.hisp.dhis.tracker.imports.domain.Attribute.builder()
        .attribute(MetadataIdentifier.ofUid(this.uniqueAttribute))
        .value(value)
        .build();
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
    return attributeWithValue(UNIQUE_VALUE);
  }
}
