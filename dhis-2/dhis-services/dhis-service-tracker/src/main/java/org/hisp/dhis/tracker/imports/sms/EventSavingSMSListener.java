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
package org.hisp.dhis.tracker.imports.sms;

import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_TRACKER;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.listener.CompressionSMSListener;
import org.hisp.dhis.sms.listener.SMSProcessingException;
import org.hisp.dhis.smscompression.SmsConsts.SmsEnrollmentStatus;
import org.hisp.dhis.smscompression.SmsConsts.SmsEventStatus;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.GeoPoint;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.smscompression.models.Uid;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.event.TrackedEntityDataValueChangeLog;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

@Slf4j
public abstract class EventSavingSMSListener extends CompressionSMSListener {

  protected final EventService eventService;

  protected final EventChangeLogService eventChangeLogService;

  protected final FileResourceService fileResourceService;

  protected final DhisConfigurationProvider config;

  protected EventSavingSMSListener(
      IncomingSmsService incomingSmsService,
      MessageSender smsSender,
      UserService userService,
      TrackedEntityTypeService trackedEntityTypeService,
      TrackedEntityAttributeService trackedEntityAttributeService,
      ProgramService programService,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      DataElementService dataElementService,
      IdentifiableObjectManager identifiableObjectManager,
      EventService eventService,
      EventChangeLogService eventChangeLogService,
      FileResourceService fileResourceService,
      DhisConfigurationProvider config) {
    super(
        incomingSmsService,
        smsSender,
        userService,
        trackedEntityTypeService,
        trackedEntityAttributeService,
        programService,
        organisationUnitService,
        categoryService,
        dataElementService,
        identifiableObjectManager);
    this.eventService = eventService;
    this.eventChangeLogService = eventChangeLogService;
    this.fileResourceService = fileResourceService;
    this.config = config;
  }

  protected List<Object> saveEvent(
      String eventUid,
      OrganisationUnit orgUnit,
      ProgramStage programStage,
      Enrollment enrollment,
      CategoryOptionCombo aoc,
      User user,
      List<SmsDataValue> values,
      SmsEventStatus eventStatus,
      Date occurredDate,
      Date scheduledDate,
      GeoPoint coordinates) {
    ArrayList<Object> errorUids = new ArrayList<>();

    Event event = null;
    // try to find an event with given UID else create a new event with given UID or let tracker
    // auto-generate one if no UID was given
    eventUid = StringUtils.trimToNull(eventUid);
    if (eventUid != null) {
      try {
        event = eventService.getEvent(UID.of(eventUid), UserDetails.fromUser(user));
      } catch (ForbiddenException e) {
        throw new SMSProcessingException(SmsResponse.INVALID_EVENT.set(eventUid));
      } catch (NotFoundException e) {
        // we'll create a new event if none was found
      }
    }

    if (event == null) {
      event = new Event();
      event.setUid(eventUid);
    }

    event.setOrganisationUnit(orgUnit);
    event.setProgramStage(programStage);
    event.setEnrollment(enrollment);
    event.setOccurredDate(occurredDate);
    event.setScheduledDate(scheduledDate);
    event.setAttributeOptionCombo(aoc);
    event.setStoredBy(user.getUsername());

    UserDetails currentUserDetails = UserDetails.fromUser(user);
    UserInfoSnapshot currentUserInfo = UserInfoSnapshot.from(currentUserDetails);

    event.setCreatedByUserInfo(currentUserInfo);
    event.setLastUpdatedByUserInfo(currentUserInfo);

    event.setStatus(getCoreEventStatus(eventStatus));
    event.setGeometry(convertGeoPointToGeometry(coordinates));

    if (eventStatus.equals(SmsEventStatus.COMPLETED)) {
      event.setCompletedBy(user.getUsername());
      event.setCompletedDate(new Date());
    }

    Map<DataElement, EventDataValue> dataElementsAndEventDataValues = new HashMap<>();
    if (values != null) {
      for (SmsDataValue dv : values) {
        Uid deid = dv.getDataElement();
        String val = dv.getValue();

        DataElement de = dataElementService.getDataElement(deid.getUid());

        // TODO: Is this the correct way of handling errors here?
        if (de == null) {
          log.warn(
              String.format(
                  "Given data element [%s] could not be found. Continuing with submission...",
                  deid));
          errorUids.add(deid);

          continue;
        } else if (val == null || StringUtils.isEmpty(val)) {
          log.warn(
              String.format(
                  "Value for atttribute [%s] is null or empty. Continuing with submission...",
                  deid));
          continue;
        }

        EventDataValue eventDataValue =
            new EventDataValue(deid.getUid(), dv.getValue(), currentUserInfo);
        eventDataValue.setAutoFields();
        dataElementsAndEventDataValues.put(de, eventDataValue);
      }
    }

    saveEventDataValuesAndSaveEvent(event, dataElementsAndEventDataValues);

    return errorUids;
  }

