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
package org.hisp.dhis.tracker.job;

import java.io.IOException;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import org.hisp.dhis.artemis.MessageManager;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.render.RenderService;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Component
public abstract class BaseMessageManager {
  private final MessageManager messageManager;

  private final AsyncTaskExecutor taskExecutor;

  private final RenderService renderService;

  public BaseMessageManager(
      MessageManager messageManager, AsyncTaskExecutor taskExecutor, RenderService renderService) {
    this.messageManager = messageManager;
    this.taskExecutor = taskExecutor;
    this.renderService = renderService;
  }

  public String addJob(TrackerSideEffectDataBundle sideEffectDataBundle) {
    String jobId = CodeGenerator.generateUid();
    sideEffectDataBundle.setJobId(jobId);

    messageManager.sendQueue(getTopic(), sideEffectDataBundle);

    return jobId;
  }

  public void executeJob(Runnable runnable) {
    taskExecutor.executeTask(runnable);
  }

  public TrackerSideEffectDataBundle toBundle(TextMessage message)
      throws JMSException, IOException {
    String payload = message.getText();

    return renderService.fromJson(payload, TrackerSideEffectDataBundle.class);
  }

  public abstract String getTopic();
}
