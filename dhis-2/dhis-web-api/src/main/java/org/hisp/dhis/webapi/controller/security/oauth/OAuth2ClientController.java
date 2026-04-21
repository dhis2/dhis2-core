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

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.query.Filter;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.operators.NotEqualOperator;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for managing OAuth2 clients for the DHIS2 OAuth2 authorization server.
 *
 * <p>Delegates admin-input validation and name-defaulting to {@link OAuth2ClientAdminValidator};
 * retains only the parts that are specific to the REST CRUD pipeline (authority gate, list-query
 * filter to hide the system registrar, client-settings defaulting, delete-protection for the system
 * registrar which doesn't flow through the bundle import).
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
  private final OAuth2ClientAdminValidator validator;

  @Override
  protected void preCreateEntity(Dhis2OAuth2Client entity) throws ConflictException {
    validator.rejectReservedClientId(entity.getClientId(), "create a client with");
    validator.validateGrantTypes(entity);
    validator.validateRedirectUris(entity);
    validator.defaultNameFromClientId(entity);

    if (entity.getClientSettings() == null) {
      ClientSettings clientSettings =
          ClientSettings.builder().requireAuthorizationConsent(true).build();
      entity.setClientSettings(clientService.writeMap(clientSettings.getSettings()));
    }
    if (entity.getTokenSettings() == null) {
      TokenSettings tokenSettings = TokenSettings.builder().build();
      entity.setTokenSettings(clientService.writeMap(tokenSettings.getSettings()));
    }
  }

  @Override
  protected void preUpdateEntity(Dhis2OAuth2Client entity, Dhis2OAuth2Client newEntity)
      throws ConflictException {
    validator.rejectIfSystemRegistrar(entity, "update");
    if (entity == null || !entity.getClientId().equals(newEntity.getClientId())) {
      validator.rejectReservedClientId(newEntity.getClientId(), "rename a client to");
    }
    validator.validateGrantTypes(newEntity);
    validator.validateRedirectUris(newEntity);
    validator.preserveNameOnUpdate(entity, newEntity);

    super.preUpdateEntity(entity, newEntity);
  }

  @Override
  protected void preDeleteEntity(Dhis2OAuth2Client entity) throws ConflictException {
    validator.rejectIfSystemRegistrar(entity, "delete");
  }

  /**
   * Hide the DCR system registrar client from list queries. It's created and owned by the server
   * itself to bootstrap dynamic client registration; admins should never touch it through the
   * settings UI. Direct-by-uid fetches are left alone since DCR / other server code may need to
   * read it, and the UI only discovers client uids via this list.
   */
  @Override
  protected void modifyGetObjectList(GetObjectListParams params, Query<Dhis2OAuth2Client> query) {
    query.add(new Filter("clientId", new NotEqualOperator<>(SYSTEM_REGISTRAR_CLIENTID)));
  }
}
