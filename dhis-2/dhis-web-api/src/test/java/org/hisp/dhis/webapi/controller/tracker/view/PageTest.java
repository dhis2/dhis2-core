/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.view;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PageTest {

  @Test
  void shouldSetDeprecatedPagerWithoutTotals() {
    List<String> fruits = List.of("apple", "banana", "cherry");
    org.hisp.dhis.tracker.export.Page<String> exportPage =
        org.hisp.dhis.tracker.export.Page.withoutTotals(fruits, 2, 3);

    Page page = Page.withPager("fruits", exportPage);

    // deprecated fields
    assertEquals(2, page.getPage());
    assertEquals(3, page.getPageSize());
    assertNull(page.getTotal());
    assertNull(page.getPageCount());

    assertEquals(2, page.getPager().getPage());
    assertEquals(3, page.getPager().getPageSize());
    assertNull(page.getPager().getTotal());
    assertNull(page.getPager().getPageCount());
  }

  @Test
  void shouldSetDeprecatedPagerWithTotals() {
    List<String> fruits = List.of("apple", "banana", "cherry");
    org.hisp.dhis.tracker.export.Page<String> exportPage =
        org.hisp.dhis.tracker.export.Page.withTotals(fruits, 2, 3, 17);

    Page page = Page.withPager("fruits", exportPage);

    // deprecated fields
    assertEquals(2, page.getPage());
    assertEquals(3, page.getPageSize());
    assertEquals(17, page.getTotal());
    assertEquals(6, page.getPageCount());

    assertEquals(2, page.getPager().getPage());
    assertEquals(3, page.getPager().getPageSize());
    assertEquals(17, page.getPager().getTotal());
    assertEquals(6, page.getPager().getPageCount());
  }

  @Test
  void shouldSetPrevPage() {
    // TODO write tests for all permutations
    // build the url from the same page/pageSize values as the export page
    List<String> fruits = List.of("apple", "banana", "cherry");
    org.hisp.dhis.tracker.export.Page<String> exportPage =
        org.hisp.dhis.tracker.export.Page.withPrevAndNext(fruits, 2, 3, true, false);

    // TODO double check using debugger that this is how the request will look like in the
    // controller
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "https://dhis2.org/dev/api/organisationUnits");
    request.setParameter("page", "2");
    request.setParameter("pageSize", "3");
    request.setParameter("fields", "displayName");

    Page<String> page = Page.withPager("fruits", exportPage, request);

    // deprecated fields should not be returned with this new factory!
    assertNull(page.getTotal());
    assertNull(page.getPageCount());
    assertNull(page.getPage());
    assertNull(page.getPageSize());

    assertEquals(2, page.getPager().getPage());
    assertEquals(3, page.getPager().getPageSize());
    assertEquals(
        "https://dhis2.org/dev/api/organisationUnits?page=1&pageSize=3&fields=displayName",
        page.getPager().getPrevPage());
    assertNull(page.getPager().getNextPage());
  }
}
