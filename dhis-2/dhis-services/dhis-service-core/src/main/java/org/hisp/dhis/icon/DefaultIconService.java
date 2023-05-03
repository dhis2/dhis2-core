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

import static org.hisp.dhis.schema.descriptors.FileResourceSchemaDescriptor.API_ENDPOINT;

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
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.schema.descriptors.IconSchemaDescriptor;
import org.hisp.dhis.user.CurrentUserService;
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

    private final CurrentUserService currentUserService;

    private final Map<String, StandardIcon> standardIcons = Arrays.stream( Icon.values() )
        .map( Icon::getVariants )
        .flatMap( Collection::stream )
        .collect( Collectors.toMap( StandardIcon::getKey, Function.identity() ) );

    @Override
    public Collection<BaseIcon> getIcons( String contextApiPath )
    {
        List<StandardIcon> standardIconList = updateStandardIconReference( standardIcons, contextApiPath );
        List<CustomIcon> customIconList = updateCustomIconReference( customIconStore.getAllIcons().stream()
            .map( ci -> CustomIcon.of( ci, currentUserService.getCurrentUser(), contextApiPath, API_ENDPOINT ) )
            .toList(), contextApiPath );

        return Stream.concat( standardIconList.stream(), customIconList.stream() ).toList();
    }

    @Override
    public Collection<BaseIcon> getIcons( Collection<String> keywords, String contextApiPath )
    {
        List<StandardIcon> standardIconList = updateStandardIconReference( standardIcons, contextApiPath );
        List<CustomIcon> customIconList = updateCustomIconReference(
            customIconStore.getIconsByKeywords( keywords ).stream()
                .map( ci -> CustomIcon.of( ci, currentUserService.getCurrentUser(), contextApiPath, API_ENDPOINT ) )
                .toList(),
            contextApiPath );

        return Stream.concat( standardIconList.stream()
            .filter( icon -> new HashSet<>( icon.getKeywords() ).containsAll( keywords ) ).toList().stream(),
            customIconList.stream() )
            .toList();
    }

    @Override
    public BaseIcon getIcon( String key, String contextApiPath )
        throws NotFoundException
    {
        if ( standardIcons.containsKey( key ) )
        {
            StandardIcon standardIcon = standardIcons.get( key );
            standardIcon.setReference( contextApiPath, IconSchemaDescriptor.API_ENDPOINT, standardIcon.getKey() );

            return standardIcon;
        }
        else
        {
            CustomIcon customIcon = customIconStore.getIconByKey( key );
            if ( customIcon == null )
            {
                throw new NotFoundException( String.format( "Icon not found: %s", key ) );
            }

            return CustomIcon.of( customIcon, currentUserService.getCurrentUser(), contextApiPath, API_ENDPOINT );
        }
    }

    @Override
    public CustomIcon getCustomIcon( String key )
        throws NotFoundException
    {
        CustomIcon customIcon = customIconStore.getIconByKey( key );
        if ( customIcon == null )
        {
            throw new NotFoundException( String.format( "Custom icon not found: %s", key ) );
        }

        return customIcon;
    }

    @Override
    public Optional<Resource> getIconResource( String key )
        throws BadRequestException
    {
        if ( standardIcons.containsKey( key ) )
        {
            return Optional.of( new ClassPathResource( String.format( "%s/%s.%s", ICON_PATH, key, Icon.SUFFIX ) ) );
        }

        throw new BadRequestException( String.format( "Icon with key %s is not a standard icon.", key ) );
    }

    @Override
    public List<String> getKeywords()
    {
        return Stream.concat( standardIcons.values().stream()
            .map( BaseIcon::getKeywords )
            .flatMap( List::stream ), customIconStore.getKeywords().stream() ).toList();
    }

    @Override
    public boolean iconExists( String key )
    {
        return standardIcons.get( key ) != null || customIconStore.getIconByKey( key ) != null;
    }

    @Override
    @Transactional
    public void addCustomIcon( CustomIcon customIcon )
        throws BadRequestException,
        NotFoundException
    {
        validateIconExists( customIcon.getKey() );
        validateFileResourceExists( customIcon.getFileResource() );
        customIconStore.save( customIcon );
    }

    @Override
    @Transactional
    public void updateCustomIcon( String key, String description, List<String> keywords )
        throws BadRequestException,
        NotFoundException
    {
        CustomIcon icon = validateCustomIconExists( key );

        if ( description != null )
        {
            icon.setDescription( description );
        }

        if ( keywords != null )
        {
            icon.setKeywords( keywords );
        }

        customIconStore.update( icon );
    }

    @Override
    @Transactional
    public void deleteCustomIcon( String key )
        throws BadRequestException,
        NotFoundException
    {
        CustomIcon icon = validateCustomIconExists( key );
        fileResourceService.getFileResource( icon.getFileResource().getUid() ).setAssigned( false );
        customIconStore.delete( icon );
    }

    private void validateIconExists( String key )
        throws BadRequestException
    {
        validateIconKeyNotNullOrEmpty( key );

        if ( iconExists( key ) )
        {
            throw new BadRequestException( String.format( "Icon with key %s already exists.", key ) );
        }
    }

    private void validateIconKeyNotNullOrEmpty( String key )
        throws BadRequestException
    {
        if ( Strings.isNullOrEmpty( key ) )
        {
            throw new BadRequestException( "Icon key not specified." );
        }
    }

    private void validateFileResourceExists( FileResource fileResource )
        throws BadRequestException,
        NotFoundException
    {
        if ( fileResource == null || Strings.isNullOrEmpty( fileResource.getUid() ) )
        {
            throw new BadRequestException( "File resource id not specified." );
        }

        if ( fileResourceService.getFileResource( fileResource.getUid() ) == null )
        {
            throw new NotFoundException( String.format( "File resource %s does not exist", fileResource.getUid() ) );
        }
    }

    private CustomIcon validateCustomIconExists( String key )
        throws NotFoundException,
        BadRequestException
    {
        validateIconKeyNotNullOrEmpty( key );
        return getCustomIcon( key );
    }

    private List<StandardIcon> updateStandardIconReference( Map<String, StandardIcon> icons, String contextApiPath )
    {
        return icons.values().stream().map( icon -> {
            icon.setReference( contextApiPath, IconSchemaDescriptor.API_ENDPOINT, icon.getKey() );
            return icon;
        } ).toList();
    }

    private List<CustomIcon> updateCustomIconReference( List<CustomIcon> icons, String contextApiPath )
    {
        return icons.stream().map( icon -> {
            icon.setReference( contextApiPath, API_ENDPOINT, icon.getFileResourceUid() );
            return icon;
        } ).toList();
    }
}