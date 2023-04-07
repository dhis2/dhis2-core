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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;

/**
 * @author Kristian Wærstad
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.icon.IconService" )
public class DefaultIconService
    implements IconService
{
    private static final String ICON_PATH = "SVGs";

    private final CustomIconStore customIconStore;

    private final FileResourceService fileResourceService;

    private final Map<String, IconData> standardIcons = Arrays.stream( Icon.values() )
        .map( Icon::getVariants )
        .flatMap( Collection::stream )
        .collect( Collectors.toMap( IconData::getKey, Function.identity() ) );

    public Collection<IconData> getIcons()
    {
        return Stream.concat( standardIcons.values().stream(), customIconStore.getAll().stream() )
            .collect( Collectors.toList() );
    }

    public Collection<IconData> getIcons( Collection<String> keywords )
    {
        return Stream.concat( standardIcons.values().stream()
            .filter( icon -> new HashSet<>( icon.getKeywords() ).containsAll( keywords ) )
            .collect( Collectors.toList() ).stream(), customIconStore.getIconsByKeywords( keywords ).stream() )
            .collect( Collectors.toList() );
    }

    @Override
    public IconData getIcon( String key )
    {
        if ( standardIcons.containsKey( key ) )
        {
            return standardIcons.get( key );
        }
        else
        {
            return customIconStore.getIconByKey( key );
        }
    }

    private IconData getCustomIcon( String key )
    {
        return customIconStore.getIconByKey( key );
    }

    @Override
    public Optional<Resource> getIconResource( String key )
    {
        return Optional.ofNullable( new ClassPathResource( String.format( "%s/%s.%s", ICON_PATH, key, Icon.SUFFIX ) ) );
    }

    @Override
    public Collection<String> getKeywords()
    {
        return Stream.concat( standardIcons.values().stream()
            .map( IconData::getKeywords )
            .flatMap( List::stream ), customIconStore.getKeywords().stream() ).collect( Collectors.toList() );
    }

    @Override
    @Transactional
    public void addIcon( String key, String description, List<String> keywords, FileResource fileResourceId )
        throws BadRequestException
    {
        validateIconProperties( key, fileResourceId );
        customIconStore.save( new IconData( key, description, keywords, fileResourceId ) );
    }

    @Override
    @Transactional
    public void updateIconDescription( String key, String description )
        throws BadRequestException
    {
        IconData icon = validateCustomIconExists( key );
        icon.setDescription( description );

        customIconStore.update( icon );
    }

    @Override
    @Transactional
    public void updateIconKeywords( String key, List<String> keywords )
        throws BadRequestException
    {
        IconData icon = validateCustomIconExists( key );
        icon.setKeywords( keywords );

        customIconStore.update( icon );
    }

    @Override
    @Transactional
    public void updateIconDescriptionAndKeywords( String key, String description, List<String> keywords )
        throws BadRequestException
    {
        IconData icon = validateCustomIconExists( key );
        icon.setDescription( description );
        icon.setKeywords( keywords );

        customIconStore.update( icon );
    }

    @Override
    @Transactional
    public void deleteIcon( String key )
        throws BadRequestException
    {
        IconData icon = validateCustomIconExists( key );

        fileResourceService.getFileResource( icon.getFileResource().getId() ).setAssigned( false );
        customIconStore.delete( icon );
    }

    private void validateIconProperties( String key, FileResource fileResource )
        throws BadRequestException
    {
        validateIconKey( key );

        if ( fileResource == null )
        {
            throw new BadRequestException( "File resource id not specified." );
        }
        if ( getIcon( key ) != null )
        {
            throw new BadRequestException( String.format( "Icon with key %s already exists.", key ) );
        }
        if ( fileResourceService.getFileResource( fileResource.getId() ) == null )
        {
            throw new BadRequestException( String.format( "File resource %s does not exist", fileResource.getId() ) );
        }
    }

    private void validateIconKey( String key )
        throws BadRequestException
    {
        if ( Strings.isNullOrEmpty( key ) )
        {
            throw new BadRequestException( "Icon key not specified." );
        }
    }

    private IconData validateCustomIconExists( String key )
        throws BadRequestException
    {
        validateIconKey( key );

        IconData icon = getCustomIcon( key );

        if ( icon == null )
        {
            throw new BadRequestException( String.format( "Custom icon with key %s does not exists.", key ) );
        }

        return icon;
    }
}