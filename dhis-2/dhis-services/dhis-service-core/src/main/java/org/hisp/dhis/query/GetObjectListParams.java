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
package org.hisp.dhis.query;

import static java.lang.Math.max;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Base for parameters supported by CRUD {@code CRUD.getObjectList}.
 *
 * @author Jan Bernitt
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class GetObjectListParams extends GetObjectParams {

  @JsonProperty("filter")
  @CheckForNull
  List<String> filters;

  @JsonProperty("order")
  @CheckForNull
  List<String> orders;

  @JsonProperty Junction.Type rootJunction = Junction.Type.AND;

  @JsonProperty boolean paging = true;
  @JsonProperty int page = 1;
  @JsonProperty int pageSize = 50;

  /**
   * A special filter that matches the query term against the ID and code (equals) and against name
   * (contains). Can be used in combination with regular filters.
   */
  @JsonProperty String query;

  @Nonnull
  @JsonIgnore
  public Pagination getPagination() {
    if (!paging) return new Pagination();
    // ignore if page < 0
    return new Pagination((max(page, 1) - 1) * pageSize, pageSize);
  }

  public GetObjectListParams addFilter(String filter) {
    if (filters == null) filters = new ArrayList<>();
    filters.add(filter);
    return this;
  }

  public GetObjectListParams addOrder(String order) {
    if (orders == null) orders = new ArrayList<>();
    orders.add(order);
    return this;
  }

  /**
   * For spring URL parameter injection only.
   *
   * <p>The issue is that a filter value may contain comma as part of the value, not to separate
   * values which can be misunderstood by spring so the splitting needs to be done with custom
   * logic.
   *
   * @param filters all filters as a comma seperated string
   */
  public void setFilter(String filters) {
    this.filters = splitFilters(filters);
  }

  public void setOrder(List<String> orders) {
    this.orders = orders;
  }

  private static final Pattern FILTER_PARTS =
      Pattern.compile(
          "(?<property>[a-zA-Z0-9.]+):(?<op>[^:,\\n\\r]+)(?::(?<value>\\[[^]]*]|[^,\\n\\r]+))?");

  private static List<String> splitFilters(String filters) {
    if (filters == null) return null;
    if (!filters.contains(",")) return new ArrayList<>(List.of(filters));
    List<String> res = new ArrayList<>();
    Matcher m = FILTER_PARTS.matcher(filters);
    while (m.find()) {
      res.add(m.group());
    }
    return res;
  }
}
