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
package org.hisp.dhis.webapi.controller.icon;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.icon.IconQueryParams;
import org.hisp.dhis.icon.IconService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.descriptors.IconSchemaDescriptor;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Kristian Wærstad
 */
@OpenApi.Tags("ui")
@RestController
@RequestMapping(value = IconSchemaDescriptor.API_ENDPOINT)
@Slf4j
@AllArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class IconController {
  private static final int TTL = 365;

  private final IconService iconService;

  private final FileResourceService fileResourceService;

  private final DhisConfigurationProvider dhisConfig;

  private final RenderService renderService;

  private final IconMapper iconMapper;

  private final FieldFilterService fieldFilterService;

  private final ContextService contextService;

  private final LinkService linkService;

  private final IconRequestParamMapper iconRequestParamMapper;

  @GetMapping
  public @ResponseBody PaginatedIconResponse getAllIcons(IconRequestParams iconRequestParams)
      throws BadRequestException {
    IconQueryParams iconQueryParams = iconRequestParamMapper.map(iconRequestParams);

    Pager pager = null;

    if (iconRequestParams.isPaging()) {
      long total = iconService.count(iconQueryParams);
      pager = new Pager(iconRequestParams.getPage(), total, iconRequestParams.getPageSize());
      iconQueryParams.setPager(pager);
    }

    List<Icon> icons = iconService.getIcons(iconQueryParams);

    icons.forEach(i -> i.setHref(getCustomIconReference(i.getKey())));

    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(icons.stream().toList(), iconRequestParams.getFields());

    linkService.generatePagerLinks(pager, Icon.class);

    return new PaginatedIconResponse(pager, objectNodes);
  }

  @GetMapping(value = "/{key}/icon")
  public void getIconData(@PathVariable String key, HttpServletResponse response)
      throws NotFoundException, WebMessageException {
    Icon icon = iconService.getIcon(key);

    downloadIconFile(icon, response);
  }

  @GetMapping(value = "/{key}")
  public ResponseEntity<Icon> getIconByKey(@PathVariable String key) throws NotFoundException {
    Icon icon = iconService.getIcon(key);

    if (icon == null) {
      throw new NotFoundException(String.format("Icon with key %s not found", key));
    }

    icon.setHref(getCustomIconReference(icon.getKey()));

    return new ResponseEntity<>(icon, HttpStatus.OK);
  }

  @GetMapping("/{key}/icon.svg")
  @Deprecated
  public void getIconData(
      HttpServletResponse response, HttpServletRequest request, @PathVariable String key)
      throws IOException, NotFoundException {

    if (!iconService.iconExists(key)) {
      throw new NotFoundException(String.format("Icon with key %s not found", key));
    }

    String location = response.encodeRedirectURL("/icons/" + key + "/icon");
    response.sendRedirect(ContextUtils.getRootPath(request) + location);
  }

  @PostMapping
  public WebMessage addIcon(HttpServletRequest request, @CurrentUser User user)
      throws IOException, BadRequestException, NotFoundException, SQLException {

    CustomIconRequest customIconRequest =
        renderService.fromJson(request.getInputStream(), CustomIconRequest.class);
    Icon icon = iconMapper.to(customIconRequest);
    icon.setCreatedBy(user);
    iconService.addIcon(icon);

    return WebMessageUtils.created(String.format("Icon created with key %s", icon.getKey()));
  }

  @PutMapping(value = "/{key}")
  public WebMessage updateIcon(@PathVariable String key, HttpServletRequest request)
      throws IOException, NotFoundException, BadRequestException, SQLException {
    Icon persisted = iconService.getIcon(key);

    if (persisted == null) {
      throw new NotFoundException(String.format("Icon with key %s not found", key));
    }

    if (!persisted.isCustom()) {
      throw new BadRequestException("Not allowed to update default icon");
    }

    CustomIconRequest customIconRequest =
        renderService.fromJson(request.getInputStream(), CustomIconRequest.class);

    iconMapper.merge(persisted, customIconRequest);

    iconService.updateIcon(persisted);

    return WebMessageUtils.ok(String.format("Icon with key %s updated", key));
  }

  @DeleteMapping(value = "/{key}")
  public WebMessage deleteIcon(@PathVariable String key)
      throws NotFoundException, BadRequestException {

    iconService.deleteIcon(key);

    return WebMessageUtils.ok(String.format("Icon with key %s deleted", key));
  }

  private void downloadIconFile(Icon icon, HttpServletResponse response)
      throws NotFoundException, WebMessageException {

    FileResource fileResource =
        fileResourceService.getFileResource(icon.getFileResource().getUid());

    if (fileResource == null) {
      throw new NotFoundException(FileResource.class, icon.getFileResource().getUid());
    }

    response.setContentType(
        icon.isCustom() ? fileResource.getContentType() : MediaType.SVG_UTF_8.toString());

    response.setHeader("Cache-Control", CacheControl.maxAge(TTL, TimeUnit.DAYS).getHeaderValue());
    response.setHeader(
        HttpHeaders.CONTENT_LENGTH,
        String.valueOf(fileResourceService.getFileResourceContentLength(fileResource)));
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName());
    HeaderUtils.setSecurityHeaders(
        response, dhisConfig.getProperty(ConfigurationKey.CSP_HEADER_VALUE));

    try {
      fileResourceService.copyFileResourceContent(fileResource, response.getOutputStream());
    } catch (IOException e) {
      log.error("Could not retrieve file.", e);
      throw new WebMessageException(
          error(
              "Failed fetching the file from storage",
              "There was an exception when trying to fetch the file from the storage backend. "
                  + "Depending on the provider the root cause could be network or file system related."));
    }
  }

  private String getCustomIconReference(String key) {
    return String.format(
        "%s%s/%s/icon", contextService.getApiPath(), IconSchemaDescriptor.API_ENDPOINT, key);
  }
}
