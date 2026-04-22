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
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2Authorization;
import org.springframework.stereotype.Component;

/**
 * Reject any attempt to create or update {@link Dhis2OAuth2Authorization} via the metadata import
 * pipeline. These records are runtime OAuth2 state — access tokens, refresh tokens, device codes —
 * written exclusively by Spring Authorization Server through {@code
 * Dhis2OAuth2AuthorizationServiceImpl.save(OAuth2Authorization)}. Allowing {@code /api/metadata} to
 * POST arbitrary authorization rows would let an admin forge bearer tokens for any principal.
 *
 * <p>The read-only {@code OAuth2AuthorizationController} still exposes GET, and Spring AS keeps its
 * own persistence path. This hook only closes the metadata-import write path.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class Dhis2OAuth2AuthorizationObjectBundleHook
    extends AbstractObjectBundleHook<Dhis2OAuth2Authorization> {

  @Override
  public void validate(
      Dhis2OAuth2Authorization object, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    addReports.accept(
        new ErrorReport(
            Dhis2OAuth2Authorization.class,
            ErrorCode.E6002,
            Dhis2OAuth2Authorization.class.getSimpleName()));
  }
}
