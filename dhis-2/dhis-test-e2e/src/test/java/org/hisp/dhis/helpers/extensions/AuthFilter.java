/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.helpers.extensions;

import io.restassured.authentication.NoAuthScheme;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class AuthFilter implements io.restassured.spi.AuthFilter {
  private String lastLoggedInUser = "";

  private String lastLoggedInUserPsw = "";

  @Override
  public Response filter(
      FilterableRequestSpecification requestSpec,
      FilterableResponseSpecification responseSpec,
      FilterContext ctx) {
    if (requestSpec.getAuthenticationScheme() instanceof NoAuthScheme) {
      if (hasSessionCookie(requestSpec)) {
        requestSpec.removeCookies();
      }

      lastLoggedInUser = "";
      lastLoggedInUserPsw = "";
    }

    if (requestSpec.getAuthenticationScheme() instanceof PreemptiveBasicAuthScheme
        && (((PreemptiveBasicAuthScheme) requestSpec.getAuthenticationScheme()).getUserName()
                != lastLoggedInUser
            || ((PreemptiveBasicAuthScheme) requestSpec.getAuthenticationScheme()).getPassword()
                != lastLoggedInUserPsw)) {
      if (hasSessionCookie(requestSpec)) {
        requestSpec.removeCookies();
      }

      lastLoggedInUser =
          ((PreemptiveBasicAuthScheme) requestSpec.getAuthenticationScheme()).getUserName();
      lastLoggedInUserPsw =
          ((PreemptiveBasicAuthScheme) requestSpec.getAuthenticationScheme()).getPassword();
    }

    final Response response = ctx.next(requestSpec, responseSpec);
    return response;
  }

  private boolean hasSessionCookie(FilterableRequestSpecification requestSpec) {
    return requestSpec.getCookies().hasCookieWithName("JSESSIONID")
        || requestSpec.getCookies().hasCookieWithName("SESSION");
  }
}
