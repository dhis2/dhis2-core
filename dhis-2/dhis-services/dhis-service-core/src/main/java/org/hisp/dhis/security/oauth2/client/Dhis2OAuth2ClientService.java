/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.security.oauth2.client;

import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.hisp.dhis.user.UserDetails;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * Service for managing OAuth2 clients in DHIS2.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface Dhis2OAuth2ClientService {
  void save(RegisteredClient registeredClient);

  void save(RegisteredClient registeredClient, UserDetails userDetails);

  RegisteredClient findByUID(String uid);

  RegisteredClient findById(String id);

  /*
   * Returns the RegisteredClient with the given clientId, or null if not found.
   */
  @CheckForNull
  RegisteredClient findByClientId(String clientId);

  Dhis2OAuth2Client getAsDhis2OAuth2ClientByClientId(String clientId);

  /**
   * Converts a DHIS2 OAuth2Client entity to Spring's RegisteredClient domain object.
   *
   * @param client The DHIS2 OAuth2Client entity
   * @return The Spring RegisteredClient
   */
  RegisteredClient toObject(Dhis2OAuth2Client client);

  /**
   * Converts Spring's RegisteredClient domain object to a DHIS2 OAuth2Client entity.
   *
   * @param registeredClient The Spring RegisteredClient
   * @return The DHIS2 OAuth2Client entity
   */
  Dhis2OAuth2Client toEntity(RegisteredClient registeredClient);

  /**
   * Converts a Map to a JSON string.
   *
   * @param data The Map to convert
   * @return The JSON string
   */
  String writeMap(Map<String, Object> data);

  List<Dhis2OAuth2Client> getAll();
}
