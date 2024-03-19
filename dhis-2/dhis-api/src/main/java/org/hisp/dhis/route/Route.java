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
package org.hisp.dhis.route;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.auth.Auth;

/**
 * @author Morten Olav Hansen
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class Route extends BaseIdentifiableObject implements MetadataObject {
  public static final String PATH_WILDCARD_SUFFIX = "/**";

  @JsonProperty private String description;

  @JsonProperty(required = true)
  private boolean disabled;

  @JsonProperty(required = true)
  private String url;

  @JsonProperty(required = true)
  private Map<String, String> headers = new HashMap<>();

  @JsonProperty private Auth auth;

  @JsonProperty private List<String> authorities = new ArrayList<>();

  /**
   * If the route url ends with /** return true. Otherwise return false.
   *
   * @return true if the route is configured to allow subpaths
   */
  public boolean allowsSubpaths() {
    return url.endsWith(PATH_WILDCARD_SUFFIX);
  }

  /**
   * If this route supports sub-paths, return the base URL without the /** path wildcard suffix but
   * including the trailing slash. For example, if the url is configured as https://google.com/**,
   * return https://google.com/. If the route does not support sub-paths, return the full configured
   * url.
   *
   * @return the base url String
   */
  public String getBaseUrl() {
    if (allowsSubpaths()) {
      return url.substring(0, url.length() - PATH_WILDCARD_SUFFIX.length()) + "/";
    }
    return url;
  }
}
