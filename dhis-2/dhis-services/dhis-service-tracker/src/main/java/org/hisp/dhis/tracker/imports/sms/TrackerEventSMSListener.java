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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.listener.CompressionSMSListener;
import org.hisp.dhis.sms.listener.SMSProcessingException;
import org.hisp.dhis.smscompression.SmsConsts.SmsEventStatus;
import org.hisp.dhis.smscompression.SmsConsts.SubmissionType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.GeoPoint;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.smscompression.models.TrackerEventSmsSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event.EventBuilder;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.tracker.sms.TrackerEventSMSListener")
@Transactional
public class TrackerEventSMSListener extends CompressionSMSListener {
  private final TrackerImportService trackerImportService;

  public TrackerEventSMSListener(
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      UserService userService,
      TrackedEntityTypeService trackedEntityTypeService,
      TrackedEntityAttributeService trackedEntityAttributeService,
      ProgramService programService,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      DataElementService dataElementService,
      IdentifiableObjectManager identifiableObjectManager,
      TrackerImportService trackerImportService) {
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
    this.trackerImportService = trackerImportService;
  }

  @Override
  protected SmsResponse postProcess(IncomingSms sms, SmsSubmission submission, User user)
      throws SMSProcessingException {
    TrackerEventSmsSubmission subm = (TrackerEventSmsSubmission) submission;

    EventBuilder event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .event(subm.getEvent() != null ? subm.getEvent().getUid() : null)
            .enrollment(subm.getEnrollment().getUid())
            .orgUnit(MetadataIdentifier.ofUid(subm.getOrgUnit().getUid()))
            .programStage(MetadataIdentifier.ofUid(subm.getProgramStage().getUid()))
            .attributeOptionCombo(MetadataIdentifier.ofUid(subm.getAttributeOptionCombo().getUid()))
            .storedBy(user.getUsername())
            .occurredAt(subm.getEventDate() != null ? subm.getEventDate().toInstant() : null)
            .scheduledAt(subm.getDueDate() != null ? subm.getDueDate().toInstant() : null)
            .status(map(subm.getEventStatus()))
            .geometry(map(subm.getCoordinates()))
            .dataValues(map(user, subm.getValues()));

    TrackerImportParams params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
            .build();
    TrackerObjects trackerObjects = TrackerObjects.builder().events(List.of(event.build())).build();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    if (Status.OK == importReport.getStatus()) {
      return SmsResponse.SUCCESS;
    }
    // TODO(DHIS2-18003) we need to map tracker import report errors/warnings to an sms
    return SmsResponse.INVALID_EVENT.set(subm.getEvent());
  }

  private EventStatus map(SmsEventStatus eventStatus) {
    return switch (eventStatus) {
      case ACTIVE -> EventStatus.ACTIVE;
      case COMPLETED -> EventStatus.COMPLETED;
      case VISITED -> EventStatus.VISITED;
      case SCHEDULE -> EventStatus.SCHEDULE;
      case OVERDUE -> EventStatus.OVERDUE;
      case SKIPPED -> EventStatus.SKIPPED;
    };
  }

  private Geometry map(GeoPoint coordinates) {
    if (coordinates == null) {
      return null;
    }

    return new GeometryFactory()
        .createPoint(new Coordinate(coordinates.getLongitude(), coordinates.getLatitude()));
  }

  private Set<DataValue> map(User user, List<SmsDataValue> dataValues) {
    if (CollectionUtils.isEmpty(dataValues)) {
      return Set.of();
    }

    return dataValues.stream()
        .map(
            dv ->
                DataValue.builder()
                    .dataElement(MetadataIdentifier.ofUid(dv.getDataElement().getUid()))
                    .value(dv.getValue())
                    .storedBy(user.getUsername())
                    .build())
        .collect(Collectors.toSet());
  }

  @Override
  protected boolean handlesType(SubmissionType type) {
    return (type == SubmissionType.TRACKER_EVENT);
  }
}
