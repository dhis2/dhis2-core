package org.hisp.dhis.tracker.imports.sms;

import static java.util.Objects.requireNonNullElseGet;

import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.notification.event.ProgramEnrollmentNotificationEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.springframework.context.ApplicationEventPublisher;

@RequiredArgsConstructor
@Deprecated (since = "2.42")
//TODO(tracker) This class will be removed as soon as the SMS feature uses the importer
public class SMSEnrollmentService {

  private final IdentifiableObjectManager manager;
  private final TrackerOwnershipManager trackerOwnershipAccessManager;
  private final ApplicationEventPublisher eventPublisher;
  private final TrackedEntityService trackedEntityService;

  public Enrollment enrollTrackedEntity(
      TrackedEntity trackedEntity,
      Program program,
      OrganisationUnit organisationUnit,
      Date occurredDate) {
    Enrollment enrollment =
        prepareEnrollment(
            trackedEntity,
            program,
            occurredDate,
            organisationUnit);
    manager.save(enrollment);
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntity, program, organisationUnit, true, true);
    eventPublisher.publishEvent(new ProgramEnrollmentNotificationEvent(this, enrollment.getId()));
    manager.update(enrollment);
    trackedEntityService.updateTrackedEntity(trackedEntity);

    return enrollment;
  }

  private Enrollment prepareEnrollment(
      TrackedEntity trackedEntity,
      Program program,
      Date occurredDate,
      OrganisationUnit organisationUnit) {
    if (program.getTrackedEntityType() != null
        && !program.getTrackedEntityType().equals(trackedEntity.getTrackedEntityType())) {
      throw new IllegalQueryException(
          "Tracked entity must have same tracked entity as program: " + program.getUid());
    }

    Enrollment enrollment = new Enrollment();
    enrollment.setUid(CodeGenerator.generateUid());
    enrollment.setOrganisationUnit(organisationUnit);
    enrollment.enrollTrackedEntity(trackedEntity, program);
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(requireNonNullElseGet(occurredDate, Date::new));
    enrollment.setStatus(EnrollmentStatus.ACTIVE);

    return enrollment;
  }
}
