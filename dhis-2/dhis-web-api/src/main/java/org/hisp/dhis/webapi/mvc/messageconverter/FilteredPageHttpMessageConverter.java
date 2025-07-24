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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.fieldfiltering.better.Fields;
import org.hisp.dhis.fieldfiltering.better.FieldsPropertyFilter;
import org.hisp.dhis.webapi.controller.tracker.view.FilteredPage;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * HttpMessageConverter for trackers {@link FilteredPage} that handles streaming of field filtered
 * JSON pages directly to the HTTPs response body's output stream.
 */
@Component
@RequiredArgsConstructor
public class FilteredPageHttpMessageConverter
    extends AbstractHttpMessageConverter<FilteredPage<?>> {

  @Qualifier("jsonFilterMapper")
  private final ObjectMapper filterMapper;

  @Override
  protected boolean supports(@Nonnull Class<?> clazz) {
    return FilteredPage.class.isAssignableFrom(clazz);
  }

  @Nonnull
  @Override
  protected FilteredPage<?> readInternal(
      @Nonnull Class<? extends FilteredPage<?>> clazz, @Nonnull HttpInputMessage inputMessage)
      throws HttpMessageNotReadableException {
    throw new UnsupportedOperationException("Reading FilteredPage is not supported");
  }

  @Override
  protected void writeInternal(
      @Nonnull FilteredPage<?> filteredPage, @Nonnull HttpOutputMessage outputMessage)
      throws IOException, HttpMessageNotWritableException {

    if (outputMessage instanceof StreamingHttpOutputMessage) {
      writeStreaming(filteredPage, (StreamingHttpOutputMessage) outputMessage);
    } else {
      writeStandard(filteredPage, outputMessage);
    }
  }

  private void writeStreaming(
      FilteredPage<?> filteredPage, StreamingHttpOutputMessage streamingMessage) {
    streamingMessage.setBody(
        outputStream -> {
          try {
            writePageToStream(filteredPage, outputStream);
          } catch (IOException e) {
            throw new RuntimeException("Failed to write streaming response", e);
          }
        });
  }

  private void writeStandard(FilteredPage<?> filteredPage, HttpOutputMessage outputMessage)
      throws IOException {
    writePageToStream(filteredPage, outputMessage.getBody());
  }

  private void writePageToStream(FilteredPage<?> filteredPage, OutputStream outputStream)
      throws IOException {
    // TODO(ivo) will this properly set the content type?
    Page<?> page = filteredPage.page();
    Fields pageFields = createPageFields(page.getKey(), filteredPage.fields());

    ObjectWriter writer =
        filterMapper.writer().withAttribute(FieldsPropertyFilter.FIELDS_ATTRIBUTE, pageFields);

    try (JsonGenerator generator = writer.getFactory().createGenerator(outputStream)) {
      writer.writeValue(generator, page);
    }
  }

  /**
   * Creates a predicate wrapper equivalent to how the items are wrapped into a page. Users write
   * the {@code fields} filter from the perspective of an item in the collection of the resource
   * they are asking for. For example {@code /events?fields=event,orgUnit} while the response is
   * actually an object with a {@code pager} and an {@code events} collection. We thus need to
   * pretend the user sent {@code fields=events[event,orgUnit]}.
   */
  private Fields createPageFields(String key, Fields fields) {
    // TODO(ivo) improve the Fields API to make this easier. Do I also need to worry about safety?
    // or not as the effective computation is done internally so "odd/invalid" fields can be put in
    // but will be handled correctly
    // Include pager field with all sub-properties
    // Include dynamic key (e.g., "events") with user's filtering predicate
    Fields pageFields =
        new Fields(
            false,
            Set.of("pager", key),
            Set.of(),
            Map.of("pager", Fields.all(), key, fields),
            Map.of());
    return pageFields;
  }
}
