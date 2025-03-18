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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.security.Authorities.F_LEGEND_SET_DELETE;
import static org.hisp.dhis.security.Authorities.F_LEGEND_SET_PRIVATE_ADD;
import static org.hisp.dhis.security.Authorities.F_LEGEND_SET_PUBLIC_ADD;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping("/api/legendSets")
@OpenApi.Document(classifiers = {"team:analytics", "purpose:metadata"})
public class LegendSetController extends AbstractCrudController<LegendSet, GetObjectListParams> {
  @Override
  @RequiresAuthority(anyOf = {F_LEGEND_SET_PUBLIC_ADD, F_LEGEND_SET_PRIVATE_ADD})
  public WebMessage postJsonObject(HttpServletRequest request)
      throws ForbiddenException,
          ConflictException,
          IOException,
          HttpRequestMethodNotSupportedException,
          NotFoundException {
    return super.postJsonObject(request);
  }

  @Override
  @RequiresAuthority(anyOf = {F_LEGEND_SET_PUBLIC_ADD, F_LEGEND_SET_PRIVATE_ADD})
  public WebMessage putJsonObject(
      @PathVariable String uid, @CurrentUser UserDetails currentUser, HttpServletRequest request)
      throws ForbiddenException,
          ConflictException,
          NotFoundException,
          IOException,
          HttpRequestMethodNotSupportedException {
    return super.putJsonObject(uid, currentUser, request);
  }

  @Override
  @RequiresAuthority(anyOf = F_LEGEND_SET_DELETE)
  public WebMessage deleteObject(
      @PathVariable String uid,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request,
      HttpServletResponse response)
      throws ForbiddenException,
          ConflictException,
          NotFoundException,
          HttpRequestMethodNotSupportedException {
    return super.deleteObject(uid, currentUser, request, response);
  }
}
