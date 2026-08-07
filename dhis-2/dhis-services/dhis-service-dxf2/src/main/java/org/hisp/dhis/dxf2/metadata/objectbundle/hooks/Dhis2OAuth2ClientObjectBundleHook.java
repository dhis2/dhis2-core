/*
 * Copyright (c) 2004-2026, University of Oslo
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

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.springframework.stereotype.Component;

/**
 * Wires {@link Dhis2OAuth2ClientService}'s admin validators + defaulting into the metadata import
 * pipeline so the same checks run on bulk {@code /api/metadata} imports as on REST CRUD. The REST
 * controller already calls the same service methods directly. Idempotent on the REST path.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class Dhis2OAuth2ClientObjectBundleHook extends AbstractObjectBundleHook<Dhis2OAuth2Client> {

  private final Dhis2OAuth2ClientService clientService;

  @Override
  public void validate(
      Dhis2OAuth2Client object, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    if (bundle.isPersisted(object)) {
      Dhis2OAuth2Client persisted = bundle.getPreheat().get(bundle.getPreheatIdentifier(), object);
      clientService.validateUpdate(persisted, object, addReports);
    } else {
      clientService.validateCreate(object, addReports);
    }
  }

  @Override
  public void preCreate(Dhis2OAuth2Client object, ObjectBundle bundle) {
    clientService.applyCreateDefaults(object);
  }

  @Override
  public void preUpdate(
      Dhis2OAuth2Client object, Dhis2OAuth2Client persistedObject, ObjectBundle bundle) {
    clientService.applyUpdateDefaults(persistedObject, object);
  }
}
