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
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataDimensionItemStoreTest extends PostgresIntegrationTestBase {

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
