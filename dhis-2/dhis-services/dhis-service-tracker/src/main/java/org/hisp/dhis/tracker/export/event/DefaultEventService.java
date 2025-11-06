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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service("org.hisp.dhis.tracker.export.event.EventService")
@RequiredArgsConstructor
class DefaultEventService implements EventService {

  private final JdbcEventStore eventStore;

  private final IdentifiableObjectManager manager;

  private final TrackerAccessManager trackerAccessManager;

  private final DataElementService dataElementService;

  private final FileResourceService fileResourceService;

  private final EventOperationParamsMapper paramsMapper;

  private final RelationshipService relationshipService;
  private final EventOperationParamsMapper eventOperationParamsMapper;

  @Override
  @Transactional(readOnly = true)
  public FileResourceStream getFileResource(@Nonnull UID event, @Nonnull UID dataElement)
      throws NotFoundException, ForbiddenException {
    FileResource fileResource = getFileResourceMetadata(event, dataElement);
    return FileResourceStream.of(fileResourceService, fileResource);
  }

  @Override
  @Transactional(readOnly = true)
  public FileResourceStream getFileResourceImage(
      @Nonnull UID event, @Nonnull UID dataElement, ImageFileDimension dimension)
      throws NotFoundException, ForbiddenException {
    FileResource fileResource = getFileResourceMetadata(event, dataElement);
    return FileResourceStream.ofImage(fileResourceService, fileResource, dimension);
  }

