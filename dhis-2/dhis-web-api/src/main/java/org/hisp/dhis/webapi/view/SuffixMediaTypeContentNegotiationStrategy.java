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
package org.hisp.dhis.webapi.view;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;

/**
 * Resolves the requested {@link MediaType} from a URL path extension (e.g. {@code .json}, {@code
 * .xml}, {@code .adx.xml.gz}).
 *
 * <p>Spring Framework 7.0 removes path-extension content negotiation ({@code
 * PathExtensionContentNegotiationStrategy}, {@code favorPathExtension}, suffix-pattern flags). To
 * preserve DHIS2's {@code /api/resource.json} contract, {@code CustomRequestMappingHandlerMapping}
 * records the resolved media type on the request when it falls back to a suffix-stripped path. This
 * strategy reads that attribute during content negotiation.
 *
 * <p>Forward-compatible on Spring 6.2 (Spring 7 readiness PR-F).
 *
 * @author Morten Svanæs
 */
public class SuffixMediaTypeContentNegotiationStrategy implements ContentNegotiationStrategy {
  /** Request attribute holding the {@link MediaType} resolved from the URL path extension. */
  public static final String SUFFIX_MEDIA_TYPE_ATTRIBUTE =
      SuffixMediaTypeContentNegotiationStrategy.class.getName() + ".SUFFIX_MEDIA_TYPE";

  @Override
  public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) {
    MediaType mediaType =
        (MediaType)
            webRequest.getAttribute(SUFFIX_MEDIA_TYPE_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

    return mediaType != null ? List.of(mediaType) : MEDIA_TYPE_ALL_LIST;
  }
}
