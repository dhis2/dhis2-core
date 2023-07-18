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
package org.hisp.dhis.metadata;

import org.hisp.dhis.dxf2.metadata.MetadataValidationException;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;

/**
 * Service to handle life-cycle of {@link MetadataProposal}s.
 *
 * @author Jan Bernitt
 */
public interface MetadataWorkflowService {
  /**
   * Get a {@link MetadataProposal} by UID
   *
   * @param uid non-null
   * @return the proposal or null
   */
  MetadataProposal getByUid(String uid);

  /**
   * Create a new Proposal from parameters.
   *
   * @param params input parameters for the proposal
   * @return the created proposal
   * @throws IllegalStateException when the proposal is inconsistent
   * @throws MetadataValidationException when the proposal cannot possibly be applied successful
   */
  MetadataProposal propose(MetadataProposeParams params) throws MetadataValidationException;

  /**
   * Adjusts an existing proposal with status {@link MetadataProposalStatus#NEEDS_UPDATE}.
   *
   * @param uid UID of the adjusted proposal
   * @param params updated fields of the proposal
   * @return the updated proposal
   * @throws IllegalStateException when the proposal is in not in state {@link
   *     MetadataProposalStatus#NEEDS_UPDATE} or the adjustment inconsistent
   * @throws MetadataValidationException when the proposal cannot possibly be applied successful
   */
  MetadataProposal adjust(String uid, MetadataAdjustParams params)
      throws MetadataValidationException;

  /**
   * Accepts the given proposal.
   *
   * @param proposal proposal to accept, not null
   * @return the result of the change - if {@link org.hisp.dhis.feedback.Status} is error the
   *     proposal is not accepted but changes its status to {@link
   *     MetadataProposalStatus#NEEDS_UPDATE} containing the error summary in the {@link
   *     MetadataProposal#getReason()}
   * @throws IllegalStateException When the proposal is not in state {@link
   *     MetadataProposalStatus#PROPOSED}
   */
  ImportReport accept(MetadataProposal proposal);

  /**
   * Asks for updates on the proposed change by the original creator.
   *
   * @param proposal proposal to oppose
   * @param reason the reason the proposal is not acceptable yet
   * @throws IllegalStateException When the proposal is not in state {@link
   *     MetadataProposalStatus#PROPOSED}
   */
  void oppose(MetadataProposal proposal, String reason);

  /**
   * Rejects the proposal forever.
   *
   * @param proposal the proposal being rejected
   * @throws IllegalStateException When the proposal is not in state {@link
   *     MetadataProposalStatus#PROPOSED}
   */
  void reject(MetadataProposal proposal, String reason);
}
