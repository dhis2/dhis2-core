/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.programrule.engine.RuleActionKey.NOTIFICATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.program.notification.template.snapshot.NotificationTemplateService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleActionScheduleMessageImplementerTest {

  private static final String TEMPLATE_UID = "templateUid";
  private static final String RULE_UID = "ruleUid";
  @Mock private ProgramNotificationTemplateService programNotificationTemplateService;

  @Mock private NotificationLoggingService notificationLoggingService;

  @InjectMocks private NotificationHelper notificationHelper;

  @Mock private ProgramNotificationInstanceService programNotificationInstanceService;

  @Mock private NotificationTemplateService notificationTemplateService;

  private RuleActionScheduleMessageImplementer ruleActionScheduleMessageImplementer =
      new RuleActionScheduleMessageImplementer(
          notificationHelper, programNotificationInstanceService, notificationTemplateService);

  @BeforeEach
  void setUp() {
    ruleActionScheduleMessageImplementer =
        new RuleActionScheduleMessageImplementer(
            notificationHelper, programNotificationInstanceService, notificationTemplateService);
  }

  @Test
  void shouldFailValidationWhenTemplateIsNotFound() {
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID)).thenReturn(null);

    ruleActionScheduleMessageImplementer.implement(ruleEffect(), enrollment());

    verify(notificationTemplateService, times(0))
        .createNotificationInstance(any(), any(Date.class));
    verify(programNotificationInstanceService, times(0)).save(any());
    verify(notificationLoggingService, times(0)).save(any());
  }

  private Enrollment enrollment() {
    return new Enrollment();
  }

  private RuleEffect ruleEffect() {
    return new RuleEffect(
        RULE_UID,
        new RuleAction(
            "data", ProgramRuleActionType.SENDMESSAGE.name(), Map.of(NOTIFICATION, TEMPLATE_UID)),
        "");
  }
}
