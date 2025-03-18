/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.QueryItem;
import org.junit.jupiter.api.Test;

class EventQueryParamsUtilsTest {
  @Test
  void testWithoutProgramStageItems() {
    // Create mock QueryItems
    QueryItem item1 = mock(QueryItem.class);
    QueryItem item2 = mock(QueryItem.class);
    QueryItem item3 = mock(QueryItem.class);

    // Set behavior for hasProgramStage()
    when(item1.hasProgramStage()).thenReturn(false); // This item should be retained
    when(item2.hasProgramStage()).thenReturn(true); // This item should be removed
    when(item3.hasProgramStage()).thenReturn(false); // This item should be retained

    // Create an EventQueryParams instance with these items
    EventQueryParams originalParams =
        new EventQueryParams.Builder().addItem(item1).addItem(item2).addItem(item3).build();

    // Apply the method under test
    EventQueryParams resultParams = EventQueryParamsUtils.withoutProgramStageItems(originalParams);

    // Assert the resulting params contain only the filtered items
    List<QueryItem> resultItems = resultParams.getItems();
    assertEquals(2, resultItems.size());
    assertTrue(resultItems.contains(item1));
    assertTrue(resultItems.contains(item3));
    assertFalse(resultItems.contains(item2));
  }
}
