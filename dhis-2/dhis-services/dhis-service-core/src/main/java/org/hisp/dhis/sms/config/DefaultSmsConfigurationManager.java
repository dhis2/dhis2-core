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

import static org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType.RESTRICTED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.SystemUser;
import org.springframework.stereotype.Component;

/** Manages the {@link SmsConfiguration} for the instance. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSmsConfigurationManager implements SmsConfigurationManager {

  private static final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

  private final DatastoreService datastore;

  @PostConstruct
  private void init() {
    // Note: the trick here is that read-access is RESTRICTED but when a read
    // is performed by this service the SystemUser is used
    // this allows programmatic access but not access via datastore API
    datastore.addProtection(
        new DatastoreNamespaceProtection(
            "sms-config", RESTRICTED, RESTRICTED, Authorities.F_MOBILE_SENDSMS.toString()));
  }

  @Nonnull
  @Override
  @IndirectTransactional
  public SmsConfiguration getSmsConfiguration() {
    DatastoreEntry entry = null;
    try {
      entry = datastore.getEntry("sms-config", "config", new SystemUser());
    } catch (ForbiddenException e) {
      return new SmsConfiguration();
    }
    if (entry == null) return new SmsConfiguration();
    try {
      return jsonMapper.readValue(entry.getValue(), SmsConfiguration.class);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse SMS configuration", e);
      return new SmsConfiguration();
    }
  }

  @Override
  @IndirectTransactional
  public void updateSmsConfiguration(@Nonnull SmsConfiguration config)
      throws ConflictException, ForbiddenException, BadRequestException {
    DatastoreEntry entry = new DatastoreEntry();
    entry.setNamespace("sms-config");
    entry.setKey("config");
    try {
      entry.setValue(jsonMapper.writeValueAsString(config));
    } catch (JsonProcessingException e) {
      ConflictException ex = new ConflictException("Failed to convert config to JSON");
      ex.initCause(e);
      throw ex;
    }
    datastore.saveOrUpdateEntry(entry);
  }
}
