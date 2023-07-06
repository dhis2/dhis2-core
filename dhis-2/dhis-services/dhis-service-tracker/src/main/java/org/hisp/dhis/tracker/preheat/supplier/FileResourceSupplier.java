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
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
@Component
public class FileResourceSupplier extends AbstractPreheatSupplier {
  @Nonnull private final FileResourceService fileResourceService;

  @Override
  public void preheatAdd(TrackerImportParams params, TrackerPreheat preheat) {
    TrackerIdSchemeParams idSchemes = params.getIdSchemes();

    List<MetadataIdentifier> fileResourceAttributes =
        preheat.getAll(TrackedEntityAttribute.class).stream()
            .filter(at -> at.getValueType().isFile())
            .map(idSchemes::toMetadataIdentifier)
            .collect(Collectors.toList());

    List<MetadataIdentifier> fileResourceDataElements =
        preheat.getAll(DataElement.class).stream()
            .filter(at -> at.getValueType().isFile())
            .map(idSchemes::toMetadataIdentifier)
            .collect(Collectors.toList());

    List<String> fileResourceIds = new ArrayList<>();
    params
        .getTrackedEntities()
        .forEach(
            te -> collectResourceIds(fileResourceAttributes, fileResourceIds, te.getAttributes()));
    params
        .getEnrollments()
        .forEach(
            en -> collectResourceIds(fileResourceAttributes, fileResourceIds, en.getAttributes()));
    params
        .getEvents()
        .forEach(
            en ->
                collectResourceIds(fileResourceDataElements, fileResourceIds, en.getDataValues()));

    preheat.put(TrackerIdSchemeParam.UID, fileResourceService.getFileResources(fileResourceIds));
  }

  private void collectResourceIds(
      List<MetadataIdentifier> fileResourceAttributes,
      List<String> fileResourceIds,
      List<Attribute> attributes) {
    attributes.forEach(
        at -> {
          if (fileResourceAttributes.contains(at.getAttribute())
              && !StringUtils.isEmpty(at.getValue())) {
            fileResourceIds.add(at.getValue());
          }
        });
  }

  private void collectResourceIds(
      List<MetadataIdentifier> fileResourceDataElements,
      List<String> fileResourceIds,
      Set<DataValue> dataElements) {
    dataElements.forEach(
        de -> {
          if (fileResourceDataElements.contains(de.getDataElement())
              && !StringUtils.isEmpty(de.getValue())) {
            fileResourceIds.add(de.getValue());
          }
        });
  }
}
