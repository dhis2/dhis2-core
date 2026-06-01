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
package org.hisp.dhis.webapi.view;

import java.util.List;
import org.hisp.dhis.webapi.filter.MediaTypeSuffixFilter;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;

/**
 * Resolves the requested {@link MediaType} from the URL path extension (e.g. {@code .json},
 * {@code .xml}, {@code adx.xml.gz}).
 *
 * <p>Spring Framework 7.0 removed path-extension content negotiation entirely (the
 * {@code PathExtensionContentNegotiationStrategy} base class and the {@code favorPathExtension} /
 * {@code setUseRegisteredSuffixPatternMatch} flags). To preserve DHIS2's long-standing {@code
 * /api/resource.json} behaviour, {@link MediaTypeSuffixFilter} resolves the extension to a media
 * type up-front and stores it as a request attribute (while stripping the suffix from the path so
 * handler mapping still matches the extension-less mapping). This strategy simply reads that
 * attribute back during content negotiation.
 *
 * @author Morten Olav Hansen
 */
public class SuffixMediaTypeContentNegotiationStrategy implements ContentNegotiationStrategy {
  @Override
  public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) {
    MediaType mediaType =
        (MediaType)
            webRequest.getAttribute(
                MediaTypeSuffixFilter.SUFFIX_MEDIA_TYPE_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

    return mediaType != null ? List.of(mediaType) : MEDIA_TYPE_ALL_LIST;
  }
}
