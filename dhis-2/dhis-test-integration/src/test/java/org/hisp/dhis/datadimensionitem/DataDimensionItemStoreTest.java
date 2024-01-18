package org.hisp.dhis.datadimensionitem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataDimensionItemStoreTest extends SingleSetupIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private DataDimensionItemStore store;

  @Test
  @DisplayName("Get data dimension items with indicator refs")
  void getDataDimensionItemsWithIndicators() {
    // given 4 data dimension items exist
    IndicatorType indicatorType = createIndicatorType('t');
    manager.save(indicatorType);

    Indicator indicator1 = createIndicator('a', indicatorType);
    Indicator indicator2 = createIndicator('b', indicatorType);
    Indicator indicator3 = createIndicator('c', indicatorType);

    manager.save(indicator1);
    manager.save(indicator2);
    manager.save(indicator3);

    DataElement element = createDataElement('a');
    manager.save(element);

    // 3 of which have indicators
    DataDimensionItem item1 = DataDimensionItem.create(indicator1);
    DataDimensionItem item2 = DataDimensionItem.create(indicator2);
    DataDimensionItem item3 = DataDimensionItem.create(indicator3);

    // 1 of which has no indicator
    DataDimensionItem item4 = DataDimensionItem.create(element);

    store.save(item1);
    store.save(item2);
    store.save(item3);
    store.save(item4);

    // assert all 4 items exist before further checks
    List<DataDimensionItem> all = store.getAll();
    assertEquals(4, all.size());

    // when searching for data dimension items with specific indicator refs
    List<DataDimensionItem> matchedItems =
        store.getIndicatorDataDimensionItems(List.of(indicator1, indicator2));

    // then 2 data dimension items with matching indicators are retrieved
    assertEquals(2, matchedItems.size());
    assertTrue(matchedItems.contains(item1));
    assertTrue(matchedItems.contains(item2));

    // and the other 2 data dimension items with non-matching indicator refs are not retrieved
    assertFalse(matchedItems.contains(item3));
    assertFalse(matchedItems.contains(item4));
  }
}
