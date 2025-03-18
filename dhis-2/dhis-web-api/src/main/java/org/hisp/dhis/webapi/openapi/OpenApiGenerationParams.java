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
import org.hisp.dhis.common.Maturity.Beta;
import org.hisp.dhis.common.OpenApi;

/**
 * All parameters of the OpenAPI document generation that control the content of the generated
 * document.
 *
 * <p>This affects multiple stages. Both extraction of the {@link Api} and using the extracted
 * {@link Api} to generate a JSON/YAML OpenAPI document.
 *
 * @author Jan Bernitt
 */
@Data
@OpenApi.Shared
public class OpenApiGenerationParams {
  @OpenApi.Description(
      """
    Regenerate the response even if a cached response is available.
    Mainly used during development to see effects of hot-swap code changes.""")
  boolean skipCache = false;

  @OpenApi.Description(
      """
    Shared schema names must be unique.
    To ensure this either a unique name is picked (`false`) or the generation fails (`true`)""")
  boolean failOnNameClash = false;

  @OpenApi.Description(
      """
    Declarations can be logically inconsistent; for example a parameter that is both required and has a default value.
    Either this is ignored and only logs a warning (`false`) or the generation fails (`true`)""")
  boolean failOnInconsistency = false;

  @OpenApi.Description(
      """
    Annotations that narrow the rendered type to one of the super-types of the type declared will be ignored
    and as a consequence types occur fully expanded (with all their properties).
    Note that this does not affect types that are re-defined as a different type using annotations.""")
  @Beta
  @OpenApi.Since(42)
  boolean expandedRefs = false;

  @Beta
  @OpenApi.Since(42)
  @OpenApi.Description(
      """
    Set `true` to include non-standard OpenAPI properties supported by DHIS2
    (excluding `x-classifiers` which is switched separately by `includeXClassifiers`)""")
  boolean includeXProperties = false;

  /**
   * @return cache key to use for {@link Api} objects
   */
  @OpenApi.Ignore
  String getApiCacheKey() {
    return expandedRefs ? "full-extended" : "full-default";
  }

  /**
   * @return cache key used for generated JSON/YAML documents
   */
  @OpenApi.Ignore
  String getDocumentCacheKey() {
    return getApiCacheKey() + "-" + (includeXProperties ? "x" : "s");
  }
}
