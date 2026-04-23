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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import static org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationUtils.createObjectReport;

import java.util.List;
import java.util.function.Consumer;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2Authorization;
import org.hisp.dhis.security.oauth2.consent.Dhis2OAuth2AuthorizationConsent;
import org.springframework.stereotype.Component;

/**
 * Rejects {@code /api/metadata} operations that touch runtime Spring Authorization Server state —
 * {@link Dhis2OAuth2Authorization} (access/refresh tokens, authorization codes, device codes, OIDC
 * id-tokens) and {@link Dhis2OAuth2AuthorizationConsent} (principal scope grants). These rows are
 * written, updated and deleted exclusively by Spring Authorization Server through the {@code
 * Dhis2OAuth2Authorization{,Consent}ServiceImpl.save/remove} path; admin-initiated mutations via
 * the metadata bundle pipeline would either forge bearer tokens (CREATE/UPDATE) or invalidate every
 * live OAuth session in one call (DELETE).
 *
 * <p>CREATE and UPDATE are already closed by {@link
 * org.hisp.dhis.dxf2.metadata.objectbundle.hooks.Dhis2OAuth2AuthorizationObjectBundleHook} and
 * {@link
 * org.hisp.dhis.dxf2.metadata.objectbundle.hooks.Dhis2OAuth2AuthorizationConsentObjectBundleHook}
 * via {@code ValidationHooksCheck}. The DELETE chain in {@code ServiceConfig} does not run
 * hook-backed checks, so this check is registered there to emit {@link ErrorCode#E6023} and mark
 * the object for removal before {@code DeletionCheck}/{@code session.delete} would run.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class OAuth2RuntimeStateCheck implements ObjectValidationCheck {

  @Override
  public <T extends IdentifiableObject> void check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext ctx,
      Consumer<ObjectReport> addReports) {
    if (!isOAuth2RuntimeState(klass)) {
      return;
    }
    rejectAll(bundle, klass, persistedObjects, ctx, addReports);
    rejectAll(bundle, klass, nonPersistedObjects, ctx, addReports);
  }

  private <T extends IdentifiableObject> void rejectAll(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> objects,
      ValidationContext ctx,
      Consumer<ObjectReport> addReports) {
    if (objects == null || objects.isEmpty()) {
      return;
    }
    for (T object : objects) {
      ErrorReport error = new ErrorReport(klass, ErrorCode.E6023, klass.getSimpleName());
      addReports.accept(createObjectReport(error, object, bundle));
      ctx.markForRemoval(object);
    }
  }

  private static boolean isOAuth2RuntimeState(Class<?> klass) {
    return Dhis2OAuth2Authorization.class.isAssignableFrom(klass)
        || Dhis2OAuth2AuthorizationConsent.class.isAssignableFrom(klass);
  }
}
