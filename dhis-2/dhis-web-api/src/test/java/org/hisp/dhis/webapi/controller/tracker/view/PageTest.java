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

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.PageParams;
import org.junit.jupiter.api.Test;

class PageTest {
  @Test
  void shouldNotSetNoPageLinksIfThereAreNone() throws BadRequestException {
    List<String> fruits = List.of("apple", "banana", "cherry");
    PageParams pageParams = PageParams.of(1, 3, false);
    org.hisp.dhis.tracker.Page<String> exportPage =
        new org.hisp.dhis.tracker.Page<>(fruits, pageParams);

    Page<String> page =
        Page.withPager(
            "fruits",
            exportPage,
            "http://localhost/organisationUnits?page=1&pageSize=3&fields=displayName");

    assertEquals(Map.of("fruits", fruits), page.getItems());

    assertEquals(1, page.getPager().getPage());
    assertEquals(3, page.getPager().getPageSize());
    assertNull(page.getPager().getTotal());
    assertNull(page.getPager().getPageCount());

    assertNull(page.getPager().getPrevPage());
    assertNull(page.getPager().getNextPage());
  }

  @Test
  void shouldSetPrevPage() throws BadRequestException {
    List<String> fruits = List.of("apple", "banana", "cherry");
    PageParams pageParams = PageParams.of(2, 3, false);
    org.hisp.dhis.tracker.Page<String> exportPage =
        new org.hisp.dhis.tracker.Page<>(fruits, pageParams);

    Page<String> page =
        Page.withPager(
            "fruits",
            exportPage,
            "http://localhost/organisationUnits?page=2&pageSize=3&fields=displayName");

    assertEquals(Map.of("fruits", fruits), page.getItems());

    assertEquals(2, page.getPager().getPage());
    assertEquals(3, page.getPager().getPageSize());
    assertNull(page.getPager().getTotal());
    assertNull(page.getPager().getPageCount());

    assertPagerLink(
        page.getPager().getPrevPage(),
        1,
        3,
        "http://localhost/organisationUnits",
        "fields=displayName");
    assertNull(page.getPager().getNextPage());
  }

  @Test
  void shouldSetNextPage() throws BadRequestException {
    List<String> fruits = List.of("apple", "banana", "cherry", "mango");
    PageParams pageParams = PageParams.of(1, 3, false);
    org.hisp.dhis.tracker.Page<String> exportPage =
        new org.hisp.dhis.tracker.Page<>(fruits, pageParams);

    Page<String> page =
        Page.withPager(
            "fruits",
            exportPage,
            "http://localhost/organisationUnits?page=1&pageSize=3&fields=displayName");

    assertEquals(Map.of("fruits", List.of("apple", "banana", "cherry")), page.getItems());

    assertEquals(1, page.getPager().getPage());
    assertEquals(3, page.getPager().getPageSize());
    assertNull(page.getPager().getTotal());
    assertNull(page.getPager().getPageCount());

    assertNull(page.getPager().getPrevPage());
    assertPagerLink(
        page.getPager().getNextPage(),
        2,
        3,
        "http://localhost/organisationUnits",
        "fields=displayName");
  }

  private static void assertPagerLink(
      String actual, int page, int pageSize, String start, String additionalParam) {
    assertNotNull(actual);
    assertAll(
        () -> assertStartsWith(start, actual),
        () -> assertContains("page=" + page, actual),
        () -> assertContains("pageSize=" + pageSize, actual),
        () -> assertContains(additionalParam, actual));
  }
}
