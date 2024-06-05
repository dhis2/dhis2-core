package org.hisp.dhis.section;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SectionStoreTest extends TransactionalIntegrationTest {

  @Autowired private SectionStore sectionStore;
  @Autowired private IdentifiableObjectManager manager;

  @Test
  @DisplayName("retrieving Sections by DataElement returns expected entries")
  void sectionsByDataElementTest() {
    // given
    DataElement deW = createDataElementAndSave('W');
    DataElement deX = createDataElementAndSave('X');
    DataElement deY = createDataElementAndSave('Y');
    DataElement deZ = createDataElementAndSave('Z');

    createSectionAndSave(deW, 'a');
    createSectionAndSave(deX, 'b');
    createSectionAndSave(deY, 'c');
    createSectionAndSave(deZ, 'd');

    // when
    List<Section> sections = sectionStore.getByDataElement(List.of(deW, deX, deY));

    // then
    assertEquals(3, sections.size());
    assertTrue(
        sections.stream()
            .flatMap(s -> s.getDataElements().stream())
            .toList()
            .containsAll(List.of(deW, deX, deY)));
  }

  private DataElement createDataElementAndSave(char c) {
    CategoryCombo cc = createCategoryCombo(c);
    manager.save(cc);

    DataElement de = createDataElement(c, cc);
    manager.save(de);
    return de;
  }

  private void createSectionAndSave(DataElement de, char c) {
    DataSet ds = createDataSet(c, PeriodType.getPeriodType(PeriodTypeEnum.DAILY));
    manager.save(ds);

    Section section = new Section();
    section.setName("section " + c);
    section.getDataElements().add(de);
    section.setDataSet(ds);
    manager.save(section);
  }
}
