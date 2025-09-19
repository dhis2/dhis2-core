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
import org.hisp.dhis.common.OpenApi;

/**
 * Base for parameters supported by CRUD {@code CRUD.getObjectList}.
 *
 * @author Jan Bernitt
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class GetObjectListParams extends GetObjectParams {

  @OpenApi.Description(
      """
      Filter results using `filter={property}:{operator}[:{value}]` expressions as described in detail in
      [Metadata-object-filter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_object_filter).
    """)
  @JsonProperty("filter")
  @CheckForNull
  List<String> filters;

  @OpenApi.Description(
      """
    Adds ordering to the result list.
    Case-sensitive: `{property}:asc`, `{property}:desc`
    Case-insensitive: `{property}:iasc`, `{property}:idesc`
    Only supports properties that are both persisted and simple.
    """)
  @JsonProperty("order")
  @CheckForNull
  List<String> orders;

  @OpenApi.Description("Combine `filter`s with `AND` (default) or `OR` logic.")
  @JsonProperty
  Junction.Type rootJunction = Junction.Type.AND;

  @OpenApi.Description(
      "Use paging controlled by `page` and `pageSize` or return all matches (use with caution).")
  @JsonProperty
  boolean paging = true;

  @OpenApi.Description("The page number to show.")
  @JsonProperty
  int page = 1;

  @OpenApi.Description("The maximum number of elements on a page.")
  @JsonProperty
  int pageSize = 50;

  @OpenApi.Description(
      """
   Adds a filter equivalent to the following three `filter`s combined _OR_ (independent of `rootJunction`):
   `id:eq:{query}`, `code:eq:{query}`, `name:like:{query}`. Can be used in addition to regular `filter`s.
    """)
  @JsonProperty
  String query;

  @OpenApi.Ignore
  @CheckForNull
  public List<String> getFilters() {
    return filters;
  }

  @OpenApi.Ignore
  @CheckForNull
  public List<String> getOrders() {
    return orders;
  }

  @OpenApi.Ignore
  public GetObjectListParams setFilters(List<String> filters) {
    this.filters = filters;
    return this;
  }

  @OpenApi.Ignore
  public GetObjectListParams setOrders(List<String> orders) {
    this.orders = orders;
    return this;
  }

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

  /**
   * Pattern to split {@code filter} expressions which have 2 or 3 components: {@code
   * property:operator} or {@code property:operator:value}.
   *
   * <p>Examples
   *
   * <pre>
   * name:eq:Peter
   * organisationUnits:empty
   * </pre>
   *
   * Unfortunately the value can also contain : and when used with [] it can also contain , which
   * needs extra caution to not read too little or too much into operation and value.
   */
  private static final Pattern FILTER_PARTS =
      Pattern.compile(
          "(?<property>[a-zA-Z0-9.]+):(?<op>[!$a-zA-Z]{2,10})(?::(?<value>\\[[^]]*]|[^,\\n\\r]+))?");

  /**
   * Splits a string {@code filter,filter} into a list {@code [filter,filter]}.
   *
   * <p>This is non-trivial since filters can contain comma symbols.
   *
   * @param filters a comma seperated list of filter expressions
   * @return a list where each element is a single filter expression
   */
  @CheckForNull
  static List<String> splitFilters(@CheckForNull String filters) {
    if (filters == null || filters.isEmpty()) return null;
    if (!filters.contains(",")) return new ArrayList<>(List.of(filters));
    List<String> res = new ArrayList<>();
    Matcher m = FILTER_PARTS.matcher(filters);
    while (m.find()) {
      res.add(m.group());
    }
    return res;
  }
}
