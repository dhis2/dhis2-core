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
package org.hisp.dhis.program.message;

import static org.hisp.dhis.audit.AuditOperationType.READ;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUsername;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageBatchService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.ApiTrackedEntityAuditService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.program.message.ProgramMessageService")
public class DefaultProgramMessageService implements ProgramMessageService {
  private final IdentifiableObjectManager manager;

  private final ProgramMessageStore programMessageStore;

  private final OrganisationUnitService organisationUnitService;

  private final OutboundMessageBatchService messageBatchService;

  private final List<DeliveryChannelStrategy> strategies;

  private final List<MessageBatchCreatorService> batchCreators;

  private final AclService aclService;

  private final ProgramMessageOperationParamMapper operationParamMapper;

  private final ApiTrackedEntityAuditService apiTrackedEntityAuditService;

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public ProgramMessage getProgramMessage(long id) {
    return programMessageStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramMessage getProgramMessage(String uid) {
    return programMessageStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramMessage> getAllProgramMessages() {
    return programMessageStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramMessage> getProgramMessages(ProgramMessageOperationParams params)
      throws NotFoundException {
    ProgramMessageQueryParams queryParams = operationParamMapper.map(params);

    return programMessageStore.getProgramMessages(queryParams);
  }

  @Override
  @Transactional
  public long saveProgramMessage(ProgramMessage programMessage) {
    programMessageStore.save(programMessage);
    return programMessage.getId();
  }

  @Override
  @Transactional
  public void updateProgramMessage(ProgramMessage programMessage) {
    programMessageStore.update(programMessage);
  }

  @Override
  public void deleteProgramMessage(ProgramMessage programMessage) {
    programMessageStore.delete(programMessage);
  }

  @Override
  @Transactional
  public BatchResponseStatus sendMessages(List<ProgramMessage> programMessages) {
    List<ProgramMessage> populatedProgramMessages =
        programMessages.stream()
            .filter(this::hasDataWriteAccess)
            .map(this::setAttributesBasedOnStrategy)
            .collect(Collectors.toList());

    List<OutboundMessageBatch> batches = createBatches(populatedProgramMessages);

    BatchResponseStatus status = new BatchResponseStatus(messageBatchService.sendBatches(batches));

    saveProgramMessages(programMessages, status);

    return status;
  }

  @Override
  @Transactional(readOnly = true)
  public void validatePayload(ProgramMessage message) {
    List<String> violations = new ArrayList<>();

    ProgramMessageRecipients recipients = message.getRecipients();

    if (message.getText() == null) {
      violations.add("Message content must be provided");
    }

    if (message.getDeliveryChannels() == null || message.getDeliveryChannels().isEmpty()) {
      violations.add("Delivery channel must be specified");
    }

    if (message.getEnrollment() == null && message.getEvent() == null) {
      violations.add("Enrollment or Event must be specified");
    }

    if (recipients.getTrackedEntity() != null) {
      TrackedEntity trackedEntity = getEntity(TrackedEntity.class, recipients.getTrackedEntity());
      if (trackedEntity == null) {
        violations.add("Tracked entity does not exist");
      }

      apiTrackedEntityAuditService.addTrackedEntityAudit(trackedEntity, getCurrentUsername(), READ);
    }

    if (recipients.getOrganisationUnit() != null
        && organisationUnitService.getOrganisationUnit(recipients.getOrganisationUnit().getUid())
            == null) {
      violations.add("Organisation unit does not exist");
    }

    if (!violations.isEmpty()) {
      throw new IllegalQueryException(String.join(", ", violations));
    }
  }

  // ---------------------------------------------------------------------
  // Supportive Methods
  // ---------------------------------------------------------------------

  private boolean hasDataWriteAccess(ProgramMessage message) {
    IdentifiableObject object = null;

    boolean isAuthorized;

    if (message.hasEnrollment()) {
      object = message.getEnrollment().getProgram();
    } else if (message.hasTrackerEvent()) {
      object = message.getTrackerEvent().getProgramStage();
    } else if (message.hasSingleEvent()) {
      object = message.getSingleEvent().getProgramStage();
    }

    if (object != null) {
      isAuthorized = aclService.canDataWrite(CurrentUserUtil.getCurrentUserDetails(), object);

      if (!isAuthorized) {
        log.error(
            String.format(
                "Sending message failed. User does not have write access for %s.",
                object.getName()));

        return false;
      }
    }

    return true;
  }

  private void saveProgramMessages(List<ProgramMessage> messageBatch, BatchResponseStatus status) {
    messageBatch.parallelStream()
        .map(pm -> setParameters(pm, status))
        .forEach(this::saveProgramMessage);
  }

  private ProgramMessage setParameters(ProgramMessage message, BatchResponseStatus status) {
    message.setEnrollment(getEntity(Enrollment.class, message.getEnrollment()));
    message.setTrackerEvent(getEntity(TrackerEvent.class, message.getEvent()));
    message.setSingleEvent(getEntity(SingleEvent.class, message.getEvent()));
    message.setProcessedDate(new Date());
    message.setMessageStatus(
        status.isOk() ? ProgramMessageStatus.SENT : ProgramMessageStatus.FAILED);

    return message;
  }

  private List<OutboundMessageBatch> createBatches(List<ProgramMessage> programMessages) {
    return batchCreators.stream()
        .map(bc -> bc.getMessageBatch(programMessages))
        .filter(bc -> !bc.getMessages().isEmpty())
        .toList();
  }

  private <T extends IdentifiableObject> T getEntity(Class<T> klass, IdentifiableObject entity) {
    if (entity == null) {
      return null;
    }

    return manager.get(klass, entity.getUid());
  }

  private ProgramMessage setAttributesBasedOnStrategy(ProgramMessage message) {
    Set<DeliveryChannel> channels = message.getDeliveryChannels();

    for (DeliveryChannel channel : channels) {
      for (DeliveryChannelStrategy strategy : strategies) {
        if (strategy.getDeliveryChannel().equals(channel)) {
          strategy.setAttributes(message);
        }
      }
    }

    return message;
  }
}