  private EventStatus getCoreEventStatus(SmsEventStatus eventStatus) {
    return switch (eventStatus) {
      case ACTIVE -> EventStatus.ACTIVE;
      case COMPLETED -> EventStatus.COMPLETED;
      case VISITED -> EventStatus.VISITED;
      case SCHEDULE -> EventStatus.SCHEDULE;
      case OVERDUE -> EventStatus.OVERDUE;
      case SKIPPED -> EventStatus.SKIPPED;
    };
  }

  protected EnrollmentStatus getCoreEnrollmentStatus(SmsEnrollmentStatus enrollmentStatus) {
    return switch (enrollmentStatus) {
      case ACTIVE -> EnrollmentStatus.ACTIVE;
      case COMPLETED -> EnrollmentStatus.COMPLETED;
      case CANCELLED -> EnrollmentStatus.CANCELLED;
    };
  }

  protected Geometry convertGeoPointToGeometry(GeoPoint coordinates) {
    if (coordinates == null) {
      return null;
    }

    GeometryFactory gf = new GeometryFactory();
    Coordinate co = new Coordinate(coordinates.getLongitude(), coordinates.getLatitude());

    return gf.createPoint(co);
  }

  private void saveEventDataValuesAndSaveEvent(
      Event event, Map<DataElement, EventDataValue> dataElementEventDataValueMap) {
    validateEventDataValues(dataElementEventDataValueMap);
    Set<EventDataValue> eventDataValues = new HashSet<>(dataElementEventDataValueMap.values());
    event.setEventDataValues(eventDataValues);

    event.setAutoFields();
    if (!event.hasAttributeOptionCombo()) {
      CategoryOptionCombo aoc = categoryService.getDefaultCategoryOptionCombo();
      event.setAttributeOptionCombo(aoc);
    }
    manager.save(event);

    for (Map.Entry<DataElement, EventDataValue> entry : dataElementEventDataValueMap.entrySet()) {
      entry.getValue().setAutoFields();
      createAndAddChangeLog(entry.getValue(), entry.getKey(), event);
      handleFileDataValueSave(entry.getValue(), entry.getKey());
    }
  }

  private String validateEventDataValue(DataElement dataElement, EventDataValue eventDataValue) {

    if (StringUtils.isEmpty(eventDataValue.getStoredBy())) {
      return "Stored by is null or empty";
    }

    if (StringUtils.isEmpty(eventDataValue.getDataElement())) {
      return "Data element is null or empty";
    }

    if (!dataElement.getUid().equals(eventDataValue.getDataElement())) {
      throw new IllegalQueryException(
          "DataElement "
              + dataElement.getUid()
              + " assigned to EventDataValues does not match with one EventDataValue: "
              + eventDataValue.getDataElement());
    }

    String result =
        ValidationUtils.valueIsValid(eventDataValue.getValue(), dataElement.getValueType());

    return result == null ? null : "Value is not valid:  " + result;
  }

  private void validateEventDataValues(
      Map<DataElement, EventDataValue> dataElementEventDataValueMap) {
    String result;
    for (Map.Entry<DataElement, EventDataValue> entry : dataElementEventDataValueMap.entrySet()) {
      result = validateEventDataValue(entry.getKey(), entry.getValue());
      if (result != null) {
        throw new IllegalQueryException(result);
      }
    }
  }

  private void createAndAddChangeLog(
      EventDataValue dataValue, DataElement dataElement, Event event) {
    if (!config.isEnabled(CHANGELOG_TRACKER) || dataElement == null) {
      return;
    }

    TrackedEntityDataValueChangeLog dataValueChangeLog =
        new TrackedEntityDataValueChangeLog(
            dataElement,
            event,
            dataValue.getValue(),
            dataValue.getStoredBy(),
            dataValue.getProvidedElsewhere(),
            ChangeLogType.CREATE);

    eventChangeLogService.addTrackedEntityDataValueChangeLog(dataValueChangeLog);
  }

  /** Update FileResource with 'assigned' status. */
  private void handleFileDataValueSave(EventDataValue dataValue, DataElement dataElement) {
    if (dataElement == null) {
      return;
    }

    FileResource fileResource = fetchFileResource(dataValue, dataElement);

    if (fileResource == null) {
      return;
    }

    setAssigned(fileResource);
  }

  private FileResource fetchFileResource(EventDataValue dataValue, DataElement dataElement) {
    if (!dataElement.isFileType()) {
      return null;
    }

    return fileResourceService.getFileResource(dataValue.getValue());
  }

  private void setAssigned(FileResource fileResource) {
    fileResource.setAssigned(true);
    fileResourceService.updateFileResource(fileResource);
  }
}
