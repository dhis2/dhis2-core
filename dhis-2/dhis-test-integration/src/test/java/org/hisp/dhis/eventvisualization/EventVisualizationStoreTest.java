package org.hisp.dhis.eventvisualization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.AnalyticalObjectStore;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventVisualizationStoreTest extends SingleSetupIntegrationTestBase {

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
