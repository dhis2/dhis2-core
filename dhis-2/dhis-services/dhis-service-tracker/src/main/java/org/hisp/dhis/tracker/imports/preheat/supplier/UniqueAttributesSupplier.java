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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.UniqueAttributeValue;
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
  public void preheatAdd(TrackerImportParams params, TrackerPreheat preheat) {
    List<TrackedEntityAttribute> uniqueTrackedEntityAttributes =
        trackedEntityAttributeService.getAllUniqueTrackedEntityAttributes();

    Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>>
        allUniqueAttributesByTrackedEntity =
            getAllAttributesByTrackedEntity(params, preheat, uniqueTrackedEntityAttributes);

    List<UniqueAttributeValue> uniqueAttributeValuesFromPayload =
        getDuplicatedUniqueValuesInPayload(allUniqueAttributesByTrackedEntity);

    List<UniqueAttributeValue> uniqueAttributeValuesFromDB =
        getAlreadyPresentInDbUniqueValues(
            params.getIdSchemes(),
            allUniqueAttributesByTrackedEntity,
            uniqueTrackedEntityAttributes);

    List<UniqueAttributeValue> uniqueAttributeValues =
        Stream.concat(
                uniqueAttributeValuesFromPayload.stream(), uniqueAttributeValuesFromDB.stream())
            .distinct()
            .collect(toList());

    preheat.setUniqueAttributeValues(uniqueAttributeValues);
  }

  private Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>>
      getAllAttributesByTrackedEntity(
          TrackerImportParams params,
          TrackerPreheat preheat,
          List<TrackedEntityAttribute> uniqueTrackedEntityAttributes) {
    Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>> teUniqueAttributes =
        params.getTrackedEntities().stream()
            .collect(
                toMap(
                    Function.identity(),
                    te ->
                        filterUniqueAttributes(te.getAttributes(), uniqueTrackedEntityAttributes)));

    Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>>
        enrollmentUniqueAttributes =
            params.getEnrollments().stream()
                .collect(
                    groupingBy(e -> getEntityForEnrollment(params, preheat, e.getTrackedEntity())))
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

  private org.hisp.dhis.tracker.imports.domain.TrackedEntity getEntityForEnrollment(
      TrackerImportParams params, TrackerPreheat preheat, String teUid) {
    TrackedEntity trackedEntity = preheat.getTrackedEntity(teUid);

    // Get te from Preheat
    Optional<org.hisp.dhis.tracker.imports.domain.TrackedEntity> optionalTe =
        params.getTrackedEntities().stream()
            .filter(te -> Objects.equals(te.getTrackedEntity(), teUid))
            .findAny();
    if (optionalTe.isPresent()) {
      return optionalTe.get();
    } else if (trackedEntity != null) // Otherwise build it from Payload
    {
      org.hisp.dhis.tracker.imports.domain.TrackedEntity te =
          new org.hisp.dhis.tracker.imports.domain.TrackedEntity();
      te.setTrackedEntity(teUid);
      te.setOrgUnit(
          params.getIdSchemes().toMetadataIdentifier(trackedEntity.getOrganisationUnit()));
      return te;
    } else // TE is not present. but we do not fail here.
    // A validation error will be thrown in validation phase
    {
      org.hisp.dhis.tracker.imports.domain.TrackedEntity te =
          new org.hisp.dhis.tracker.imports.domain.TrackedEntity();
      te.setTrackedEntity(teUid);
      return te;
    }
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

  private List<UniqueAttributeValue> getDuplicatedUniqueValuesInPayload(
      Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>> allAttributes) {

    // TE grouped by attribute and value.
    // Two attributes with the same value and uid are considered equals
    TreeMap<Attribute, List<org.hisp.dhis.tracker.imports.domain.TrackedEntity>>
        teByAttributeValue =
            new TreeMap<>(
                (o1, o2) -> {
                  if (Objects.equals(o1.getValue(), o2.getValue())
                      && Objects.equals(o1.getAttribute(), o2.getAttribute())) {
                    return 0;
                  }
                  return 1;
                });

    for (Map.Entry<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>> entry :
        allAttributes.entrySet()) {
      for (Attribute attribute : entry.getValue()) {
        if (!teByAttributeValue.containsKey(attribute)) {
          teByAttributeValue.put(attribute, Lists.newArrayList());
        }
        teByAttributeValue.get(attribute).add(entry.getKey());
      }
    }

    // If an attribute and value appear in 2 or more tracked entities
    // it means that is not unique
    return teByAttributeValue.entrySet().stream()
        .filter(e -> e.getValue().size() > 1)
        .flatMap(av -> buildUniqueAttributeValues(av.getKey(), av.getValue()).stream())
        .collect(toList());
  }

  private Collection<UniqueAttributeValue> buildUniqueAttributeValues(
      Attribute attribute, List<org.hisp.dhis.tracker.imports.domain.TrackedEntity> teList) {
    return teList.stream()
        .map(
            te ->
                new UniqueAttributeValue(
                    te.getUid(), attribute.getAttribute(), attribute.getValue(), te.getOrgUnit()))
        .collect(toList());
  }

  private List<UniqueAttributeValue> getAlreadyPresentInDbUniqueValues(
      TrackerIdSchemeParams idSchemes,
      Map<org.hisp.dhis.tracker.imports.domain.TrackedEntity, Set<Attribute>>
          allAttributesByTrackedEntity,
      List<TrackedEntityAttribute> uniqueTrackedEntityAttributes) {

    Map<TrackedEntityAttribute, List<String>> uniqueValuesByAttribute =
        allAttributesByTrackedEntity.values().stream()
            .flatMap(Collection::stream)
            .distinct()
            .collect(
                groupingBy(
                    a -> extractAttribute(a.getAttribute(), uniqueTrackedEntityAttributes),
                    mapping(Attribute::getValue, toList())));

    List<TrackedEntityAttributeValue> uniqueAttributeAlreadyPresentInDB =
        trackedEntityAttributeValueService.getUniqueAttributeByValues(uniqueValuesByAttribute);

    return uniqueAttributeAlreadyPresentInDB.stream()
        .map(
            av ->
                new UniqueAttributeValue(
                    av.getTrackedEntity().getUid(),
                    idSchemes.toMetadataIdentifier(av.getAttribute()),
                    av.getValue(),
                    idSchemes.toMetadataIdentifier(av.getTrackedEntity().getOrganisationUnit())))
        .collect(toList());
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
        .filter(
            tea ->
                uniqueTrackedEntityAttributes.stream()
                    .anyMatch(uniqueAttr -> tea.getAttribute().isEqualTo(uniqueAttr)))
        .collect(toSet());
  }
}
