/*
 * Copyright (c) 2004-2025, University of Oslo
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

import lombok.Data;
import org.hisp.dhis.common.OpenApi;

@Data
@OpenApi.Shared
public class GistObjectParams {

  @OpenApi.Description(
      """
      Switch translation language of display names. If not specified the translation language is the one configured in the users account settings.
      See [Gist locale parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_locale).""")
  String locale = "";

  @OpenApi.Description(
      """
      Fields like _name_ or _shortName_ can be translated (internationalised).
      By default, any translatable field that has a translation is returned translated given that the user requesting the gist has an interface language configured.
      To return the plain non-translated field use `translate=false`.
      See [Gist translate parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_translate).""")
  boolean translate = true;

  @OpenApi.Description(
      """
      The extent of fields to include when no specific list of fields is provided using `fields` so that  that listed fields are automatically determined.
      See [Gist auto parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#the-auto-parameter).""")
  GistAutoType auto;

  @OpenApi.Description(
      """
      Use absolute (`true`) or relative URLs (`false`, default) when linking to other objects in `apiEndpoints`.
      See [Gist absoluteUrls parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_absoluteUrls).""")
  boolean absoluteUrls = false;

  @OpenApi.Description(
      """
      By default, the Gist API includes links to referenced objects. This can be disabled by using `references=false`.""")
  boolean references = true;

  @OpenApi.Description(
      """
      A comma seperated list of fields to include in the response. `*` includes all `auto` detected fields.
      See [Gist fields parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_fields).""")
  @OpenApi.Shared.Inline
  @OpenApi.Property(OpenApi.PropertyNames[].class)
  String fields;

  public GistAutoType getAuto(GistAutoType defaultValue) {
    return auto == null ? defaultValue : auto;
  }
}
