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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.mockito.Mockito.verify;

import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramNotificationTemplateObjectBundleHookTest {
  private static final String TEMPLATE_UID = "abcdefghij0";

  @Mock private ProgramNotificationTemplateService programNotificationTemplateService;

  private ProgramNotificationTemplateObjectBundleHook hook;

  @BeforeEach
  void setUp() {
    hook = new ProgramNotificationTemplateObjectBundleHook(programNotificationTemplateService);
  }

  @Test
  void shouldInvalidateCacheOnPostUpdate() {
    hook.postUpdate(template(), null);

    verify(programNotificationTemplateService).invalidateCache(TEMPLATE_UID);
  }

  @Test
  void shouldInvalidateCacheOnPostCreate() {
    hook.postCreate(template(), null);

    verify(programNotificationTemplateService).invalidateCache(TEMPLATE_UID);
  }

  @Test
  void shouldInvalidateCacheOnPreDelete() {
    hook.preDelete(template(), null);

    verify(programNotificationTemplateService).invalidateCache(TEMPLATE_UID);
  }

  private ProgramNotificationTemplate template() {
    ProgramNotificationTemplate template = new ProgramNotificationTemplate();
    template.setUid(TEMPLATE_UID);
    template.setNotificationRecipient(ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE);
    return template;
  }
}
