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
package org.hisp.dhis.webapi.controller.sms;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.F_MOBILE_SENDSMS;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.commons.jackson.domain.JsonRoot;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.sms.config.GatewayAdministrationService;
import org.hisp.dhis.sms.config.SmsConfiguration;
import org.hisp.dhis.sms.config.SmsConfigurationManager;
import org.hisp.dhis.sms.config.SmsGateway;
import org.hisp.dhis.sms.config.SmsGatewayConfig;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** Zubair <rajazubair.asghar@gmail.com> */
@OpenApi.Document(
    entity = SmsGateway.class,
    classifiers = {"team:tracker", "purpose:metadata"})
@RestController
@RequestMapping("/api/gateways")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class SmsGatewayController {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  @Autowired private RenderService renderService;

  @Autowired private GatewayAdministrationService gatewayAdminService;

  @Autowired private SmsConfigurationManager smsConfigurationManager;

  @Autowired private FieldFilterService fieldFilterService;

  // -------------------------------------------------------------------------
  // GET
  // -------------------------------------------------------------------------

  @OpenApi.Response(
      status = OpenApi.Response.Status.OK,
      object = {
        @OpenApi.Property(name = "pager", value = Pager.class),
        @OpenApi.Property(name = "gateways", value = SmsGatewayConfig[].class)
      })
  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonRoot> getGateways(
      @RequestParam(defaultValue = "*") List<String> fields) {
    SmsConfiguration config = smsConfigurationManager.getSmsConfiguration();
    FieldFilterParams<?> params = FieldFilterParams.of(config.getGateways(), fields);

    return ResponseEntity.ok(JsonRoot.of("gateways", fieldFilterService.toObjectNodes(params)));
  }

  @OpenApi.Response(SmsGatewayConfig.class)
  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @GetMapping(value = "/{uid}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<ObjectNode> getGatewayConfiguration(
      @PathVariable String uid, @RequestParam(defaultValue = "*") List<FieldPath> fields)
      throws NotFoundException {
    SmsGatewayConfig gateway = getExistingConfig(uid);

    return ResponseEntity.ok(fieldFilterService.toObjectNode(gateway, fields));
  }

  // -------------------------------------------------------------------------
  // PUT,POST
  // -------------------------------------------------------------------------

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @PutMapping("/default/{uid}")
  @ResponseBody
  public WebMessage setDefault(@PathVariable String uid)
      throws NotFoundException, ForbiddenException, ConflictException, BadRequestException {
    SmsGatewayConfig gateway = getExistingConfig(uid);

    gatewayAdminService.setDefaultGateway(gateway);

    return ok(gateway.getName() + " is set to default");
  }

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @PutMapping("/{uid}")
  public WebMessage updateGateway(@PathVariable String uid, HttpServletRequest request)
      throws IOException,
          NotFoundException,
          ConflictException,
          ForbiddenException,
          BadRequestException {
    SmsGatewayConfig config = getExistingConfig(uid);

    SmsGatewayConfig updatedConfig =
        renderService.fromJson(request.getInputStream(), SmsGatewayConfig.class);

    if (gatewayAdminService.hasDefaultGateway()
        && updatedConfig.isDefault()
        && !config.isDefault()) {
      throw new ConflictException("Default gateway already exists");
    }

    updatedConfig.setUid(uid); // just make sure it is set and identical to the path parameter
    gatewayAdminService.updateGateway(config, updatedConfig);

    return ok(String.format("Gateway with uid: %s has been updated", uid));
  }

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @PostMapping
  @ResponseBody
  public WebMessage addGateway(HttpServletRequest request)
      throws IOException, ConflictException, ForbiddenException, BadRequestException {
    SmsGatewayConfig config =
        renderService.fromJson(request.getInputStream(), SmsGatewayConfig.class);

    if (config == null) {
      throw new ConflictException("Cannot de-serialize SMS configurations");
    }

    gatewayAdminService.addGateway(config);

    return ok("Gateway configuration added").setLocation("/gateways/" + config.getUid());
  }

  // -------------------------------------------------------------------------
  // DELETE
  // -------------------------------------------------------------------------

  @RequiresAuthority(anyOf = F_MOBILE_SENDSMS)
  @DeleteMapping("/{uid}")
  @ResponseBody
  public WebMessage removeGateway(@PathVariable String uid)
      throws NotFoundException, ForbiddenException, ConflictException, BadRequestException {
    getExistingConfig(uid);

    gatewayAdminService.removeGatewayByUid(uid);

    return ok("Gateway removed successfully");
  }

  private SmsGatewayConfig getExistingConfig(String uid) throws NotFoundException {
    SmsGatewayConfig gateway = gatewayAdminService.getByUid(uid);
    if (gateway == null) {
      throw new NotFoundException(SmsGatewayConfig.class, uid);
    }
    return gateway;
  }
}
