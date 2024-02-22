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

import com.google.common.net.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.icon.CustomIcon;
import org.hisp.dhis.icon.CustomIconService;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.descriptors.IconSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractFullReadOnlyController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
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
public class IconController extends AbstractFullReadOnlyController<CustomIcon> {
  private static final int TTL = 365;

  private final CustomIconService iconService;

  private final FileResourceService fileResourceService;

  private final DhisConfigurationProvider dhisConfig;

  private final RenderService renderService;

  private final IconMapper iconMapper;

  @GetMapping(value = "/{key}/icon")
  public void getIconData(@PathVariable String key, HttpServletResponse response)
      throws NotFoundException, WebMessageException, IOException {

    CustomIcon customIcon = iconService.getCustomIcon(key);

    if (!customIcon.getCustom()) {
      downloadDefaultIcon(customIcon.getIconKey(), response);
    } else {
      downloadCustomIcon(customIcon.getFileResource(), response);
    }
  }

  @PostMapping
  public WebMessage addCustomIcon(HttpServletRequest request)
      throws IOException, BadRequestException, NotFoundException {

    CustomIconRequest customIconRequest =
        renderService.fromJson(request.getInputStream(), CustomIconRequest.class);
    CustomIcon customIcon = iconMapper.to(customIconRequest);

    iconService.addCustomIcon(customIcon);

    return WebMessageUtils.created(
        String.format("CustomIcon created with key %s", customIcon.getIconKey()));
  }

  @PutMapping(value = "/{uid}")
  public WebMessage updateCustomIcon(@PathVariable String uid, HttpServletRequest request)
      throws IOException, NotFoundException, WebMessageException, BadRequestException {

    CustomIcon customIcon = iconService.getCustomIconByUid(uid);

    if (customIcon == null) {
      throw new WebMessageException(
          WebMessageUtils.notFound(String.format("CustomIcon with uid %s not found", uid)));
    }

    CustomIconRequest customIconRequest =
        renderService.fromJson(request.getInputStream(), CustomIconRequest.class);

    customIcon = iconMapper.to(customIconRequest);

    iconService.updateCustomIcon(customIcon);

    return WebMessageUtils.ok(String.format("CustomIcon with uid %s updated", uid));
  }

  @DeleteMapping(value = "/{iconKey}")
  public WebMessage deleteCustomIcon(@PathVariable String iconKey)
      throws NotFoundException, WebMessageException, BadRequestException {

    CustomIcon customIcon = iconService.getCustomIcon(iconKey);

    if (customIcon == null) {
      throw new WebMessageException(
          WebMessageUtils.notFound(String.format("CustomIcon with iconKey %s not found", iconKey)));
    }

    iconService.deleteCustomIcon(iconKey);

    return WebMessageUtils.ok(String.format("CustomIcon with iconKey %s deleted", iconKey));
  }

  @GetMapping("/keywords")
  public @ResponseBody Set<String> getKeywords() {
    return iconService.getKeywords();
  }

  @Override
  protected void postProcessResponseEntities(
      List<CustomIcon> entityList, WebOptions options, Map<String, String> parameters) {

    entityList.forEach(
        ci ->
            ci.setReference(
                ci.getCustom()
                    ? getCustomIconReference(ci.getIconKey())
                    : getDefaultIconReference(ci.getIconKey())));
  }

  @Override
  protected void postProcessResponseEntity(
      CustomIcon entity, WebOptions options, Map<String, String> parameters) {

    entity.setReference(
        entity.getCustom()
            ? getCustomIconReference(entity.getIconKey())
            : getDefaultIconReference(entity.getIconKey()));
  }

  private void downloadDefaultIcon(String key, HttpServletResponse response)
      throws IOException, NotFoundException {
    Resource icon = iconService.getCustomIconResource(key);

    response.setHeader("Cache-Control", CacheControl.maxAge(TTL, TimeUnit.DAYS).getHeaderValue());
    response.setContentType(MediaType.SVG_UTF_8.toString());

    StreamUtils.copyThenCloseInputStream(icon.getInputStream(), response.getOutputStream());
  }

  private void downloadCustomIcon(FileResource iconFileResource, HttpServletResponse response)
      throws NotFoundException, WebMessageException {
    FileResource fileResource = fileResourceService.getFileResource(iconFileResource.getUid());

    if (fileResource == null) {
      throw new NotFoundException(FileResource.class, iconFileResource.getUid());
    }

    response.setContentType(fileResource.getContentType());
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

  private String getCustomIconReference(String fileResourceUid) {
    return String.format(
        "%s%s/%s/icon",
        contextService.getApiPath(), IconSchemaDescriptor.API_ENDPOINT, fileResourceUid);
  }

  private String getDefaultIconReference(String key) {
    return String.format(
        "%s%s/%s/icon.%s",
        contextService.getApiPath(), IconSchemaDescriptor.API_ENDPOINT, key, Icon.SUFFIX);
  }
}
