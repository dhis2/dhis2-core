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

import java.util.List;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
public interface ProgramMessageService {
  /**
   * To validate {@link ProgramMessage message} payload in order to make sure prerequisite values
   * exist before message can be processed.
   *
   * @param message the ProgramMessage.
   */
  void validatePayload(ProgramMessage message);

  // -------------------------------------------------------------------------
  // Transport Service methods
  // -------------------------------------------------------------------------

  /**
   * Send message batch based on their {@link DeliveryChannel channel}. If the DeliveryChannel is
   * not configured with suitable value, batch will be invalidated.
   *
   * @param programMessages the ProgramMessage.
   */
  BatchResponseStatus sendMessages(List<ProgramMessage> programMessages);

  // -------------------------------------------------------------------------
  // GET
  // -------------------------------------------------------------------------

  ProgramMessage getProgramMessage(long id);

  ProgramMessage getProgramMessage(String uid);

  List<ProgramMessage> getAllProgramMessages();

  List<ProgramMessage> getProgramMessages(ProgramMessageOperationParams params)
      throws NotFoundException;

  // -------------------------------------------------------------------------
  // Save OR Update
  // -------------------------------------------------------------------------

  long saveProgramMessage(ProgramMessage programMessage);

  void updateProgramMessage(ProgramMessage programMessage);

  // -------------------------------------------------------------------------
  // Delete
  // -------------------------------------------------------------------------

  void deleteProgramMessage(ProgramMessage programMessage);
}
