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
package org.hisp.dhis.gist;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hisp.dhis.gist.GistQuery.Owner;
import org.hisp.dhis.schema.Schema;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Pager POJO for paging gist lists.
 *
 * @author Jan Bernitt
 */
@Getter
@AllArgsConstructor
public final class GistPager {
  @JsonProperty private final int page;

  @JsonProperty private final int pageSize;

  @JsonProperty private final Integer total;

  @JsonProperty private final String prevPage;

  @JsonProperty private final String nextPage;

  @JsonProperty
  public Integer getPageCount() {
    return total == null ? null : (int) Math.ceil(total / (double) pageSize);
  }

  public String toString() {
    return "[Page: " + page + " size: " + pageSize + "]";
  }

  public static URI computeBaseURL(
      GistQuery query, Map<String, String[]> params, Function<Class<?>, Schema> schemaByType) {
    UriBuilder url = UriComponentsBuilder.fromUriString(query.getEndpointRoot());
    Owner owner = query.getOwner();
    if (owner != null) {
      Schema o = schemaByType.apply(owner.getType());
      url.pathSegment(
          o.getRelativeApiEndpoint().substring(1),
          owner.getId(),
          o.getProperty(owner.getCollectionProperty()).key(),
          "gist");
    } else {
      Schema s = schemaByType.apply(query.getElementType());
      url.pathSegment(s.getRelativeApiEndpoint().substring(1), "gist");
    }
    params.forEach(url::queryParam);
    return url.build();
  }
}
