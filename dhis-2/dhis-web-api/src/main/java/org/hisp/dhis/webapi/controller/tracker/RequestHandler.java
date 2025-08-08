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
package org.hisp.dhis.webapi.controller.tracker;

import static org.hisp.dhis.webapi.utils.HeaderUtils.X_CONTENT_TYPE_OPTIONS_VALUE;
import static org.hisp.dhis.webapi.utils.HeaderUtils.X_XSS_PROTECTION_VALUE;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.FileResourceStream.Content;
import org.hisp.dhis.webapi.controller.tracker.export.ResponseHeader;
import org.hisp.dhis.webapi.utils.ResponseEntityUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Handles common tracker requests like
 *
 * <ul>
 *   <li>serving files and images given a {@link FileResourceStream}
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RequestHandler {
  private static final CacheControl CACHE_CONTROL_DIRECTIVES =
      CacheControl.noCache().cachePrivate();

  private final DhisConfigurationProvider dhisConfig;

  public ResponseEntity<InputStreamResource> serve(
      HttpServletRequest request, FileResourceStream file)
      throws ConflictException, BadRequestException {
    final String etag = file.uid();
    final String cspHeaders = dhisConfig.getProperty(ConfigurationKey.CSP_HEADER_VALUE);

    if (ResponseEntityUtils.checkNotModified(etag, request)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
          .cacheControl(CACHE_CONTROL_DIRECTIVES)
          .eTag(etag)
          .header("Content-Security-Policy", cspHeaders)
          .header("X-Content-Type-Options", X_CONTENT_TYPE_OPTIONS_VALUE)
          .header("X-XSS-Protection", X_XSS_PROTECTION_VALUE)
          .build();
    }

    Content content = file.contentSupplier().get();
    return ResponseEntity.ok()
        .cacheControl(CACHE_CONTROL_DIRECTIVES)
        .eTag(etag)
        .header("Content-Security-Policy", cspHeaders)
        .header("X-Content-Type-Options", X_CONTENT_TYPE_OPTIONS_VALUE)
        .header("X-XSS-Protection", X_XSS_PROTECTION_VALUE)
        .contentType(MediaType.valueOf(file.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION, ResponseHeader.contentDispositionInline(file.name()))
        .contentLength(content.length())
        .body(new InputStreamResource(content.stream()));
  }
}
