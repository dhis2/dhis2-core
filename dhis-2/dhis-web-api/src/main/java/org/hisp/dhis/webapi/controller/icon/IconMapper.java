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

import static org.hisp.dhis.fileresource.FileResourceDomain.CUSTOM_ICON;

import java.util.Optional;

import lombok.AllArgsConstructor;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.icon.CustomIcon;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.icon.IconDto;
import org.hisp.dhis.icon.StandardIcon;
import org.hisp.dhis.schema.descriptors.FileResourceSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IconSchemaDescriptor;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class IconMapper
{
    private FileResourceService fileResourceService;

    private CurrentUserService currentUserService;

    private ContextService contextService;

    public IconDto from( Icon icon) {
        if (icon instanceof CustomIcon ci) {
            return new IconDto(icon.getKey(), icon.getDescription(), icon.getKeywords(),
                    ci.getFileResource().getUid(), ci.getCreatedBy().getUid(), getCustomIconReference(ci.getFileResource().getUid()));
        } else {
            return new IconDto(icon.getKey(), icon.getDescription(), icon.getKeywords(), getStandardIconReference(icon.getKey()));
        }
    }

    public CustomIcon to( IconDto iconDto )
        throws BadRequestException
    {
        Optional<FileResource> fileResource = fileResourceService.getFileResource( iconDto.getFileResourceUid(),
            CUSTOM_ICON );
        if ( fileResource.isEmpty() )
        {
            throw new BadRequestException(
                String.format( "File resource with uid %s does not exist", iconDto.getFileResourceUid() ) );
        }

        return new CustomIcon( iconDto.getKey(), iconDto.getDescription(), iconDto.getKeywords(),
            fileResource.get(), currentUserService.getCurrentUser() );
    }

    private String getCustomIconReference( String fileResourceUid )
    {
        return String.format( "%s%s/%s/data", contextService.getApiPath(), FileResourceSchemaDescriptor.API_ENDPOINT,
            fileResourceUid );
    }

    private String getStandardIconReference( String key )
    {
        return String.format( "%s%s/%s/icon.%s", contextService.getApiPath(), IconSchemaDescriptor.API_ENDPOINT, key,
            StandardIcon.Icon.SUFFIX );
    }
}
