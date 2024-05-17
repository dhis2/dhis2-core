package org.hisp.dhis.program.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramNotificationStoreTest extends SingleSetupIntegrationTestBase {

  @Autowired private DataElementService dataElementService;
  @Autowired private ProgramNotificationTemplateStore programNotificationTemplateStore;

  @Test
  @DisplayName("retrieving SMS Codes by data element returns expected entries")
  void getProgramNotificationTemplatesByDataElementTest() {
    // given
    DataElement deA = createDataElementAndSave('A');
    DataElement deB = createDataElementAndSave('B');
    DataElement deC = createDataElementAndSave('C');

    ProgramNotificationTemplate pnt1 = createProgramNotificationTemplate("temp 1", 2, null, null);
    pnt1.setRecipientDataElement(deA);
    ProgramNotificationTemplate pnt2 = createProgramNotificationTemplate("temp 2", 2, null, null);
    pnt2.setRecipientDataElement(deB);
    ProgramNotificationTemplate pnt3 = createProgramNotificationTemplate("temp 3", 2, null, null);
    pnt3.setRecipientDataElement(deC);
    ProgramNotificationTemplate pnt4 = createProgramNotificationTemplate("temp 4", 2, null, null);
    programNotificationTemplateStore.save(pnt1);
    programNotificationTemplateStore.save(pnt2);
    programNotificationTemplateStore.save(pnt3);
    programNotificationTemplateStore.save(pnt4);

    // when
    List<ProgramNotificationTemplate> programNotificationTemplates =
        programNotificationTemplateStore.getByDataElement(List.of(deA, deB, deC));

    // then
    assertEquals(3, programNotificationTemplates.size());
    assertTrue(
        programNotificationTemplates.stream()
            .map(ProgramNotificationTemplate::getRecipientDataElement)
            .toList()
            .containsAll(List.of(deA, deB, deC)));
  }

  private DataElement createDataElementAndSave(char c) {
    DataElement de = createDataElement(c);
    dataElementService.addDataElement(de);
    return de;
  }
}
