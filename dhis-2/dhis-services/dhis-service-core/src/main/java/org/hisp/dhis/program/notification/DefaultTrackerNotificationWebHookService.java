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
package org.hisp.dhis.program.notification;

import com.google.common.collect.Lists;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.notification.ProgramNotificationMessageRenderer;
import org.hisp.dhis.notification.ProgramStageNotificationMessageRenderer;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.sms.config.SmsGateway;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Zubair Asghar
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.program.notification.TrackerNotificationWebHookService")
public class DefaultTrackerNotificationWebHookService implements TrackerNotificationWebHookService {
  private final ProgramInstanceService programInstanceService;

  private final ProgramStageInstanceService programStageInstanceService;

  private final ProgramNotificationTemplateService templateService;

  private final RestTemplate restTemplate;

  private final RenderService renderService;

  @Override
  @Transactional
  public void handleEnrollment(String pi) {
    ProgramInstance instance = programInstanceService.getProgramInstance(pi);

    if (instance == null
        || !templateService.isProgramLinkedToWebHookNotification(instance.getProgram())) {
      return;
    }

    List<ProgramNotificationTemplate> templates =
        templateService.getProgramLinkedToWebHookNotifications(instance.getProgram());

    Map<String, String> requestPayload = new HashMap<>();
    ProgramNotificationMessageRenderer.VARIABLE_RESOLVERS.forEach(
        (key, value) -> requestPayload.put(key.name(), value.apply(instance)));

    // populate tracked entity attributes
    instance
        .getEntityInstance()
        .getTrackedEntityAttributeValues()
        .forEach(attr -> requestPayload.put(attr.getAttribute().getUid(), attr.getValue()));
    sendPost(templates, renderService.toJsonAsString(requestPayload));
  }

  @Override
  @Transactional
  public void handleEvent(String psi) {
    ProgramStageInstance instance = programStageInstanceService.getProgramStageInstance(psi);

    if (instance == null
        || !templateService.isProgramStageLinkedToWebHookNotification(instance.getProgramStage())) {
      return;
    }

    List<ProgramNotificationTemplate> templates =
        templateService.getProgramStageLinkedToWebHookNotifications(instance.getProgramStage());

    // populate environment variables
    Map<String, String> requestPayload = new HashMap<>();
    ProgramStageNotificationMessageRenderer.VARIABLE_RESOLVERS.forEach(
        (key, value) -> requestPayload.put(key.name(), value.apply(instance)));

    // populate data values
    instance
        .getEventDataValues()
        .forEach(dv -> requestPayload.put(dv.getDataElement(), dv.getValue()));

    // populate tracked entity attributes
    instance
        .getProgramInstance()
        .getEntityInstance()
        .getTrackedEntityAttributeValues()
        .forEach(attr -> requestPayload.put(attr.getAttribute().getUid(), attr.getValue()));

    sendPost(templates, renderService.toJsonAsString(requestPayload));
  }

  private void sendPost(List<ProgramNotificationTemplate> templates, String payload) {
    ResponseEntity<String> responseEntity = null;

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.put("Content-type", Lists.newArrayList("application/json"));

    HttpEntity<String> httpEntity = new HttpEntity<>(payload, httpHeaders);

    for (ProgramNotificationTemplate t : templates) {
      if (!ValidationUtils.urlIsValid(t.getMessageTemplate())) {
        log.error(
            String.format(
                "Webhook url: %s is invalid for template: %s", t.getMessageTemplate(), t.getUid()));
        continue;
      }

      URI uri = UriComponentsBuilder.fromHttpUrl(t.getMessageTemplate()).build().encode().toUri();

      try {
        responseEntity = restTemplate.exchange(uri, HttpMethod.POST, httpEntity, String.class);
      } catch (HttpClientErrorException ex) {
        log.error("Client error " + ex.getMessage());
      } catch (HttpServerErrorException ex) {
        log.error("Server error " + ex.getMessage());
      } catch (Exception ex) {
        log.error("Error " + ex.getMessage());
      }

      if (responseEntity != null && SmsGateway.OK_CODES.contains(responseEntity.getStatusCode())) {
        log.info(
            String.format("Post request successful for url: %s and template: %s", uri, t.getUid()));
      } else {
        log.info(
            String.format("Post request failed for url: %s and template: %s", uri, t.getUid()));
      }
    }
  }
}
