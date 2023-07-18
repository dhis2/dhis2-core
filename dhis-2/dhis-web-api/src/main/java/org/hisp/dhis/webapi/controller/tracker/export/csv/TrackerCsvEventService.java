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
package org.hisp.dhis.webapi.controller.tracker.export.csv;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.controller.tracker.view.DataValue;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.User;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;

/**
 * @author Enrico Colasante
 */
@Service("org.hisp.dhis.webapi.controller.tracker.export.csv.CsvEventService")
public class TrackerCsvEventService implements CsvEventService<Event> {
  private static final CsvMapper CSV_MAPPER =
      new CsvMapper().enable(CsvParser.Feature.WRAP_AS_ARRAY);

  private static final Pattern TRIM_SINGLE_QUOTES = Pattern.compile("^'|'$");

  @Override
  public void writeEvents(OutputStream outputStream, List<Event> events, boolean withHeader)
      throws IOException {
    final CsvSchema csvSchema =
        CSV_MAPPER
            .schemaFor(CsvEventDataValue.class)
            .withLineSeparator("\n")
            .withUseHeader(withHeader);

    ObjectWriter writer = CSV_MAPPER.writer(csvSchema.withUseHeader(withHeader));

    List<CsvEventDataValue> dataValues = new ArrayList<>();

    for (Event event : events) {
      CsvEventDataValue templateDataValue = new CsvEventDataValue();
      templateDataValue.setEvent(event.getEvent());
      templateDataValue.setStatus(event.getStatus() != null ? event.getStatus().name() : null);
      templateDataValue.setProgram(event.getProgram());
      templateDataValue.setProgramStage(event.getProgramStage());
      templateDataValue.setEnrollment(event.getEnrollment());
      templateDataValue.setOrgUnit(event.getOrgUnit());
      templateDataValue.setOrgUnitName(event.getOrgUnitName());
      templateDataValue.setOccurredAt(
          event.getOccurredAt() == null ? null : event.getOccurredAt().toString());
      templateDataValue.setScheduledAt(
          event.getScheduledAt() == null ? null : event.getScheduledAt().toString());
      templateDataValue.setFollowup(event.isFollowup());
      templateDataValue.setDeleted(event.isDeleted());
      templateDataValue.setCreatedAt(
          event.getCreatedAt() == null ? null : event.getCreatedAt().toString());
      templateDataValue.setCreatedAtClient(
          event.getCreatedAtClient() == null ? null : event.getCreatedAtClient().toString());
      templateDataValue.setUpdatedAt(
          event.getUpdatedAt() == null ? null : event.getUpdatedAt().toString());
      templateDataValue.setUpdatedAtClient(
          event.getUpdatedAtClient() == null ? null : event.getUpdatedAtClient().toString());
      templateDataValue.setCompletedAt(
          event.getCompletedAt() == null ? null : event.getCompletedAt().toString());
      templateDataValue.setUpdatedBy(
          event.getUpdatedBy() == null ? null : event.getUpdatedBy().getUsername());
      templateDataValue.setStoredBy(event.getStoredBy());
      templateDataValue.setCompletedAt(
          event.getCompletedAt() == null ? null : event.getCompletedAt().toString());
      templateDataValue.setCompletedBy(event.getCompletedBy());
      templateDataValue.setAttributeOptionCombo(event.getAttributeOptionCombo());
      templateDataValue.setAttributeCategoryOptions(event.getAttributeCategoryOptions());
      templateDataValue.setAssignedUser(
          event.getAssignedUser() == null ? null : event.getAssignedUser().getUsername());

      if (event.getGeometry() != null) {
        templateDataValue.setGeometry(event.getGeometry().toText());

        if (event.getGeometry().getGeometryType().equals("Point")) {
          templateDataValue.setLongitude(event.getGeometry().getCoordinate().x);
          templateDataValue.setLatitude(event.getGeometry().getCoordinate().y);
        }
      }

      if (event.getDataValues().isEmpty()) {
        dataValues.add(templateDataValue);
        continue;
      }

      for (DataValue value : event.getDataValues()) {
        CsvEventDataValue dataValue = new CsvEventDataValue(templateDataValue);
        dataValue.setDataElement(value.getDataElement());
        dataValue.setValue(value.getValue());
        dataValue.setProvidedElsewhere(value.isProvidedElsewhere());
        dataValue.setCreatedAtDataValue(
            value.getCreatedAt() == null ? null : value.getCreatedAt().toString());
        dataValue.setUpdatedAtDataValue(
            value.getUpdatedAt() == null ? null : value.getUpdatedAt().toString());

        if (value.getStoredBy() != null) {
          dataValue.setStoredBy(value.getStoredBy());
        }

        dataValues.add(dataValue);
      }
    }

    writer.writeValue(outputStream, dataValues);
  }

