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
package org.hisp.dhis.webapi.controller.event.webrequest;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
public class PagingWrapper<T> {
  @JsonIgnore private String identifier;

  @JsonIgnore @JsonAnyGetter private Map<String, Collection<T>> elements = new LinkedHashMap<>();

  @JsonUnwrapped private Pager pager;

  public PagingWrapper() {
    this("instances");
    this.identifier = "instances";
  }

  public PagingWrapper(String identifier) {
    this.identifier = identifier;
  }

  public PagingWrapper<T> withPager(Pager pager) {
    PagingWrapper<T> pagingWrapper = new PagingWrapper<>(identifier);
    pagingWrapper.pager = pager;
    pagingWrapper.elements = elements;
    return pagingWrapper;
  }

  public PagingWrapper<T> withInstances(Collection<T> elements) {
    PagingWrapper<T> pagingWrapper = new PagingWrapper<>(identifier);
    pagingWrapper.pager = pager;
    pagingWrapper.elements.put(identifier, elements);
    return pagingWrapper;
  }

  @Data
  @Builder
  public static class Pager {
    @Builder.Default @JsonProperty private Integer page = 1;

    @JsonProperty private Long total;

    @JsonProperty private Integer pageCount;

    @Builder.Default @JsonProperty
    private Integer pageSize = org.hisp.dhis.common.Pager.DEFAULT_PAGE_SIZE;

    @JsonProperty private String nextPage;

    @JsonProperty private String prevPage;

    public static Pager fromLegacy(
        PagingCriteria pagingCriteria, org.hisp.dhis.common.Pager pager) {
      return Pager.builder()
          .prevPage(pager.getPrevPage())
          .page(pager.getPage())
          .pageSize(pager.getPageSize())
          .pageCount(pagingCriteria.isTotalPages() ? pager.getPageCount() : null)
          .total(pagingCriteria.isTotalPages() ? pager.getTotal() : null)
          .nextPage(pager.getNextPage())
          .build();
    }
  }
}
