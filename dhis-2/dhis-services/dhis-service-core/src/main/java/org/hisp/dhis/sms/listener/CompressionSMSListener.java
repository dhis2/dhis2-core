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
package org.hisp.dhis.sms.listener;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsConsts.SubmissionType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.SmsSubmissionReader;
import org.hisp.dhis.smscompression.models.SmsMetadata;
import org.hisp.dhis.smscompression.models.SmsMetadata.Id;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.smscompression.models.SmsSubmissionHeader;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public abstract class CompressionSMSListener extends BaseSMSListener {
  protected abstract SmsResponse postProcess(
      IncomingSms sms, SmsSubmission submission, UserDetails smsCreatedBy)
      throws SMSProcessingException, ConflictException;

  protected abstract boolean handlesType(SubmissionType type);

  protected final IdentifiableObjectManager manager;

  public CompressionSMSListener(
      IncomingSmsService incomingSmsService,
      MessageSender smsSender,
      IdentifiableObjectManager manager) {
    super(incomingSmsService, smsSender);
    this.manager = manager;
  }

  @Override
  public boolean accept(@Nonnull IncomingSms sms) {
    if (!SmsUtils.isBase64(sms)) {
      return false;
    }

    SmsSubmissionHeader header = getHeader(sms);
    if (header == null) {
      // If the header is null we simply accept any listener
      // and handle the error in receive() below
      return true;
    }
    return handlesType(header.getType());
  }

  @Override
  public void receive(@Nonnull IncomingSms sms, @Nonnull UserDetails smsCreatedBy)
      throws ConflictException {
    SmsSubmissionReader reader = new SmsSubmissionReader();
    SmsSubmissionHeader header = getHeader(sms);
    if (header == null) {
      // Error with the header, we have no message ID, use -1
      sendSMSResponse(SmsResponse.HEADER_ERROR, sms, -1);
      return;
    }

    SmsMetadata meta = getMetadata(header.getLastSyncDate());
    SmsSubmission subm;
    try {
      subm = reader.readSubmission(SmsUtils.getBytes(sms), meta);
    } catch (Exception e) {
      log.error(e.getMessage());
      sendSMSResponse(SmsResponse.READ_ERROR, sms, header.getSubmissionId());
      return;
    }

    SmsResponse resp;
    try {
      resp = postProcess(sms, subm, smsCreatedBy);
    } catch (SMSProcessingException e) {
      log.error(e.getMessage());
      sendSMSResponse(e.getResp(), sms, header.getSubmissionId());
      return;
    }

    log.info("Sms Response: {}", resp.toString());
    sendSMSResponse(resp, sms, header.getSubmissionId());
  }

  private SmsSubmissionHeader getHeader(IncomingSms sms) {
    byte[] smsBytes = SmsUtils.getBytes(sms);
    SmsSubmissionReader reader = new SmsSubmissionReader();
    try {
      return reader.readHeader(smsBytes);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  private SmsMetadata getMetadata(Date lastSyncDate) {
    SmsMetadata meta = new SmsMetadata();
    meta.dataElements = getTypeUidsBefore(DataElement.class, lastSyncDate);
    meta.categoryOptionCombos = getTypeUidsBefore(CategoryOptionCombo.class, lastSyncDate);
    meta.users = getTypeUidsBefore(User.class, lastSyncDate);
    meta.trackedEntityTypes = getTypeUidsBefore(TrackedEntityType.class, lastSyncDate);
    meta.trackedEntityAttributes = getTypeUidsBefore(TrackedEntityAttribute.class, lastSyncDate);
    meta.programs = getTypeUidsBefore(Program.class, lastSyncDate);
    meta.organisationUnits = getTypeUidsBefore(OrganisationUnit.class, lastSyncDate);
    meta.programStages = getTypeUidsBefore(ProgramStage.class, lastSyncDate);
    meta.relationshipTypes = getTypeUidsBefore(RelationshipType.class, lastSyncDate);
    meta.dataSets = getTypeUidsBefore(DataSet.class, lastSyncDate);

    return meta;
  }

  private List<SmsMetadata.Id> getTypeUidsBefore(
      Class<? extends IdentifiableObject> klass, Date lastSyncDate) {
    return manager.getUidsCreatedBefore(klass, lastSyncDate).stream()
        .map(Id::new)
        .collect(Collectors.toList());
  }
}
