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

import static org.hisp.dhis.webapi.controller.tracker.export.FieldFilterRequestHandler.getRequestURL;
import static org.hisp.dhis.webapi.utils.HeaderUtils.X_CONTENT_TYPE_OPTIONS_VALUE;
import static org.hisp.dhis.webapi.utils.HeaderUtils.X_XSS_PROTECTION_VALUE;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.better.FieldsPredicate;
import org.hisp.dhis.fieldfiltering.better.FieldsPropertyFilter;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.FileResourceStream.Content;
import org.hisp.dhis.webapi.controller.tracker.export.ResponseHeader;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.utils.ResponseEntityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
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
 *   <li>serving `fields` filtered JSON
 *   <li>serving files and images given a {@link FileResourceStream}
 * </ul>
 *
 * <p>The tracker equivalent for serving `fields` filtered JSON is {@link
 * org.hisp.dhis.webapi.controller.AbstractFullReadOnlyController#getObjectListInternal}. Tracker
 * can currently not reuse the {@link org.hisp.dhis.common.Pager} as not all tracker endpoints
 * support or should support getting totals. Tracker exporters also do not return totals by default.
 * Metadata does not allow disabling totals. This is the reason why we are not reusing the {@code
 * StreamingJsonRoot}.
 */
@Component
@RequiredArgsConstructor
public class RequestHandler {
  private static final CacheControl CACHE_CONTROL_DIRECTIVES =
      CacheControl.noCache().cachePrivate();

  private final DhisConfigurationProvider dhisConfig;

  private final FieldFilterService fieldFilterService;

  @Qualifier("jsonFilterMapper")
  private final ObjectMapper filterMapper;

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

  public <T> void serve(
      HttpServletRequest request,
      HttpServletResponse response,
      String key,
      org.hisp.dhis.tracker.Page<Event> page,
      FieldsPredicate fieldsPredicate)
      throws IOException {

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    // TODO(ivo) can we encapsulate this into a Spring mechanism like a converter? Or is it good
    // enough to have this here in the handler?
    ObjectWriter objectWriter =
        filterMapper
            .writer()
            .withAttribute(FieldsPropertyFilter.PREDICATE_ATTRIBUTE, fieldsPredicate);

    try (JsonGenerator generator =
        objectWriter.getFactory().createGenerator(response.getOutputStream())) {
      objectWriter.writeValue(generator, Page.withPager(key, page, getRequestURL(request)));
    }
  }

  public <T> ResponseEntity<Page<ObjectNode>> serve(
      HttpServletRequest request,
      String key,
      org.hisp.dhis.tracker.Page<T> page,
      FieldsRequestParam fieldParams) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Page.withPager(
                key,
                page.withMappedItems(
                    i -> fieldFilterService.toObjectNode(i, fieldParams.getFields())),
                getRequestURL(request)));
  }

  public <T> void serve(
      HttpServletResponse response, String key, List<Event> items, FieldsPredicate fieldsPredicate)
      throws IOException {

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    // TODO(ivo) can we encapsulate this into a Spring mechanism like a converter? Or is it good
    // enough to have this here in the handler?
    ObjectWriter objectWriter =
        filterMapper
            .writer()
            .withAttribute(FieldsPropertyFilter.PREDICATE_ATTRIBUTE, fieldsPredicate);

    try (JsonGenerator generator =
        objectWriter.getFactory().createGenerator(response.getOutputStream())) {
      objectWriter.writeValue(generator, Page.withoutPager(key, items));
    }
  }

  public <T> ResponseEntity<Page<ObjectNode>> serve(
      String key, List<T> items, FieldsRequestParam fieldsParam) {
    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(items, fieldsParam.getFields());
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(Page.withoutPager(key, objectNodes));
  }

  public <T> ResponseEntity<ObjectNode> serve(T item, List<FieldPath> fields) {
    return ResponseEntity.ok(fieldFilterService.toObjectNode(item, fields));
  }
}
