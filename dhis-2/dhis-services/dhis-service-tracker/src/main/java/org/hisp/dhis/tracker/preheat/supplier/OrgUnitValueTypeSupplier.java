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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.springframework.stereotype.Component;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
@RequiredArgsConstructor
@Component
public class OrgUnitValueTypeSupplier extends AbstractPreheatSupplier {
  @NonNull private final IdentifiableObjectManager manager;

  @Override
  public void preheatAdd(TrackerImportParams params, TrackerPreheat preheat) {
    List<DataElement> dataElements = preheat.getAll(DataElement.class);

    List<String> orgUnitDataElements =
        dataElements.stream()
            .filter(de -> de.getValueType() == ValueType.ORGANISATION_UNIT)
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toList());

    List<TrackedEntityAttribute> attributes = preheat.getAll(TrackedEntityAttribute.class);

    List<String> orgUnitAttributes =
        attributes.stream()
            .filter(at -> at.getValueType() == ValueType.ORGANISATION_UNIT)
            .map(BaseIdentifiableObject::getUid)
            .collect(Collectors.toList());

    List<String> orgUnitIds = new ArrayList<>();

    params
        .getTrackedEntities()
        .forEach(te -> collectResourceIds(orgUnitAttributes, orgUnitIds, te.getAttributes()));
    params
        .getEnrollments()
        .forEach(en -> collectResourceIds(orgUnitAttributes, orgUnitIds, en.getAttributes()));
    params
        .getEvents()
        .forEach(ev -> collectResourceIds(orgUnitDataElements, orgUnitIds, ev.getDataValues()));

    preheat.put(TrackerIdSchemeParam.UID, manager.getByUid(OrganisationUnit.class, orgUnitIds));
  }

  private void collectResourceIds(
      List<String> orgUnitAttributes, List<String> orgUnitIds, List<Attribute> attributes) {
    attributes.forEach(
        at -> {
          if (orgUnitAttributes.contains(at.getAttribute())
              && !StringUtils.isEmpty(at.getValue())) {
            orgUnitIds.add(at.getValue());
          }
        });
  }

  private void collectResourceIds(
      List<String> orgUnitDataElements, List<String> orgUnitIds, Set<DataValue> dataValues) {
    dataValues.forEach(
        dv -> {
          if (orgUnitDataElements.contains(dv.getDataElement())
              && !StringUtils.isEmpty(dv.getValue())) {
            orgUnitIds.add(dv.getValue());
          }
        });
  }
}
