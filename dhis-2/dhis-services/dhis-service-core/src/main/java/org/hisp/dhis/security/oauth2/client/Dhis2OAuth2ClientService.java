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
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.user.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * Admin-facing service for managing DHIS2 OAuth2 clients and the bridge to Spring Authorization
 * Server's {@link
 * org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository}.
 *
 * <p>Two concerns live on this interface:
 *
 * <ul>
 *   <li>CRUD + lookup used by the metadata import path and the REST controller (see {@code save},
 *       {@code findByUID}, {@code findByClientId}, {@code getAll}).
 *   <li>Validation and defaulting hooks ({@link #validateCreate}, {@link #validateUpdate}, {@link
 *       #applyCreateDefaults}, {@link #applyUpdateDefaults}) invoked by both the CRUD controller
 *       and the metadata-bundle import so admin input is checked the same way on every entry point.
 * </ul>
 *
 * <p>The token endpoint calls {@link #findByClientId} on every incoming authentication, so this
 * lookup sits on a hot path.
 *
 * <p>Admin callers can only set the {@code authorization_code} and {@code refresh_token} grant
 * types; {@code client_credentials} is reserved for the DCR system registrar created server-side by
 * {@code OAuth2DcrService}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public interface Dhis2OAuth2ClientService {
  /**
   * Persist a {@link RegisteredClient}. If the current security context is authenticated with a JWT
   * (i.e. a DCR Initial Access Token), the token's {@code sub} claim is resolved to a DHIS2 user
   * and used as {@code createdBy} on the new client; otherwise the caller's current user is used.
   */
  void save(RegisteredClient registeredClient);

  /** Persist a {@link RegisteredClient} recording the given user as {@code createdBy}. */
  void save(RegisteredClient registeredClient, UserDetails userDetails);

  /** Look up a client by its DHIS2 UID. Returns {@code null} if no match. */
  RegisteredClient findByUID(String uid);

  /** Look up a client by its internal id. Returns {@code null} if no match. */
  RegisteredClient findById(String id);

  /**
   * Look up a client by OAuth2 {@code client_id}. Called by Spring Authorization Server on every
   * token-endpoint authentication.
   *
   * @return the matching {@link RegisteredClient}, or {@code null} if none.
   */
  @CheckForNull
  RegisteredClient findByClientId(String clientId);

  /**
   * Look up the raw {@link Dhis2OAuth2Client} entity (not the Spring AS projection) by OAuth2
   * {@code client_id}. Used where DHIS2-specific fields (e.g. {@code createdBy}) are needed.
   */
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

  /** Return all persisted clients. */
  List<Dhis2OAuth2Client> getAll();

  /**
   * Collect validation errors that would block creating the given client. Errors are reported to
   * the consumer rather than thrown; callers (REST controller, metadata-import bundle hook) decide
   * whether to translate to a {@code ConflictException} or merge into a bundle report.
   */
  void validateCreate(Dhis2OAuth2Client entity, Consumer<ErrorReport> errors);

  /** Collect validation errors that would block updating an existing client. */
  void validateUpdate(
      Dhis2OAuth2Client persisted, Dhis2OAuth2Client newEntity, Consumer<ErrorReport> errors);

  /** Apply server-side defaults that fill in fields on create (name, client/token settings). */
  void applyCreateDefaults(Dhis2OAuth2Client entity);

  /**
   * Apply update-time defaults; preserves the existing persisted name when the caller did not send
   * one (the settings UI has no name field, so a REPLACE merge would otherwise clobber it).
   */
  void applyUpdateDefaults(Dhis2OAuth2Client persisted, Dhis2OAuth2Client newEntity);

  /**
   * Parse the comma-separated {@code authorizationGrantTypes} field into a typed set. Storage stays
   * as a string column for now; callers should prefer this typed view.
   */
  Set<AuthorizationGrantType> getAuthorizationGrantTypesSet(Dhis2OAuth2Client entity);
}
