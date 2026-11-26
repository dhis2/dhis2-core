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
package org.hisp.dhis.tracker.imports.validation.validator;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1400;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1401;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * PROOF OF CONCEPT. Sends the events of every program that has the {@link #ATTRIBUTE_CODE}
 * attribute set to the URL in that attribute, one POST per URL, and reports the events the external
 * service rejects. 200 allows all; 409 with {@code {"rejected":[{"uid","reason"}]}} rejects the
 * listed events; anything else rejects all events sent to that URL (fail closed).
 */
@Slf4j
@Component("org.hisp.dhis.tracker.imports.validation.validator.ExternalAuthorizationValidator")
public class ExternalAuthorizationValidator implements Validator<TrackerBundle> {

  public static final String ATTRIBUTE_CODE = "EXTERNAL_AUTHORIZATION_URL";

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

  private final IdentifiableObjectManager manager;

  private final ObjectMapper objectMapper;

  private final Duration timeout;

  private final HttpClient httpClient;

  @Autowired
  public ExternalAuthorizationValidator(
      IdentifiableObjectManager manager, ObjectMapper objectMapper) {
    this(manager, objectMapper, DEFAULT_TIMEOUT);
  }

  ExternalAuthorizationValidator(
      IdentifiableObjectManager manager, ObjectMapper objectMapper, Duration timeout) {
    this.manager = manager;
    this.objectMapper = objectMapper;
    this.timeout = timeout;
    this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  @Override
  public void validate(Reporter reporter, TrackerBundle bundle, TrackerBundle input) {
    Attribute attribute = manager.getByCode(Attribute.class, ATTRIBUTE_CODE);
    if (attribute == null) {
      return;
    }

    Map<String, List<Event>> eventsByUrl = new LinkedHashMap<>();
    for (Event event : bundle.getEvents()) {
      Program program = resolveProgram(bundle.getPreheat(), event);
      String url = program == null ? null : program.getAttributeValues().get(attribute.getUid());
      if (StringUtils.isNotBlank(url)) {
        eventsByUrl.computeIfAbsent(url, k -> new ArrayList<>()).add(event);
      }
    }

    eventsByUrl.forEach((url, events) -> authorize(reporter, bundle, url, events));
  }

  @Override
  public boolean needsToRun(TrackerImportStrategy strategy) {
    return true;
  }

  private void authorize(Reporter reporter, TrackerBundle bundle, String url, List<Event> events) {
    Map<String, Event> byUid = new LinkedHashMap<>();
    events.forEach(e -> byUid.put(e.getUID().getValue(), e));

    HttpResponse<String> response;
    try {
      String body = objectMapper.writeValueAsString(requestBody(bundle, events));
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(timeout)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException | IllegalArgumentException e) {
      rejectAll(reporter, events, e.getClass().getSimpleName() + ": " + e.getMessage());
      return;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      rejectAll(reporter, events, "interrupted");
      return;
    }

    if (response.statusCode() == 200) {
      return;
    }
    if (response.statusCode() != 409) {
      rejectAll(reporter, events, "HTTP " + response.statusCode() + " from " + url);
      return;
    }

    JsonNode rejected;
    try {
      rejected = objectMapper.readTree(response.body()).path("rejected");
    } catch (IOException e) {
      rejectAll(reporter, events, "unreadable 409 response from " + url);
      return;
    }
    if (!rejected.isArray()) {
      rejectAll(reporter, events, "409 response without rejected list from " + url);
      return;
    }
    for (JsonNode r : rejected) {
      Event event = byUid.get(r.path("uid").asText());
      if (event != null) {
        reporter.addError(event, E1400, r.path("reason").asText("no reason given"));
      }
    }
  }

  private static void rejectAll(Reporter reporter, List<Event> events, String reason) {
    log.warn("External authorization failed, rejecting {} events: {}", events.size(), reason);
    events.forEach(e -> reporter.addError(e, E1401, reason));
  }

  private Map<String, Object> requestBody(TrackerBundle bundle, List<Event> events) {
    TrackerPreheat preheat = bundle.getPreheat();
    List<Map<String, Object>> items = new ArrayList<>();
    for (Event event : events) {
      TrackerImportStrategy strategy = bundle.getStrategy(event);
      if (strategy == null) {
        strategy = bundle.getImportStrategy();
      }
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("operation", strategy.name());
      if (!strategy.isDelete()) {
        item.put("event", fromPayload(preheat, event));
      }
      Map<String, Object> existing = fromStored(preheat, event.getUID());
      if (!strategy.isCreate() && existing != null) {
        item.put("existing", existing);
      }
      items.add(item);
    }

    UserDetails user = bundle.getUser();
    Map<String, Object> userMap = new LinkedHashMap<>();
    userMap.put("uid", user.getUid());
    userMap.put("username", user.getUsername());
    userMap.put("userGroups", sorted(user.getUserGroupIds()));
    userMap.put("userRoles", sorted(user.getUserRoleIds()));
    userMap.put("organisationUnits", sorted(user.getUserOrgUnitIds()));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("user", userMap);
    body.put("events", items);
    return body;
  }

  private static List<String> sorted(Set<String> values) {
    return values == null ? List.of() : values.stream().sorted().toList();
  }

  private static Program resolveProgram(TrackerPreheat preheat, Event event) {
    if (event.getProgram() != null && event.getProgram().isNotBlank()) {
      Program program = preheat.getProgram(event.getProgram());
      if (program != null) {
        return program;
      }
    }
    if (event.getProgramStage() != null && event.getProgramStage().isNotBlank()) {
      ProgramStage stage = preheat.getProgramStage(event.getProgramStage());
      if (stage != null && stage.getProgram() != null) {
        return stage.getProgram();
      }
    }
    TrackerEvent trackerEvent = preheat.getTrackerEvent(event.getUID());
    if (trackerEvent != null && trackerEvent.getProgramStage() != null) {
      return trackerEvent.getProgramStage().getProgram();
    }
    SingleEvent singleEvent = preheat.getSingleEvent(event.getUID());
    if (singleEvent != null && singleEvent.getProgramStage() != null) {
      return singleEvent.getProgramStage().getProgram();
    }
    return null;
  }

  /** The event as sent in the payload, after preprocessing and program rule assignments. */
  private static Map<String, Object> fromPayload(TrackerPreheat preheat, Event event) {
    Program program = resolveProgram(preheat, event);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("event", event.getUID().getValue());
    m.put("program", program == null ? null : program.getUid());
    m.put("programStage", identifier(event.getProgramStage()));
    m.put(
        "enrollment",
        event instanceof org.hisp.dhis.tracker.imports.domain.TrackerEvent te
                && te.getEnrollment() != null
            ? te.getEnrollment().getValue()
            : null);
    m.put("orgUnit", identifier(event.getOrgUnit()));
    m.put("status", event.getStatus() == null ? null : event.getStatus().name());
    m.put("occurredAt", event.getOccurredAt() == null ? null : event.getOccurredAt().toString());
    Map<String, String> dataValues = new TreeMap<>();
    event
        .getDataValues()
        .forEach(dv -> dataValues.put(identifier(dv.getDataElement()), dv.getValue()));
    m.put("dataValues", dataValues);
    return m;
  }

  /** The event as currently stored, or null when it does not exist yet. */
  private static Map<String, Object> fromStored(TrackerPreheat preheat, UID uid) {
    TrackerEvent trackerEvent = preheat.getTrackerEvent(uid);
    if (trackerEvent != null) {
      return stored(
          uid,
          trackerEvent.getProgramStage(),
          trackerEvent.getEnrollment() == null ? null : trackerEvent.getEnrollment().getUid(),
          trackerEvent.getOrganisationUnit(),
          trackerEvent.getStatus(),
          trackerEvent.getOccurredDate(),
          trackerEvent.getEventDataValues());
    }
    SingleEvent singleEvent = preheat.getSingleEvent(uid);
    if (singleEvent != null) {
      return stored(
          uid,
          singleEvent.getProgramStage(),
          null,
          singleEvent.getOrganisationUnit(),
          singleEvent.getStatus(),
          singleEvent.getOccurredDate(),
          singleEvent.getEventDataValues());
    }
    return null;
  }

  private static Map<String, Object> stored(
      UID uid,
      ProgramStage stage,
      String enrollment,
      OrganisationUnit orgUnit,
      Object status,
      java.util.Date occurredDate,
      Set<EventDataValue> eventDataValues) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("event", uid.getValue());
    m.put(
        "program",
        stage == null || stage.getProgram() == null ? null : stage.getProgram().getUid());
    m.put("programStage", stage == null ? null : stage.getUid());
    m.put("enrollment", enrollment);
    m.put("orgUnit", orgUnit == null ? null : orgUnit.getUid());
    m.put("status", status == null ? null : status.toString());
    m.put("occurredAt", occurredDate == null ? null : occurredDate.toInstant().toString());
    Map<String, String> dataValues = new TreeMap<>();
    if (eventDataValues != null) {
      eventDataValues.forEach(dv -> dataValues.put(dv.getDataElement(), dv.getValue()));
    }
    m.put("dataValues", dataValues);
    return m;
  }

  private static String identifier(MetadataIdentifier id) {
    return id == null ? null : id.getIdentifierOrAttributeValue();
  }
}
