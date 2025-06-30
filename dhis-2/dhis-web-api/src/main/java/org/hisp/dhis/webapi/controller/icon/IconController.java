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
package org.hisp.dhis.webapi.controller.icon;

import static java.lang.String.format;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.icon.AddIconRequest;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.icon.IconQueryParams;
import org.hisp.dhis.icon.IconService;
import org.hisp.dhis.icon.UpdateIconRequest;
import org.hisp.dhis.schema.descriptors.IconSchemaDescriptor;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Kristian Wærstad
 */
@OpenApi.Document(
    entity = Icon.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/icons")
@Slf4j
@AllArgsConstructor
public class IconController {
  private static final int TTL = 365;

  private final IconService iconService;
  private final FileResourceService fileResourceService;
  private final DhisConfigurationProvider dhisConfig;
  private final FieldFilterService fieldFilterService;
  private final ContextService contextService;
  private final LinkService linkService;

  @GetMapping
  public @ResponseBody IconListResponse getAllIcons(
      IconQueryParams params, @RequestParam(required = false) List<FieldPath> fields)
      throws BadRequestException {

    Pager pager = null;
    if (params.isPaging()) {
      long total = iconService.count(params);
      pager = new Pager(params.getPage(), total, params.getPageSize());
    }

    List<Icon> icons = iconService.getIcons(params);
    icons.forEach(i -> i.setHref(getIconHref(i.getKey())));

    if (fields == null || fields.isEmpty()) {
      fields = FieldFilterParser.parse("key,keywords,description,fileResource,createdBy,href");
    }
    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(icons.stream().toList(), fields);
    linkService.generatePagerLinks(pager, Icon.class);

    return new IconListResponse(pager, objectNodes);
  }

  @GetMapping(value = "/{key}/icon")
  public void getIconData(@PathVariable String key, HttpServletResponse response)
      throws NotFoundException, ConflictException {
    Icon icon = iconService.getIcon(key);
    downloadIconImage(icon, response);
  }

  @GetMapping(value = "/{key}")
  public ResponseEntity<Icon> getIconByKey(@PathVariable String key) throws NotFoundException {
    Icon icon = iconService.getIcon(key);
    icon.setHref(getIconHref(icon.getKey()));
    return new ResponseEntity<>(icon, HttpStatus.OK);
  }

  @GetMapping("/{key}/icon.svg")
  @Deprecated
  public void getIconData(
      HttpServletResponse response, HttpServletRequest request, @PathVariable String key)
      throws IOException, NotFoundException {
    if (!iconService.iconExists(key)) {
      throw new NotFoundException(Icon.class, key);
    }

    String location = response.encodeRedirectURL("/api/icons/" + key + "/icon");
    response.sendRedirect(ContextUtils.getRootPath(request) + location);
  }

  @PostMapping
  public WebMessage addIcon(@RequestBody AddIconRequest request)
      throws BadRequestException, NotFoundException {
    Icon icon = iconService.addIcon(request, null);

    return created(format("Icon created with key %s", icon.getKey()));
  }

  @Maturity.Alpha
  @PatchMapping
  public WebMessage repairPhantomIcons() throws ConflictException {
    int repaired = iconService.repairPhantomDefaultIcons();
    return ok(format("%d icons repaired", repaired));
  }

  @PutMapping(value = "/{key}")
  public WebMessage updateIcon(@PathVariable String key, @RequestBody UpdateIconRequest request)
      throws NotFoundException, BadRequestException {
    iconService.updateIcon(key, request);

    return ok(format("Icon with key %s updated", key));
  }

  @DeleteMapping(value = "/{key}")
  public WebMessage deleteIcon(@PathVariable String key)
      throws NotFoundException, BadRequestException {
    iconService.deleteIcon(key);

    return ok(format("Icon with key %s deleted", key));
  }

  private void downloadIconImage(Icon icon, HttpServletResponse response)
      throws NotFoundException, ConflictException {
    FileResource image = fileResourceService.getFileResource(icon.getFileResource().getUid());
    if (image == null) {
      throw new NotFoundException(FileResource.class, icon.getFileResource().getUid());
    }

    response.setContentType(image.getContentType());
    response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(image.getContentLength()));
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + image.getName());
    response.setHeader("Cache-Control", CacheControl.maxAge(TTL, TimeUnit.DAYS).getHeaderValue());
    HeaderUtils.setSecurityHeaders(
        response, dhisConfig.getProperty(ConfigurationKey.CSP_HEADER_VALUE));

    try {
      fileResourceService.copyFileResourceContent(image, response.getOutputStream());
    } catch (IOException e) {
      log.error("Could not retrieve file.", e);
      throw new ConflictException(FileResourceStream.EXCEPTION_IO)
          .setDevMessage(FileResourceStream.EXCEPTION_IO_DEV);
    }
  }

  private String getIconHref(String key) {
    return format(
        "%s%s/%s/icon", contextService.getApiPath(), IconSchemaDescriptor.API_ENDPOINT, key);
  }
}
