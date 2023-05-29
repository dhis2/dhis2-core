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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.icon.CustomIcon;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.icon.IconResponse;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class IconMapperTest
{

    @Mock
    private FileResourceService fileResourceService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ContextService contextService;

    private static final String KEY = "icon key";

    private static final String DESCRIPTION = "description";

    private static final String[] KEYWORDS = { "k1", "k2" };

    private static final FileResource fileResource = new FileResource();

    private IconMapper iconMapper;

    @BeforeEach
    void setUp()
    {
        fileResource.setUid( "file resource uid" );
        iconMapper = new IconMapper( fileResourceService, currentUserService, contextService );
    }

    @Test
    void shouldReturnCustomIconFromIconDto()
        throws BadRequestException
    {
        IconDto iconDto = new IconDto( KEY, DESCRIPTION, KEYWORDS, fileResource.getUid() );
        when( fileResourceService.getFileResource( fileResource.getUid(), CUSTOM_ICON ) )
            .thenReturn( Optional.of( fileResource ) );
        User user = new User();
        user.setUid( "user uid" );
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        CustomIcon customIcon = iconMapper.to( iconDto );

        assertEquals( KEY, customIcon.getKey() );
        assertEquals( DESCRIPTION, customIcon.getDescription() );
        assertEquals( KEYWORDS, customIcon.getKeywords() );
        assertEquals( fileResource.getUid(), customIcon.getFileResourceUid() );
        assertEquals( currentUserService.getCurrentUser().getUid(), customIcon.getCreatedByUserUid() );
    }

    @Test
    void shouldFailWhenMappingToCustomIconWithNonExistentFileResource()
    {
        IconDto iconDto = new IconDto( KEY, DESCRIPTION, KEYWORDS, fileResource.getUid() );

        Exception exception = assertThrows( BadRequestException.class, () -> iconMapper.to( iconDto ) );
        assertEquals( String.format( "File resource with uid %s does not exist", iconDto.getFileResourceUid() ),
            exception.getMessage() );
    }

    @Test
    void shouldReturnIconResponseFromIcon()
    {
        User user = new User();
        user.setUid( "user uid" );
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        Icon icon = new CustomIcon( KEY, DESCRIPTION, KEYWORDS, fileResource.getUid(),
            currentUserService.getCurrentUser().getUid() );

        IconResponse iconResponse = iconMapper.from( icon );

        assertEquals( KEY, iconResponse.getKey() );
        assertEquals( DESCRIPTION, iconResponse.getDescription() );
        assertEquals( KEYWORDS, iconResponse.getKeywords() );
        assertEquals( fileResource.getUid(), iconResponse.getFileResourceUid() );
        assertEquals( currentUserService.getCurrentUser().getUid(), iconResponse.getUserUid() );
    }
}