/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.dimension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.hisp.dhis.common.Pager;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.webapi.service.LinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author maikel arabori
 */
@ExtendWith(MockitoExtension.class)
class DimensionItemPageHandlerTest {
  @Mock private LinkService linkService;

  private DimensionItemPageHandler dimensionItemPageHandler;

  @BeforeEach
  public void setUp() {
    dimensionItemPageHandler = new DimensionItemPageHandler(linkService);
  }

  @Test
  void testAddPaginationToNodeWithSuccess() {
    // Given
    final RootNode anyRootNode = new RootNode("any");
    final GetObjectListParams params = mockWebOptions(10, 1);
    final String anyUid = "LFsZ8v5v7rq";
    final int anyTotals = 12;

    // When
    dimensionItemPageHandler.addPaginationToNodeIfEnabled(anyRootNode, params, anyUid, anyTotals);

    // Then
    assertThat(anyRootNode, is(notNullValue()));
    assertThat(anyRootNode.getName(), is(equalTo("any")));
    assertThat(anyRootNode.getChildren(), hasSize(1));
    assertThat(anyRootNode.getChildren().get(0).isComplex(), is(true));
    verify(linkService, times(1)).generatePagerLinks(any(Pager.class), anyString());
  }

  @Test
  void testAddPaginationToNodeWhenPagingIsFalse() {
    // Given
    final RootNode anyRootNode = new RootNode("any");
    final GetObjectListParams params = mockWebOptionsWithPagingFlagFalse();
    final String anyUid = "LFsZ8v5v7rq";
    final int anyTotals = 12;

    // When
    dimensionItemPageHandler.addPaginationToNodeIfEnabled(anyRootNode, params, anyUid, anyTotals);

    // Then
    assertThat(anyRootNode, is(notNullValue()));
    assertThat(anyRootNode.getName(), is(equalTo("any")));
    assertThat(anyRootNode.getChildren(), is(empty()));
    verify(linkService, never()).generatePagerLinks(any(Pager.class), anyString());
  }

  private GetObjectListParams mockWebOptions(int pageSize, int pageNumber) {
    return new GetObjectListParams().setPaging(true).setPage(pageNumber).setPageSize(pageSize);
  }

  private GetObjectListParams mockWebOptionsWithPagingFlagFalse() {
    return new GetObjectListParams().setPaging(false);
  }
}
