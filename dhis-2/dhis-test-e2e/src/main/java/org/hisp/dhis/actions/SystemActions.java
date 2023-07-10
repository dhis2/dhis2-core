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
package org.hisp.dhis.actions;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;

import java.util.List;
import java.util.concurrent.Callable;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.ImportSummary;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class SystemActions extends RestApiActions {

  public SystemActions() {
    super("/system");
  }

  public ApiResponse getTask(String taskType, String taskId) {
    return get("/tasks/" + taskType + "/" + taskId);
  }

  /**
   * Waits until the task is completed and returns a response. The default timeout is 20 seconds.
   *
   * @param taskType
   * @param taskId
   * @return
   */
  public ApiResponse waitUntilTaskCompleted(String taskType, String taskId) {
    return waitUntilTaskCompleted(taskType, taskId, 20);
  }

  /**
   * Waits until the task is completed and returns a response.
   *
   * @param taskType the task type
   * @param taskId the task unique id
   * @param timeout maximum time to wait for the task to complete (in seconds)
   * @return
   */
  public ApiResponse waitUntilTaskCompleted(String taskType, String taskId, long timeout) {
    Callable<Boolean> taskIsCompleted =
        () -> getTask(taskType, taskId).validateStatus(200).extractList("completed").contains(true);

    with().atMost(timeout, SECONDS).await().until(() -> taskIsCompleted.call());

    return getTask(taskType, taskId);
  }

  public List<ImportSummary> getTaskSummaries(String taskType, String taskId) {
    return waitForTaskSummaries(taskType, taskId).validateStatus(200).getImportSummaries();
  }

  /**
   * Waits until task summaries are generated and returns the response
   *
   * @param taskType
   * @param taskId
   * @return
   */
  public ApiResponse waitForTaskSummaries(String taskType, String taskId) {
    String url = String.format("/taskSummaries/%s/%s", taskType, taskId);

    await().ignoreExceptions().until(() -> !get(url).validateStatus(200).getBody().equals(null));

    return get(url);
  }
}
