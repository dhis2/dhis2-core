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
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.query.Junction;

/**
 * Web API input params for {@link GistQuery}.
 *
 * @author Jan Bernitt
 */
@Data
@OpenApi.Shared
@OpenApi.Property // all fields are public properties
public final class GistParams {

  @OpenApi.Description(
      """
      Switch translation language of display names. If not specified the translation language is the one configured in the users account settings.
      See [Gist locale parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_locale).""")
  String locale = "";

  @OpenApi.Description(
      """
      The extent of fields to include when no specific list of fields is provided using `fields` so that  that listed fields are automatically determined.
      See [Gist auto parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#the-auto-parameter).""")
  GistAutoType auto;

  @OpenApi.Description(
      """
      Opt-out of paging. Only permitted for organisation units with simple `fields` list.""")
  boolean paging = true;

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

  @OpenApi.Description(
      """
      Fields like _name_ or _shortName_ can be translated (internationalised).
      By default, any translatable field that has a translation is returned translated given that the user requesting the gist has an interface language configured.
      To return the plain non-translated field use `translate=false`.
      See [Gist translate parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_translate).""")
  boolean translate = true;

  @OpenApi.Description(
      """
      Inverse can be used in context of a collection field gist of the form /api/<object-type>/<object-id>/<field-name>/gist to not list all items that are contained in the member collection but all items that are not contained in the member collection.
      See [Gist inverse parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#the-inverse-parameter).""")
  boolean inverse = false;

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
      Use absolute (`true`) or relative URLs (`false`, default) when linking to other objects in `apiEndpoints`.
      See [Gist absoluteUrls parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_absoluteUrls).""")
  boolean absoluteUrls = false;

  @OpenApi.Description(
      """
      Endpoints returning a list by default wrap the items with an envelope containing the pager and the list, which is named according to the type of object listed.
      See [Gist headless parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_headless).""")
  boolean headless;

  @OpenApi.Description(
      "When `true` the query is not executed but the planned execution is described back similar to using _describe_ in SQL/database context.")
  boolean describe = false;

  @OpenApi.Description(
      """
      By default, the Gist API includes links to referenced objects. This can be disabled by using `references=false`.""")
  boolean references = true;

  @OpenApi.Description(
      """
      Combine `filter`s with `AND` (default) or `OR` logic combinator
      See [Gist rootJunction parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_rootJunction).""")
  Junction.Type rootJunction = Junction.Type.AND;

  @OpenApi.Description(
      """
      A comma seperated list of fields to include in the response. `*` includes all `auto` detected fields.
      See [Gist fields parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_fields).""")
  @OpenApi.Shared.Inline
  @OpenApi.Property(OpenApi.PropertyNames[].class)
  String fields;

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

  public GistAutoType getAuto(GistAutoType defaultValue) {
    return auto == null ? defaultValue : auto;
  }

  @JsonIgnore
  public boolean isCountTotalPages() throws BadRequestException {
    if (totalPages != null && total != null && totalPages != total)
      throw new BadRequestException(
          "totalPages and total request parameters are contradicting each other");
    if (totalPages != null) return totalPages;
    if (total != null) return total;
    return false;
  }
}
