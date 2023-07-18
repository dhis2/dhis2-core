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
package org.hisp.dhis.webapi.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class PaginationUtilsTest {

  @Test
  void verifyPaginationStartsAtZero() {
    Map<String, String> options = new HashMap<>();
    options.put(WebOptions.PAGING, "true");
    options.put(WebOptions.PAGE, "1");
    options.put(WebOptions.PAGE_SIZE, "20");
    WebOptions webOptions = new WebOptions(options);
    Pagination paginationData = PaginationUtils.getPaginationData(webOptions);
    assertThat(paginationData.getFirstResult(), is(0));
    assertThat(paginationData.getSize(), is(20));
  }

  @Test
  void verifyPaginationCalculation() {
    Map<String, String> options = new HashMap<>();
    options.put(WebOptions.PAGING, "true");
    options.put(WebOptions.PAGE, "14");
    options.put(WebOptions.PAGE_SIZE, "200");
    WebOptions webOptions = new WebOptions(options);
    Pagination paginationData = PaginationUtils.getPaginationData(webOptions);
    assertThat(paginationData.getFirstResult(), is(2600));
    assertThat(paginationData.getSize(), is(200));
  }

  @Test
  void verifyPaginationIsDisabled() {
    Map<String, String> options = new HashMap<>();
    options.put(WebOptions.PAGING, "false");
    WebOptions webOptions = new WebOptions(options);
    Pagination paginationData = PaginationUtils.getPaginationData(webOptions);
    assertThat(paginationData.getFirstResult(), is(0));
    assertThat(paginationData.getSize(), is(0));
    assertThat(paginationData.hasPagination(), is(false));
  }

  @Test
  void verifyIgnoreNegativePage() {
    Map<String, String> options = new HashMap<>();
    options.put(WebOptions.PAGING, "true");
    options.put(WebOptions.PAGE, "-2");
    options.put(WebOptions.PAGE_SIZE, "200");
    WebOptions webOptions = new WebOptions(options);
    Pagination paginationData = PaginationUtils.getPaginationData(webOptions);
    assertThat(paginationData.getFirstResult(), is(0));
    assertThat(paginationData.getSize(), is(200));
  }
}
