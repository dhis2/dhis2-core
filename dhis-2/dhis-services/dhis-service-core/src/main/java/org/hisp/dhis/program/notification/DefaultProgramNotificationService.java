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
package org.hisp.dhis.program.notification;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.program.notification.NotificationTrigger.PROGRAM_RULE;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.notification.template.snapshot.NotificationTemplateMapper;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@Service("org.hisp.dhis.program.notification.ProgramNotificationService")
public class DefaultProgramNotificationService extends HibernateGenericStore<TrackerEvent>
    implements ProgramNotificationService {
  private static final Predicate<NotificationInstanceWithTemplate> IS_SCHEDULED_BY_PROGRAM_RULE =
      (iwt) ->
          Objects.nonNull(iwt.getProgramNotificationInstance())
              && PROGRAM_RULE.equals(iwt.getProgramNotificationTemplate().getNotificationTrigger());

  private static final Set<NotificationTrigger> SCHEDULED_EVENT_TRIGGERS =
      Sets.intersection(
          NotificationTrigger.getAllApplicableToEvent(),
          NotificationTrigger.getAllScheduledTriggers());

  private static final Set<NotificationTrigger> SCHEDULED_ENROLLMENT_TRIGGERS =
      Sets.intersection(
          NotificationTrigger.getAllApplicableToEnrollment(),
          NotificationTrigger.getAllScheduledTriggers());

  private final ProgramMessageService programMessageService;

  private final MessageService messageService;

  private final IdentifiableObjectManager manager;

  private final NotificationMessageRenderer<Enrollment> programNotificationRenderer;

  private final NotificationMessageRenderer<TrackerEvent> programStageNotificationRenderer;

  private final ProgramNotificationTemplateService notificationTemplateService;

  private final NotificationTemplateMapper notificationTemplateMapper;

  private final ProgramNotificationInstanceService notificationInstanceService;

  public DefaultProgramNotificationService(
      ProgramMessageService programMessageService,
      MessageService messageService,
      IdentifiableObjectManager manager,
      NotificationMessageRenderer<Enrollment> programNotificationRenderer,
      NotificationMessageRenderer<TrackerEvent> programStageNotificationRenderer,
      ProgramNotificationTemplateService notificationTemplateService,
      NotificationTemplateMapper notificationTemplateMapper,
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      ProgramNotificationInstanceService notificationInstanceService) {
    super(entityManager, jdbcTemplate, publisher, TrackerEvent.class, false);
    this.programMessageService = programMessageService;
    this.messageService = messageService;
    this.manager = manager;
    this.programNotificationRenderer = programNotificationRenderer;
    this.programStageNotificationRenderer = programStageNotificationRenderer;
    this.notificationTemplateService = notificationTemplateService;
    this.notificationTemplateMapper = notificationTemplateMapper;
    this.notificationInstanceService = notificationInstanceService;
  }

  @Override
  @Transactional
  public void sendScheduledNotificationsForDay(Date notificationDate, JobProgress progress) {
    progress.startingStage("Fetching and filtering scheduled templates ");
    List<ProgramNotificationTemplate> scheduledTemplates =
        progress.runStage(List.of(), this::getScheduledTemplates);

    progress.startingStage(
        "Processing ProgramStageNotification messages",
        scheduledTemplates.size(),
        SKIP_ITEM_OUTLIER);
    AtomicInteger totalMessageCount = new AtomicInteger();
    progress.runStage(
        scheduledTemplates.stream(),
        template -> "Processing template " + template.getName(),
        template -> {
          MessageBatch batch = createScheduledMessageBatchForDay(template, notificationDate);
          sendAll(batch);
          totalMessageCount.addAndGet(batch.messageCount());
        },
        (success, failed) -> format("Created and sent %d messages", totalMessageCount.get()));
  }

  @Override
  @Transactional
  public void sendScheduledNotifications(JobProgress progress) {
    progress.startingStage(
        "Fetching and filtering ProgramStageNotification messages scheduled by program rules");

    ProgramNotificationInstanceParam param =
        ProgramNotificationInstanceParam.builder()
            .scheduledAt(DateUtils.removeTimeStamp(new Date()))
            .build();

    List<NotificationInstanceWithTemplate> instancesWithTemplates =
        progress.runStage(
            List.of(),
            () ->
                notificationInstanceService.getProgramNotificationInstances(param).stream()
                    .map(this::withTemplate)
                    .filter(this::hasTemplate)
                    .filter(IS_SCHEDULED_BY_PROGRAM_RULE)
                    .collect(toList()));

    progress.startingStage(
        "Processing ProgramStageNotification messages scheduled by program rules",
        instancesWithTemplates.size(),
        SKIP_ITEM_OUTLIER);
    if (instancesWithTemplates.isEmpty()) {
      progress.completedStage("No instances with templates found");
      return;
    }

    List<MessageBatch> batches =
        progress.runStage(
            List.of(),
            () -> {
              Stream<MessageBatch> enrollmentBatches =
                  instancesWithTemplates.stream()
                      .filter(this::hasEnrollment)
                      .map(
                          iwt ->
                              createEnrollmentMessageBatch(
                                  iwt.getProgramNotificationTemplate(),
                                  List.of(iwt.getProgramNotificationInstance().getEnrollment())));

              Stream<MessageBatch> eventBatches =
                  instancesWithTemplates.stream()
                      .filter(this::hasEvent)
                      .map(
                          iwt ->
                              createEventMessageBatch(
                                  iwt.getProgramNotificationTemplate(),
                                  List.of(iwt.getProgramNotificationInstance().getEvent())));

              return Stream.concat(enrollmentBatches, eventBatches).collect(toList());
            });

    progress.startingStage("Sending message batches", batches.size(), SKIP_ITEM_OUTLIER);
    progress.runStage(
        batches.stream(),
        batch ->
            format(
                "Sending batch with %d DHIS messages and %d program messages",
                batch.dhisMessages.size(), batch.programMessages.size()),
        this::sendAll,
        (success, failed) ->
            format(
                "Created and sent %d messages",
                batches.stream().mapToInt(MessageBatch::messageCount).sum()));
  }

  private boolean hasEvent(NotificationInstanceWithTemplate notificationInstanceWithTemplate) {
    return Optional.of(notificationInstanceWithTemplate)
        .map(NotificationInstanceWithTemplate::getProgramNotificationInstance)
        .filter(ProgramNotificationInstance::hasEvent)
        .isPresent();
  }

  private boolean hasEnrollment(NotificationInstanceWithTemplate instanceWithTemplate) {
    return Optional.of(instanceWithTemplate)
        .map(NotificationInstanceWithTemplate::getProgramNotificationInstance)
        .filter(ProgramNotificationInstance::hasEnrollment)
        .isPresent();
  }

  private boolean hasTemplate(NotificationInstanceWithTemplate instanceWithTemplate) {
    if (Objects.isNull(instanceWithTemplate.getProgramNotificationTemplate())) {
      log.warn(
          "Cannot process scheduled notification with id: {} since it has no associated templates",
          instanceWithTemplate.getProgramNotificationInstance().getId());
      return false;
    }
    return true;
  }

  private NotificationInstanceWithTemplate withTemplate(
      ProgramNotificationInstance programNotificationInstance) {
    return NotificationInstanceWithTemplate.builder()
        .programNotificationInstance(programNotificationInstance)
        .programNotificationTemplate(getApplicableTemplate(programNotificationInstance))
        .build();
  }

  private ProgramNotificationTemplate getApplicableTemplate(
      ProgramNotificationInstance programNotificationInstance) {
    return Optional.of(programNotificationInstance)
        .map(ProgramNotificationInstance::getProgramNotificationTemplateSnapshot)
        .map(notificationTemplateMapper::toProgramNotificationTemplate)
        .orElseGet(() -> this.getDatabaseTemplate(programNotificationInstance));
  }

  private ProgramNotificationTemplate getDatabaseTemplate(
      ProgramNotificationInstance programNotificationInstance) {
    log.warn("Couldn't use template from jsonb column, using the one from database if possible");
    if (Objects.nonNull(programNotificationInstance.getProgramNotificationTemplateId())) {
      ProgramNotificationTemplate programNotificationTemplate =
          notificationTemplateService.get(
              programNotificationInstance.getProgramNotificationTemplateId());
      if (Objects.isNull(programNotificationTemplate)) {
        log.warn(
            "Unable to load program notification template from database, because it might have been deleted.");
      }
      return programNotificationTemplate;
    }
    return null;
  }

  @Override
  @Transactional
  public void sendEventCompletionNotifications(long eventId) {
    sendEventNotifications(manager.get(TrackerEvent.class, eventId));
  }

  @Override
  @Transactional
  public void sendEnrollmentCompletionNotifications(long enrollment) {
    sendEnrollmentNotifications(
        manager.get(Enrollment.class, enrollment), NotificationTrigger.COMPLETION);
  }

  @Override
  @Transactional
  public void sendEnrollmentNotifications(long enrollment) {
    sendEnrollmentNotifications(
        manager.get(Enrollment.class, enrollment), NotificationTrigger.ENROLLMENT);
  }

  @Override
  @Transactional
  public void sendProgramRuleTriggeredNotifications(
      ProgramNotificationTemplate template, Enrollment enrollment) {
    MessageBatch messageBatch =
        createEnrollmentMessageBatch(template, Collections.singletonList(enrollment));
    sendAll(messageBatch);
  }

  @Override
  @Transactional
  public void sendProgramRuleTriggeredEventNotifications(
      ProgramNotificationTemplate template, TrackerEvent event) {
    MessageBatch messageBatch = createEventMessageBatch(template, Collections.singletonList(event));
    sendAll(messageBatch);
  }

  @Override
  public List<TrackerEvent> getWithScheduledNotifications(
      ProgramNotificationTemplate template, Date notificationDate) {
    if (notificationDate == null
        || !SCHEDULED_EVENT_TRIGGERS.contains(template.getNotificationTrigger())) {
      return List.of();
    }

    if (template.getRelativeScheduledDays() == null) {
      return List.of();
    }

    Date targetDate =
        org.apache.commons.lang3.time.DateUtils.addDays(
            notificationDate, template.getRelativeScheduledDays() * -1);

    String hql =
        "select distinct ev from TrackerEvent as ev "
            + "inner join ev.programStage as ps "
            + "where :notificationTemplate in elements(ps.notificationTemplates) "
            + "and ev.scheduledDate is not null "
            + "and ev.occurredDate is null "
            + "and ev.status != :skippedEventStatus "
            + "and cast(:targetDate as date) = ev.scheduledDate "
            + "and ev.deleted is false";

    return getQuery(hql)
        .setParameter("notificationTemplate", template)
        .setParameter("skippedEventStatus", EventStatus.SKIPPED)
        .setParameter("targetDate", targetDate)
        .list();
  }

  @Override
  protected void preProcessPredicates(
      CriteriaBuilder builder,
      List<Function<Root<TrackerEvent>, jakarta.persistence.criteria.Predicate>> predicates) {
    predicates.add(root -> builder.equal(root.get("deleted"), false));
  }

  @Override
  protected TrackerEvent postProcessObject(TrackerEvent event) {
    return (event == null || event.isDeleted()) ? null : event;
  }

  private MessageBatch createScheduledMessageBatchForDay(
      ProgramNotificationTemplate template, Date day) {
    List<TrackerEvent> events = getWithScheduledNotifications(template, day);

    List<Enrollment> enrollments = getEnrollmentsWithScheduledNotifications(template, day);

    MessageBatch eventBatch = createEventMessageBatch(template, events);
    MessageBatch psBatch = createEnrollmentMessageBatch(template, enrollments);

    return new MessageBatch(eventBatch, psBatch);
  }

  @Override
  public List<Enrollment> getEnrollmentsWithScheduledNotifications(
      ProgramNotificationTemplate template, Date notificationDate) {
    if (notificationDate == null
        || !SCHEDULED_ENROLLMENT_TRIGGERS.contains(template.getNotificationTrigger())) {
      return Lists.newArrayList();
    }

    String dateProperty = toDateProperty(template.getNotificationTrigger());

    if (dateProperty == null) {
      return Lists.newArrayList();
    }

    Date targetDate =
        org.apache.commons.lang3.time.DateUtils.addDays(
            notificationDate, template.getRelativeScheduledDays() * -1);

    String hql =
        "select distinct en from Enrollment as en "
            + "inner join en.program as p "
            + "where :notificationTemplate in elements(p.notificationTemplates) "
            + "and en."
            + dateProperty
            + " is not null "
            + "and en.status = :activeEnrollmentStatus "
            + "and cast(:targetDate as date) = en."
            + dateProperty;

    return getSession()
        .createQuery(hql, Enrollment.class)
        .setParameter("notificationTemplate", template)
        .setParameter("activeEnrollmentStatus", EnrollmentStatus.ACTIVE)
        .setParameter("targetDate", targetDate)
        .list();
  }

  private String toDateProperty(NotificationTrigger trigger) {
    if (trigger == NotificationTrigger.SCHEDULED_DAYS_ENROLLMENT_DATE) {
      return "enrollmentDate";
    } else if (trigger == NotificationTrigger.SCHEDULED_DAYS_INCIDENT_DATE) {
      return "occurredDate";
    }

    return null;
  }

  private List<ProgramNotificationTemplate> getScheduledTemplates() {
    return manager.getAll(ProgramNotificationTemplate.class).stream()
        .filter(n -> n.getNotificationTrigger().isScheduled())
        .collect(toList());
  }

  private void sendEventNotifications(TrackerEvent event) {
    if (event == null) {
      return;
    }

    Set<ProgramNotificationTemplate> templates = resolveTemplates(event);

    if (templates.isEmpty()) {
      return;
    }

    for (ProgramNotificationTemplate template : templates) {
      MessageBatch batch = createEventMessageBatch(template, Lists.newArrayList(event));
      sendAll(batch);
    }
  }

  private void sendEnrollmentNotifications(Enrollment enrollment, NotificationTrigger trigger) {
    if (enrollment == null) {
      return;
    }

    Set<ProgramNotificationTemplate> templates = resolveTemplates(enrollment, trigger);

    for (ProgramNotificationTemplate template : templates) {
      MessageBatch batch = createEnrollmentMessageBatch(template, Lists.newArrayList(enrollment));
      sendAll(batch);
    }
  }

  private MessageBatch createEventMessageBatch(
      ProgramNotificationTemplate template, List<TrackerEvent> events) {
    MessageBatch batch = new MessageBatch();

    if (template.getNotificationRecipient().isExternalRecipient()) {
      batch.programMessages.addAll(
          events.stream()
              .map(event -> createProgramMessage(event, template))
              .collect(Collectors.toSet()));
    } else {
      batch.dhisMessages.addAll(
          events.stream()
              .map(event -> createDhisMessage(event, template))
              .collect(Collectors.toSet()));
    }

    return batch;
  }

  private MessageBatch createEnrollmentMessageBatch(
      ProgramNotificationTemplate template, List<Enrollment> enrollments) {
    MessageBatch batch = new MessageBatch();

    if (template.getNotificationRecipient().isExternalRecipient()) {
      batch.programMessages.addAll(
          enrollments.stream()
              .map(e -> createProgramMessage(e, template))
              .collect(Collectors.toSet()));
    } else {
      batch.dhisMessages.addAll(
          enrollments.stream()
              .map(ps -> createDhisMessage(ps, template))
              .collect(Collectors.toSet()));
    }

    return batch;
  }

  private ProgramMessage createProgramMessage(
      TrackerEvent event, ProgramNotificationTemplate template) {
    NotificationMessage message = programStageNotificationRenderer.render(event, template);

    return ProgramMessage.builder()
        .subject(message.getSubject())
        .text(message.getMessage())
        .recipients(
            resolveProgramStageNotificationRecipients(template, event.getOrganisationUnit(), event))
        .deliveryChannels(Sets.newHashSet(template.getDeliveryChannels()))
        .event(event)
        .notificationTemplate(Optional.ofNullable(template.getUid()).orElse(StringUtils.EMPTY))
        .build();
  }

  private ProgramMessage createProgramMessage(
      Enrollment enrollment, ProgramNotificationTemplate template) {
    NotificationMessage message = programNotificationRenderer.render(enrollment, template);

    return ProgramMessage.builder()
        .subject(message.getSubject())
        .text(message.getMessage())
        .recipients(
            resolveProgramNotificationRecipients(
                template, enrollment.getOrganisationUnit(), enrollment))
        .deliveryChannels(Sets.newHashSet(template.getDeliveryChannels()))
        .enrollment(enrollment)
        .notificationTemplate(Optional.ofNullable(template.getUid()).orElse(StringUtils.EMPTY))
        .build();
  }

  private Set<User> resolveDhisMessageRecipients(
      ProgramNotificationTemplate template,
      @Nullable Enrollment enrollment,
      @Nullable TrackerEvent event) {
    if (enrollment == null && event == null) {
      throw new IllegalArgumentException(
          "Either of the arguments [enrollment, event] must be non-null");
    }

    Set<User> userGroupMembers = Sets.newHashSet();

    OrganisationUnit orgUnit =
        enrollment != null ? enrollment.getOrganisationUnit() : event.getOrganisationUnit();

    Set<OrganisationUnit> orgUnitInHierarchy = Sets.newHashSet();

    ProgramNotificationRecipient recipientType = template.getNotificationRecipient();

    if (recipientType == ProgramNotificationRecipient.USER_GROUP) {
      userGroupMembers =
          Optional.ofNullable(template)
              .map(ProgramNotificationTemplate::getRecipientUserGroup)
              .map(UserGroup::getMembers)
              .orElse(userGroupMembers);

      final boolean limitToHierarchy =
          BooleanUtils.toBoolean(template.getNotifyUsersInHierarchyOnly());

      final boolean parentOrgUnitOnly =
          BooleanUtils.toBoolean(template.getNotifyParentOrganisationUnitOnly());

      if (limitToHierarchy) {
        orgUnitInHierarchy.add(orgUnit);
        orgUnitInHierarchy.addAll(orgUnit.getAncestors());

        return userGroupMembers.stream()
            .filter(r -> orgUnitInHierarchy.contains(r.getOrganisationUnit()))
            .collect(Collectors.toSet());

      } else if (parentOrgUnitOnly) {

        OrganisationUnit parentOrgUnit = orgUnit.getParent();

        return userGroupMembers.stream()
            .filter(u -> u.getOrganisationUnit().equals(parentOrgUnit))
            .collect(Collectors.toSet());
      }

      userGroupMembers.addAll(template.getRecipientUserGroup().getMembers());
    } else if (recipientType == ProgramNotificationRecipient.USERS_AT_ORGANISATION_UNIT) {
      userGroupMembers.addAll(orgUnit.getUsers());
    }

    // filter out all users that are disabled
    userGroupMembers.removeIf(User::isDisabled);

    return userGroupMembers;
  }

  private ProgramMessageRecipients resolveProgramNotificationRecipients(
      ProgramNotificationTemplate template,
      OrganisationUnit organisationUnit,
      Enrollment enrollment) {
    return resolveRecipients(template, organisationUnit, enrollment.getTrackedEntity(), enrollment);
  }

  private ProgramMessageRecipients resolveProgramStageNotificationRecipients(
      ProgramNotificationTemplate template, OrganisationUnit organisationUnit, TrackerEvent event) {
    ProgramMessageRecipients recipients = new ProgramMessageRecipients();

    if (template.getNotificationRecipient() == ProgramNotificationRecipient.DATA_ELEMENT
        && template.getRecipientDataElement() != null) {
      List<String> recipientList =
          event.getEventDataValues().stream()
              .filter(dv -> template.getRecipientDataElement().getUid().equals(dv.getDataElement()))
              .map(EventDataValue::getValue)
              .toList();

      if (template.getDeliveryChannels().contains(DeliveryChannel.SMS)) {
        recipients.getPhoneNumbers().addAll(recipientList);
      } else if (template.getDeliveryChannels().contains(DeliveryChannel.EMAIL)) {
        recipients.getEmailAddresses().addAll(recipientList);
      }

      return recipients;
    } else {
      TrackedEntity trackedEntity = event.getEnrollment().getTrackedEntity();

      return resolveRecipients(template, organisationUnit, trackedEntity, event.getEnrollment());
    }
  }

  private ProgramMessageRecipients resolveRecipients(
      ProgramNotificationTemplate template,
      OrganisationUnit ou,
      TrackedEntity te,
      Enrollment enrollment) {
    ProgramMessageRecipients recipients = new ProgramMessageRecipients();

    ProgramNotificationRecipient recipientType = template.getNotificationRecipient();

    if (recipientType == ProgramNotificationRecipient.ORGANISATION_UNIT_CONTACT) {
      recipients.setOrganisationUnit(ou);
    } else if (recipientType == ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE) {
      recipients.setTrackedEntity(te);
    } else if (recipientType == ProgramNotificationRecipient.PROGRAM_ATTRIBUTE
        && template.getRecipientProgramAttribute() != null) {
      List<String> recipientList =
          enrollment.getTrackedEntity().getTrackedEntityAttributeValues().stream()
              .filter(
                  av ->
                      template
                          .getRecipientProgramAttribute()
                          .getUid()
                          .equals(av.getAttribute().getUid()))
              .map(TrackedEntityAttributeValue::getPlainValue)
              .toList();

      if (template.getDeliveryChannels().contains(DeliveryChannel.SMS)) {
        recipients.getPhoneNumbers().addAll(recipientList);
      } else if (template.getDeliveryChannels().contains(DeliveryChannel.EMAIL)) {
        recipients.getEmailAddresses().addAll(recipientList);
      }
    }

    return recipients;
  }

  private Set<ProgramNotificationTemplate> resolveTemplates(
      Enrollment enrollment, final NotificationTrigger trigger) {
    return enrollment.getProgram().getNotificationTemplates().stream()
        .filter(t -> t.getNotificationTrigger() == trigger)
        .collect(Collectors.toSet());
  }

  private Set<ProgramNotificationTemplate> resolveTemplates(TrackerEvent event) {
    return event.getProgramStage().getNotificationTemplates().stream()
        .filter(t -> t.getNotificationTrigger() == NotificationTrigger.COMPLETION)
        .collect(Collectors.toSet());
  }

  private DhisMessage createDhisMessage(TrackerEvent event, ProgramNotificationTemplate template) {
    DhisMessage dhisMessage = new DhisMessage();

    dhisMessage.message = programStageNotificationRenderer.render(event, template);
    dhisMessage.recipients = resolveDhisMessageRecipients(template, null, event);

    return dhisMessage;
  }

  private DhisMessage createDhisMessage(
      Enrollment enrollment, ProgramNotificationTemplate template) {
    DhisMessage dhisMessage = new DhisMessage();

    dhisMessage.message = programNotificationRenderer.render(enrollment, template);

    dhisMessage.recipients = resolveDhisMessageRecipients(template, enrollment, null);

    return dhisMessage;
  }

  private void sendDhisMessages(Set<DhisMessage> messages) {
    messages.forEach(
        m ->
            messageService.sendMessage(
                new MessageConversationParams.Builder(
                        m.recipients,
                        null,
                        m.message.getSubject(),
                        m.message.getMessage(),
                        MessageType.SYSTEM,
                        null)
                    .withForceNotification(true)
                    .build()));
  }

  private void sendProgramMessages(Set<ProgramMessage> messages) {
    if (messages.isEmpty()) {
      return;
    }

    log.debug(format("Dispatching %d ProgramMessages", messages.size()));

    BatchResponseStatus status = programMessageService.sendMessages(Lists.newArrayList(messages));

    log.debug(format("Resulting status from ProgramMessageService:%n %s", status.toString()));
  }

  private void sendAll(MessageBatch messageBatch) {
    sendDhisMessages(messageBatch.dhisMessages);
    sendProgramMessages(messageBatch.programMessages);
  }

  // -------------------------------------------------------------------------
  // Internal classes
  // -------------------------------------------------------------------------

  private static class DhisMessage {
    NotificationMessage message;

    Set<User> recipients;
  }

  private static class MessageBatch {
    Set<DhisMessage> dhisMessages = Sets.newHashSet();

    Set<ProgramMessage> programMessages = Sets.newHashSet();

    MessageBatch(MessageBatch... batches) {
      for (MessageBatch batch : batches) {
        dhisMessages.addAll(batch.dhisMessages);
        programMessages.addAll(batch.programMessages);
      }
    }

    int messageCount() {
      return dhisMessages.size() + programMessages.size();
    }
  }

  @Data
  @Builder
  static class NotificationInstanceWithTemplate {
    private final ProgramNotificationInstance programNotificationInstance;

    private final ProgramNotificationTemplate programNotificationTemplate;
  }
}
