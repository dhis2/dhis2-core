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
package org.hisp.dhis.webapi.mvc.messageconverter;

import static org.hisp.dhis.webapi.utils.ContextUtils.BINARY_HEADER_CONTENT_TRANSFER_ENCODING;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON_GZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON_ZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.HEADER_CONTENT_DISPOSITION;
import static org.hisp.dhis.webapi.utils.ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nonnull;
import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsPropertyFilter;
import org.hisp.dhis.webapi.controller.tracker.view.FilteredEntity;
import org.hisp.dhis.webapi.controller.tracker.view.FilteredPage;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * HttpMessageConverter for trackers {@link FilteredPage} and {@link FilteredEntity} that handles
 * streaming of field filtered JSON pages and entities directly to the HTTP response body's output
 * stream. Supports compression variants (gzip, zip) based on media type for pages.
 */
@Component
public class FilteredPageHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

  private static final org.springframework.http.MediaType MEDIA_TYPE_JSON_GZIP =
      org.springframework.http.MediaType.valueOf(CONTENT_TYPE_JSON_GZIP);
  private static final org.springframework.http.MediaType MEDIA_TYPE_JSON_ZIP =
      org.springframework.http.MediaType.valueOf(CONTENT_TYPE_JSON_ZIP);

  @Qualifier("jsonFilterMapper")
  private final ObjectMapper filterMapper;

  public FilteredPageHttpMessageConverter(
      @Qualifier("jsonFilterMapper") ObjectMapper filterMapper) {
    super(
        org.springframework.http.MediaType.APPLICATION_JSON,
        org.springframework.http.MediaType
            .TEXT_HTML, // return JSON when a generic request comes from a browser
        MEDIA_TYPE_JSON_GZIP,
        MEDIA_TYPE_JSON_ZIP);
    this.filterMapper = filterMapper;
  }

  @Override
  protected boolean supports(@Nonnull Class<?> clazz) {
    return FilteredPage.class.isAssignableFrom(clazz)
        || FilteredEntity.class.isAssignableFrom(clazz);
  }

  @Nonnull
  @Override
  protected Object readInternal(@Nonnull Class<?> clazz, @Nonnull HttpInputMessage inputMessage)
      throws HttpMessageNotReadableException {
    throw new UnsupportedOperationException("Reading filtered objects is not supported");
  }

  @Override
  protected void writeInternal(
      @Nonnull Object filteredObject, @Nonnull HttpOutputMessage outputMessage)
      throws IOException, HttpMessageNotWritableException {
    setCompressionHeaders(filteredObject, outputMessage);
    setContentType(outputMessage);

    if (outputMessage instanceof StreamingHttpOutputMessage streamingHttpOutputMessage) {
      writeStreaming(filteredObject, streamingHttpOutputMessage);
    } else {
      writeStandard(filteredObject, outputMessage);
    }
  }

  private static void setCompressionHeaders(
      Object filteredObject, HttpOutputMessage outputMessage) {
    org.springframework.http.MediaType mediaType = outputMessage.getHeaders().getContentType();
    if (MEDIA_TYPE_JSON_GZIP.isCompatibleWith(mediaType)
        || MEDIA_TYPE_JSON_ZIP.isCompatibleWith(mediaType)) {
      String filename = getFilename(filteredObject, mediaType);
      outputMessage
          .getHeaders()
          .set(HEADER_CONTENT_DISPOSITION, "attachment; filename=" + filename);
      outputMessage
          .getHeaders()
          .set(HEADER_CONTENT_TRANSFER_ENCODING, BINARY_HEADER_CONTENT_TRANSFER_ENCODING);
    }
  }

  private static String getFilename(
      Object filteredObject, org.springframework.http.MediaType mediaType) {
    String baseName = getBaseName(filteredObject);

    if (MEDIA_TYPE_JSON_GZIP.isCompatibleWith(mediaType)) {
      return baseName + ".gz";
    } else if (MEDIA_TYPE_JSON_ZIP.isCompatibleWith(mediaType)) {
      return baseName + ".zip";
    }
    return baseName;
  }

  private static String getBaseName(Object filteredObject) {
    if (filteredObject instanceof FilteredPage<?> filteredPage) {
      return filteredPage.page().getKey() + ".json";
    }

    throw new UnsupportedOperationException("Only FilteredPage supports compressed responses");
  }

  private static void setContentType(HttpOutputMessage outputMessage) {
    // Set content-type to application/json for text/html as that is what we return. Rely on
    // Springs' default content negotiation otherwise.
    MediaType contentType = outputMessage.getHeaders().getContentType();
    if (MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
      outputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    }
  }

  private void writeStreaming(Object filteredObject, StreamingHttpOutputMessage streamingMessage) {
    streamingMessage.setBody(
        outputStream ->
            writeObjectToStream(
                filteredObject, outputStream, streamingMessage.getHeaders().getContentType()));
  }

  private void writeStandard(Object filteredObject, HttpOutputMessage outputMessage)
      throws IOException {
    writeObjectToStream(
        filteredObject, outputMessage.getBody(), outputMessage.getHeaders().getContentType());
  }

  private void writeObjectToStream(
      Object filteredObject,
      OutputStream outputStream,
      org.springframework.http.MediaType mediaType)
      throws IOException {
    OutputStream targetStream = wrapStreamForCompression(filteredObject, outputStream, mediaType);

    try {
      if (filteredObject instanceof FilteredPage<?> filteredPage) {
        writePageToStream(filteredPage, targetStream);
      } else if (filteredObject instanceof FilteredEntity<?> filteredEntity) {
        writeEntityToStream(filteredEntity, targetStream);
      } else {
        throw new IllegalArgumentException(
            "Unsupported filtered object type: " + filteredObject.getClass());
      }
    } finally {
      if (targetStream != outputStream) { // only close a stream we created
        targetStream.close();
      }
    }
  }

  private OutputStream wrapStreamForCompression(
      Object filteredObject, OutputStream original, MediaType mediaType) throws IOException {
    if (MEDIA_TYPE_JSON_GZIP.isCompatibleWith(mediaType)) {
      return new GZIPOutputStream(original);
    } else if (MEDIA_TYPE_JSON_ZIP.isCompatibleWith(mediaType)) {
      ZipOutputStream zip = new ZipOutputStream(original);
      zip.putNextEntry(new ZipEntry(getBaseName(filteredObject)));
      return zip;
    }
    return original;
  }

  private void writePageToStream(FilteredPage<?> filteredPage, OutputStream outputStream)
      throws IOException {
    Page<?> page = filteredPage.page();
    Fields pageFields = createPageFields(filteredPage.fields());

    ObjectWriter writer =
        filterMapper.writer().withAttribute(FieldsPropertyFilter.FIELDS_ATTRIBUTE, pageFields);
    try (JsonGenerator generator = writer.getFactory().createGenerator(outputStream)) {
      writer.writeValue(generator, page);
    }
  }

  /**
   * Creates a fields wrapper equivalent to how the items are wrapped into a page. Users write the
   * {@code fields} filter from the perspective of an item in the collection of the resource they
   * are asking for. For example {@code /events?fields=event,orgUnit} while the response is actually
   * an object with a {@code pager} and an {@code events} collection. We thus need to pretend the
   * user sent {@code fields=events[event,orgUnit]}.
   */
  private static Fields createPageFields(Fields fields) {
    return new Fields(
        false,
        Set.of("pager", "getDynamicItems"),
        Map.of("pager", Fields.all(), "getDynamicItems", fields),
        Map.of());
  }

  private void writeEntityToStream(FilteredEntity<?> filteredEntity, OutputStream outputStream)
      throws IOException {
    Object entity = filteredEntity.entity();
    Fields fields = filteredEntity.fields();

    ObjectWriter writer =
        filterMapper.writer().withAttribute(FieldsPropertyFilter.FIELDS_ATTRIBUTE, fields);

    try (JsonGenerator generator = writer.getFactory().createGenerator(outputStream)) {
      writer.writeValue(generator, entity);
    }
  }
}
