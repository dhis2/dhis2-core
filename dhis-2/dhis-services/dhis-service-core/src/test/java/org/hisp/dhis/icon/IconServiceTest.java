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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
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

    @Spy
    @InjectMocks
    private DefaultIconService iconService;

    @Test
    void shouldSaveIconWhenIconHasNoDuplicatedKeyAndFileResourceExists()
        throws BadRequestException
    {
        String uniqueKey = "key";
        long fileResourceId = 12345;
        FileResource fileResource = new FileResource();
        fileResource.setId( fileResourceId );
        when( customIconStore.getIconByKey( uniqueKey ) ).thenReturn( null );
        when( fileResourceService.getFileResource( fileResourceId ) ).thenReturn( new FileResource() );

        iconService.addIcon( uniqueKey, "description", List.of( "keyword1" ), fileResource );

        verify( customIconStore, times( 1 ) ).save( any( IconData.class ) );
    }

    @Test
    void shouldFailWhenSavingIconWithEmptyKey()
    {
        String emptyKey = "";

        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.addIcon( emptyKey, "description", List.of( "keyword1" ), new FileResource() ) );

        String expectedMessage = "Icon key not specified.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenSavingIconWithNoFileResourceId()
    {
        String iconKey = "key";

        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.addIcon( iconKey, "description", List.of( "keyword1" ), null ) );

        String expectedMessage = "File resource id not specified.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenSavingIconWithNonExistentFileResourceId()
    {
        String iconKey = "standard key";
        long fileResourceId = 12345;
        FileResource fileResource = new FileResource();
        fileResource.setId( fileResourceId );
        when( customIconStore.getIconByKey( iconKey ) ).thenReturn( null );
        when( fileResourceService.getFileResource( anyLong() ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.addIcon( iconKey, "description", List.of( "keyword1" ), fileResource ) );

        String expectedMessage = String.format( "File resource %d does not exist", fileResourceId );
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenSavingIconAndStandardIconWithSameKeyExists()
    {
        String duplicatedKey = "standard key";
        when( customIconStore.getIconByKey( duplicatedKey ) ).thenReturn( null );
        when( iconService.getIcon( duplicatedKey ) )
            .thenReturn( new IconData( duplicatedKey, "desc", Collections.emptyList() ) );

        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.addIcon( duplicatedKey, "description", List.of( "keyword1" ), new FileResource() ) );

        String expectedMessage = "Icon with key " + duplicatedKey + " already exists.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenSavingIconAndCustomIconWithSameKeyExists()
    {
        String duplicatedKey = "custom key";
        when( customIconStore.getIconByKey( duplicatedKey ) )
            .thenReturn( new IconData( "key", "description", Collections.emptyList() ) );

        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.addIcon( duplicatedKey, "description", List.of( "keyword1" ), new FileResource() ) );

        String expectedMessage = "Icon with key " + duplicatedKey + " already exists.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldUpdateIconDescriptionWhenKeyPresentAndCustomIconExists()
        throws BadRequestException
    {
        String uniqueKey = "key";
        when( customIconStore.getIconByKey( uniqueKey ) ).thenReturn( new IconData() );

        iconService.updateIconDescription( uniqueKey, "new description" );

        verify( customIconStore, times( 1 ) ).update( any( IconData.class ) );
    }

    @Test
    void shouldFailWhenUpdatingIconWithoutKey()
    {
        String emptyKey = "";

        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.updateIconDescription( emptyKey, "new description" ) );

        String expectedMessage = "Icon key not specified.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenUpdatingNonExistingIcon()
    {
        String key = "key";
        when( customIconStore.getIconByKey( key ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class,
            () -> iconService.updateIconDescription( key, "new description" ) );

        String expectedMessage = String.format( "Custom icon with key %s does not exists.", key );
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldDeleteIconWhenKeyPresentAndCustomIconExists()
        throws BadRequestException
    {
        String uniqueKey = "key";
        when( customIconStore.getIconByKey( uniqueKey ) )
            .thenReturn( new IconData( uniqueKey, "description", Collections.emptyList(), new FileResource() ) );
        when( fileResourceService.getFileResource( anyLong() ) ).thenReturn( new FileResource() );

        iconService.deleteIcon( uniqueKey );

        verify( customIconStore, times( 1 ) ).delete( any( IconData.class ) );
    }

    @Test
    void shouldFailWhenDeletingIconWithoutKey()
    {
        String emptyKey = "";

        Exception exception = assertThrows( BadRequestException.class, () -> iconService.deleteIcon( emptyKey ) );

        String expectedMessage = "Icon key not specified.";
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void shouldFailWhenDeletingNonExistingIcon()
    {
        String key = "key";
        when( customIconStore.getIconByKey( key ) ).thenReturn( null );

        Exception exception = assertThrows( BadRequestException.class, () -> iconService.deleteIcon( key ) );

        String expectedMessage = String.format( "Custom icon with key %s does not exists.", key );
        assertEquals( expectedMessage, exception.getMessage() );
    }
}