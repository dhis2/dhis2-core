/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.gist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hisp.dhis.common.Maturity.Beta;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.query.Junction;

/**
 * Web API input params for {@link GistQuery}.
 *
 * @author Jan Bernitt
 */
@Data
@OpenApi.Property // all fields are public properties
@EqualsAndHashCode(callSuper = true)
public class GistObjectListParams extends GistObjectParams {

  @OpenApi.Description(
      """
      The viewed page in paged list starting with 1 for the first page
      See [Gist page parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_page).""")
  int page = 1;

  @OpenApi.Description(
      """
      The number of items on a page. Maximum is 1000 items.
      See [Gist pageSize parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_pageSize).""")
  int pageSize = 50;

  @OpenApi.Description(
      """
      The name of the property in the response object that holds the list of response objects when a paged response is used.""")
  String pageListName;

  @OpenApi.Description("Old name for `totalPages`.")
  @Deprecated(since = "2.41", forRemoval = true)
  Boolean total;

  @OpenApi.Description(
      """
      By default, a gist query will not count the total number of matches should those exceed the `pageSize` limit.
      Using `totalPages=true` the pager includes the total number of matches.
      See [Gist total parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_total).""")
  Boolean totalPages;

  @OpenApi.Description(
      """
      Endpoints returning a list by default wrap the items with an envelope containing the pager and the list, which is named according to the type of object listed.
      See [Gist headless parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_headless).""")
  boolean headless;

  @Beta
  @OpenApi.Since(43)
  @OpenApi.Description(
      """
      When true, the list is adjusted for efficient listing of organisation units for offline caching.
      Most importantly the list will not have paging (not possible otherwise).
      Overrides `fields` with the equivalent of `fields=path,displayName,children::isNotEmpty`.
      Overrides `references` with `references=false`.
      """)
  boolean orgUnitsOffline = false;

  @Beta
  @OpenApi.Since(43)
  @OpenApi.Description(
      """
      When true, the list is adjusted for efficient paged listing of organisation units displaying search results as tree.
      Overrides `order` with the equivalent of `order=path`.
      Overrides `references` with `references=false`.
      Always adds `path` to the `fields` list, if not already contained.
      For organisation units `/api/organisationUnits/gist` lists all parents up to the root (ancestors) of any match are included in the result list.
      In that case a boolean property `match` is added to each entry indicating if the entry was added as ancestor (`false`) or as query match (`true`)""")
  boolean orgUnitsTree = false;

  @OpenApi.Description(
      """
      Combine `filter`s with `AND` (default) or `OR` logic combinator
      See [Gist rootJunction parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_rootJunction).""")
  Junction.Type rootJunction = Junction.Type.AND;

  @OpenApi.Description(
      """
      A comma seperated list of filters.
      See [Gist filter parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_filter).""")
  String filter;

  @OpenApi.Description(
      """
      To sort the list of items - one or more order expressions can be given.
      See [Gist order parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_order).""")
  String order;

  @JsonIgnore
  public boolean isCountTotalPages() throws BadRequestException {
    if (totalPages != null && total != null && !totalPages.equals(total))
      throw new BadRequestException(
          "totalPages and total request parameters are contradicting each other");
    if (totalPages != null) return totalPages;
    if (total != null) return total;
    return false;
  }
}
