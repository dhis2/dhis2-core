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
package org.hisp.dhis.webapi;

import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * Base class to create context free reusable snippets.
 *
 * <p>The purpose of {@link WebSnippet}s is to allow definition of sequences of {@link WebClient}
 * usage that become reusable building blocks to create more complex scenarios.
 *
 * @author Jan Bernitt
 * @param <T> optional return type of the snippet to send information back to the caller, like IDs,
 *     or {@link org.hisp.dhis.webapi.json.JsonValue}s
 */
public abstract class WebSnippet<T> implements WebClient {

  private final WebClient client;

  public WebSnippet(WebClient client) {
    this.client = client;
  }

  @Override
  public final HttpResponse webRequest(
      HttpMethod method, String url, List<Header> headers, MediaType contentType, String content) {
    return client.webRequest(method, url, headers, contentType, content);
  }

  /**
   * Runs the snippet.
   *
   * @return a optional result value, like an ID or a {@link org.hisp.dhis.webapi.json.JsonValue}
   *     that can be used by the caller to continue working with data created or used in this
   *     snippet.
   */
  protected abstract T run();
}
