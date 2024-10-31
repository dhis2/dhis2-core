/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.route.Route;
import org.hisp.dhis.route.RouteService;
import org.hisp.dhis.schema.descriptors.RouteSchemaDescriptor;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Morten Olav Hansen
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@OpenApi.Document(classifiers = {"team:extensibility", "purpose:metadata"})
public class RouteController extends AbstractCrudController<Route> {
  private final RouteService routeService;

  @RequestMapping(
      value = "/{id}/run",
      method = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH
      })
  public ResponseEntity<String> run(
      @PathVariable("id") String id,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request)
      throws IOException, ForbiddenException, NotFoundException, BadRequestException {
    return runWithSubpath(id, currentUser, request);
  }

  @RequestMapping(
      value = "/{id}/run/**",
      method = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH
      })
  public ResponseEntity<String> runWithSubpath(
      @PathVariable("id") String id,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request)
      throws IOException, ForbiddenException, NotFoundException, BadRequestException {

    Route route = routeService.getRouteWithDecryptedAuth(id);

    if (route == null) {
      throw new NotFoundException(String.format("Route not found: '%s'", id));
    }

    if (!aclService.canRead(currentUser, route)
        && !currentUser.hasAnyAuthority(route.getAuthorities())) {
      throw new ForbiddenException("User not authorized to execute route");
    }

    Optional<String> subPath = getSubPath(request.getPathInfo(), id);

    return routeService.execute(route, currentUser, subPath, request);
  }

  private Optional<String> getSubPath(String path, String id) {
    String apiPrefix = "/api";
    String prefix = String.format("%s/%s/run/", RouteSchemaDescriptor.API_ENDPOINT, id);

    // /api/{api-version}/
    if (path.startsWith(prefix, apiPrefix.length() + 3)) {
      return Optional.of(path.substring(apiPrefix.length() + 3 + prefix.length()));
      // /api/
    } else if (path.startsWith(prefix, apiPrefix.length())) {
      return Optional.of(path.substring(prefix.length() + apiPrefix.length()));
    }

    return Optional.empty();
  }

  /**
   * Disable the collection API for /api/routes endpoint. This conflicts with sub-path based routes
   * and is not supported by the Route API (no identifiable object collections).
   */
  @Override
  @PostMapping(value = "/addCollectionItem__disabled")
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public WebMessage addCollectionItem(String pvUid, String pvProperty, String pvItemId)
      throws NotFoundException, ConflictException, ForbiddenException, BadRequestException {
    throw new NotFoundException("Method not allowed");
  }

  @Override
  @PostMapping(value = "/deleteCollectionItem__disabled")
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public WebMessage deleteCollectionItem(
      String pvUid, String pvProperty, String pvItemId, HttpServletResponse response)
      throws NotFoundException, ForbiddenException, ConflictException, BadRequestException {
    throw new NotFoundException("Method not allowed");
  }
}
