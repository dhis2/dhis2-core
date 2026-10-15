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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.UniqueAttributeValueInOrgUnit;
import org.hisp.dhis.tracker.imports.preheat.UniqueAttributeValueSystemWide;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.springframework.stereotype.Component;

/**
 * This supplier populates the preheat with the values of unique attributes sent in the payload that
 * are either already present in the DB or duplicated in the payload, as {@link
 * UniqueAttributeValueSystemWide}s and {@link UniqueAttributeValueInOrgUnit}s
 *
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
@Component
public class UniqueAttributesSupplier extends AbstractPreheatSupplier {
  @Nonnull private final TrackedEntityAttributeService trackedEntityAttributeService;

  @Nonnull private final TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Override
  public void preheatAdd(TrackerObjects trackerObjects, TrackerPreheat preheat) {
    List<TrackedEntityAttribute> uniqueTrackedEntityAttributes =
        trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes();

    Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>>
        allUniqueAttributesByTrackedEntity =
            getAllAttributesByTrackedEntity(trackerObjects, preheat, uniqueTrackedEntityAttributes);

    UniqueValues fromPayload =
        getDuplicatedUniqueValuesInPayload(
            allUniqueAttributesByTrackedEntity, uniqueTrackedEntityAttributes);

    UniqueValues fromDb =
        getAlreadyPresentInDbUniqueValues(
            preheat, allUniqueAttributesByTrackedEntity, uniqueTrackedEntityAttributes);

    preheat.setSystemWideUniqueAttributeValues(
        Stream.concat(fromPayload.systemWide().stream(), fromDb.systemWide().stream())
            .distinct()
            .toList());
    preheat.setUniqueAttributeValuesInOrgUnit(
        Stream.concat(fromPayload.inOrgUnit().stream(), fromDb.inOrgUnit().stream())
            .distinct()
            .toList());
  }

  private record UniqueValues(
      List<UniqueAttributeValueSystemWide> systemWide,
      List<UniqueAttributeValueInOrgUnit> inOrgUnit) {}

  private Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>>
      getAllAttributesByTrackedEntity(
          TrackerObjects trackerObjects,
          TrackerPreheat preheat,
          List<TrackedEntityAttribute> uniqueTrackedEntityAttributes) {
    Map<UID, org.hisp.dhis.tracker.imports.domain.TrackedEntity> teByUid =
        trackerObjects.getTrackedEntities().stream()
            .collect(
                toMap(
                    org.hisp.dhis.tracker.imports.domain.TrackedEntity::getUID,
                    Function.identity(),
                    (a, b) -> a));

    Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>> teUniqueAttributes =
        trackerObjects.getTrackedEntities().stream()
            .collect(
                toMap(
                    Function.identity(),
                    te ->
                        filterUniqueAttributes(te.getAttributes(), uniqueTrackedEntityAttributes)));

    Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>>
        enrollmentUniqueAttributes =
            trackerObjects.getEnrollments().stream()
                .collect(
                    groupingBy(e -> getEntityForEnrollment(teByUid, preheat, e.getTrackedEntity())))
                .entrySet()
                .stream()
                .collect(
                    toMap(
                        Map.Entry::getKey,
                        e ->
                            e.getValue().stream()
                                .flatMap(
                                    en ->
                                        filterUniqueAttributes(
                                            en.getAttributes(), uniqueTrackedEntityAttributes)
                                            .stream())
                                .collect(toSet())));

    return mergeAttributes(teUniqueAttributes, enrollmentUniqueAttributes);
  }

  /**
   * Finds the tracked entity referenced by an enrollment. Checks the payload first, then falls back
   * to building a minimal TE from preheat (DB). If the TE is not found in either, a stub with just
   * the UID is returned; validation will report the error later.
   */
  private org.hisp.dhis.tracker.imports.domain.TrackedEntity getEntityForEnrollment(
      Map<UID, org.hisp.dhis.tracker.imports.domain.TrackedEntity> teByUid,
      TrackerPreheat preheat,
      UID teUid) {
    org.hisp.dhis.tracker.imports.domain.TrackedEntity payloadTe = teByUid.get(teUid);
    if (payloadTe != null) {
      return payloadTe;
    }

    org.hisp.dhis.tracker.imports.domain.TrackedEntity te =
        new org.hisp.dhis.tracker.imports.domain.TrackedEntity();
    te.setTrackedEntity(teUid);

    TrackedEntity trackedEntity = preheat.getTrackedEntity(teUid);
    if (trackedEntity != null) {
      te.setOrgUnit(
          preheat.getIdSchemes().toMetadataIdentifier(trackedEntity.getOrganisationUnit()));
    }
    return te;
  }

  private Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>> mergeAttributes(
      Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>> teAttributes,
      Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>>
          enrollmentAttributes) {
    return Stream.concat(teAttributes.entrySet().stream(), enrollmentAttributes.entrySet().stream())
        .collect(
            toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (v1, v2) -> {
                  Set<Attribute> attributes = Sets.newHashSet(v1);
                  attributes.addAll(v2);
                  return attributes;
                }));
  }

  /**
   * Finds the unique values sent for more than one tracked entity in the payload. Values are
   * compared ignoring case, like the validation does, so the case folded value is the one added to
   * the preheat. A value of an attribute unique within an org unit is skipped when the org unit of
   * its tracked entity is unknown, as it can't conflict with any other value.
   */
  private UniqueValues getDuplicatedUniqueValuesInPayload(
      Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>> allAttributes,
      List<TrackedEntityAttribute> uniqueTrackedEntityAttributes) {
    Map<AttributeValueKey, Set<org.hisp.dhis.tracker.imports.domain.TrackedEntity>>
        trackedEntitiesByAttributeValue = new HashMap<>();

    for (Map.Entry<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>> entry :
        allAttributes.entrySet()) {
      for (Attribute attribute : entry.getValue()) {
        trackedEntitiesByAttributeValue
            .computeIfAbsent(
                new AttributeValueKey(
                    attribute.getAttribute(),
                    TrackerPreheat.caseInsensitiveKey(attribute.getValue())),
                k -> new HashSet<>())
            .add(entry.getKey());
      }
    }

    List<UniqueAttributeValueSystemWide> systemWide = new ArrayList<>();
    List<UniqueAttributeValueInOrgUnit> inOrgUnit = new ArrayList<>();
    for (Map.Entry<AttributeValueKey, Set<org.hisp.dhis.tracker.imports.domain.TrackedEntity>>
        entry : trackedEntitiesByAttributeValue.entrySet()) {
      if (entry.getValue().size() < 2) {
        continue;
      }

      AttributeValueKey key = entry.getKey();
      boolean orgUnitScoped =
          extractAttribute(key.attribute(), uniqueTrackedEntityAttributes)
              .getOrgUnitScopeNullSafe();
      for (org.hisp.dhis.tracker.imports.domain.TrackedEntity te : entry.getValue()) {
        if (!orgUnitScoped) {
          systemWide.add(
              new UniqueAttributeValueSystemWide(te.getUID(), key.attribute(), key.value()));
        } else if (te.getOrgUnit() != null) {
          inOrgUnit.add(
              new UniqueAttributeValueInOrgUnit(
                  te.getUID(), key.attribute(), key.value(), te.getOrgUnit()));
        }
      }
    }
    return new UniqueValues(systemWide, inOrgUnit);
  }

  private record AttributeValueKey(MetadataIdentifier attribute, String value) {}

  /**
   * Finds the values in the DB that collide with the unique values in the payload. Values of an
   * attribute unique within an org unit are only looked up in the org unit of the tracked entity
   * they are sent for, as the validation only compares them within that org unit.
   */
  private UniqueValues getAlreadyPresentInDbUniqueValues(
      TrackerPreheat preheat,
      Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>>
          allAttributesByTrackedEntity,
      List<TrackedEntityAttribute> uniqueTrackedEntityAttributes) {
    Map<TrackedEntityAttribute, Set<String>> valuesByAttribute = new HashMap<>();
    Map<TrackedEntityAttribute, Map<Long, Set<String>>> valuesByOrgUnitIdByAttribute =
        new HashMap<>();
    Map<Long, OrganisationUnit> orgUnitsById = new HashMap<>();

    for (Map.Entry<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>> entry :
        allAttributesByTrackedEntity.entrySet()) {
      for (Attribute attribute : entry.getValue()) {
        TrackedEntityAttribute tea =
            extractAttribute(attribute.getAttribute(), uniqueTrackedEntityAttributes);
        if (!tea.getOrgUnitScopeNullSafe()) {
          valuesByAttribute.computeIfAbsent(tea, k -> new HashSet<>()).add(attribute.getValue());
          continue;
        }

        // an org unit that cannot be resolved can't match any stored value in the validation
        OrganisationUnit orgUnit = getOrgUnit(preheat, entry.getKey());
        if (orgUnit != null) {
          orgUnitsById.put(orgUnit.getId(), orgUnit);
          valuesByOrgUnitIdByAttribute
              .computeIfAbsent(tea, k -> new HashMap<>())
              .computeIfAbsent(orgUnit.getId(), k -> new HashSet<>())
              .add(attribute.getValue());
        }
      }
    }

    TrackerIdSchemeParams idSchemes = preheat.getIdSchemes();
    List<UniqueAttributeValueSystemWide> systemWide = new ArrayList<>();
    valuesByAttribute.forEach(
        (attribute, values) -> {
          MetadataIdentifier attributeIdentifier = idSchemes.toMetadataIdentifier(attribute);
          trackedEntityAttributeValueService
              .getUniqueAttributeValues(attribute, values)
              .forEach(
                  match ->
                      systemWide.add(
                          new UniqueAttributeValueSystemWide(
                              match.trackedEntity(), attributeIdentifier, match.value())));
        });
    List<UniqueAttributeValueInOrgUnit> inOrgUnit = new ArrayList<>();
    valuesByOrgUnitIdByAttribute.forEach(
        (attribute, valuesByOrgUnitId) -> {
          MetadataIdentifier attributeIdentifier = idSchemes.toMetadataIdentifier(attribute);
          trackedEntityAttributeValueService
              .getUniqueAttributeValues(attribute, valuesByOrgUnitId)
              .forEach(
                  match ->
                      inOrgUnit.add(
                          new UniqueAttributeValueInOrgUnit(
                              match.trackedEntity(),
                              attributeIdentifier,
                              match.value(),
                              idSchemes.toMetadataIdentifier(
                                  orgUnitsById.get(match.orgUnitId())))));
        });
    return new UniqueValues(systemWide, inOrgUnit);
  }

  /**
   * The org unit of the tracked entity, falling back to the one of the tracked entity in the DB.
   * The latter is only preheated when the payload references it, e.g. not when only an enrollment
   * of the tracked entity is sent.
   */
  private static OrganisationUnit getOrgUnit(
      TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.TrackedEntity te) {
    OrganisationUnit orgUnit = preheat.getOrganisationUnit(te.getOrgUnit());
    if (orgUnit != null) {
      return orgUnit;
    }
    TrackedEntity trackedEntity = preheat.getTrackedEntity(te.getUID());
    return trackedEntity == null ? null : trackedEntity.getOrganisationUnit();
  }

  private TrackedEntityAttribute extractAttribute(
      MetadataIdentifier attribute, List<TrackedEntityAttribute> uniqueTrackedEntityAttributes) {
    return uniqueTrackedEntityAttributes.stream()
        .filter(attribute::isEqualTo)
        .findAny()
        .orElse(null);
  }

  private Set<Attribute> filterUniqueAttributes(
      List<Attribute> attributes, List<TrackedEntityAttribute> uniqueTrackedEntityAttributes) {
    return attributes.stream()
        .filter(tea -> tea.getValue() != null)
        .filter(
            tea ->
                uniqueTrackedEntityAttributes.stream()
                    .anyMatch(uniqueAttr -> tea.getAttribute().isEqualTo(uniqueAttr)))
        .collect(toSet());
  }
}
