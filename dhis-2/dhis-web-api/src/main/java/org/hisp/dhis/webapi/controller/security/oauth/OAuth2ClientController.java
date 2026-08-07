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
package org.hisp.dhis.webapi.controller.security.oauth;

import static org.hisp.dhis.security.Authorities.F_OAUTH2_CLIENT_MANAGE;
import static org.hisp.dhis.security.oauth2.OAuth2Constants.SYSTEM_REGISTRAR_CLIENTID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.query.Filter;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.operators.NotEqualOperator;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * REST controller that exposes DHIS2 metadata CRUD for registered OAuth2 clients at {@code
 * /api/oAuth2Clients}. Requires the {@code F_OAUTH2_CLIENT_MANAGE} authority. Behaves like a
 * standard DHIS2 metadata resource: supports list/get/create/update/delete, import/export, sharing,
 * and translations via the {@link AbstractCrudController} pipeline.
 *
 * <p>Validation and defaulting live on {@link Dhis2OAuth2ClientService}; this controller is the
 * REST pipeline glue: authority gate, list-query filter to hide the system DCR registrar client,
 * REST-only guards against mutating that reserved system client, and translation of the first
 * collected validation error into a {@link ConflictException}.
 *
 * <p>Domain rules enforced by the service layer include: only {@code authorization_code} and {@code
 * refresh_token} grant types are accepted (the {@code client_credentials} grant is rejected with
 * HTTP 409 and {@link org.hisp.dhis.feedback.ErrorCode#E4000}), redirect URIs with custom schemes
 * must appear verbatim in the {@code deviceEnrollmentRedirectAllowlist} system setting, and the
 * reserved system client id {@link
 * org.hisp.dhis.security.oauth2.OAuth2Constants#SYSTEM_REGISTRAR_CLIENTID} is filtered from list
 * responses and rejected on create, update, and delete through this controller.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Controller
@RequestMapping({"/api/oAuth2Clients"})
@RequiredArgsConstructor
@RequiresAuthority(anyOf = F_OAUTH2_CLIENT_MANAGE)
public class OAuth2ClientController
    extends AbstractCrudController<Dhis2OAuth2Client, GetObjectListParams> {

  private final Dhis2OAuth2ClientService clientService;

  /**
   * Runs create-time validation and applies defaults via {@link Dhis2OAuth2ClientService} before
   * the client is persisted. The first reported validation error is translated into a {@link
   * ConflictException} (HTTP 409), for example when a disallowed grant type such as {@code
   * client_credentials} is submitted or when a custom-scheme redirect URI is not present in the
   * {@code deviceEnrollmentRedirectAllowlist}.
   *
   * @param entity the incoming client payload to validate and default
   * @throws ConflictException if the service reports one or more validation errors
   */
  @Override
  protected void preCreateEntity(Dhis2OAuth2Client entity) throws ConflictException {
    throwFirst(errors -> clientService.validateCreate(entity, errors));
    clientService.applyCreateDefaults(entity);
  }

  /**
   * Runs update-time validation, applies defaults, and blocks attempts to mutate the reserved
   * system DCR registrar client via REST.
   *
   * @param entity the existing persisted client
   * @param newEntity the incoming update payload
   * @throws ConflictException if the target is the system registrar client (HTTP 409), or if {@link
   *     Dhis2OAuth2ClientService} reports a validation error such as renaming the client id to the
   *     reserved system id, switching to a disallowed grant type, or introducing a custom-scheme
   *     redirect URI that is not allowlisted
   */
  @Override
  protected void preUpdateEntity(Dhis2OAuth2Client entity, Dhis2OAuth2Client newEntity)
      throws ConflictException {
    if (entity != null && SYSTEM_REGISTRAR_CLIENTID.equals(entity.getClientId())) {
      throw new ConflictException(
          "Cannot update the system-managed DCR registrar client ("
              + SYSTEM_REGISTRAR_CLIENTID
              + ").");
    }
    throwFirst(errors -> clientService.validateUpdate(entity, newEntity, errors));
    clientService.applyUpdateDefaults(entity, newEntity);
    super.preUpdateEntity(entity, newEntity);
  }

  /**
   * Blocks deletion of the reserved system DCR registrar client through REST. Other entities are
   * deleted by the default {@link AbstractCrudController} pipeline.
   *
   * @param entity the client targeted for deletion
   * @throws ConflictException if the target is the system registrar client (HTTP 409)
   */
  @Override
  protected void preDeleteEntity(Dhis2OAuth2Client entity) throws ConflictException {
    if (entity != null && SYSTEM_REGISTRAR_CLIENTID.equals(entity.getClientId())) {
      throw new ConflictException(
          "Cannot delete the system-managed DCR registrar client ("
              + SYSTEM_REGISTRAR_CLIENTID
              + ").");
    }
  }

  /**
   * Hides the DCR system registrar client from list queries. It is created and owned by the server
   * itself to bootstrap dynamic client registration; administrators should never touch it through
   * the settings UI. Direct by-uid fetches are left alone since DCR and other server code may need
   * to read it, and the UI only discovers client uids via this list.
   *
   * @param params the incoming list parameters (unmodified)
   */
  @Nonnull
  @Override
  protected List<Filter> getAdditionalFilters(GetObjectListParams params) throws ConflictException {
    List<Filter> filters = super.getAdditionalFilters(params);
    filters.add(new Filter("clientId", new NotEqualOperator<>(SYSTEM_REGISTRAR_CLIENTID)));
    return filters;
  }

  /**
   * Runs a collecting validator and translates the first reported error into a {@link
   * ConflictException}. The full set of errors is available to non-REST callers (for example the
   * metadata import bundle hook) which prefer to merge them into a bundle report instead of
   * throwing.
   *
   * @param validator a consumer that accepts an {@link ErrorReport} collector and populates it with
   *     any validation failures
   * @throws ConflictException carrying the message of the first collected error, if any
   */
  private static void throwFirst(Consumer<Consumer<ErrorReport>> validator)
      throws ConflictException {
    List<ErrorReport> errors = new ArrayList<>();
    validator.accept(errors::add);
    if (!errors.isEmpty()) {
      throw new ConflictException(errors.get(0).getMessage());
    }
  }
}
