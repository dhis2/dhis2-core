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
package org.hisp.dhis.web.tomcat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.catalina.valves.ValveBase;

/**
 * Removes the remote IP header (for example {@code X-Forwarded-For}) once {@link RemoteIpValve} has
 * resolved the client address into {@code getRemoteAddr()}.
 *
 * <p>{@link RemoteIpValve} keeps the entries it does not trust in the header, and keeps the whole
 * header when the connecting peer is not a trusted proxy. Those entries are client supplied.
 * Application code that prefers the header over {@code getRemoteAddr()}, such as the API token IP
 * allow-list, would trust them. Without the header it reads the address the valve resolved.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class RemoteIpHeaderRemovalValve extends ValveBase {
  private final String remoteIpHeader;

  RemoteIpHeaderRemovalValve(String remoteIpHeader) {
    super(true);
    this.remoteIpHeader = remoteIpHeader;
  }

  @Override
  public void invoke(Request request, Response response) throws IOException, ServletException {
    request.getCoyoteRequest().getMimeHeaders().removeHeader(remoteIpHeader);
    getNext().invoke(request, response);
  }
}
