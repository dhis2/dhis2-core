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
import org.hisp.dhis.tracker.imports.preheat.UniqueAttributeValue;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.trackedentityattributevalue.UniqueAttributeValueMatch;
import org.springframework.stereotype.Component;

/**
 * This supplier will populate a list of {@link UniqueAttributeValue}s that contains all the
 * attribute values that are unique and which value is either already present in the DB or
 * duplicated in the payload
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

    List<UniqueAttributeValue> uniqueAttributeValuesFromPayload =
        getDuplicatedUniqueValuesInPayload(allUniqueAttributesByTrackedEntity);

    List<UniqueAttributeValue> uniqueAttributeValuesFromDB =
        getAlreadyPresentInDbUniqueValues(
            preheat, allUniqueAttributesByTrackedEntity, uniqueTrackedEntityAttributes);

    List<UniqueAttributeValue> uniqueAttributeValues =
        Stream.concat(
                uniqueAttributeValuesFromPayload.stream(), uniqueAttributeValuesFromDB.stream())
            .distinct()
            .toList();

    preheat.setUniqueAttributeValues(uniqueAttributeValues);
  }

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
   * the preheat.
   */
  private List<UniqueAttributeValue> getDuplicatedUniqueValuesInPayload(
      Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>> allAttributes) {
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

    return trackedEntitiesByAttributeValue.entrySet().stream()
        .filter(e -> e.getValue().size() > 1)
        .flatMap(
            e ->
                e.getValue().stream()
                    .map(
                        te ->
                            new UniqueAttributeValue(
                                te.getUID(),
                                e.getKey().attribute(),
                                e.getKey().value(),
                                te.getOrgUnit())))
        .toList();
  }

  private record AttributeValueKey(MetadataIdentifier attribute, String value) {}

  /**
   * Finds the values in the DB that collide with the unique values in the payload. Values of an
   * attribute unique within an org unit are only looked up in the org unit of the tracked entity
   * they are sent for, as the validation only compares them within that org unit. Values of an
   * attribute unique in the whole system get no org unit, as the validation does not use it.
   */
  private List<UniqueAttributeValue> getAlreadyPresentInDbUniqueValues(
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
        if (tea != null && !tea.getOrgUnitScopeNullSafe()) {
          valuesByAttribute.computeIfAbsent(tea, k -> new HashSet<>()).add(attribute.getValue());
          continue;
        }

        // an org unit that cannot be resolved can't match any stored value in the validation
        OrganisationUnit orgUnit = preheat.getOrganisationUnit(entry.getKey().getOrgUnit());
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
    List<UniqueAttributeValue> uniqueAttributeValues = new ArrayList<>();
    valuesByAttribute.forEach(
        (attribute, values) ->
            uniqueAttributeValues.addAll(
                toUniqueAttributeValues(
                    idSchemes,
                    attribute,
                    orgUnitsById,
                    trackedEntityAttributeValueService.getUniqueAttributeValues(
                        attribute, values))));
    valuesByOrgUnitIdByAttribute.forEach(
        (attribute, valuesByOrgUnitId) ->
            uniqueAttributeValues.addAll(
                toUniqueAttributeValues(
                    idSchemes,
                    attribute,
                    orgUnitsById,
                    trackedEntityAttributeValueService.getUniqueAttributeValues(
                        attribute, valuesByOrgUnitId))));
    return uniqueAttributeValues;
  }

  private static List<UniqueAttributeValue> toUniqueAttributeValues(
      TrackerIdSchemeParams idSchemes,
      TrackedEntityAttribute attribute,
      Map<Long, OrganisationUnit> orgUnitsById,
      List<UniqueAttributeValueMatch> matches) {
    MetadataIdentifier attributeIdentifier = idSchemes.toMetadataIdentifier(attribute);
    // only org units of org unit scoped lookups are known. The org unit of a value unique in the
    // whole system is not needed by the validation, and resolving it would need a query
    boolean orgUnitScoped = attribute.getOrgUnitScopeNullSafe();
    return matches.stream()
        .map(
            match ->
                new UniqueAttributeValue(
                    match.trackedEntity(),
                    attributeIdentifier,
                    match.value(),
                    orgUnitScoped
                        ? idSchemes.toMetadataIdentifier(orgUnitsById.get(match.orgUnitId()))
                        : null))
        .toList();
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
