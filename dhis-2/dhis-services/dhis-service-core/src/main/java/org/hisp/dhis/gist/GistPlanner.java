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

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.gist.GistLogic.effectiveTransform;
import static org.hisp.dhis.gist.GistLogic.isCollectionSizeFilter;
import static org.hisp.dhis.gist.GistLogic.isIncludedField;
import static org.hisp.dhis.gist.GistLogic.isNonNestedPath;
import static org.hisp.dhis.gist.GistLogic.isPersistentCollectionField;
import static org.hisp.dhis.gist.GistLogic.isPersistentReferenceField;
import static org.hisp.dhis.gist.GistLogic.parentPath;
import static org.hisp.dhis.gist.GistLogic.pathOnSameParent;
import static org.hisp.dhis.schema.PropertyType.COLLECTION;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.gist.GistQuery.Field;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.annotation.Gist.Transform;

/**
 * The {@link GistPlanner} is responsible to expand the list of {@link Field}s
 * following the {@link GistQuery} and {@link Property} preferences.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
class GistPlanner
{
    private final GistQuery query;

    private final RelativePropertyContext context;

    public GistQuery plan()
    {
        return query.withFields( planFields() ).withFilters( planFilters() );
    }

    private List<Field> planFields()
    {
        List<Field> fields = query.getFields();
        if ( fields.isEmpty() )
        {
            fields = singletonList( Field.ALL );
        }
        fields = withDefaultFields( fields ); // 1:n
        fields = withDisplayAsTranslatedFields( fields ); // 1:1
        fields = withInnerAsSeparateFields( fields ); // 1:n
        fields = withCollectionItemPropertyAsTransformation( fields ); // 1:1
        fields = withEffectiveTransformation( fields ); // 1:1
        fields = withEndpointsField( fields ); // 1:1+1
        return fields;
    }

    private List<Filter> planFilters()
    {
        List<Filter> filters = new ArrayList<>();
        for ( Filter f : query.getFilters() )
        {
            Property p = context.resolveMandatory( f.getPropertyPath() );
            if ( p.isCollection() && !isCollectionSizeFilter( f, p )
                && IdentifiableObject.class.isAssignableFrom( p.getItemKlass() ) )
            {
                filters.add( f.withPropertyPath( f.getPropertyPath() + ".id" ) );
            }
            else
            {
                filters.add( f );
            }
        }
        return filters;
    }

    private static int propertyTypeOrder( Property a, Property b )
    {
        if ( a.isCollection() && b.isCollection() )
        {
            return modifiedPropertyTypeOrder( a.getItemPropertyType(), b.getItemPropertyType() );
        }
        return modifiedPropertyTypeOrder( a.getPropertyType(), b.getPropertyType() );
    }

    private static int modifiedPropertyTypeOrder( PropertyType a, PropertyType b )
    {
        if ( a == COLLECTION && b != COLLECTION )
        {
            return 1;
        }
        if ( b == COLLECTION && a != COLLECTION )
        {
            return -1;
        }
        return a.compareTo( b );
    }

    private List<Field> withEffectiveTransformation( List<Field> fields )
    {
        return fields.stream().map( this::withEffectiveTransformation ).collect( toList() );
    }

    private Field withEffectiveTransformation( Field field )
    {
        return field.withTransformation( effectiveTransform( context.resolveMandatory( field.getPropertyPath() ),
            query.getDefaultTransformation(), field.getTransformation() ) );
    }

    private List<Field> withDefaultFields( List<Field> fields )
    {
        Set<String> explicit = fields.stream().map( Field::getPropertyPath ).collect( toSet() );
        List<Field> expanded = new ArrayList<>();
        for ( Field f : fields )
        {
            String path = f.getPropertyPath();
            if ( isPresetField( path ) )
            {
                context.getHome().getProperties().stream()
                    .filter( getPresetFilter( path ) )
                    .sorted( GistPlanner::propertyTypeOrder )
                    .forEach( p -> {
                        if ( !explicit.contains( p.key() ) && !explicit.contains( "-" + p.key() )
                            && !explicit.contains( "!" + p.key() ) )
                        {
                            expanded.add( new Field( p.key(), Transform.AUTO ) );
                        }
                    } );
            }
            else if ( isExcludeField( path ) )
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

    private List<Field> withDisplayAsTranslatedFields( List<Field> fields )
    {
        List<Field> mapped = new ArrayList<>();
        for ( Field f : fields )
        {
            String path = f.getPropertyPath();
            if ( path.equals( "displayName" ) || path.endsWith( ".displayName" ) )
            {
                mapped.add( f.withAlias( f.getName() ).withTranslate()
                    .withPropertyPath( pathOnSameParent( f.getPropertyPath(), "name" ) ) );
            }
            else if ( path.equals( "displayShortName" ) || path.endsWith( ".displayShortName" ) )
            {
                mapped.add( f.withAlias( f.getName() ).withTranslate()
                    .withPropertyPath( pathOnSameParent( f.getPropertyPath(), "shortName" ) ) );
            }
            else
            {
                mapped.add( f );
            }
        }
        return mapped;
    }

    /**
     * Transforms {@code field[a,b]} syntax to {@code field.a,field.b}
     */
    private List<Field> withInnerAsSeparateFields( List<Field> fields )
    {
        List<Field> expanded = new ArrayList<>();
        for ( Field f : fields )
        {
            String path = f.getPropertyPath();
            if ( path.indexOf( '[' ) >= 0 )
            {
                String outerPath = path.substring( 0, path.indexOf( '[' ) );
                String innerList = path.substring( path.indexOf( '[' ) + 1, path.lastIndexOf( ']' ) );
                for ( String innerFieldName : innerList.split( "," ) )
                {
                    Field child = Field.parse( innerFieldName );
                    expanded.add( child
                        .withPropertyPath( outerPath + "." + child.getPropertyPath() )
                        .withAlias(
                            (f.getAlias().isEmpty() ? outerPath : f.getAlias()) + "." + child.getName() ) );
                }
            }
            else
            {
                expanded.add( f );
            }
        }
        return expanded;
    }

    private List<Field> withCollectionItemPropertyAsTransformation( List<Field> fields )
    {
        List<Field> mapped = new ArrayList<>();
        for ( Field f : fields )
        {
            String path = f.getPropertyPath();
            if ( !isNonNestedPath( path ) && context.resolveMandatory( parentPath( path ) ).isCollection() )
            {
                String parentPath = parentPath( path );
                String propertyName = path.substring( path.lastIndexOf( '.' ) + 1 );
                Property collection = context.resolveMandatory( parentPath );
                if ( "id".equals( propertyName )
                    && IdentifiableObject.class.isAssignableFrom( collection.getItemKlass() ) )
                {
                    mapped.add( f
                        .withPropertyPath( parentPath )
                        .withAlias( path )
                        .withTransformation( Transform.IDS ) );
                }
                else
                {
                    mapped.add( Field.builder()
                        .propertyPath( parentPath )
                        .alias( path )
                        .transformation( Transform.PLUCK )
                        .transformationArgument( propertyName )
                        .build() );
                }
            }
            else
            {
                mapped.add( f );
            }
        }
        return mapped;
    }

    private Predicate<Property> getPresetFilter( String path )
    {
        if ( isAllField( path ) )
        {
            return p -> isIncludedField( p, query.getAutoType() );
        }
        if ( ":identifiable".equals( path ) )
        {
            return getPresetFilter( IdentifiableObject.class );
        }
        if ( ":nameable".equals( path ) )
        {
            return getPresetFilter( NameableObject.class );
        }
        if ( ":persisted".equals( path ) )
        {
            return Property::isPersisted;
        }
        if ( ":owner".equals( path ) )
        {
            return p -> p.isPersisted() && p.isOwner();
        }
        throw new UnsupportedOperationException();
    }

    private Predicate<Property> getPresetFilter( Class<?> api )
    {
        return p -> stream( api.getMethods() )
            .anyMatch( m -> m.getName().equals( p.getGetterMethod().getName() )
                && m.getParameterCount() == 0
                && p.isPersisted() );
    }

    private static boolean isPresetField( String path )
    {
        return path.startsWith( ":" ) || Field.ALL_PATH.equals( path );
    }

    private static boolean isExcludeField( String path )
    {
        return path.startsWith( "-" ) || path.startsWith( "!" );
    }

    private static boolean isAllField( String path )
    {
        return Field.ALL_PATH.equals( path ) || ":*".equals( path ) || ":all".equals( path );
    }

    private List<Field> withEndpointsField( List<Field> fields )
    {
        boolean hasReferences = fields.stream().anyMatch( field -> {
            Property p = context.resolveMandatory( field.getPropertyPath() );
            return isPersistentReferenceField( p ) && p.isIdentifiableObject()
                || isPersistentCollectionField( p );
        } );
        if ( !hasReferences )
        {
            return fields;
        }
        ArrayList<Field> extended = new ArrayList<>( fields );
        extended.add( new Field( Field.REFS_PATH, Transform.NONE, "apiEndpoints", null, false ) );
        return extended;
    }
}