  private FileResource getFileResourceMetadata(UID eventUid, UID dataElementUid)
      throws NotFoundException, ForbiddenException {
    // EventOperationParamsMapper throws BadRequestException if the data element is not found but
    // we need the NotFoundException here
    DataElement dataElement = dataElementService.getDataElement(dataElementUid.getValue());
    if (dataElement == null) {
      throw new NotFoundException(DataElement.class, dataElementUid.getValue());
    }
    if (!dataElement.getValueType().isFile()) {
      throw new NotFoundException(
          "Data element " + dataElementUid.getValue() + " is not a file (or image).");
    }

    Page<Event> events;
    try {
      EventOperationParams operationParams =
          EventOperationParams.builder()
              .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
              .events(Set.of(eventUid))
              .filterByDataElement(dataElementUid)
              .build();
      events = findEvents(operationParams, PageParams.single());
    } catch (BadRequestException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EventOperationParams are built");
    }
    if (events.getItems().isEmpty()) {
      throw new NotFoundException(
          "Event "
              + eventUid.getValue()
              + " with data element "
              + dataElementUid.getValue()
              + " could not be found.");
    }
    Event event = events.getItems().get(0);

    List<String> errors =
        trackerAccessManager.canRead(getCurrentUserDetails(), event, dataElement, false);
    if (!errors.isEmpty()) {
      throw new NotFoundException(DataElement.class, dataElementUid.getValue());
    }

    String fileResourceUid = null;
    for (EventDataValue eventDataValue : event.getEventDataValues()) {
      if (dataElementUid.getValue().equals(eventDataValue.getDataElement())) {
        fileResourceUid = eventDataValue.getValue();
        break;
      }
    }

    if (fileResourceUid == null) {
      throw new NotFoundException(
          "DataValue for data element " + dataElementUid.getValue() + " could not be found.");
    }

    return fileResourceService.getExistingFileResource(fileResourceUid);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Optional<Event> findEvent(@Nonnull UID event) {
    try {
      return Optional.of(getEvent(event));
    } catch (NotFoundException e) {
      return Optional.empty();
    }
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Event getEvent(@Nonnull UID event) throws NotFoundException {
    return getEvent(event, TrackerIdSchemeParams.builder().build(), EventFields.none());
  }

  @Override
  @Transactional(readOnly = true)
  public long countEvents(@Nonnull EventOperationParams operationParams)
      throws ForbiddenException, BadRequestException {
    EventQueryParams queryParams = paramsMapper.map(operationParams, getCurrentUserDetails());
    return eventStore.countEvents(queryParams);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Event getEvent(
      @Nonnull UID eventUid,
      @Nonnull TrackerIdSchemeParams idSchemeParams,
      @Nonnull EventFields fields)
      throws NotFoundException {
    Page<Event> events;
    try {
      EventOperationParams operationParams =
          EventOperationParams.builder()
              .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
              .events(Set.of(eventUid))
              .fields(fields)
              .idSchemeParams(idSchemeParams)
              .build();
      events = findEvents(operationParams, PageParams.single());
    } catch (BadRequestException | ForbiddenException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EventOperationParams are built");
    }

    if (events.getItems().isEmpty()) {
      throw new NotFoundException(Event.class, eventUid.getValue());
    }
    Event event = events.getItems().get(0);

    Set<EventDataValue> dataValues = new HashSet<>(event.getEventDataValues().size());
    for (EventDataValue dataValue : event.getEventDataValues()) {
      DataElement dataElement = null;
      TrackerIdSchemeParam dataElementIdScheme = idSchemeParams.getDataElementIdScheme();
      if (TrackerIdScheme.UID == dataElementIdScheme.getIdScheme()) {
        dataElement = dataElementService.getDataElement(dataValue.getDataElement());
      } else if (TrackerIdScheme.CODE == dataElementIdScheme.getIdScheme()) {
        dataElement = manager.getByCode(DataElement.class, dataValue.getDataElement());
      } else if (TrackerIdScheme.NAME == dataElementIdScheme.getIdScheme()) {
        dataElement = manager.getByName(DataElement.class, dataValue.getDataElement());
      } else if (TrackerIdScheme.ATTRIBUTE == dataElementIdScheme.getIdScheme()) {
        dataElement =
            manager.getObject(
                DataElement.class,
                new IdScheme(IdentifiableProperty.ATTRIBUTE, dataElementIdScheme.getAttributeUid()),
                dataValue.getDataElement());
      }

      if (dataElement != null) // check permissions
      {
        dataValues.add(dataValue);
      } else {
        log.info("Cannot find data element with UID {}", dataValue.getDataElement());
      }
    }
    event.setEventDataValues(dataValues);

    return event;
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public List<Event> findEvents(@Nonnull EventOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    EventQueryParams queryParams = paramsMapper.map(operationParams, getCurrentUserDetails());
    List<Event> events = eventStore.getEvents(queryParams);
    if (operationParams.getFields().isIncludesRelationships()) {
      for (Event event : events) {
        event.setRelationshipItems(
            relationshipService.findRelationshipItems(
                TrackerType.EVENT,
                UID.of(event),
                operationParams.getFields().getRelationshipFields(),
                queryParams.isIncludeDeleted()));
      }
    }
    return events;
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public List<Event> findEvents(
      @Nonnull EventOperationParams params, @Nonnull Map<String, Set<String>> psdesWithSkipSyncTrue)
      throws BadRequestException, ForbiddenException {
    EventQueryParams queryParams = paramsMapper.map(params, getCurrentUserDetails());
    return eventStore.getEvents(queryParams, psdesWithSkipSyncTrue);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Page<Event> findEvents(
      @Nonnull EventOperationParams operationParams, @Nonnull PageParams pageParams)
      throws BadRequestException, ForbiddenException {
    EventQueryParams queryParams = paramsMapper.map(operationParams, getCurrentUserDetails());
    Page<Event> events = eventStore.getEvents(queryParams, pageParams);
    if (operationParams.getFields().isIncludesRelationships()) {
      for (Event event : events.getItems()) {
        event.setRelationshipItems(
            relationshipService.findRelationshipItems(
                TrackerType.EVENT,
                UID.of(event),
                operationParams.getFields().getRelationshipFields(),
                queryParams.isIncludeDeleted()));
      }
    }
    return events;
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getOrderableFields() {
    return eventStore.getOrderableFields();
  }

  @Override
  @Transactional
  public void updateEventsSyncTimestamp(
      @Nonnull List<String> eventsUIDs, @Nonnull Date lastSynchronized) {
    eventStore.updateEventsSyncTimestamp(eventsUIDs, lastSynchronized);
  }
}
