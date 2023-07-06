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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Set;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Halvdan Hoem Grelland
 */
@Component
public class ProgramNotificationTemplateObjectBundleHook
    extends AbstractObjectBundleHook<ProgramNotificationTemplate> {
  private ImmutableMap<
          ProgramNotificationRecipient, Function<ProgramNotificationTemplate, ValueType>>
      RECIPIENT_TO_VALUETYPE_RESOLVER =
          new ImmutableMap.Builder<
                  ProgramNotificationRecipient, Function<ProgramNotificationTemplate, ValueType>>()
              .put(
                  ProgramNotificationRecipient.PROGRAM_ATTRIBUTE,
                  template -> template.getRecipientProgramAttribute().getValueType())
              .put(
                  ProgramNotificationRecipient.DATA_ELEMENT,
                  template -> template.getRecipientDataElement().getValueType())
              .put(ProgramNotificationRecipient.WEB_HOOK, template -> ValueType.URL)
              .build();

  private static final ImmutableMap<ValueType, Set<DeliveryChannel>> CHANNEL_MAPPER =
      new ImmutableMap.Builder<ValueType, Set<DeliveryChannel>>()
          .put(ValueType.PHONE_NUMBER, Sets.newHashSet(DeliveryChannel.SMS))
          .put(ValueType.EMAIL, Sets.newHashSet(DeliveryChannel.EMAIL))
          .put(ValueType.URL, Sets.newHashSet(DeliveryChannel.HTTP))
          .build();

  @Override
  public void preCreate(ProgramNotificationTemplate template, ObjectBundle bundle) {
    preProcess(template);
  }

  @Override
  public void preUpdate(
      ProgramNotificationTemplate template,
      ProgramNotificationTemplate persistedObject,
      ObjectBundle bundle) {
    preProcess(template);
  }

  @Override
  public void postCreate(ProgramNotificationTemplate template, ObjectBundle bundle) {
    postProcess(template);
  }

  @Override
  public void postUpdate(ProgramNotificationTemplate template, ObjectBundle bundle) {
    postProcess(template);
  }

  /** Removes any non-valid combinations of properties on the template object. */
  private void preProcess(ProgramNotificationTemplate template) {
    if (template.getNotificationTrigger().isImmediate()) {
      template.setRelativeScheduledDays(null);
    }

    if (ProgramNotificationRecipient.USER_GROUP != template.getNotificationRecipient()) {
      template.setRecipientUserGroup(null);
    }

    if (ProgramNotificationRecipient.PROGRAM_ATTRIBUTE != template.getNotificationRecipient()) {
      template.setRecipientProgramAttribute(null);
    }

    if (ProgramNotificationRecipient.DATA_ELEMENT != template.getNotificationRecipient()) {
      template.setRecipientDataElement(null);
    }

    if (!(template.getNotificationRecipient().isExternalRecipient())) {
      template.setDeliveryChannels(Sets.newHashSet());
    }
  }

  private void postProcess(ProgramNotificationTemplate template) {
    ProgramNotificationRecipient pnr = template.getNotificationRecipient();

    ValueType valueType = null;

    if (RECIPIENT_TO_VALUETYPE_RESOLVER.containsKey(pnr)) {
      Function<ProgramNotificationTemplate, ValueType> resolver =
          RECIPIENT_TO_VALUETYPE_RESOLVER.get(pnr);
      valueType = resolver.apply(template);
    }

    template.setDeliveryChannels(CHANNEL_MAPPER.getOrDefault(valueType, Sets.newHashSet()));
  }
}
