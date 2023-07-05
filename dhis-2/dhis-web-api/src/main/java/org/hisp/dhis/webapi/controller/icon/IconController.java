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

import com.google.common.net.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.icon.IconResponse;
import org.hisp.dhis.icon.IconService;
import org.hisp.dhis.schema.descriptors.IconSchemaDescriptor;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Kristian Wærstad
 */
@OpenApi.Tags("ui")
@Controller
@RequestMapping(value = IconSchemaDescriptor.API_ENDPOINT)
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class IconController {
  private static final int TTL = 365;

  private final IconService iconService;

  private final FileResourceService fileResourceService;

  private final IconMapper iconMapper;

  @GetMapping("/{iconKey}")
  public @ResponseBody IconResponse getIcon(@PathVariable String iconKey) throws NotFoundException {
    Icon icon = iconService.getIcon(iconKey);

    return iconMapper.from(icon);
  }

  @GetMapping("/{iconKey}/icon.svg")
  public void getIconData(HttpServletResponse response, @PathVariable String iconKey)
      throws IOException, NotFoundException {
    Resource icon = iconService.getDefaultIconResource(iconKey);

    response.setHeader("Cache-Control", CacheControl.maxAge(TTL, TimeUnit.DAYS).getHeaderValue());
    response.setContentType(MediaType.SVG_UTF_8.toString());

    StreamUtils.copyThenCloseInputStream(icon.getInputStream(), response.getOutputStream());
  }

  @GetMapping
  public @ResponseBody List<IconResponse> getAllIcons(
      @RequestParam(required = false) String[] keywords) {
    List<Icon> icons;

    if (keywords == null) {
      icons = iconService.getIcons();
    } else {
      icons = iconService.getIcons(keywords);
    }

    return icons.stream().map(iconMapper::from).toList();
  }

  @GetMapping("/keywords")
  public @ResponseBody Set<String> getKeywords() {
    return iconService.getKeywords();
  }

  @PostMapping
  public @ResponseBody WebMessage addCustomIcon(@RequestBody IconDto iconDto)
      throws BadRequestException, NotFoundException {
    iconService.addCustomIcon(iconMapper.to(iconDto));
    WebMessage webMessage = new WebMessage(Status.OK, HttpStatus.CREATED);
    webMessage.setMessage(String.format("Icon %s created", iconDto.getKey()));
    return webMessage;
  }

  @PutMapping
  public @ResponseBody WebMessage updateCustomIcon(@RequestBody IconDto iconDto)
      throws BadRequestException, NotFoundException {
    iconService.updateCustomIcon(iconDto.getKey(), iconDto.getDescription(), iconDto.getKeywords());

    WebMessage webMessage = new WebMessage(Status.OK, HttpStatus.OK);
    webMessage.setMessage(String.format("Icon %s updated", iconDto.getKey()));
    return webMessage;
  }

  @DeleteMapping("/{iconKey}")
  public @ResponseBody WebMessage deleteCustomIcon(@PathVariable String iconKey)
      throws BadRequestException, NotFoundException {
    iconService.deleteCustomIcon(iconKey);

    WebMessage webMessage = new WebMessage(Status.OK, HttpStatus.OK);
    webMessage.setMessage(String.format("Icon %s deleted", iconKey));
    return webMessage;
  }
}
