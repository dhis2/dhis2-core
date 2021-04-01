/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.gist;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.gist.GistLogic.effectiveLinkage;
import static org.hisp.dhis.gist.GistLogic.isDefaultField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.gist.GistQuery.Field;
import org.hisp.dhis.schema.GistLinkage;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.RelativePropertyContext;

/**
 * The {@link GistPlanner} is responsible to expand the list of {@link Field}s
 * following the {@link GistQuery} and {@link Property} preferences.
 *
 * @author Jan Bernitt
 *
 * @param <T> Type of {@link GistQuery} result list item object
 */
@AllArgsConstructor
class GistPlanner<T extends IdentifiableObject>
{
    private final GistQuery<T> query;

    private final RelativePropertyContext context;

    public GistQuery<T> plan()
    {
        List<Field> fields = query.getFields();
        if ( fields.isEmpty() )
        {
            fields = singletonList( new Field( "*", GistLinkage.AUTO ) );
        }
        fields = withDefaultFields( fields );
        fields = withFlatEmbeddedFields( fields );
        fields = withEffectiveLinkage( fields );
        return query.withFields( fields );
    }

    private static int propertyTypeOrder( Property a, Property b )
    {
        return a.getPropertyType().compareTo( b.getPropertyType() );
    }

    private List<Field> withEffectiveLinkage( List<Field> fields )
    {
        return fields.stream().map( this::withEffectiveLinkage ).collect( toList() );
    }

    private Field withEffectiveLinkage( Field field )
    {
        return field.with( effectiveLinkage( context.resolveMandatory( field.getPropertyPath() ),
            query.getDefaultLinkage(), field.getLinkage() ) );
    }

    private List<Field> withDefaultFields( List<Field> fields )
    {
        List<Field> expanded = new ArrayList<>();
        for ( Field f : fields )
        {
            String path = f.getPropertyPath();
            if ( "*".equals( path ) )
            {
                context.getHome().getProperties().stream()
                    .filter( p -> isDefaultField( p, query.getVerbosity() ) )
                    .sorted( GistPlanner::propertyTypeOrder )
                    .forEach( p -> expanded.add( new Field( p.key(), GistLinkage.AUTO ) ) );
            }
            else if ( path.startsWith( "-" ) )
            {
                expanded.removeIf( field -> field.getPropertyPath().equals( path.substring( 1 ) ) );
            }
            else
            {
                expanded.add( f );
            }
        }
        return expanded;
    }

    private List<Field> withFlatEmbeddedFields( List<Field> fields )
    {
        // OBS! We use a map to not get duplicate fields, last wins
        Map<String, Field> fieldsByPath = new LinkedHashMap<>();
        Consumer<Field> add = field -> fieldsByPath.put( field.getPropertyPath(), field );
        for ( Field f : fields )
        {
            String path = f.getPropertyPath();
            Property field = context.resolveMandatory( path );
            if ( field.isEmbeddedObject() && !field.isCollection() && field.isOneToMany() )
            {
                Class<?> embeddedType = field.getKlass();
                RelativePropertyContext embeddedContext = context.switchedTo( embeddedType );
                List<String> annotatedDefaultFields = field.getGistPreferences().getFields();
                if ( !annotatedDefaultFields.isEmpty() )
                {
                    annotatedDefaultFields.stream()
                        .map( embeddedContext::resolveMandatory )
                        .forEach( p -> add.accept( createNestedField( path, p ) ) );
                }
                else
                {
                    embeddedContext.getHome().getProperties().stream()
                        .filter( p -> isDefaultField( p, GistVerbosity.CONCISE ) )
                        .sorted( GistPlanner::propertyTypeOrder )
                        .forEach( p -> add.accept( createNestedField( path, p ) ) );
                }
            }
            else
            {
                add.accept( f );
            }
        }
        return new ArrayList<>( fieldsByPath.values() );
    }

    private Field createNestedField( String parentPath, Property field )
    {
        return new Field( parentPath + "." + field.key(), GistVerbosity.CONCISE.getAutoLinkage() );
    }
}
