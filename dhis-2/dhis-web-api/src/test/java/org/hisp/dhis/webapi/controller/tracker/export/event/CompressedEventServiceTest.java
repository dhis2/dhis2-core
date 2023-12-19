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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompressedEventServiceTest {

  private CompressedEventService compressedEventService;
  private static final Event FIRST_EVENT = new Event();
  private static final Event SECOND_EVENT = new Event();
  @InjectMocks private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    compressedEventService = new CompressedEventService(objectMapper);
    FIRST_EVENT.setEvent(CodeGenerator.generateUid());
    SECOND_EVENT.setEvent(CodeGenerator.generateUid());
  }

  @Test
  void shouldUnzipFileAndMatchEvents_whenCreateZipFileFromEventList() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    List<Event> eventToZip = getEvents();

    compressedEventService.writeZip(outputStream, eventToZip, "file.json.zip");

    ZipInputStream zipInputStream =
        new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
    var buff = new byte[1024];
    List<Event> eventsFromZip;

    ZipEntry zipEntry = zipInputStream.getNextEntry();

    assertNotNull(zipEntry, "Events Zip file has no entry");
    assertEquals("file.json.zip", zipEntry.getName(), "Events Zip file has a wrong name");

    var byteArrayOutputStream = new ByteArrayOutputStream();
    int l;
    while ((l = zipInputStream.read(buff)) > 0) {
      byteArrayOutputStream.write(buff, 0, l);
    }
    eventsFromZip =
        objectMapper.readValue(byteArrayOutputStream.toString(), new TypeReference<>() {});

    assertNull(zipInputStream.getNextEntry()); // assert only one file is created
    assertEquals(eventToZip.size(), eventsFromZip.size());
    assertEquals(
        FIRST_EVENT,
        eventsFromZip.stream()
            .filter(e -> e.getEvent().equals(FIRST_EVENT.getEvent()))
            .findAny()
            .orElse(null),
        "The event does not match or does not exist in the Zip File.");
    assertEquals(
        SECOND_EVENT,
        eventsFromZip.stream()
            .filter(e -> e.getEvent().equals(SECOND_EVENT.getEvent()))
            .findAny()
            .orElse(null),
        "The event does not match or does not exist in the Zip File.");
  }

  @Test
  void shouldGUnzipFileAndMatchEvents_whenCreateGZipFileFromEventList() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    List<Event> eventToGZip = getEvents();

    compressedEventService.writeGzip(outputStream, eventToGZip);

    GZIPInputStream gzipInputStream =
        new GZIPInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
    var buff = new byte[1024];
    List<Event> eventsFromGZip;

    var byteArrayOutputStream = new ByteArrayOutputStream();
    int l;
    while ((l = gzipInputStream.read(buff)) > 0) {
      byteArrayOutputStream.write(buff, 0, l);
    }
    eventsFromGZip =
        objectMapper.readValue(byteArrayOutputStream.toString(), new TypeReference<>() {});

    assertEquals(eventToGZip.size(), eventsFromGZip.size());
    assertEquals(
        FIRST_EVENT,
        eventToGZip.stream()
            .filter(e -> e.getEvent().equals(FIRST_EVENT.getEvent()))
            .findAny()
            .orElse(null),
        "The event does not match or does not exist in the GZip File.");
    assertEquals(
        SECOND_EVENT,
        eventToGZip.stream()
            .filter(e -> e.getEvent().equals(SECOND_EVENT.getEvent()))
            .findAny()
            .orElse(null),
        "The event does not match or does not exist in the GZip File.");
  }

  List<Event> getEvents() {
    return List.of(FIRST_EVENT, SECOND_EVENT);
  }
}
