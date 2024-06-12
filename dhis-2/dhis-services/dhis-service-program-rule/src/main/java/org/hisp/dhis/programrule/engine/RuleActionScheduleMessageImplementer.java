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
package org.hisp.dhis.programrule.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.notification.logging.NotificationValidationResult;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.template.snapshot.NotificationTemplateService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Slf4j
@RequiredArgsConstructor
@Component("org.hisp.dhis.programrule.engine.RuleActionScheduleMessageImplementer")
public class RuleActionScheduleMessageImplementer implements RuleActionImplementer {
  public static final String LOG_MESSAGE = "Notification with id:%s has been scheduled";
  private final NotificationHelper notificationRuleActionImplementer;
  private final ProgramNotificationInstanceService programNotificationInstanceService;

  private final NotificationTemplateService notificationTemplateService;

  @Override
  public boolean accept(RuleAction ruleAction) {
    return ruleAction.getType().equals(ProgramRuleActionType.SCHEDULEMESSAGE.name());
  }

  @Override
  public void implement(RuleEffect ruleEffect, Enrollment enrollment) {
    ProgramNotificationTemplate template =
        notificationRuleActionImplementer.getNotificationTemplate(ruleEffect.getRuleAction());
    if (template == null) {
      return;
    }

    String date = StringUtils.unwrap(ruleEffect.getData(), '\'');

    if (isInvalid(date)) {
      return;
    }

    NotificationValidationResult result =
        notificationRuleActionImplementer.validate(template, enrollment);

    if (result.isValid()) {
      ProgramNotificationInstance notificationInstance =
          notificationTemplateService.createNotificationInstance(template, date);
      notificationInstance.setEvent(null);
      notificationInstance.setEnrollment(enrollment);

      programNotificationInstanceService.save(notificationInstance);

      log.info(String.format(LOG_MESSAGE, template.getUid()));

      if (result.isNeedsToCreatelogEntry()) {
        notificationRuleActionImplementer.createLogEntry(template, enrollment);
      }
    }
  }

  @Override
  public void implement(RuleEffect ruleEffect, Event event) {
    ProgramNotificationTemplate template =
        notificationRuleActionImplementer.getNotificationTemplate(ruleEffect.getRuleAction());
    if (template == null) {
      return;
    }

    String date = StringUtils.unwrap(ruleEffect.getData(), '\'');

    if (isInvalid(date)) {
      return;
    }

    NotificationValidationResult result =
        notificationRuleActionImplementer.validate(template, event.getEnrollment());

    if (result.isValid()) {
      ProgramNotificationInstance notificationInstance =
          notificationTemplateService.createNotificationInstance(template, date);
      notificationInstance.setEvent(event);
      notificationInstance.setEnrollment(null);

      programNotificationInstanceService.save(notificationInstance);

      log.info(String.format(LOG_MESSAGE, template.getUid()));

      if (result.isNeedsToCreatelogEntry()) {
        notificationRuleActionImplementer.createLogEntry(template, event.getEnrollment());
      }
    }
  }

  private boolean isInvalid(String date) {
    return StringUtils.isBlank(date) || !DateUtils.dateIsValid(date);
  }
}
