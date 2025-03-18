/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.eventhook;

import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;

import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.auth.AuthScheme;
import org.hisp.dhis.eventhook.targets.JmsTarget;
import org.hisp.dhis.eventhook.targets.KafkaTarget;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author Morten Olav Hansen
 */
@Service
@RequiredArgsConstructor
public class EventHookSecretManager {
  @Qualifier(AES_128_STRING_ENCRYPTOR)
  private final PBEStringCleanablePasswordEncryptor encryptor;

  public void encrypt(EventHook eventHook) {
    handleSecrets(eventHook, true);
  }

  public void decrypt(EventHook eventHook) {
    handleSecrets(eventHook, false);
  }

  private void handleSecrets(EventHook eventHook, boolean encrypt) {
    for (Target target : eventHook.getTargets()) {
      if (target.getType().equals(WebhookTarget.TYPE)) {
        handleWebhook((WebhookTarget) target, encrypt);
      } else {
        UnaryOperator<String> callback = encrypt ? encryptor::encrypt : encryptor::decrypt;
        if (target.getType().equals(JmsTarget.TYPE)) {
          handleJms((JmsTarget) target, callback);
        } else if (target.getType().equals(KafkaTarget.TYPE)) {
          handleKafka((KafkaTarget) target, callback);
        }
      }
    }
  }

  private void handleWebhook(WebhookTarget target, boolean encrypt) {
    AuthScheme auth = target.getAuth();

    if (auth != null) {
      if (encrypt) {
        target.setAuth(auth.encrypt(encryptor::encrypt));
      } else {
        target.setAuth(auth.decrypt(encryptor::decrypt));
      }
    }
  }

  private void handleJms(JmsTarget target, UnaryOperator<String> callback) {
    if (StringUtils.hasText(target.getPassword())) {
      target.setPassword(callback.apply(target.getPassword()));
    }
  }

  private void handleKafka(KafkaTarget target, UnaryOperator<String> callback) {
    if (StringUtils.hasText(target.getPassword())) {
      target.setPassword(callback.apply(target.getPassword()));
    }
  }
}
