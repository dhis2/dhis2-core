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
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client;
import org.hisp.dhis.security.oauth2.client.OAuth2ClientAdminValidator;
import org.springframework.stereotype.Component;

/**
 * Runs {@link OAuth2ClientAdminValidator} against any {@link Dhis2OAuth2Client} that flows through
 * the metadata import pipeline. The REST CRUD controller already calls the validator in its
 * pre-hooks; the ObjectBundleHook is what catches the bulk {@code /api/metadata} path, which
 * bypasses the controller but runs the same import pipeline. Double-execution on the REST path is
 * harmless — the validator is idempotent.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class Dhis2OAuth2ClientObjectBundleHook extends AbstractObjectBundleHook<Dhis2OAuth2Client> {

  private final OAuth2ClientAdminValidator validator;

  @Override
  public void validate(
      Dhis2OAuth2Client object, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    if (bundle.isPersisted(object)) {
      // On bulk UPDATE we intentionally do NOT run rejectIfSystemRegistrar: a
      // round-trip metadata export/import legitimately re-sends the real
      // system-dcr-registrar-client row, and rejecting it would break full
      // metadata backups/restores. The admin still needs F_OAUTH2_CLIENT_MANAGE
      // + F_METADATA_IMPORT to reach this path. The REST PUT path keeps the
      // strict check so the settings UI can't touch the registrar directly.
      Dhis2OAuth2Client persisted = bundle.getPreheat().get(bundle.getPreheatIdentifier(), object);
      if (persisted == null || !persisted.getClientId().equals(object.getClientId())) {
        runAsErrorReport(
            () -> validator.rejectReservedClientId(object.getClientId(), "rename a client to"),
            addReports);
      }
    } else {
      runAsErrorReport(
          () -> validator.rejectReservedClientId(object.getClientId(), "create a client with"),
          addReports);
    }
    runAsErrorReport(() -> validator.validateGrantTypes(object), addReports);
    runAsErrorReport(() -> validator.validateRedirectUris(object), addReports);
  }

  @Override
  public void preCreate(Dhis2OAuth2Client object, ObjectBundle bundle) {
    validator.defaultNameFromClientId(object);
  }

  @Override
  public void preUpdate(
      Dhis2OAuth2Client object, Dhis2OAuth2Client persistedObject, ObjectBundle bundle) {
    validator.preserveNameOnUpdate(persistedObject, object);
  }

  /**
   * Bridge from the validator's {@link ConflictException}-based API to the bundle hook's {@link
   * ErrorReport}-based error channel. Using {@code ErrorCode.E4000} (generic message) keeps the
   * same http status (409) the import pipeline already maps validation conflicts to.
   */
  private void runAsErrorReport(CheckedRunnable check, Consumer<ErrorReport> addReports) {
    try {
      check.run();
    } catch (ConflictException e) {
      addReports.accept(new ErrorReport(Dhis2OAuth2Client.class, ErrorCode.E4000, e.getMessage()));
    }
  }

  @FunctionalInterface
  private interface CheckedRunnable {
    void run() throws ConflictException;
  }
}
