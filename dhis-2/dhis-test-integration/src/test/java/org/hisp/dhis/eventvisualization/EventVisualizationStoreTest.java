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
package org.hisp.dhis.eventvisualization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class EventVisualizationStoreTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager identifiableObjectManager;
  @Autowired private EventVisualizationStore eventVisualizationStore;
  @Autowired private AnalyticalObjectStore<EventVisualization> analyticalObjectStore;

  @Test
  @DisplayName("retrieving Event Visualizations by DataElement should return the correct results")
  void retrievingEventVisualizationsByDataElementShouldReturnTheCorrectResults() {
    // given EventVisualizations with DataElement references
    DataElement de1 = createDataElementAndSave('1');
    DataElement de2 = createDataElementAndSave('2');
    DataElement de3 = createDataElementAndSave('3');

    EventVisualization eventVis1 = createEventVisualization('1', null);
    eventVis1.setDataElementValueDimension(de1);
    EventVisualization eventVis2 = createEventVisualization('2', null);
    eventVis2.setDataElementValueDimension(de2);
    EventVisualization eventVis3 = createEventVisualization('3', null);
    eventVis3.setDataElementValueDimension(de3);

    eventVisualizationStore.save(eventVis1);
    eventVisualizationStore.save(eventVis2);
    eventVisualizationStore.save(eventVis3);

    // when
    List<EventVisualization> allByDataElement =
        eventVisualizationStore.getByDataElement(List.of(de1, de2));

    // then
    assertEquals(2, allByDataElement.size());
    assertTrue(
        allByDataElement.stream()
            .map(ev -> ev.getDataElementValueDimension().getUid())
            .toList()
            .containsAll(List.of(de1.getUid(), de2.getUid())));
  }

  private DataElement createDataElementAndSave(char c) {
    DataElement de = createDataElement(c);
    identifiableObjectManager.save(de);
    return de;
  }
}
