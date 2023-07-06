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
package org.hisp.dhis.validation.notification;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.commons.util.TextUtils.LN;
import static org.hisp.dhis.validation.Importance.HIGH;
import static org.hisp.dhis.validation.Importance.LOW;
import static org.hisp.dhis.validation.Importance.MEDIUM;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hisp.dhis.message.MessageConversationPriority;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.notification.SendStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.validation.Importance;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@Service("org.hisp.dhis.validation.notification.ValidationNotificationService")
@Transactional
public class DefaultValidationNotificationService implements ValidationNotificationService {
  private static final Predicate<ValidationResult> IS_APPLICABLE_RESULT =
      vr ->
          Objects.nonNull(vr)
              && Objects.nonNull(vr.getValidationRule())
              && !vr.getValidationRule().getNotificationTemplates().isEmpty();

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final NotificationMessageRenderer<ValidationResult> notificationMessageRenderer;

  private final MessageService messageService;

  private final ValidationResultService validationResultService;

  public DefaultValidationNotificationService(
      NotificationMessageRenderer<ValidationResult> notificationMessageRenderer,
      MessageService messageService,
      ValidationResultService validationResultService) {

    checkNotNull(notificationMessageRenderer);
    checkNotNull(messageService);
    checkNotNull(validationResultService);

    this.notificationMessageRenderer = notificationMessageRenderer;
    this.messageService = messageService;
    this.validationResultService = validationResultService;
  }

  // -------------------------------------------------------------------------
  // ValidationNotificationService implementation
  // -------------------------------------------------------------------------

  @Override
  public Set<ValidationResult> sendNotifications(Set<ValidationResult> validationResults) {
    if (validationResults.isEmpty()) {
      return Sets.newHashSet();
    }

    Clock clock =
        new Clock(log)
            .startClock()
            .logTime(
                String.format(
                    "Creating notifications for %d validation results", validationResults.size()));

    // Filter out un-applicable validation results and put in (natural)
    // order
    SortedSet<ValidationResult> applicableResults =
        validationResults.stream()
            .filter(IS_APPLICABLE_RESULT)
            .collect(Collectors.toCollection(TreeSet::new));

    // Transform into distinct pairs of ValidationRule and
    // ValidationNotificationTemplate
    SortedSet<MessagePair> messagePairs = createMessagePairs(applicableResults);

    // Segregate MessagePairs based on SendStrategy
    Map<SendStrategy, SortedSet<MessagePair>> segregatedMap =
        segregateMessagePairBasedOnStrategy(messagePairs);

    Map<SendStrategy, Map<Set<User>, NotificationMessage>> notficationCollections = new HashMap<>();

    notficationCollections.put(
        SendStrategy.SINGLE_NOTIFICATION,
        createSingleNotifications(
            segregatedMap.getOrDefault(SendStrategy.SINGLE_NOTIFICATION, new TreeSet<>())));

    notficationCollections.put(
        SendStrategy.COLLECTIVE_SUMMARY,
        createSummaryNotifications(
            segregatedMap.getOrDefault(SendStrategy.COLLECTIVE_SUMMARY, new TreeSet<>())));

    for (Map.Entry<SendStrategy, Map<Set<User>, NotificationMessage>> entry :
        notficationCollections.entrySet()) {
      clock.logTime(
          String.format(
              "Sending %d %s notification(s)",
              entry.getValue().size(), entry.getKey().getDescription()));

      entry.getValue().forEach(this::sendNotification);
    }

    clock.logTime("Done sending validation notifications");

    return applicableResults;
  }

