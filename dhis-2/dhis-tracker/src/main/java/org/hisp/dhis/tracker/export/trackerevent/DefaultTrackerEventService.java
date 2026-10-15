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
package org.hisp.dhis.tracker.export.trackerevent;

import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
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
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("org.hisp.dhis.tracker.export.trackerevent.TrackerEventService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
class DefaultTrackerEventService implements TrackerEventService {

  private final JdbcTrackerEventStore eventStore;

  private final IdentifiableObjectManager manager;

  private final DataElementService dataElementService;

  private final FileResourceService fileResourceService;

  private final TrackerEventOperationParamsMapper paramsMapper;

  private final RelationshipService relationshipService;

  private final AclService aclService;

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

    Page<TrackerEvent> events;
    try {
      TrackerEventOperationParams operationParams =
          TrackerEventOperationParams.builderForEvent(eventUid)
              .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
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
    TrackerEvent event = events.getItems().get(0);

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
  public boolean exists(@Nonnull UID event) {
    return findEvent(event).isPresent();
  }

  @Nonnull
  @Override
  public Optional<TrackerEvent> findEvent(@Nonnull UID event) {
    try {
      return Optional.of(getEvent(event));
    } catch (NotFoundException e) {
      return Optional.empty();
    }
  }

  @Nonnull
  @Override
  public TrackerEvent getEvent(@Nonnull UID event) throws NotFoundException {
    return getEvent(event, TrackerIdSchemeParams.builder().build(), TrackerEventFields.none());
  }

  @Nonnull
  @Override
  public TrackerEvent getEvent(
      @Nonnull UID eventUid,
      @Nonnull TrackerIdSchemeParams idSchemeParams,
      @Nonnull TrackerEventFields fields)
      throws NotFoundException {
    Page<TrackerEvent> events;
    try {
      TrackerEventOperationParams operationParams =
          TrackerEventOperationParams.builderForEvent(eventUid)
              .orgUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)
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
    // findEvents already dropped data values whose data element cannot be resolved for the
    // requested idScheme or the user is not allowed to read.
    return events.getItems().get(0);
  }

  @Nonnull
  @Override
  public List<TrackerEvent> findEvents(@Nonnull TrackerEventOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    TrackerEventQueryParams queryParams =
        paramsMapper.map(operationParams, getCurrentUserDetails());
    List<TrackerEvent> events = eventStore.getEvents(queryParams);
    filterReadableDataValues(events, queryParams.getIdSchemeParams());
    if (operationParams.getFields().isIncludesRelationships()) {
      for (TrackerEvent event : events) {
        event.setRelationshipItems(
            relationshipService.findRelationshipItems(
                TrackerType.EVENT,
                event.getUID(),
                operationParams.getFields().getRelationshipFields(),
                queryParams.isIncludeDeleted()));
      }
    }
    return events;
  }

  @Nonnull
  @Override
  public Page<TrackerEvent> findEvents(
      @Nonnull TrackerEventOperationParams operationParams, @Nonnull PageParams pageParams)
      throws BadRequestException, ForbiddenException {
    TrackerEventQueryParams queryParams =
        paramsMapper.map(operationParams, getCurrentUserDetails());
    Page<TrackerEvent> events = eventStore.getEvents(queryParams, pageParams);
    filterReadableDataValues(events.getItems(), queryParams.getIdSchemeParams());
    if (operationParams.getFields().isIncludesRelationships()) {
      for (TrackerEvent event : events.getItems()) {
        event.setRelationshipItems(
            relationshipService.findRelationshipItems(
                TrackerType.EVENT,
                event.getUID(),
                operationParams.getFields().getRelationshipFields(),
                queryParams.isIncludeDeleted()));
      }
    }
    return events;
  }

  /**
   * Removes the data values whose data element the user is not allowed to read. Data values only
   * carry the data element identifier (a UID, code or name depending on the requested idScheme)
   * stored in the {@code eventdatavalues} jsonb column, not the data element metadata, so we
   * resolve each data element and honor its (metadata) sharing. Data values whose data element
   * cannot be resolved for the requested idScheme are dropped as well. The readability of a data
   * element is cached per call since the same data element is typically shared across many events.
   */
  private void filterReadableDataValues(
      List<TrackerEvent> events, TrackerIdSchemeParams idSchemeParams) {
    UserDetails user = getCurrentUserDetails();
    Map<String, Boolean> readableByIdentifier = new HashMap<>();
    for (TrackerEvent event : events) {
      Set<EventDataValue> readable =
          event.getEventDataValues().stream()
              .filter(
                  dataValue ->
                      readableByIdentifier.computeIfAbsent(
                          dataValue.getDataElement(),
                          identifier -> isDataElementReadable(identifier, idSchemeParams, user)))
              .collect(Collectors.toCollection(LinkedHashSet::new));
      event.setEventDataValues(readable);
    }
  }

  private boolean isDataElementReadable(
      String identifier, TrackerIdSchemeParams idSchemeParams, UserDetails user) {
    DataElement dataElement = null;
    TrackerIdSchemeParam dataElementIdScheme = idSchemeParams.getDataElementIdScheme();
    if (TrackerIdScheme.UID == dataElementIdScheme.getIdScheme()) {
      dataElement = dataElementService.getDataElement(identifier);
    } else if (TrackerIdScheme.CODE == dataElementIdScheme.getIdScheme()) {
      dataElement = manager.getByCode(DataElement.class, identifier);
    } else if (TrackerIdScheme.NAME == dataElementIdScheme.getIdScheme()) {
      dataElement = manager.getByName(DataElement.class, identifier);
    } else if (TrackerIdScheme.ATTRIBUTE == dataElementIdScheme.getIdScheme()) {
      dataElement =
          manager.getObject(
              DataElement.class,
              new IdScheme(IdentifiableProperty.ATTRIBUTE, dataElementIdScheme.getAttributeUid()),
              identifier);
    }

    return dataElement != null && aclService.canRead(user, dataElement);
  }

  @Override
  public Set<String> getOrderableFields() {
    return eventStore.getOrderableFields();
  }
}
