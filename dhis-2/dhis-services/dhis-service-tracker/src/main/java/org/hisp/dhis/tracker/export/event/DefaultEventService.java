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

import com.google.common.hash.Hashing;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
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

  private final UserService userService;

  private final EventStore eventStore;

  private final org.hisp.dhis.program.EventService eventService;

  private final TrackerAccessManager trackerAccessManager;

  private final DataElementService dataElementService;

  private final FileResourceService fileResourceService;

  private final EventOperationParamsMapper paramsMapper;

  @Override
  public FileResourceStream getFileResource(UID eventUid, UID dataElementUid)
      throws NotFoundException {
    FileResource fileResource = getFileResourceMetadata(eventUid, dataElementUid);

    return new FileResourceStream(
        fileResource,
        () -> {
          try {
            return fileResourceService.openContentStream(fileResource);
          } catch (NoSuchElementException e) {
            // Note: we are assuming that the file resource is not available yet. The same approach
            // is taken in other file endpoints or code relying on the storageStatus = PENDING.
            // All we know for sure is the file resource is in the DB but not in the store.
            throw new ConflictException(
                "The content is being processed and is not available yet. Try again later.");
          } catch (IOException e) {
            throw new ConflictException(
                "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend. "
                    + "Depending on the provider the root cause could be network or file system related.");
          }
        });
  }

  @Override
  public FileResourceStream getFileResourceImage(
      UID eventUid, UID dataElementUid, ImageFileDimension dimension)
      throws NotFoundException, ConflictException, BadRequestException {
    FileResource fileResource = getFileResourceMetadata(eventUid, dataElementUid);

    // The FileResource only stores the storageKey, contentLength and md5Hash of the original image.
    // At least for now we are losing the benefit of not fetching the file from storage if the
    // client already has an up-to-date version of the image in the given dimension other than the
    // original. We have to fetch and compute the length and hash of the image again.
    ImageFileDimension imageDimension =
        ObjectUtils.firstNonNull(dimension, ImageFileDimension.ORIGINAL);
    FileResourceStream fileResourceStream = new FileResourceStream(fileResource);
    if (imageDimension != ImageFileDimension.ORIGINAL) {
      byte[] content;
      try {
        content = fileResourceService.copyImageContent(fileResource, imageDimension);
        fileResourceStream.setInputStreamSupplier(() -> new ByteArrayInputStream(content));
      } catch (NoSuchElementException e) {
        // Note: we are assuming that the file resource is not available yet. The same approach
        // is taken in other file endpoints or code relying on the storageStatus = PENDING.
        // All we know for sure is the file resource is in the DB but not in the store.
        throw new ConflictException(
            "The content is being processed and is not available yet. Try again later.");
      } catch (IOException e) {
        throw new ConflictException(
            "Failed fetching the file from storage",
            "There was an exception when trying to fetch the file from the storage backend. "
                + "Depending on the provider the root cause could be network or file system related.");
      }
      fileResource.setContentLength(content.length);
      fileResource.setContentMd5(Hashing.md5().hashBytes(content).toString());
    } else {
      fileResourceStream.setInputStreamSupplier(
          () -> {
            try {
              return fileResourceService.openContentStreamToImage(fileResource, dimension);
            } catch (NoSuchElementException e) {
              // Note: we are assuming that the file resource is not available yet. The same
              // approach
              // is taken in other file endpoints or code relying on the storageStatus = PENDING.
              // All we know for sure is the file resource is in the DB but not in the store.
              throw new ConflictException(
                  "The content is being processed and is not available yet. Try again later.");
            } catch (IOException e) {
              throw new ConflictException(
                  "Failed fetching the file from storage",
                  "There was an exception when trying to fetch the file from the storage backend. "
                      + "Depending on the provider the root cause could be network or file system related.");
            }
          });
    }
    return fileResourceStream;
  }

  private FileResource getFileResourceMetadata(UID eventUid, UID dataElementUid)
      throws NotFoundException {
    Event event = eventService.getEvent(eventUid.getValue());
    if (event == null) {
      throw new NotFoundException(Event.class, eventUid.getValue());
    }

    DataElement dataElement = dataElementService.getDataElement(dataElementUid.getValue());
    if (dataElement == null) {
      throw new NotFoundException(DataElement.class, dataElementUid.getValue());
    }

    if (!dataElement.getValueType().isFile()) {
      throw new NotFoundException(
          "Data element " + dataElementUid.getValue() + " is not a file (or image).");
    }

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    List<String> errors = trackerAccessManager.canRead(currentUser, event, dataElement, false);
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
  public Event getEvent(String uid, EventParams eventParams)
      throws NotFoundException, ForbiddenException {
    Event event = eventService.getEvent(uid);
    if (event == null) {
      throw new NotFoundException(Event.class, uid);
    }

    return getEvent(event, eventParams);
  }

  public Event getEvent(@Nonnull Event event, EventParams eventParams) throws ForbiddenException {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    List<String> errors = trackerAccessManager.canRead(currentUser, event, false);
    if (!errors.isEmpty()) {
      throw new ForbiddenException(errors.toString());
    }

    Event result = new Event();
    result.setId(event.getId());
    result.setUid(event.getUid());

    result.setStatus(event.getStatus());
    result.setOccurredDate(event.getOccurredDate());
    result.setScheduledDate(event.getScheduledDate());
    result.setStoredBy(event.getStoredBy());
    result.setCompletedBy(event.getCompletedBy());
    result.setCompletedDate(event.getCompletedDate());
    result.setCreated(event.getCreated());
    result.setCreatedByUserInfo(event.getCreatedByUserInfo());
    result.setLastUpdatedByUserInfo(event.getLastUpdatedByUserInfo());
    result.setCreatedAtClient(event.getCreatedAtClient());
    result.setLastUpdated(event.getLastUpdated());
    result.setLastUpdatedAtClient(event.getLastUpdatedAtClient());
    result.setGeometry(event.getGeometry());
    result.setDeleted(event.isDeleted());
    result.setAssignedUser(event.getAssignedUser());

    OrganisationUnit ou = event.getOrganisationUnit();

    result.setEnrollment(event.getEnrollment());
    result.setProgramStage(event.getProgramStage());

    result.setOrganisationUnit(ou);
    result.setProgramStage(event.getProgramStage());

    result.setAttributeOptionCombo(event.getAttributeOptionCombo());

    for (EventDataValue dataValue : event.getEventDataValues()) {
      if (dataElementService.getDataElement(dataValue.getDataElement())
          != null) // check permissions
      {
        EventDataValue value = new EventDataValue();
        value.setCreated(dataValue.getCreated());
        value.setCreatedByUserInfo(dataValue.getCreatedByUserInfo());
        value.setLastUpdated(dataValue.getLastUpdated());
        value.setLastUpdatedByUserInfo(dataValue.getLastUpdatedByUserInfo());
        value.setDataElement(dataValue.getDataElement());
        value.setValue(dataValue.getValue());
        value.setProvidedElsewhere(dataValue.getProvidedElsewhere());
        value.setStoredBy(dataValue.getStoredBy());

        result.getEventDataValues().add(value);
      } else {
        log.info("Can not find a Data Element having UID [" + dataValue.getDataElement() + "]");
      }
    }

    result.getNotes().addAll(event.getNotes());

    if (eventParams.isIncludeRelationships()) {
      Set<RelationshipItem> relationshipItems = new HashSet<>();

      for (RelationshipItem relationshipItem : event.getRelationshipItems()) {
        Relationship daoRelationship = relationshipItem.getRelationship();
        if (trackerAccessManager.canRead(currentUser, daoRelationship).isEmpty()
            && (!daoRelationship.isDeleted())) {
          relationshipItems.add(relationshipItem);
        }
      }

      result.setRelationshipItems(relationshipItems);
    }

    return result;
  }

  @Override
  public List<Event> getEvents(EventOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    EventQueryParams queryParams = paramsMapper.map(operationParams);
    return eventStore.getEvents(queryParams);
  }

  @Override
  public Page<Event> getEvents(EventOperationParams operationParams, PageParams pageParams)
      throws BadRequestException, ForbiddenException {
    EventQueryParams queryParams = paramsMapper.map(operationParams);
    return eventStore.getEvents(queryParams, pageParams);
  }

  @Override
  public Set<String> getOrderableFields() {
    return eventStore.getOrderableFields();
  }
}
