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

import java.util.List;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Service("org.hisp.dhis.program.ProgramNotificationTemplateService")
public class DefaultProgramNotificationTemplateService
    implements ProgramNotificationTemplateService {
  private final ProgramNotificationTemplateStore store;

  private final Cache<Boolean> programWebHookNotificationCache;

  private final Cache<Boolean> programStageWebHookNotificationCache;

  public DefaultProgramNotificationTemplateService(
      ProgramNotificationTemplateStore store, CacheProvider cacheProvider) {
    this.store = store;
    this.programWebHookNotificationCache =
        cacheProvider.createProgramWebHookNotificationTemplateCache();
    this.programStageWebHookNotificationCache =
        cacheProvider.createProgramStageWebHookNotificationTemplateCache();
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramNotificationTemplate get(long programNotificationTemplate) {
    return store.get(programNotificationTemplate);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramNotificationTemplate getByUid(String programNotificationTemplate) {
    return store.getByUid(programNotificationTemplate);
  }

  @Override
  @Transactional
  public void save(ProgramNotificationTemplate programNotificationTemplate) {
    store.save(programNotificationTemplate);
  }

  @Override
  @Transactional
  public void update(ProgramNotificationTemplate programNotificationTemplate) {
    store.update(programNotificationTemplate);
  }

  @Override
  @Transactional
  public void delete(ProgramNotificationTemplate programNotificationTemplate) {
    store.delete(programNotificationTemplate);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramNotificationTemplate> getProgramNotificationByTriggerType(
      NotificationTrigger trigger) {
    return store.getProgramNotificationByTriggerType(trigger);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isProgramLinkedToWebHookNotification(Program program) {
    return programWebHookNotificationCache.get(
        program.getUid(), uid -> store.isProgramLinkedToWebHookNotification(program.getId()));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isProgramStageLinkedToWebHookNotification(ProgramStage programStage) {
    return programStageWebHookNotificationCache.get(
        programStage.getUid(),
        uid -> store.isProgramStageLinkedToWebHookNotification(programStage.getId()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramNotificationTemplate> getProgramLinkedToWebHookNotifications(Program program) {
    return store.getProgramLinkedToWebHookNotifications(program);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramNotificationTemplate> getProgramStageLinkedToWebHookNotifications(
      ProgramStage programStage) {
    return store.getProgramStageLinkedToWebHookNotifications(programStage);
  }

  @Override
  @Transactional(readOnly = true)
  public int countProgramNotificationTemplates(ProgramNotificationTemplateParam param) {
    return store.countProgramNotificationTemplates(param);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramNotificationTemplate> getProgramNotificationTemplates(
      ProgramNotificationTemplateParam programNotificationTemplateParam) {
    return store.getProgramNotificationTemplates(programNotificationTemplateParam);
  }
}
