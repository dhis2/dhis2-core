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
package org.hisp.dhis.icon;

import static org.hisp.dhis.fileresource.FileResourceDomain.CUSTOM_ICON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class IconServiceTest
{
    @Mock
    private CustomIconStore customIconStore;

    @Mock
    private FileResourceService fileResourceService;

    @Mock
    private CurrentUserService currentUserService;

    @Spy
    @InjectMocks
    private DefaultIconService iconService;

    @Test
    void shouldSaveCustomIconWhenIconHasNoDuplicatedKeyAndFileResourceExists()
        throws BadRequestException,
        NotFoundException
    {
        String uniqueKey = "key";
        String fileResourceUid = "12345";
        when( customIconStore.getIconByKey( uniqueKey ) ).thenReturn( null );
        when( fileResourceService.getFileResource( fileResourceUid, CUSTOM_ICON ) )
            .thenReturn( Optional.of( new FileResource() ) );
        User user = new User();
        user.setId( 1234 );
        when( currentUserService.getCurrentUser() ).thenReturn( user );

        iconService
            .addCustomIcon(
                new CustomIcon( uniqueKey, "description", new String[] { "keyword1" }, fileResourceUid, "userUid" ) );

        verify( customIconStore, times( 1 ) ).save( any( CustomIcon.class ), anyLong(), anyLong() );
    }

    @Test
    void shouldFailWhenSavingCustomIconWithEmptyKey()
    {
        String emptyKey = "";

        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.addCustomIcon(
                new CustomIcon( emptyKey, "description", new String[] { "keyword1" }, "fileResourceUid",
                    "userUid" ) ) );

        String expectedMessage = "Icon key not specified.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenSavingCustomIconWithNonExistentFileResourceId()
    {
        String iconKey = "default key";
        String fileResourceUid = "12345";
        when( customIconStore.getIconByKey( iconKey ) ).thenReturn( null );
        when( fileResourceService.getFileResource( anyString(), any( FileResourceDomain.class ) ) )
            .thenReturn( Optional.empty() );

        Exception exception = assertThrows( NotFoundException.class,
            () -> iconService.addCustomIcon(
                new CustomIcon( iconKey, "description", new String[] { "keyword1" }, fileResourceUid, "userUid" ) ) );

        String expectedMessage = String.format( "File resource %s does not exist", fileResourceUid );
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenSavingCustomIconAndIconWithSameKeyExists()
    {
        String duplicatedKey = "custom key";
        when( customIconStore.getIconByKey( duplicatedKey ) )
            .thenReturn(
                new CustomIcon( "key", "description", new String[] {}, "fileResourceUid", "userUid" ) );

        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.addCustomIcon(
                new CustomIcon( duplicatedKey, "description", new String[] { "keyword1" }, "fileResourceUid",
                    "userUid" ) ) );

        String expectedMessage = "Icon with key " + duplicatedKey + " already exists.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenSavingCustomIconWithNoFileResourceId()
    {
        String iconKey = "key";
        CustomIcon customIcon = new CustomIcon( iconKey, "description", new String[] { "keyword1" }, null, "userUid" );
        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.addCustomIcon( customIcon ) );

        String expectedMessage = "File resource id not specified.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldUpdateCustomIconIconWhenKeyPresentAndCustomIconExists()
        throws BadRequestException,
        NotFoundException
    {
        String uniqueKey = "key";
        when( customIconStore.getIconByKey( uniqueKey ) ).thenReturn( new CustomIcon() );

        iconService.updateCustomIcon( uniqueKey, "new description", new String[] { "k1", "k2" } );

        verify( customIconStore, times( 1 ) ).update( any( CustomIcon.class ) );
    }

    @Test
    void shouldFailWhenUpdatingCustomIconWithoutKey()
    {
        String emptyKey = "";

        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.updateCustomIcon( emptyKey, "new description", null ) );

        String expectedMessage = "Icon key not specified.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenUpdatingNonExistingCustomIcon()
    {
        String key = "key";
        when( customIconStore.getIconByKey( key ) ).thenReturn( null );

        Exception exception = assertThrows( NotFoundException.class,
            () -> iconService.updateCustomIcon( key, "new description", null ) );

        String expectedMessage = String.format( "Custom icon not found: %s", key );
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenUpdatingCustomIconWithoutDescriptionNorKeywords()
    {
        String uniqueKey = "key";

        when( customIconStore.getIconByKey( uniqueKey ) ).thenReturn(
            new CustomIcon( uniqueKey, "description", new String[] {}, "fileResourceUid", "userUid" ) );
        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.updateCustomIcon( uniqueKey, null, null ) );

        String expectedMessage = String
            .format( "Can't update icon %s if none of description and keywords are present in the request", uniqueKey );
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldDeleteIconWhenKeyPresentAndCustomIconExists()
        throws BadRequestException,
        NotFoundException
    {
        String uniqueKey = "key";
        when( customIconStore.getIconByKey( uniqueKey ) ).thenReturn(
            new CustomIcon( uniqueKey, "description", new String[] {}, "fileResourceUid", "userUid" ) );
        when( fileResourceService.getFileResource( "fileResourceUid" ) ).thenReturn( new FileResource() );

        iconService.deleteCustomIcon( uniqueKey );

        verify( customIconStore, times( 1 ) ).delete( anyString() );
    }

    @Test
    void shouldFailWhenDeletingCustomIconWithoutKey()
    {
        String emptyKey = "";

        Exception exception = assertThrows( BadRequestException.class, () -> iconService.deleteCustomIcon( emptyKey ) );

        String expectedMessage = "Icon key not specified.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenDeletingNonExistingCustomIcon()
    {
        String key = "key";
        when( customIconStore.getIconByKey( key ) ).thenReturn( null );

        Exception exception = assertThrows( NotFoundException.class, () -> iconService.deleteCustomIcon( key ) );

        String expectedMessage = String.format( "Custom icon not found: %s", key );
        assertEquals( expectedMessage, exception.getMessage() );
    }
}