  @Override
  public List<Event> readEvents(InputStream inputStream, boolean skipFirst)
      throws IOException, ParseException {
    CsvSchema csvSchema = CsvSchema.emptySchema().withHeader().withColumnReordering(true);

    if (!skipFirst) {
      csvSchema =
          CSV_MAPPER.schemaFor(CsvEventDataValue.class).withoutHeader().withColumnReordering(true);
    }

    List<Event> events = Lists.newArrayList();

    ObjectReader reader = CSV_MAPPER.readerFor(CsvEventDataValue.class).with(csvSchema);

    MappingIterator<CsvEventDataValue> iterator = reader.readValues(inputStream);
    Event event = new Event();

    while (iterator.hasNext()) {
      CsvEventDataValue dataValue = iterator.next();

      if (!Objects.equals(event.getEvent(), dataValue.getEvent())) {
        event = new Event();
        event.setEvent(dataValue.getEvent());
        event.setStatus(
            StringUtils.isEmpty(dataValue.getStatus())
                ? EventStatus.ACTIVE
                : Enum.valueOf(EventStatus.class, dataValue.getStatus()));
        event.setProgram(dataValue.getProgram());
        event.setProgramStage(dataValue.getProgramStage());
        event.setEnrollment(dataValue.getEnrollment());
        event.setOrgUnit(dataValue.getOrgUnit());
        event.setCreatedAt(DateUtils.instantFromDateAsString(dataValue.getCreatedAt()));
        event.setCreatedAtClient(DateUtils.instantFromDateAsString(dataValue.getCreatedAtClient()));
        event.setUpdatedAt(DateUtils.instantFromDateAsString(dataValue.getUpdatedAt()));
        event.setUpdatedAtClient(DateUtils.instantFromDateAsString(dataValue.getUpdatedAtClient()));
        event.setOccurredAt(DateUtils.instantFromDateAsString(dataValue.getOccurredAt()));
        event.setScheduledAt(DateUtils.instantFromDateAsString(dataValue.getScheduledAt()));
        event.setCompletedAt(DateUtils.instantFromDateAsString(dataValue.getCompletedAt()));
        event.setCompletedBy(dataValue.getCompletedBy());
        event.setStoredBy(dataValue.getStoredBy());
        event.setAttributeOptionCombo(dataValue.getAttributeOptionCombo());
        event.setAttributeCategoryOptions(dataValue.getAttributeCategoryOptions());
        event.setAssignedUser(User.builder().username(dataValue.getAssignedUser()).build());

        if (StringUtils.isNotBlank(dataValue.getGeometry())) {
          event.setGeometry(
              new WKTReader()
                  .read(TRIM_SINGLE_QUOTES.matcher(dataValue.getGeometry()).replaceAll("")));
        } else if (dataValue.getLongitude() != null && dataValue.getLatitude() != null) {
          event.setGeometry(
              new WKTReader()
                  .read("Point(" + dataValue.getLongitude() + " " + dataValue.getLatitude() + ")"));
        }

        events.add(event);
      }

      if (ObjectUtils.anyNotNull(
          dataValue.getProvidedElsewhere(),
          dataValue.getDataElement(),
          dataValue.getValue(),
          dataValue.getCreatedAtDataValue(),
          dataValue.getUpdatedAtDataValue(),
          dataValue.getStoredByDataValue())) {
        DataValue value = new DataValue();
        value.setProvidedElsewhere(
            dataValue.getProvidedElsewhere() != null && dataValue.getProvidedElsewhere());
        value.setDataElement(dataValue.getDataElement());
        value.setValue(dataValue.getValue());
        value.setCreatedAt(DateUtils.instantFromDateAsString(dataValue.getCreatedAtDataValue()));
        value.setUpdatedAt(DateUtils.instantFromDateAsString(dataValue.getUpdatedAtDataValue()));
        value.setStoredBy(dataValue.getStoredByDataValue());
        event.getDataValues().add(value);
      }
    }

    return events;
  }
}
