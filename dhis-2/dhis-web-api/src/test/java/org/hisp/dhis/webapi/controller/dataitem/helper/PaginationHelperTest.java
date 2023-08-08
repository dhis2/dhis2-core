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
package org.hisp.dhis.webapi.controller.dataitem.helper;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hisp.dhis.webapi.controller.dataitem.helper.PaginationHelper.paginate;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGE;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGE_SIZE;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGING;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PaginationHelper.
 *
 * @author maikel arabori
 */
class PaginationHelperTest {

  @Test
  void testPaginateWhenFirstPage() {
    // Given
    final int pageSize = 5;
    final int firstPage = 1;
    final int totalOfItems = 13;
    final WebOptions theWebOptions = mockWebOptions(pageSize, firstPage);
    final List<DataItem> anyDimensionalItems = mockDimensionalItems(totalOfItems);
    // When
    final List<DataItem> resultingList = paginate(theWebOptions, anyDimensionalItems);
    // Then
    assertThat(resultingList, hasSize(5));
  }

  @Test
  void testPaginateWhenIntermediatePage() {
    // Given
    final int pageSize = 5;
    final int secondPage = 2;
    final int totalOfItems = 13;
    final WebOptions theWebOptions = mockWebOptions(pageSize, secondPage);
    final List<DataItem> anyDimensionalItems = mockDimensionalItems(totalOfItems);
    // When
    final List<DataItem> resultingList = paginate(theWebOptions, anyDimensionalItems);
    // Then
    assertThat(resultingList, hasSize(5));
  }

  @Test
  void testPaginateWhenLastPage() {
    // Given
    final int pageSize = 5;
    final int lastPage = 3;
    final int totalOfItems = 13;
    final WebOptions theWebOptions = mockWebOptions(pageSize, lastPage);
    final List<DataItem> anyDimensionalItems = mockDimensionalItems(totalOfItems);
    // When
    final List<DataItem> resultingList = paginate(theWebOptions, anyDimensionalItems);
    // Then
    assertThat(resultingList, hasSize(3));
  }

  @Test
  void testPaginateWhenPageSizeIsZero() {
    // Given
    final int pageSize = 0;
    final int lastPage = 3;
    final int totalOfItems = 13;
    final WebOptions theWebOptions = mockWebOptions(pageSize, lastPage);
    final List<DataItem> anyDimensionalItems = mockDimensionalItems(totalOfItems);
    // When
    assertThrows(
        IllegalStateException.class,
        () -> paginate(theWebOptions, anyDimensionalItems),
        "Page size must be greater than zero.");
  }

  @Test
  void testPaginateWhenDimensionalItemListIsEmpty() {
    // Given
    final int pageSize = 5;
    final int lastPage = 3;
    final WebOptions theWebOptions = mockWebOptions(pageSize, lastPage);
    final List<DataItem> emptyDimensionalItems = emptyList();
    // When
    final List<DataItem> resultingList = paginate(theWebOptions, emptyDimensionalItems);
    // Then
    assertThat(resultingList, is(emptyDimensionalItems));
    assertThat(resultingList, hasSize(0));
  }

  @Test
  void testPaginateWhenPageIsZero() {
    // Given
    final int pageSize = 5;
    final int currentPage = 0;
    final WebOptions theWebOptions = mockWebOptions(pageSize, currentPage);
    final List<DataItem> emptyDimensionalItems = emptyList();
    // When
    assertThrows(
        IllegalStateException.class,
        () -> paginate(theWebOptions, emptyDimensionalItems),
        "Current page must be greater than zero.");
  }

  private WebOptions mockWebOptions(final int pageSize, final int pageNumber) {
    final Map<String, String> options = new HashMap<>(0);
    options.put(PAGE_SIZE, valueOf(pageSize));
    options.put(PAGE, valueOf(pageNumber));
    options.put(PAGING, "true");
    return new WebOptions(options);
  }

  private List<DataItem> mockDimensionalItems(final int totalOfItems) {
    final List<DataItem> dataItemEntities = new ArrayList<>(0);
    for (int i = 0; i < totalOfItems; i++) {
      final DataItem dataItem = DataItem.builder().name("d-" + i).id("d-" + i).build();
      dataItemEntities.add(dataItem);
    }
    return dataItemEntities;
  }
}
