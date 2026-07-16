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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * @author Morten Svanæs
 */
class SuffixMediaTypeContentNegotiationStrategyTest {

  private final SuffixMediaTypeContentNegotiationStrategy strategy =
      new SuffixMediaTypeContentNegotiationStrategy();

  @Test
  void returnsAttributeWhenPresent() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dataElements.json");
    request.setAttribute(
        SuffixMediaTypeContentNegotiationStrategy.SUFFIX_MEDIA_TYPE_ATTRIBUTE,
        MediaType.APPLICATION_JSON);

    List<MediaType> mediaTypes = strategy.resolveMediaTypes(new ServletWebRequest(request));

    assertEquals(List.of(MediaType.APPLICATION_JSON), mediaTypes);
  }

  @Test
  void fallsBackToAllWhenAttributeMissing() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dataElements");

    List<MediaType> mediaTypes = strategy.resolveMediaTypes(new ServletWebRequest(request));

    assertSame(SuffixMediaTypeContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST, mediaTypes);
  }
}
