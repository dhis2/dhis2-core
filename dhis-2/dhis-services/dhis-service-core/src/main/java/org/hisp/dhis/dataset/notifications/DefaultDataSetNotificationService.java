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
package org.hisp.dhis.dataset.notifications;

import static java.lang.String.format;
import static java.util.stream.Collectors.*;
import static org.hisp.dhis.program.notification.NotificationTrigger.SCHEDULED_DAYS_DUE_DATE;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import com.google.common.base.Function;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.notification.SendStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Created by zubair on 04.07.17. */
@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class DefaultDataSetNotificationService implements DataSetNotificationService {
  private static final String SUMMARY_TEXT =
      "Organisation units : %d" + TextUtils.LN + "Period : %s" + TextUtils.LN + "DataSet : %s";

  private static final String SUMMARY_SUBJECT = " DataSet Summary";

  private static final String PENDING = "Pending";

  private static final String OVERDUE = "Overdue";

  private static final String TEXT_SEPARATOR = TextUtils.LN + TextUtils.LN;

  private final Map<
          DeliveryChannel,
          BiFunction<Set<OrganisationUnit>, ProgramMessageRecipients, ProgramMessageRecipients>>
      RECIPIENT_MAPPER =
          Map.of(
              DeliveryChannel.SMS,
              this::resolvePhoneNumbers,
              DeliveryChannel.EMAIL,
              this::resolveEmails);

  private final Map<Boolean, Function<DataSetNotificationTemplate, Integer>> DAYS_RESOLVER =
      Map.of(
          // Overdue reminder
          false,
          DataSetNotificationTemplate::getRelativeScheduledDays,
          // Future reminder
          true,
          template -> template.getRelativeScheduledDays() * -1);

  private final Map<DeliveryChannel, Predicate<OrganisationUnit>> VALIDATOR =
      Map.of(
          // Valid Ou phoneNumber
          DeliveryChannel.SMS, ou -> ou.getPhoneNumber() != null && !ou.getPhoneNumber().isEmpty(),
          // Valid Ou Email
          DeliveryChannel.EMAIL, ou -> ou.getEmail() != null && !ou.getEmail().isEmpty());

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final DataSetNotificationTemplateService dsntService;

  private final MessageService internalMessageService;

  private final ProgramMessageService externalMessageService;

  private final NotificationMessageRenderer<CompleteDataSetRegistration> renderer;

  private final CompleteDataSetRegistrationService completeDataSetRegistrationService;

  private final PeriodService periodService;

  private final CategoryService categoryService;

  private final I18nManager i18nManager;

  private final OrganisationUnitService organisationUnitService;

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public void sendScheduledDataSetNotificationsForDay(Date day, JobProgress progress) {
    List<DataSetNotificationTemplate> templates =
        dsntService.getScheduledNotifications(SCHEDULED_DAYS_DUE_DATE);

    if (templates == null || templates.isEmpty()) {
      log.info("No template found");
      return;
    }
    sendBatch("single", createBatchForSingleNotifications(templates, progress), progress);
    sendBatch("summary", createBatchForSummaryNotifications(templates, progress), progress);
  }

  @Override
  public void sendCompleteDataSetNotifications(CompleteDataSetRegistration registration) {
    if (registration == null) {
      return;
    }

    List<DataSetNotificationTemplate> templates =
        dsntService.getCompleteNotifications(registration.getDataSet());

    if (templates == null || templates.isEmpty()) {
      log.info("No template found");
      return;
    }
    sendBatch(
        "completion",
        createBatchForCompletionNotifications(registration, templates),
        NoopJobProgress.INSTANCE);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private MessageBatch createBatchForSummaryNotifications(
      Collection<DataSetNotificationTemplate> templates, JobProgress progress) {
    progress.startingStage("Creating summary batch from %d templates");
    MessageBatch batch = new MessageBatch();

    StringBuilder msgText = new StringBuilder();

    long pendingOus;

    for (Iterator<DataSetNotificationTemplate> it =
            templates.stream()
                .filter(t -> t.getSendStrategy() == SendStrategy.COLLECTIVE_SUMMARY)
                .iterator();
        it.hasNext(); ) {
      DataSetNotificationTemplate template = it.next();
      for (DataSet dataSet : template.getDataSets()) {
        if (isValidForSending(getDataSetPeriod(dataSet), template)) {
          pendingOus =
              dataSet.getSources().stream()
                  .filter(ou -> !isCompleted(createRespectiveRegistrationObject(dataSet, ou)))
                  .count();

          String itemMsg =
              format(
                  SUMMARY_TEXT,
                  pendingOus,
                  getPeriodString(dataSet.getPeriodType().createPeriod()),
                  dataSet.getName());
          msgText.append(itemMsg).append(TEXT_SEPARATOR);
        }
      }

      if (msgText.length() > 0) {
        batch.dhisMessages.add(
            new DhisMessage(
                new NotificationMessage(createSubjectString(template), msgText.toString()),
                resolveInternalRecipients(template)));

        msgText.setLength(0);
      }
    }
    progress.completedStage(
        format("%d summary dataset notifications created.", batch.dhisMessages.size()));

    return batch;
  }

  private MessageBatch createBatchForCompletionNotifications(
      CompleteDataSetRegistration registration, Collection<DataSetNotificationTemplate> templates) {
    return createMessageBatch(
        templates.stream().map(t -> Map.of(registration, t)).collect(toList()));
  }

  private String createSubjectString(DataSetNotificationTemplate template) {
    return template.getRelativeScheduledDays() < 0
        ? PENDING + SUMMARY_SUBJECT
        : OVERDUE + SUMMARY_SUBJECT;
  }

  private Period getDataSetPeriod(DataSet dataSet) {
    Period period = dataSet.getPeriodType().createPeriod();

    return periodService.getPeriod(
        period.getStartDate(), period.getEndDate(), period.getPeriodType());
  }

  private CompleteDataSetRegistration createRespectiveRegistrationObject(
      DataSet dataSet, OrganisationUnit ou) {
    Period period = dataSet.getPeriodType().createPeriod();

    CompleteDataSetRegistration registration = new CompleteDataSetRegistration();
    registration.setDataSet(dataSet);
    registration.setPeriod(
        periodService.getPeriod(
            period.getStartDate(), period.getEndDate(), period.getPeriodType()));
    registration.setPeriodName(getPeriodString(registration.getPeriod()));
    registration.setAttributeOptionCombo(categoryService.getDefaultCategoryOptionCombo());
    registration.setSource(ou);

    return registration;
  }

  private String getPeriodString(Period period) {
    I18nFormat format = i18nManager.getI18nFormat();

    return format.formatPeriod(period);
  }

  private List<Map<CompleteDataSetRegistration, DataSetNotificationTemplate>> createGroupedByMapper(
      Iterable<DataSetNotificationTemplate> templates) {
    List<Map<CompleteDataSetRegistration, DataSetNotificationTemplate>> dataSetMapList =
        new ArrayList<>();

    for (DataSetNotificationTemplate template : templates) {
      Map<CompleteDataSetRegistration, DataSetNotificationTemplate> mapper = new HashMap<>();

      for (DataSet dataSet : template.getDataSets()) {
        dataSet.getSources().stream()
            .map(ou -> createRespectiveRegistrationObject(dataSet, ou))
            .filter(r -> isScheduledNow(r, template))
            .forEach(r -> mapper.put(r, template));
      }

      dataSetMapList.add(mapper);
    }

    return dataSetMapList;
  }

  private boolean isScheduledNow(
      CompleteDataSetRegistration registration, DataSetNotificationTemplate template) {
    return !isCompleted(registration) && isValidForSending(registration.getPeriod(), template);
  }

  private boolean isCompleted(CompleteDataSetRegistration registration) {
    CompleteDataSetRegistration completed =
        completeDataSetRegistrationService.getCompleteDataSetRegistration(
            registration.getDataSet(),
            registration.getPeriod(),
            registration.getSource(),
            registration.getAttributeOptionCombo());

    return completed != null && completed.getCompleted();
  }

  private boolean isValidForSending(Period period, DataSetNotificationTemplate template) {
    int daysToCompare;

    Date dueDate = period.getEndDate();

    daysToCompare = DAYS_RESOLVER.get(template.getRelativeScheduledDays() < 0).apply(template);

    return DateUtils.daysBetween(new Date(), dueDate) == daysToCompare;
  }

  private ProgramMessageRecipients resolvePhoneNumbers(
      Set<OrganisationUnit> ous, ProgramMessageRecipients pmr) {
    pmr.setPhoneNumbers(ous.stream().map(OrganisationUnit::getPhoneNumber).collect(toSet()));

    return pmr;
  }

  private ProgramMessageRecipients resolveEmails(
      Set<OrganisationUnit> ous, ProgramMessageRecipients pmr) {
    pmr.setEmailAddresses(ous.stream().map(OrganisationUnit::getEmail).collect(toSet()));

    return pmr;
  }

  private MessageBatch createBatchForSingleNotifications(
      Collection<DataSetNotificationTemplate> templates, JobProgress progress) {
    progress.startingStage(
        format("Creating single notification batch from %d templates", templates.size()));

    List<DataSetNotificationTemplate> singleTemplates =
        templates.stream()
            .filter(t -> t.getSendStrategy() == SendStrategy.SINGLE_NOTIFICATION)
            .collect(toList());
    MessageBatch batch = createMessageBatch(createGroupedByMapper(singleTemplates));

    progress.completedStage(
        format(
            "Number of SINGLE notifications created: %d",
            batch.programMessages.size() + batch.dhisMessages.size()));
    return batch;
  }

  private MessageBatch createMessageBatch(
      List<Map<CompleteDataSetRegistration, DataSetNotificationTemplate>> pairs) {
    MessageBatch batch = new MessageBatch();
    for (Map<CompleteDataSetRegistration, DataSetNotificationTemplate> pair : pairs) {
      for (Entry<CompleteDataSetRegistration, DataSetNotificationTemplate> entry :
          pair.entrySet()) {
        if (entry.getValue().getNotificationRecipient().isExternalRecipient()) {
          batch.programMessages.add(createProgramMessage(entry.getValue(), entry.getKey()));
        } else {
          batch.dhisMessages.add(createDhisMessage(entry.getValue(), entry.getKey()));
        }
      }
    }
    return batch;
  }

  private ProgramMessage createProgramMessage(
      DataSetNotificationTemplate template, CompleteDataSetRegistration registration) {
    registration.setPeriodName(getPeriodString(registration.getPeriod()));

    NotificationMessage message = renderer.render(registration, template);

    ProgramMessageRecipients recipients;

    if (template.getDataSetNotificationTrigger().isScheduled()) {
      recipients = resolveExternalRecipientsForSchedule(template, registration);
    } else {
      recipients = resolveExternalRecipients(template, registration);
    }

    ProgramMessage programMessage =
        ProgramMessage.builder()
            .subject(message.getSubject())
            .text(message.getMessage())
            .recipients(recipients)
            .build();

    programMessage.setDeliveryChannels(template.getDeliveryChannels());

    return programMessage;
  }

  private DhisMessage createDhisMessage(
      DataSetNotificationTemplate template, CompleteDataSetRegistration registration) {
    registration.setPeriodName(getPeriodString(registration.getPeriod()));

    return new DhisMessage(
        renderer.render(registration, template), resolveInternalRecipients(template, registration));
  }

  private ProgramMessageRecipients resolveExternalRecipientsForSchedule(
      DataSetNotificationTemplate template, CompleteDataSetRegistration registration) {
    ProgramMessageRecipients recipients = new ProgramMessageRecipients();

    for (DeliveryChannel channel : template.getDeliveryChannels()) {
      Set<OrganisationUnit> ous =
          registration.getDataSet().getSources().stream()
              .filter(ou -> VALIDATOR.get(channel).test(ou))
              .collect(toSet());

      recipients = RECIPIENT_MAPPER.get(channel).apply(ous, recipients);
    }

    return recipients;
  }

  private ProgramMessageRecipients resolveExternalRecipients(
      DataSetNotificationTemplate template, CompleteDataSetRegistration registration) {
    ProgramMessageRecipients recipients = new ProgramMessageRecipients();

    OrganisationUnit ou = registration.getSource();

    for (DeliveryChannel channel : template.getDeliveryChannels()) {
      if (VALIDATOR.get(channel).test(ou)) {
        recipients = RECIPIENT_MAPPER.get(channel).apply(Set.of(ou), recipients);
      } else {
        log.error(format("DataSet notification not sent due to invalid %s recipient", channel));

        throw new IllegalArgumentException(format("Invalid %s recipient", channel));
      }
    }

    return recipients;
  }

  private Set<User> resolveInternalRecipients(DataSetNotificationTemplate template) {
    UserGroup userGroup = template.getRecipientUserGroup();

    return userGroup == null ? Set.of() : userGroup.getMembers();
  }

  private Set<User> resolveInternalRecipients(
      DataSetNotificationTemplate template, CompleteDataSetRegistration registration) {
    UserGroup userGroup = template.getRecipientUserGroup();

    if (userGroup == null || registration == null) {
      return Set.of();
    }

    return userGroup.getMembers().stream()
        .filter(
            user ->
                organisationUnitService.isInUserHierarchy(
                    registration.getSource().getUid(), user.getOrganisationUnits()))
        .collect(toSet());
  }

  private void sendInternalDhisMessages(
      String type, List<DhisMessage> messages, JobProgress progress) {
    progress.startingStage(
        "Dispatching DHIS " + type + " notification messages", messages.size(), SKIP_ITEM_OUTLIER);
    progress.runStage(
        messages,
        msg -> msg.message.getSubject(),
        msg ->
            internalMessageService.sendMessage(
                new MessageConversationParams.Builder(
                        msg.recipients,
                        null,
                        msg.message.getSubject(),
                        msg.message.getMessage(),
                        MessageType.SYSTEM,
                        null)
                    .build()));
  }

  private void sendProgramMessages(
      String type, List<ProgramMessage> messages, JobProgress progress) {
    progress.startingStage(
        "Dispatching DHIS " + type + " notification messages", messages.size(), SKIP_ITEM_OUTLIER);
    progress.runStage(
        null,
        status -> "Resulting status from ProgramMessageService:\n " + status.toString(),
        () -> externalMessageService.sendMessages(messages));
  }

  private void sendBatch(String type, MessageBatch batch, JobProgress progress) {
    if (!batch.dhisMessages.isEmpty()) {
      sendInternalDhisMessages(type, batch.dhisMessages, progress);
    }
    if (!batch.programMessages.isEmpty()) {
      sendProgramMessages(type, batch.programMessages, progress);
    }
  }

  // -------------------------------------------------------------------------
  // Internal classes
  // -------------------------------------------------------------------------

  @AllArgsConstructor
  private static final class DhisMessage {
    final NotificationMessage message;

    final Set<User> recipients;
  }

  @RequiredArgsConstructor
  private static final class MessageBatch {
    final List<DhisMessage> dhisMessages = new ArrayList<>();

    final List<ProgramMessage> programMessages = new ArrayList<>();
  }
}
