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
package org.hisp.dhis.sms.config;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.common.NonTransactional;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultGatewayAdministrationService implements GatewayAdministrationService {

  private final AtomicBoolean hasGateways = new AtomicBoolean();

  private final SmsConfigurationManager smsConfigurationManager;

  @Qualifier("tripleDesStringEncryptor")
  private final PBEStringEncryptor pbeStringEncryptor;

  @EventListener
  public void handleContextRefresh(ContextRefreshedEvent event) {
    updateHasGatewaysState();
  }

  @Override
  @IndirectTransactional
  public void setDefaultGateway(SmsGatewayConfig config)
      throws ForbiddenException, ConflictException, BadRequestException {
    SmsConfiguration configuration = getSmsConfiguration();

    configuration
        .getGateways()
        .forEach(gateway -> gateway.setDefault(Objects.equals(gateway.getUid(), config.getUid())));

    smsConfigurationManager.updateSmsConfiguration(configuration);
    updateHasGatewaysState();
  }

  @Override
  @IndirectTransactional
  public boolean addGateway(SmsGatewayConfig config)
      throws ForbiddenException, ConflictException, BadRequestException {
    if (config == null) {
      return false;
    }

    config.setUid(CodeGenerator.generateCode(10));

    SmsConfiguration smsConfiguration = getSmsConfiguration();
    if (smsConfiguration.getGateways().stream().anyMatch(c -> c.getClass() == config.getClass()))
      return false;

    config.setDefault(smsConfiguration.getGateways().isEmpty());

    if (config instanceof GenericHttpGatewayConfig) {
      ((GenericHttpGatewayConfig) config)
          .getParameters().stream()
              .filter(GenericGatewayParameter::isConfidential)
              .forEach(p -> p.setValue(pbeStringEncryptor.encrypt(p.getValue())));
    }

    config.setPassword(pbeStringEncryptor.encrypt(config.getPassword()));

    smsConfiguration.getGateways().add(config);

    smsConfigurationManager.updateSmsConfiguration(smsConfiguration);
    updateHasGatewaysState();

    return true;
  }

  @Override
  @IndirectTransactional
  public void updateGateway(
      @CheckForNull SmsGatewayConfig persisted, @CheckForNull SmsGatewayConfig updated)
      throws NotFoundException, ConflictException, ForbiddenException, BadRequestException {
    if (updated == null) throw new ConflictException("Gateway configuration cannot be null");
    if (persisted == null) throw new NotFoundException(SmsGatewayConfig.class, updated.getUid());
    if (persisted.getClass() != updated.getClass())
      throw new ConflictException("Type of an existing configuration cannot be changed");

    updated.setUid(persisted.getUid());
    updated.setDefault(persisted.isDefault());

    if (updated.getPassword() == null) {
      // keep old password when undefined
      updated.setPassword(persisted.getPassword());
    } else if (persisted.getPassword() != null
        && !persisted.getPassword().equals(updated.getPassword())) {
      // password change
      updated.setPassword(pbeStringEncryptor.encrypt(updated.getPassword()));
    }

    if (persisted instanceof ClickatellGatewayConfig from
        && updated instanceof ClickatellGatewayConfig to
        && to.getAuthToken() == null) {
      to.setAuthToken(from.getAuthToken());
    }

    if (persisted instanceof GenericHttpGatewayConfig from
        && updated instanceof GenericHttpGatewayConfig to) {
      Map<String, GenericGatewayParameter> oldParamsByKey =
          from.getParameters().stream().collect(toMap(GenericGatewayParameter::getKey, identity()));

      for (GenericGatewayParameter newParam : to.getParameters()) {
        if (newParam.isConfidential()) {
          GenericGatewayParameter oldParam = oldParamsByKey.get(newParam.getKey());
          String newValue = newParam.getValue();
          String oldValue = oldParam == null ? null : oldParam.getValue();

          if (isBlank(newValue)) {
            // keep the old already encoded value
            newParam.setValue(oldValue);
          } else if (!Objects.equals(oldValue, newValue)) {
            newParam.setValue(pbeStringEncryptor.encrypt(newValue));
          }
          // else: new=old => keep already encoded value
        }
      }
    }

    SmsConfiguration configuration = getSmsConfiguration();

    List<SmsGatewayConfig> gateways = configuration.getGateways();
    gateways.remove(persisted);
    gateways.add(updated);

    smsConfigurationManager.updateSmsConfiguration(configuration);
    updateHasGatewaysState();
  }

  @Override
  @IndirectTransactional
  public boolean removeGatewayByUid(String uid)
      throws ForbiddenException, ConflictException, BadRequestException {
    SmsConfiguration smsConfiguration = getSmsConfiguration();

    List<SmsGatewayConfig> gateways = smsConfiguration.getGateways();
    SmsGatewayConfig removed =
        gateways.stream().filter(gateway -> gateway.getUid().equals(uid)).findFirst().orElse(null);
    if (removed == null) {
      return false;
    }
    gateways.remove(removed);
    if (removed.isDefault() && !gateways.isEmpty()) {
      gateways.get(0).setDefault(true);
    }
    smsConfigurationManager.updateSmsConfiguration(smsConfiguration);
    updateHasGatewaysState();
    return true;
  }

  @Override
  @IndirectTransactional
  public SmsGatewayConfig getByUid(String uid) {
    return getSmsConfiguration().getGateways().stream()
        .filter(gw -> gw.getUid().equals(uid))
        .findFirst()
        .orElse(null);
  }

  @Override
  @IndirectTransactional
  public SmsGatewayConfig getDefaultGateway() {
    return getSmsConfiguration().getGateways().stream()
        .filter(SmsGatewayConfig::isDefault)
        .findFirst()
        .orElse(null);
  }

  @Override
  @IndirectTransactional
  public boolean hasDefaultGateway() {
    return getDefaultGateway() != null;
  }

  @Override
  @NonTransactional
  public boolean hasGateways() {
    return hasGateways.get();
  }

  private SmsConfiguration getSmsConfiguration() {
    return smsConfigurationManager.getSmsConfiguration();
  }

  private void updateHasGatewaysState() {
    SmsConfiguration config = smsConfigurationManager.getSmsConfiguration();
    List<SmsGatewayConfig> gateways = config.getGateways();
    hasGateways.set(gateways != null && !gateways.isEmpty());
  }
}
