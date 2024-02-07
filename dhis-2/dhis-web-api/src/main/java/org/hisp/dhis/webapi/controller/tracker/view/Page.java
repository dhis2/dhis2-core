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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Shared.Pattern;

// TODO(tracker): revisit if we can create a Page class used by all products when we remove the
// deprecated fields
/**
 * Translates {@link org.hisp.dhis.tracker.export.Page} to its JSON representation. Future changes
 * need to be consistent with how pagination is done across products.
 */
@OpenApi.Shared(pattern = Pattern.TRACKER)
@Getter
@ToString
@EqualsAndHashCode
public class Page<T> {
  @OpenApi.Property(value = OpenApi.EntityType[].class)
  @JsonIgnore
  @JsonAnyGetter
  private final Map<String, List<T>> items = new LinkedHashMap<>();

  @JsonProperty private final Pager pager;

  /**
   * @deprecated in favor of {@link Pager#page}
   */
  @Deprecated(since = "2.41")
  @JsonProperty
  private final Integer page;

  /**
   * @deprecated in favor of {@link Pager#pageSize}
   */
  @Deprecated(since = "2.41")
  @JsonProperty
  private final Integer pageSize;

  /**
   * @deprecated in favor of {@link Pager#total}
   */
  @Deprecated(since = "2.41")
  @JsonProperty
  private final Long total;

  /**
   * @deprecated in favor of {@link Pager#pageCount}
   */
  @Deprecated(since = "2.41")
  @JsonProperty
  private final Integer pageCount;

  private Page(
      String key, List<T> values, org.hisp.dhis.common.Pager pager, boolean showPageTotal) {
    this.items.put(key, values);
    if (pager == null) {
      this.pager = null;
      this.page = null;
      this.pageSize = null;
      this.total = null;
      this.pageCount = null;
      return;
    }

    this.page = pager.getPage();
    this.pageSize = pager.getPageSize();
    if (showPageTotal) {
      this.total = pager.getTotal();
      this.pageCount = pager.getPageCount();
      this.pager =
          new Pager(pager.getPage(), pager.getPageSize(), pager.getTotal(), pager.getPageCount());
    } else {
      this.total = null;
      this.pageCount = null;
      this.pager = new Pager(pager.getPage(), pager.getPageSize(), null, null);
    }
  }

  /**
   * Returns a page which will serialize the items into {@link #items} under given {@code key}.
   * Pagination details will be serialized as well including totals only if {@link
   * org.hisp.dhis.tracker.export.Page#isPageTotal()} is true.
   */
  public static <T, U> Page<T> withPager(
      String key, List<T> items, org.hisp.dhis.tracker.export.Page<U> pager) {
    return new Page<>(key, items, pager.getPager(), pager.isPageTotal());
  }

  /**
   * Returns a page which will only serialize the items into {@link #items} under given {@code key}.
   * All other fields will be omitted from the JSON.
   */
  public static <T> Page<T> withoutPager(String key, List<T> items) {
    return new Page<>(key, items, null, false);
  }

  @OpenApi.Shared(pattern = Pattern.TRACKER)
  @ToString
  @EqualsAndHashCode
  @AllArgsConstructor
  public static class Pager {
    @JsonProperty private Integer page;
    @JsonProperty private Integer pageSize;
    @JsonProperty private Long total;
    @JsonProperty private Integer pageCount;
  }
}
