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
package org.hisp.dhis.webapi.controller.icon;

import static org.hisp.dhis.fileresource.FileResourceDomain.ICON;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.icon.Icon;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class IconMapper {
  private FileResourceService fileResourceService;

  public Icon to(CustomIconRequest customIconRequest) throws BadRequestException {
    Optional<FileResource> fileResource;

    if (customIconRequest.getFileResourceId() == null) {
      throw new BadRequestException("FileResource must be provided with Icon");
    }

    fileResource = fileResourceService.getFileResource(customIconRequest.getFileResourceId(), ICON);
    if (fileResource.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "FileResource with uid %s does not exist", customIconRequest.getFileResourceId()));
    }

    Icon icon = new Icon();
    icon.setKey(customIconRequest.getKey().trim());
    icon.setDescription(customIconRequest.getDescription());
    icon.setKeywords(customIconRequest.getKeywords());
    icon.setFileResource(fileResource.get());
    icon.setCustom(true);

    return icon;
  }

  public void merge(Icon persisted, CustomIconRequest customIconRequest)
      throws BadRequestException {
    Optional<FileResource> fileResource;

    if (customIconRequest.getFileResourceId() != null) {
      fileResource =
          fileResourceService.getFileResource(customIconRequest.getFileResourceId(), ICON);
      if (fileResource.isEmpty()) {
        throw new BadRequestException(
            String.format(
                "FileResource with uid %s does not exist", customIconRequest.getFileResourceId()));
      }

      persisted.setFileResource(fileResource.get());
    }

    if (customIconRequest.getDescription() != null) {
      persisted.setDescription(customIconRequest.getDescription());
    }

    if (customIconRequest.getKeywords() != null) {
      persisted.setKeywords(customIconRequest.getKeywords());
    }

    persisted.setAutoFields();
  }
}
