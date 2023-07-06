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
package org.hisp.dhis.dxf2.events.importer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.StreamUtils.copyToString;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Service responsible for wrapping the event importing process, pre-processing the input data.
 *
 * @author maikel arabori
 */
@Service
public class EventServiceFacade {
  private final EventImporter eventImporter;

  private final CurrentUserService currentUserService;

  private final ObjectMapper jsonMapper;

  private final ObjectMapper xmlMapper;

  public EventServiceFacade(
      final EventImporter eventImporter,
      final CurrentUserService currentUserService,
      final ObjectMapper jsonMapper,
      @Qualifier("xmlMapper") final ObjectMapper xmlMapper) {
    checkNotNull(eventImporter);
    checkNotNull(currentUserService);
    checkNotNull(jsonMapper);
    checkNotNull(xmlMapper);

    this.eventImporter = eventImporter;
    this.currentUserService = currentUserService;
    this.jsonMapper = jsonMapper;
    this.xmlMapper = xmlMapper;
  }

  public ImportSummaries addEventsXml(
      final InputStream inputStream,
      final JobConfiguration jobConfiguration,
      final ImportOptions importOptions)
      throws IOException {
    final String input = copyToString(inputStream, UTF_8);
    final List<Event> events = parseXmlEvents(input);

    return eventImporter.importAll(events, updateImportOptions(importOptions), jobConfiguration);
  }

  public ImportSummaries addEventsJson(
      final InputStream inputStream,
      final JobConfiguration jobConfiguration,
      final ImportOptions importOptions)
      throws IOException {
    final String input = copyToString(inputStream, UTF_8);
    final List<Event> events = parseJsonEvents(input);

    return eventImporter.importAll(events, updateImportOptions(importOptions), jobConfiguration);
  }

  private ImportOptions updateImportOptions(ImportOptions importOptions) {
    if (importOptions == null) {
      importOptions = new ImportOptions();
    }

    if (importOptions.getUser() == null) {
      importOptions.setUser(currentUserService.getCurrentUser());
    }

    return importOptions;
  }

  private List<Event> parseXmlEvents(final String input) throws IOException {
    final List<Event> events = new ArrayList<>(0);

    try {
      final Events multiple = fromXml(input, Events.class);
      events.addAll(multiple.getEvents());
    } catch (JsonMappingException ex) {
      final Event single = fromXml(input, Event.class);
      events.add(single);
    }

    return events;
  }

  private List<Event> parseJsonEvents(final String input) throws IOException {
    final List<Event> events = new ArrayList<>(0);

    final JsonNode root = jsonMapper.readTree(input);

    if (root.get("events") != null) {
      final Events multiple = fromJson(input, Events.class);
      events.addAll(multiple.getEvents());
    } else {
      final Event single = fromJson(input, Event.class);
      events.add(single);
    }

    return events;
  }

  @SuppressWarnings("unchecked")
  private <T> T fromXml(final String input, final Class<?> clazz) throws IOException {
    return (T) xmlMapper.readValue(input, clazz);
  }

  @SuppressWarnings("unchecked")
  private <T> T fromJson(final String input, final Class<?> clazz) throws IOException {
    return (T) jsonMapper.readValue(input, clazz);
  }
}
