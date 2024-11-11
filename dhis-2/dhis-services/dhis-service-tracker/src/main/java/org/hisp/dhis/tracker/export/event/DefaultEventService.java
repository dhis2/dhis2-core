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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.acl.TrackerAccessManager;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service("org.hisp.dhis.tracker.export.event.EventService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
class DefaultEventService implements EventService {

  private final JdbcEventStore eventStore;

  private final TrackerAccessManager trackerAccessManager;

  private final DataElementService dataElementService;

  private final FileResourceService fileResourceService;

  private final EventOperationParamsMapper paramsMapper;

  @Override
  public FileResourceStream getFileResource(@Nonnull UID event, @Nonnull UID dataElement)
      throws NotFoundException, ForbiddenException {
    FileResource fileResource = getFileResourceMetadata(event, dataElement);
    return FileResourceStream.of(fileResourceService, fileResource);
  }

  @Override
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
              .eventParams(EventParams.FALSE)
              .dataElementFilters(Map.of(dataElementUid, List.of()))
              .build();
      events = getEvents(operationParams, new PageParams(1, 1, false));
    } catch (BadRequestException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EventOperationParams are built");
    }
    if (events.getItems().isEmpty()) {
      throw new NotFoundException(Event.class, eventUid);
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

  @Override
  public Event getEvent(@Nonnull UID event) throws ForbiddenException, NotFoundException {
    return getEvent(
        event, TrackerIdSchemeParams.builder().build(), EventParams.FALSE, getCurrentUserDetails());
  }

  @Override
  public Event getEvent(@Nonnull UID event, @Nonnull TrackerIdSchemeParams idSchemeParams)
      throws ForbiddenException, NotFoundException {
    return getEvent(event, idSchemeParams, EventParams.FALSE, getCurrentUserDetails());
  }

  @Override
  public Event getEvent(@Nonnull UID event, @Nonnull EventParams eventParams)
      throws ForbiddenException, NotFoundException {
    return getEvent(
        event, TrackerIdSchemeParams.builder().build(), eventParams, getCurrentUserDetails());
  }

  @Override
  public Event getEvent(
      @Nonnull UID event,
      @Nonnull TrackerIdSchemeParams idSchemeParams,
      @Nonnull EventParams eventParams)
      throws ForbiddenException, NotFoundException {
    return getEvent(event, idSchemeParams, eventParams, getCurrentUserDetails());
  }

  private Event getEvent(
      @Nonnull UID eventUid,
      @Nonnull TrackerIdSchemeParams idSchemeParams,
      @Nonnull EventParams eventParams,
      @Nonnull UserDetails user)
      throws NotFoundException, ForbiddenException {
    Page<Event> events;
    try {
      EventOperationParams operationParams =
          EventOperationParams.builder()
              .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
              .events(Set.of(eventUid))
              .eventParams(eventParams)
              .idSchemeParams(idSchemeParams)
              .build();
      events = getEvents(operationParams, new PageParams(1, 1, false));
    } catch (BadRequestException e) {
      throw new IllegalArgumentException(
          "this must be a bug in how the EventOperationParams are built");
    }

    if (events.getItems().isEmpty()) {
      throw new NotFoundException(Event.class, eventUid.getValue());
    }
    Event event = events.getItems().get(0);

    // TODO(ivo) do do ACL checks on data elements/relationships in query, how is that done on for
    // the
    // collections?
    // for (EventDataValue dataValue : event.getEventDataValues()) {
    //   if (dataElementService.getDataElement(dataValue.getDataElement())
    //       != null) // check permissions
    //   {
    //     EventDataValue value = new EventDataValue();
    //     value.setCreated(dataValue.getCreated());
    //     value.setCreatedByUserInfo(dataValue.getCreatedByUserInfo());
    //     value.setLastUpdated(dataValue.getLastUpdated());
    //     value.setLastUpdatedByUserInfo(dataValue.getLastUpdatedByUserInfo());
    //     value.setDataElement(dataValue.getDataElement());
    //     value.setValue(dataValue.getValue());
    //     value.setProvidedElsewhere(dataValue.getProvidedElsewhere());
    //     value.setStoredBy(dataValue.getStoredBy());
    //
    //     result.getEventDataValues().add(value);
    //   } else {
    //     log.info("Cannot find data element with UID {}", dataValue.getDataElement());
    //   }
    // }

    if (eventParams.isIncludeRelationships()) {
      Set<RelationshipItem> relationshipItems = new HashSet<>();
      for (RelationshipItem relationshipItem : event.getRelationshipItems()) {
        Relationship daoRelationship = relationshipItem.getRelationship();
        if (trackerAccessManager.canRead(user, daoRelationship).isEmpty()
            && (!daoRelationship.isDeleted())) {
          relationshipItems.add(relationshipItem);
        }
      }

      event.setRelationshipItems(relationshipItems);
    }

    return event;
  }

  @Override
  public List<Event> getEvents(@Nonnull EventOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    EventQueryParams queryParams = paramsMapper.map(operationParams, getCurrentUserDetails());
    return eventStore.getEvents(queryParams);
  }

  @Override
  public Page<Event> getEvents(
      @Nonnull EventOperationParams operationParams, @Nonnull PageParams pageParams)
      throws BadRequestException, ForbiddenException {
    EventQueryParams queryParams = paramsMapper.map(operationParams, getCurrentUserDetails());
    return eventStore.getEvents(queryParams, pageParams);
  }

  @Override
  public RelationshipItem getEventInRelationshipItem(
      @Nonnull UID uid, @Nonnull EventParams eventParams) {
    Event event;
    try {
      event = getEvent(uid, eventParams);
    } catch (NotFoundException | ForbiddenException e) {
      // events are not shown in relationships if the user has no access to them
      return null;
    }

    RelationshipItem relationshipItem = new RelationshipItem();
    relationshipItem.setEvent(event);
    return relationshipItem;
  }

  @Override
  public Set<String> getOrderableFields() {
    return eventStore.getOrderableFields();
  }
}
