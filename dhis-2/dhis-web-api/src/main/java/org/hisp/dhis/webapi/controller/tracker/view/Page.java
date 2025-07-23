/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.common.OpenApi;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Translates {@link org.hisp.dhis.tracker.Page} to its JSON representation. Future changes need to
 * be consistent with how pagination is done across products.
 */
@Getter
@ToString
@EqualsAndHashCode
public class Page<T>
    implements JsonSerializable { // JsonSerializable chosen over StdSerializer for zero
  // configuration

  private final String key;

  @OpenApi.Property(value = OpenApi.EntityType[].class)
  private final List<T> items;

  private final Pager pager;

  private Page(String key, List<T> items, Pager pager) {
    this.key = key;
    this.items = items;
    this.pager = pager;
  }

  /**
   * Returns a page which will serialize the items into {@link #items} under given {@code key}.
   * Previous and next page links will be generated based on the request if {@link
   * org.hisp.dhis.tracker.Page#getPrevPage()} or next are not null. Total and page count will also
   * be set if the pager has a non-null total.
   */
  public static <T> Page<T> withPager(
      @Nonnull String key,
      @Nonnull org.hisp.dhis.tracker.Page<T> pager,
      @Nonnull String requestURL) {

    Integer pageCount = null;
    if (pager.getTotal() != null) {
      pageCount = (int) Math.ceil(pager.getTotal() / (double) pager.getPageSize());
    }
    String prevPage = getPageLink(requestURL, pager.getPrevPage());
    String nextPage = getPageLink(requestURL, pager.getNextPage());

    Pager pagerObj =
        new Pager(
            pager.getPage(), pager.getPageSize(), pager.getTotal(), pageCount, prevPage, nextPage);

    return new Page<>(key, pager.getItems(), pagerObj);
  }

  /**
   * Returns a page which will only serialize the items into {@link #items} under given {@code key}.
   * All other fields will be omitted from the JSON.
   */
  public static <T> Page<T> withoutPager(String key, List<T> items) {
    return new Page<>(key, items, null);
  }

  @Override
  public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
    serializeWithType(gen, serializers, null);
  }

  @Override
  public void serializeWithType(
      JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer)
      throws IOException {
    gen.writeStartObject();

    if (pager != null) {
      gen.writeFieldName("pager");
      serializers.defaultSerializeValue(pager, gen);
    }

    gen.writeFieldName(key);
    serializers.defaultSerializeValue(items, gen);

    gen.writeEndObject();
  }

  @OpenApi.Shared(name = "TrackerPager")
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

  private static String getPageLink(String url, Integer page) {
    if (page == null) {
      return null;
    }

    UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(url);
    urlBuilder.replaceQueryParam("page", page);
    return urlBuilder.build().toUriString();
  }
}
