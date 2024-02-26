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
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Shared.Pattern;
import org.springframework.web.util.UriComponentsBuilder;

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

  /**
   * Create a page without totals but prev and next page links. This page will also not include any
   * of the deprecated flat pagination fields.
   */
  private Page(
      String key, List<T> values, int page, int pageSize, String prevPage, String nextPage) {
    this.items.put(key, values);
    this.page = null;
    this.pageSize = null;
    this.total = null;
    this.pageCount = null;
    this.pager = new Pager(page, pageSize, null, null, prevPage, nextPage);
  }

  /**
   * Returns a page which will only serialize the items into {@link #items} under given {@code key}.
   * All other fields will be omitted from the JSON.
   */
  private Page(String key, List<T> values) {
    this.items.put(key, values);
    this.pager = null;
    this.page = null;
    this.pageSize = null;
    this.total = null;
    this.pageCount = null;
  }

  /**
   * Create a page without totals.
   *
   * @deprecated Only use if you need to serialize the deprecated flat pagination fields in addition
   *     to the standard pager object.
   */
  @Deprecated(since = "2.41")
  private Page(String key, List<T> values, int page, int pageSize) {
    this.items.put(key, values);
    this.page = page;
    this.pageSize = pageSize;
    this.total = null;
    this.pageCount = null;
    this.pager = new Pager(page, pageSize, null, null, null, null);
  }

  /**
   * Create a page with totals.
   *
   * @deprecated Only use if you need to serialize the deprecated flat pagination fields in addition
   *     to the standard pager object.
   */
  @Deprecated(since = "2.41")
  private Page(String key, List<T> values, int page, int pageSize, long total) {
    this.items.put(key, values);
    this.page = page;
    this.pageSize = pageSize;
    this.total = total;
    this.pageCount = (int) Math.ceil(total / (double) pageSize);
    this.pager = new Pager(page, pageSize, total, this.pageCount, null, null);
  }

  /**
   * Returns a page which will serialize the items into {@link #items} under given {@code key}.
   * Pagination details will be serialized as well including totals only if {@link
   * org.hisp.dhis.tracker.export.Page#getTotal()} is not null. The deprecated flat pagination
   * fields will be serialized as well!
   *
   * @deprecated Only use if you need to serialize the deprecated flat pagination fields in addition
   *     to the standard pager object.
   */
  @Deprecated(since = "2.41")
  public static <T> Page<T> withPager(String key, org.hisp.dhis.tracker.export.Page<T> pager) {
    if (pager.getTotal() != null) {
      return new Page<>(
          key, pager.getItems(), pager.getPage(), pager.getPageSize(), pager.getTotal());
    }
    return new Page<>(key, pager.getItems(), pager.getPage(), pager.getPageSize());
  }

  /**
   * Returns a page which will serialize the items into {@link #items} under given {@code key}.
   * Previous and next page links will be generated based on the request and only if {@link
   * org.hisp.dhis.tracker.export.Page#getPrev()} or next are not null.
   */
  public static <T> Page<T> withPager(
      String key, org.hisp.dhis.tracker.export.Page<T> pager, HttpServletRequest request) {
    String url = getRequestURL(request);
    String prevPage = getPageLink(url, pager.getPrev(), pager.getPage() - 1);
    String nextPage = getPageLink(url, pager.getNext(), pager.getPage() + 1);

    return new Page<>(
        key, pager.getItems(), pager.getPage(), pager.getPageSize(), prevPage, nextPage);
  }

  /**
   * Returns a page which will only serialize the items into {@link #items} under given {@code key}.
   * All other fields will be omitted from the JSON.
   */
  public static <T> Page<T> withoutPager(String key, List<T> items) {
    return new Page<>(key, items);
  }

  @OpenApi.Shared(pattern = Pattern.TRACKER)
  @Getter
  @ToString
  @EqualsAndHashCode
  @AllArgsConstructor
  public static class Pager {
    @JsonProperty private Integer page;
    @JsonProperty private Integer pageSize;
    @JsonProperty private Long total;
    @JsonProperty private Integer pageCount;
    @JsonProperty private String prevPage;
    @JsonProperty private String nextPage;
  }

  private static String getRequestURL(HttpServletRequest request) {
    StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
    String queryString = request.getQueryString();
    if (queryString == null) {
      return requestURL.toString();
    }

    return requestURL.append('?').append(queryString).toString();
  }

  private static String getPageLink(String url, Boolean hasPage, int page) {
    if (Boolean.FALSE.equals(hasPage)) {
      return null;
    }

    UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(url);
    urlBuilder.replaceQueryParam("page", page);
    return urlBuilder.build().toUriString();
  }
}
