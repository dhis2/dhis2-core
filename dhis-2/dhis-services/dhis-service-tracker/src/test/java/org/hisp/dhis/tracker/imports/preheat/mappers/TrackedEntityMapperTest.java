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
package org.hisp.dhis.tracker.imports.preheat.mappers;

import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.tracker.imports.preheat.mappers.AttributeCreator.attributeValues;
import static org.hisp.dhis.tracker.imports.preheat.mappers.AttributeCreator.setIdSchemeFields;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.junit.jupiter.api.Test;

class TrackedEntityMapperTest {
  @Test
  void testIdSchemeRelatedFieldsAreMapped() {

    TrackedEntityType trackedEntityType =
        setIdSchemeFields(
            new TrackedEntityType(),
            "WTTYiPQDqh1",
            "friendship",
            "red",
            attributeValues("m0GpPuMUfFW", "yellow"));

    OrganisationUnit orgUnit =
        setIdSchemeFields(
            createOrganisationUnit('A'),
            "HpSAvRWtdDR",
            "meet",
            "green",
            attributeValues("m0GpPuMUfFW", "purple"));

    TrackedEntityAttribute attribute =
        setIdSchemeFields(
            new TrackedEntityAttribute(),
            "khBzbxTLo8k",
            "clouds",
            "orange",
            attributeValues("m0GpPuMUfFW", "purple"));
    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setAttribute(attribute);

    TrackedEntity te = new TrackedEntity();
    te.setTrackedEntityType(trackedEntityType);
    te.setOrganisationUnit(orgUnit);
    te.setTrackedEntityAttributeValues(Set.of(attributeValue));

    TrackedEntity mapped = TrackedEntityMapper.INSTANCE.map(te);

    assertEquals("WTTYiPQDqh1", mapped.getTrackedEntityType().getUid());
    assertEquals("friendship", mapped.getTrackedEntityType().getName());
    assertEquals("red", mapped.getTrackedEntityType().getCode());
    assertEquals(
        AttributeValues.of(Map.of("m0GpPuMUfFW", "yellow")),
        mapped.getTrackedEntityType().getAttributeValues());

    assertEquals("HpSAvRWtdDR", mapped.getOrganisationUnit().getUid());
    assertEquals("meet", mapped.getOrganisationUnit().getName());
    assertEquals("green", mapped.getOrganisationUnit().getCode());
    assertEquals(
        AttributeValues.of(Map.of("m0GpPuMUfFW", "purple")),
        mapped.getOrganisationUnit().getAttributeValues());

    Optional<TrackedEntityAttributeValue> actual =
        mapped.getTrackedEntityAttributeValues().stream().findFirst();
    assertTrue(actual.isPresent());
    TrackedEntityAttributeValue value = actual.get();
    assertEquals("khBzbxTLo8k", value.getAttribute().getUid());
    assertEquals("clouds", value.getAttribute().getName());
    assertEquals("orange", value.getAttribute().getCode());
    assertEquals(
        AttributeValues.of(Map.of("m0GpPuMUfFW", "purple")),
        value.getAttribute().getAttributeValues());
  }
}