  @Override
  public void sendUnsentNotifications() {
    Set<ValidationResult> validationResults =
        sendNotifications(
            Sets.newHashSet(validationResultService.getAllUnReportedValidationResults()));

    validationResults.forEach(vr -> vr.setNotificationSent(true));
    validationResultService.updateValidationResults(validationResults);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private Map<SendStrategy, SortedSet<MessagePair>> segregateMessagePairBasedOnStrategy(
      SortedSet<MessagePair> messagePairs) {
    Map<SendStrategy, SortedSet<MessagePair>> segregatedMap = new HashMap<>();

    for (MessagePair messagePair : messagePairs) {
      if (SendStrategy.SINGLE_NOTIFICATION.equals(messagePair.template.getSendStrategy())) {
        segregatedMap
            .computeIfAbsent(SendStrategy.SINGLE_NOTIFICATION, k -> new TreeSet<>())
            .add(messagePair);
      } else {
        segregatedMap
            .computeIfAbsent(SendStrategy.COLLECTIVE_SUMMARY, k -> new TreeSet<>())
            .add(messagePair);
      }
    }

    return segregatedMap;
  }

  private Map<Set<User>, NotificationMessage> createSingleNotifications(
      SortedSet<MessagePair> messagePairs) {
    BiMap<Set<User>, NotificationMessage> singleNotificationCollection = HashBiMap.create();

    for (MessagePair messagePair : messagePairs) {
      NotificationMessage notificationMessage =
          notificationMessageRenderer.render(messagePair.result, messagePair.template);

      notificationMessage.setPriority(
          getPriority(messagePair.result.getValidationRule().getImportance()));

      singleNotificationCollection.put(new HashSet<>(), notificationMessage);

      resolveRecipients(messagePair)
          .forEach(
              user -> singleNotificationCollection.inverse().get(notificationMessage).add(user));
    }

    return singleNotificationCollection;
  }

  private Map<Set<User>, NotificationMessage> createSummaryNotifications(
      SortedSet<MessagePair> messagePairs) {
    // Group the set of MessagePair into divisions representing a single
    // summarized message and its recipients
    Map<Set<User>, SortedSet<MessagePair>> groupedByRecipientsForSummary =
        createRecipientsToMessagePairsMap(messagePairs);

    // Flatten the grouped and sorted MessagePairs into single
    // NotificationMessages

    return createSummaryNotificationMessages(groupedByRecipientsForSummary, new Date());
  }

  private Map<Set<User>, NotificationMessage> createSummaryNotificationMessages(
      Map<Set<User>, SortedSet<MessagePair>> groupedByRecipients, final Date validationDate) {
    final Map<MessagePair, NotificationMessage> renderedNotificationsMap =
        groupedByRecipients.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream())
            .distinct()
            .collect(
                Collectors.toMap(
                    p -> p, p -> notificationMessageRenderer.render(p.result, p.template)));

    // Collect all pre-rendered messages into summaries and return mapped by
    // recipients
    return groupedByRecipients.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    createSummarizedMessage(
                        e.getValue(), renderedNotificationsMap, validationDate)));
  }

  /**
   * Creates a summarized message from the given MessagePairs and pre-rendered map of
   * NotificationMessages. The messages generated by each distinct MessagePair are concatenated in
   * their given order.
   */
  private static NotificationMessage createSummarizedMessage(
      SortedSet<MessagePair> pairs,
      final Map<MessagePair, NotificationMessage> renderedNotificationsMap,
      final Date validationDate) {
    Map<Importance, Long> counts =
        pairs.stream()
            .map(m -> m.result.getValidationRule().getImportance())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    String subject =
        String.format(
            "Validation violations as of %s", DateUtils.getLongDateString(validationDate));

    String message =
        String.format(
            "Violations: High %d, medium %d, low %d",
            counts.getOrDefault(HIGH, 0L),
            counts.getOrDefault(Importance.MEDIUM, 0L),
            counts.getOrDefault(LOW, 0L));

    // Concatenate the notifications in sorted order, divide by double
    // linebreak

    message =
        message
            + pairs.stream()
                .sorted()
                .map(renderedNotificationsMap::get)
                .map(n -> String.format("%s%s%s", n.getSubject(), LN, n.getMessage()))
                .reduce("", (initStr, newStr) -> String.format("%s%s%s", initStr, LN + LN, newStr));

    NotificationMessage notificationMessage = new NotificationMessage(subject, message);
    notificationMessage.setPriority(
        getPriority(
            counts.getOrDefault(HIGH, 0L) > 0
                ? HIGH
                : counts.getOrDefault(MEDIUM, 0L) > 0 ? MEDIUM : LOW));

    return notificationMessage;
  }

  private static MessageConversationPriority getPriority(Importance importance) {
    if (importance.equals(HIGH)) {
      return MessageConversationPriority.HIGH;
    } else if (importance.equals(MEDIUM)) {
      return MessageConversationPriority.MEDIUM;
    } else if (importance.equals(LOW)) {
      return MessageConversationPriority.LOW;
    }

    return MessageConversationPriority.NONE;
  }

  private static SortedSet<MessagePair> createMessagePairs(SortedSet<ValidationResult> results) {
    return results.stream()
        .flatMap(
            result ->
                result.getValidationRule().getNotificationTemplates().stream()
                    .map(template -> new MessagePair(result, template)))
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static Map<Set<User>, SortedSet<MessagePair>> createRecipientsToMessagePairsMap(
      SortedSet<MessagePair> messagePairs) {
    // Map each user to a distinct set of MessagePair
    Map<User, SortedSet<MessagePair>> singleUserToMessagePairs =
        getMessagePairsPerSingleUser(messagePairs);

    // Group each distinct SortedSet of MessagePair for the distinct Set of
    // recipient Users
    return groupRecipientsForMessagePairs(singleUserToMessagePairs);
  }

  private static Map<User, SortedSet<MessagePair>> getMessagePairsPerSingleUser(
      SortedSet<MessagePair> messagePairs) {
    Map<User, SortedSet<MessagePair>> messagePairsPerUsers = new HashMap<>();

    for (MessagePair pair : messagePairs) {
      Set<User> usersForThisPair = resolveRecipients(pair);

      for (User user : usersForThisPair) {
        messagePairsPerUsers.computeIfAbsent(user, k -> new TreeSet<>()).add(pair);
      }
    }

    return messagePairsPerUsers;
  }

  private static Map<Set<User>, SortedSet<MessagePair>> groupRecipientsForMessagePairs(
      Map<User, SortedSet<MessagePair>> messagePairsPerUser) {
    BiMap<Set<User>, SortedSet<MessagePair>> grouped = HashBiMap.create();

    for (Map.Entry<User, SortedSet<MessagePair>> entry : messagePairsPerUser.entrySet()) {
      User user = entry.getKey();
      SortedSet<MessagePair> setOfPairs = entry.getValue();

      if (grouped.containsValue(setOfPairs)) {
        // Value exists -> Add user to the existing key set
        grouped.inverse().get(setOfPairs).add(user);
      } else {
        // Value doesn't exist -> Add the [user, set] as a new entry
        grouped.put(Sets.newHashSet(user), setOfPairs);
      }
    }

    return grouped;
  }

  /** Resolve all distinct recipients for the given MessagePair. */
  private static Set<User> resolveRecipients(MessagePair pair) {
    ValidationResult validationResult = pair.result;
    ValidationNotificationTemplate template = pair.template;

    // Limit recipients to be withing org unit hierarchy only, effectively
    // producing a cross-cut of all users in the configured user groups.

    final boolean limitToHierarchy =
        BooleanUtils.toBoolean(template.getNotifyUsersInHierarchyOnly());

    final boolean parentOrgUnitOnly =
        BooleanUtils.toBoolean(template.getNotifyParentOrganisationUnitOnly());

    Set<OrganisationUnit> orgUnitsToInclude = Sets.newHashSet();

    Set<User> recipients =
        template.getRecipientUserGroups().stream()
            .flatMap(ug -> ug.getMembers().stream())
            .collect(Collectors.toSet());

    if (limitToHierarchy) {
      orgUnitsToInclude.add(validationResult.getOrganisationUnit()); // Include
      // self
      orgUnitsToInclude.addAll(validationResult.getOrganisationUnit().getAncestors());

      recipients =
          recipients.stream()
              .filter(user -> orgUnitsToInclude.contains(user.getOrganisationUnit()))
              .collect(Collectors.toSet());
    } else if (parentOrgUnitOnly) {
      Set<User> parents = Sets.newHashSet();
      recipients.forEach(user -> parents.addAll(user.getOrganisationUnit().getParent().getUsers()));

      return parents;
    }

    return recipients;
  }

  private void sendNotification(Set<User> users, NotificationMessage notificationMessage) {
    messageService.sendValidationMessage(
        users,
        notificationMessage.getSubject(),
        notificationMessage.getMessage(),
        notificationMessage.getPriority());
  }

  // -------------------------------------------------------------------------
  // Internal classes
  // -------------------------------------------------------------------------

  /**
   * Wrapper for a distinct pair of ValidationResult and template which correspond to one single
   * (rendered) message.
   *
   * <p>The natural order reflects the ordering of the contained ValidationResult.
   */
  private static class MessagePair implements Comparable<MessagePair> {
    final ValidationResult result;

    final ValidationNotificationTemplate template;

    private MessagePair(ValidationResult result, ValidationNotificationTemplate template) {
      this.result = result;
      this.template = template;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) return true;

      if (!(other instanceof MessagePair)) return false;

      MessagePair that = (MessagePair) other;

      return new EqualsBuilder()
          .append(result, that.result)
          .append(template, that.template)
          .isEquals();
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 37).append(result).append(template).toHashCode();
    }

    @Override
    public int compareTo(@NotNull MessagePair other) {
      return new CompareToBuilder()
          .append(this.result, other.result)
          .append(this.template, other.template)
          .build();
    }
  }
}
