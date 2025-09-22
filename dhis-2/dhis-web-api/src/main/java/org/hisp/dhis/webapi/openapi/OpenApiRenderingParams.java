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
package org.hisp.dhis.webapi.openapi;

import lombok.Data;
import org.hisp.dhis.common.Maturity.Alpha;
import org.hisp.dhis.common.Maturity.Beta;
import org.hisp.dhis.common.Maturity.Stable;
import org.hisp.dhis.common.OpenApi;

@Data
@OpenApi.Shared
public class OpenApiRenderingParams {
  @Alpha boolean sortEndpointsByMethod = true; // make this a sortEndpointsBy=method,path,id thing?

  @Stable
  @OpenApi.Description("Include the JSON source in the operations and schemas (for debug purposes)")
  boolean source = false;

  @Beta
  @OpenApi.Description(
      """
    Values of a shared enum with less than the limit values will show the first n values up to limit
    directly where the type is used""")
  int inlineEnumsLimit = 0;

  @OpenApi.Ignore String contextPath = "";

  /**
   * @return part of the overall cache key added to reflect the rendering parameters for the HTML
   *     document cache
   */
  @OpenApi.Ignore
  String getCacheKey() {
    String key = "";
    if (source) key += "-s";
    if (inlineEnumsLimit != 0) key += "-e" + inlineEnumsLimit;
    if (sortEndpointsByMethod) key += "-s";
    return key;
  }
}
