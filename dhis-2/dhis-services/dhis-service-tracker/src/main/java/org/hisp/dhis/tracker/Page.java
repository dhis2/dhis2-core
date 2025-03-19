/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker;

import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Create a page of items. A page is guaranteed to have items, a page number and page size. All
 * other fields are optional.
 */
@RequiredArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class Page<T> {
  @Nonnull private final List<T> items;
  private final int page;
  private final int pageSize;
  private final Long total;
  private final Integer prevPage;
  private final Integer nextPage;

  public static <T> Page<T> empty() {
    return new Page<>(List.of(), 0, 0, 0L, null, null);
  }

  /** Create a page without a total count of items. */
  public Page(@Nonnull List<T> items, @Nonnull PageParams pageParams) {
    this(items, pageParams, null);
  }

  /**
   * Create a page that optionally supplies a total count of items and indicates if there is a
   * previous or next page. It is assumed that there is a previous page when the current page is
   * greater than 1. It is assumed there is a next page if there are more items than the page size.
   * This means that the store has to fetch one more item than the requested page size.
   */
  public Page(
      @Nonnull List<T> items, @Nonnull PageParams pageParams, @CheckForNull LongSupplier total) {
    this.page = pageParams.getPage();
    this.pageSize = pageParams.getPageSize();

    if (pageParams.isPageTotal() && total != null) {
      this.total = total.getAsLong();
    } else {
      this.total = null;
    }

    this.prevPage = pageParams.getPage() > 1 ? pageParams.getPage() - 1 : null;
    if (items.size() > pageParams.getPageSize()) {
      this.items = items.subList(0, pageParams.getPageSize());
      this.nextPage = pageParams.getPage() + 1;
    } else {
      this.items = items;
      this.nextPage = null;
    }
  }

  /**
   * Create a new page based on an existing one but with given {@code items}.
   *
   * <p>Prefer {@link #withMappedItems(Function)} and only use this one if you have to. The only
   * reason to use this is to filter items. This obviously invalidates pageSize, counts and
   * potentially nextPage. Any filtering of result sets must move into the store.
   *
   * @deprecated use {@link #withMappedItems(Function)}
   */
  @Deprecated(forRemoval = true)
  public <U> Page<U> withFilteredItems(List<U> items) {
    return new Page<>(items, this.page, this.pageSize, this.total, this.prevPage, this.nextPage);
  }

  /** Create a new page based on this existing page mapping the individual items. */
  public <R> Page<R> withMappedItems(Function<T, R> map) {
    return new Page<>(
        items.stream().map(map).toList(),
        this.page,
        this.pageSize,
        this.total,
        this.prevPage,
        this.nextPage);
  }
}
