/*
 * Copyright (c) 2004-2023, University of Oslo
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

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;

/**
 * @author Luciano Fiandesio
 */
public class PaginationUtils {
  public static final Pagination NO_PAGINATION = new Pagination();

  /**
   * Calculates the paging first result based on pagination data from {@see WebOptions} if the
   * WebOptions have pagination information
   *
   * <p>The first result is simply calculated by multiplying page -1 * page size
   *
   * @param options a {@see WebOptions} object
   * @return a {@see PaginationData} object either empty or containing pagination data
   */
  public static Pagination getPaginationData(WebOptions options) {
    if (options.hasPaging()) {
      // ignore if page < 0
      int page = Math.max(options.getPage(), 1);
      return new Pagination((page - 1) * options.getPageSize(), options.getPageSize());
    }

    return NO_PAGINATION;
  }

  /**
   * Method to add paging if enabled. If enabled it will apply the correct paging field values e.g.
   * page, total, pageSize, pageCount
   *
   * @param metadata {@link WebMetadata} to get the pager from
   * @param options {@link WebOptions} to get paging information
   * @param entities to page
   * @return {@link PagedEntities} record which contains the {@link Pager} and the paged entities
   * @param <T> generic param list to page
   */
  public static <T> PagedEntities<T> addPagingIfEnabled(
      @Nonnull WebMetadata metadata, @Nonnull WebOptions options, @Nonnull List<T> entities) {
    Pager pager = metadata.getPager();

    if (options.hasPaging() && pager == null) {
      long totalCount = entities.size();
      long skip = (long) (options.getPage() - 1) * options.getPageSize();
      entities =
          entities.stream().skip(skip).limit(options.getPageSize()).collect(Collectors.toList());
      pager = new Pager(options.getPage(), totalCount, options.getPageSize());
    }
    return new PagedEntities<>(pager, entities);
  }

  @AllArgsConstructor
  @Data
  public static class PagedEntities<T> {
    private Pager pager;
    private List<T> entities;
  }
}
