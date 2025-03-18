/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.dataelement.handler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.datadimensionitem.DataDimensionItemStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationStore;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.springframework.stereotype.Component;

/**
 * Merge handler for analytical entities.
 *
 * @author david mackessy
 */
@Component
@RequiredArgsConstructor
public class AnalyticalDataElementMergeHandler {

  private final EventVisualizationStore eventVisualizationStore;
  private final AnalyticalObjectStore<EventVisualization> analyticalEventVisStore;
  private final AnalyticalObjectStore<MapView> mapViewStore;
  private final DataDimensionItemStore dataDimensionItemStore;

  /**
   * Method retrieving {@link EventVisualization}s by source {@link DataElement} references. All
   * retrieved {@link EventVisualization}s will have their {@link DataElement} replaced with the
   * target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link EventVisualization}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     EventVisualization}
   */
  public void handleEventVisualization(List<DataElement> sources, DataElement target) {
    List<EventVisualization> eventVisualizations =
        eventVisualizationStore.getByDataElement(sources);
    eventVisualizations.forEach(ev -> ev.setDataElementValueDimension(target));
  }

  /**
   * Method retrieving {@link BaseAnalyticalObject}s ({@link EventVisualization} & {@link MapView}
   * only) by source {@link DataElement} references in property dataElementDimensions. All retrieved
   * {@link BaseAnalyticalObject}s will have their {@link TrackedEntityDataElementDimension} {@link
   * DataElement} replaced with the target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link BaseAnalyticalObject}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     TrackedEntityDataElementDimension}
   */
  public void handleTrackedEntityDataElementDimension(
      List<DataElement> sources, DataElement target) {
    List<EventVisualization> eventVisualizations =
        analyticalEventVisStore.getByDataElementDimensionsWithAnyOf(sources);
    List<MapView> mapViews = mapViewStore.getByDataElementDimensionsWithAnyOf(sources);

    eventVisualizations.stream()
        .flatMap(ev -> ev.getDataElementDimensions().stream())
        .forEach(teded -> teded.setDataElement(target));

    mapViews.stream()
        .flatMap(mv -> mv.getDataElementDimensions().stream())
        .forEach(teded -> teded.setDataElement(target));
  }

  /**
   * Method retrieving {@link DataDimensionItem}s by source {@link DataElement} references. All
   * retrieved {@link DataDimensionItem}s will have their {@link DataElement} ref replaced with the
   * target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link DataDimensionItem}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} in each {@link
   *     DataDimensionItem}
   */
  public void handleDataDimensionItems(List<DataElement> sources, DataElement target) {
    List<DataDimensionItem> dataDimensionItems =
        dataDimensionItemStore.getDataElementDataDimensionItems(sources);
    dataDimensionItems.forEach(ddi -> ddi.setDataElement(target));
  }
}
