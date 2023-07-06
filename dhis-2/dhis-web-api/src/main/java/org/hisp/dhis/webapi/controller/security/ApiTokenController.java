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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.objectReport;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.schema.descriptors.ApiTokenSchemaDescriptor;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.security.apikey.ApiTokenType;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Controller
@RequestMapping(value = ApiTokenSchemaDescriptor.API_ENDPOINT)
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class ApiTokenController extends AbstractCrudController<ApiToken> {
  public static final String OPERATION_NOT_SUPPORTED_ON_API_TOKEN =
      "Operation not supported on ApiToken";

  private final ApiTokenService apiTokenService;

  // Overwritten to get full access control on GET single object
  @Override
  protected List<ApiToken> getEntity(String uid, WebOptions options) {
    ArrayList<ApiToken> list = new ArrayList<>();
    java.util.Optional.ofNullable(manager.get(ApiToken.class, uid)).ifPresent(list::add);
    return list;
  }

  @Override
  protected void postProcessResponseEntity(
      ApiToken entity, WebOptions options, Map<String, String> parameters) throws Exception {
    entity.setKey(null);
  }

  @Override
  protected void postProcessResponseEntities(
      List<ApiToken> entityList, WebOptions options, Map<String, String> parameters) {
    entityList.forEach(t -> t.setKey(null));
  }

  @Override
  public void partialUpdateObject(
      String pvUid,
      Map<String, String> rpParameters,
      @CurrentUser User currentUser,
      HttpServletRequest request) {
    throw new IllegalStateException(OPERATION_NOT_SUPPORTED_ON_API_TOKEN);
  }

  @Override
  public void updateObjectProperty(
      String pvUid,
      String pvProperty,
      Map<String, String> rpParameters,
      @CurrentUser User currentUser,
      HttpServletRequest request) {
    throw new IllegalStateException(OPERATION_NOT_SUPPORTED_ON_API_TOKEN);
  }

  @Override
  @PostMapping(consumes = {"application/xml", "text/xml"})
  @ResponseBody
  public WebMessage postXmlObject(HttpServletRequest request) {
    throw new IllegalStateException(OPERATION_NOT_SUPPORTED_ON_API_TOKEN);
  }

  @Override
  @PostMapping(consumes = "application/json")
  @ResponseBody
  public WebMessage postJsonObject(HttpServletRequest request) throws Exception {
    final ApiToken apiToken = deserializeJsonEntity(request);

    User user = currentUserService.getCurrentUser();
    if (!aclService.canCreate(user, getEntityClass())) {
      throw new CreateAccessDeniedException(
          "You don't have the proper permissions to create this object.");
    }

    apiToken.getTranslations().clear();

    // Validate input values is ok
    validateBeforeCreate(apiToken);

    // We only make personal access tokens for now
    apiToken.setType(ApiTokenType.PERSONAL_ACCESS_TOKEN);
    // Generate key and set default values
    apiTokenService.initToken(apiToken);

    // Save raw key to send in response
    final String rawKey = apiToken.getKey();

    // Hash the raw token key and overwrite value in the entity to persist
    final String hashedKey = apiTokenService.hashKey(apiToken.getKey());
    apiToken.setKey(hashedKey);

    // Continue POST import as usual
    MetadataImportParams params =
        importService
            .getParamsFromMap(contextService.getParameterValuesMap())
            .setImportReportMode(ImportReportMode.FULL)
            .setUser(user)
            .setImportStrategy(ImportStrategy.CREATE)
            .addObject(apiToken);

    final ObjectReport objectReport = importService.importMetadata(params).getFirstObjectReport();
    final String uid = objectReport.getUid();

    WebMessage webMessage = objectReport(objectReport);
    if (webMessage.getStatus() == Status.OK) {
      webMessage.setHttpStatus(HttpStatus.CREATED);
      webMessage.setLocation(getSchema().getRelativeApiEndpoint() + "/" + uid);

      // Set our custom web response object that includes the new
      // generated key.
      webMessage.setResponse(new ApiTokenCreationResponse(objectReport, rawKey));
    } else {
      webMessage.setStatus(Status.ERROR);
    }

    return webMessage;
  }

  @Override
  protected void prePatchEntity(ApiToken oldToken, ApiToken newToken) {
    newToken.setKey(oldToken.getKey());
    validateApiKeyAttributes(newToken);
  }

  @Override
  protected void preUpdateEntity(ApiToken oldToken, ApiToken newToken) {
    newToken.setKey(oldToken.getKey());
    validateApiKeyAttributes(newToken);
  }

  protected void validateBeforeCreate(ApiToken token) {
    validateApiKeyAttributes(token);
  }

  private void validateApiKeyAttributes(ApiToken token) {
    if (token.getIpAllowedList() != null) {
      token.getIpAllowedList().getAllowedIps().forEach(this::validateIp);
    }
    if (token.getMethodAllowedList() != null) {
      token.getMethodAllowedList().getAllowedMethods().forEach(this::validateHttpMethod);
    }
    if (token.getRefererAllowedList() != null) {
      token.getRefererAllowedList().getAllowedReferrers().forEach(this::validateReferrer);
    }
  }

  private void validateHttpMethod(String httpMethodName) {
    final ImmutableList<String> validMethods =
        ImmutableList.of("GET", "POST", "PATCH", "PUT", "DELETE");
    if (!validMethods.contains(httpMethodName.toUpperCase(Locale.ROOT))) {
      throw new IllegalArgumentException("Not a valid http method, value=" + httpMethodName);
    }
  }

  private void validateIp(String ip) {
    InetAddressValidator validator = new InetAddressValidator();
    if (!validator.isValid(ip)) {
      throw new IllegalArgumentException("Not a valid ip address, value=" + ip);
    }
  }

  private void validateReferrer(String referrer) {
    UrlValidator urlValidator = new UrlValidator();
    if (!urlValidator.isValid(referrer)) {
      throw new IllegalArgumentException("Not a valid referrer url, value=" + referrer);
    }
  }
}
