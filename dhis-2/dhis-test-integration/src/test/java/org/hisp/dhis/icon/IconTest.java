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
package org.hisp.dhis.icon;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertGreaterOrEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.SneakyThrows;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

class IconTest extends TrackerTest
{
    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private IconService iconService;

    private final String iconKey = "iconKey";

    private final List<String> keywords = List.of( "k1", "k2", "k3" );

    @SneakyThrows
    @Override
    protected void initTest()
        throws IOException
    {
        FileResource fileResource = createAndPersistFileResource( 'A' );
        iconService.addCustomIcon(
            new IconData( iconKey, "description", keywords, fileResource, currentUserService.getCurrentUser() ) );
    }

    @Test
    void shouldGetAllIconsWhenRequested()
    {
        Map<String, IconData> standardIconMap = getAllStandardIcons();

        Assertions.assertEquals( standardIconMap.size() + 1, iconService.getIcons().size(),
            String.format( "Expected to find %d icons, but found %d instead", standardIconMap.size() + 1,
                iconService.getIcons().size() ) );
    }

    @Test
    void shouldGetStandardIconWhenKeyBelongsToStandardIcon()
        throws NotFoundException
    {
        String standardIconKey = "2g_positive";

        IconData iconData = iconService.getIcon( standardIconKey );

        assertEquals( standardIconKey, iconData.getKey() );
    }

    @Test
    void shouldGetAllKeywordsWhenRequested()
    {
        List<String> keywordList = getAllStandardIcons().values().stream()
            .map( IconData::getKeywords )
            .flatMap( List::stream ).collect( Collectors.toList() );

        Assertions.assertEquals( keywordList.size() + keywords.size(), iconService.getKeywords().size(),
            String.format( "Expected to find %d icons, but found %d instead", keywordList.size() + 1,
                iconService.getIcons().size() ) );
    }

    @Test
    void shouldGetIconsFilteredByKeywordWhenRequested()
        throws BadRequestException,
        NotFoundException
    {
        FileResource fileResourceB = createAndPersistFileResource( 'B' );
        iconService.addCustomIcon( new IconData( "iconKeyB", "description", List.of( "k4", "k5", "k6" ), fileResourceB,
            currentUserService.getCurrentUser() ) );
        FileResource fileResourceC = createAndPersistFileResource( 'C' );
        iconService.addCustomIcon( new IconData( "iconKeyC", "description", List.of( "k6", "k7", "k8" ), fileResourceC,
            currentUserService.getCurrentUser() ) );
        FileResource fileResourceD = createAndPersistFileResource( 'D' );
        iconService.addCustomIcon( new IconData( "iconKeyD", "description", List.of( "world care" ), fileResourceD,
            currentUserService.getCurrentUser() ) );

        assertEquals( 1, iconService.getIcons( List.of( "k4", "k5", "k6" ) ).size(),
            "Expected one icon containing the keys k4, k5 and k6, but found "
                + iconService.getIcons( List.of( "k4", "k5", "k6" ) ).size() );
        assertEquals( 1, iconService.getIcons( List.of( "k6", "k7" ) ).size(),
            "Expected one icon containing the keys k6 and k7, but found "
                + iconService.getIcons( List.of( "k6", "k7" ) ).size() );
        assertEquals( 2, iconService.getIcons( List.of( "k6" ) ).size(),
            "Expected two icons containing the key k6, but found "
                + iconService.getIcons( List.of( "k6" ) ).size() );
        assertGreaterOrEqual( 2, iconService.getIcons( List.of( "world care" ) ).size() );
    }

    @Test
    void shouldSaveIconWhenKeyProvidedAndIconDoesNotExist()
        throws BadRequestException,
        NotFoundException
    {
        FileResource fileResource = createAndPersistFileResource( 'A' );

        iconService.addCustomIcon(
            new IconData( "newIconKey", "description", keywords, fileResource, currentUserService.getCurrentUser() ) );

        IconData icon = iconService.getIcon( "newIconKey" );
        assertEquals( "description", icon.getDescription() );
        assertContainsOnly( List.of( "k1", "k2", "k3" ), icon.getKeywords() );
        assertEquals( fileResource.getId(), icon.getFileResource().getId() );
        assertEquals( "Admin User", icon.getCreatedBy().getName() );
    }

    @Test
    void shouldUpdateIconDescriptionWhenKeyProvidedAndIconExists()
        throws BadRequestException,
        NotFoundException
    {
        iconService.updateCustomIconDescription( iconKey, "updatedDescription" );

        IconData updatedIcon = iconService.getIcon( iconKey );
        assertEquals( "updatedDescription", updatedIcon.getDescription() );
    }

    @Test
    void shouldUpdateIconKeywordsWhenKeyProvidedAndIconExists()
        throws BadRequestException,
        NotFoundException
    {
        iconService.updateCustomIconKeywords( iconKey, List.of( "new k1", "new k2" ) );

        IconData updatedIcon = iconService.getIcon( iconKey );
        assertContainsOnly( List.of( "new k1", "new k2" ), updatedIcon.getKeywords() );
    }

    @Test
    void shouldUpdateIconDescriptionAndKeywordsWhenKeyProvidedAndIconExists()
        throws NotFoundException,
        BadRequestException
    {
        iconService.updateCustomIconDescriptionAndKeywords( iconKey, "updatedDescription",
            List.of( "new k1", "new k2" ) );

        IconData updatedIcon = iconService.getIcon( iconKey );
        assertEquals( "updatedDescription", updatedIcon.getDescription() );
        assertContainsOnly( List.of( "new k1", "new k2" ), updatedIcon.getKeywords() );
    }

    @Test
    void shouldDeleteIconDescriptionWhenKeyProvidedAndIconExists()
        throws NotFoundException,
        BadRequestException
    {
        iconService.deleteCustomIcon( iconKey );

        assertFalse( iconService.iconExists( iconKey ) );
    }

    public FileResource createAndPersistFileResource( char uniqueChar )
    {
        byte[] content = "content".getBytes( StandardCharsets.UTF_8 );
        String filename = "filename" + uniqueChar;

        HashCode contentMd5 = Hashing.md5().hashBytes( content );
        String contentType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

        FileResource fileResource = new FileResource( filename, contentType, content.length,
            contentMd5.toString(), FileResourceDomain.DATA_VALUE );
        fileResource.setAssigned( false );
        fileResource.setCreated( new Date() );
        fileResource.setAutoFields();

        String fileResourceUid = fileResourceService.saveFileResource( fileResource, content );
        return fileResourceService.getFileResource( fileResourceUid );
    }

    private Map<String, IconData> getAllStandardIcons()
    {
        return Arrays.stream( Icon.values() )
            .map( Icon::getVariants )
            .flatMap( Collection::stream )
            .collect( Collectors.toMap( IconData::getKey, Function.identity() ) );
    }
}
