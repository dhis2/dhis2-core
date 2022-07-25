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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationUtils.createObjectReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class UniquenessCheck implements ObjectValidationCheck
{
    @Override
    public <T extends IdentifiableObject> void check( ObjectBundle bundle, Class<T> klass,
        List<T> persistedObjects, List<T> nonPersistedObjects,
        ImportStrategy importStrategy, ValidationContext ctx, Consumer<ObjectReport> addReports )
    {
        List<T> objects = selectObjects( persistedObjects, nonPersistedObjects, importStrategy );

        if ( objects.isEmpty() )
        {
            return;
        }

        for ( T object : objects )
        {
            List<ErrorReport> errorReports;

            errorReports = checkUniqueness( object, bundle.getPreheat(), bundle.getPreheatIdentifier(), ctx );

            if ( !errorReports.isEmpty() )
            {
                addReports.accept( createObjectReport( errorReports, object, bundle ) );
                ctx.markForRemoval( object );
            }
        }
    }

    private List<ErrorReport> checkUniqueness( IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier,
        ValidationContext ctx )
    {
        if ( object == null || preheat.isDefault( object ) )
        {
            return emptyList();
        }

        @SuppressWarnings( "unchecked" )
        Class<? extends IdentifiableObject> objType = HibernateProxyUtils.getRealClass( object );
        Map<String, Map<Object, String>> uniquenessMap = preheat.getUniquenessMap()
            .computeIfAbsent( objType, key -> new HashMap<>() );

        Schema schema = ctx.getSchemaService().getDynamicSchema( objType );
        List<Property> uniqueProperties = schema.getProperties().stream()
            .filter( p -> p.isPersisted() && p.isOwner() && p.isUnique() && p.isSimple() )
            .collect( Collectors.toList() );

        if ( uniqueProperties.isEmpty() )
        {
            return emptyList();
        }
        return checkUniqueness( object, identifier, uniquenessMap, uniqueProperties );
    }

    private List<ErrorReport> checkUniqueness( IdentifiableObject object, PreheatIdentifier identifier,
        Map<String, Map<Object, String>> uniquenessMap, List<Property> uniqueProperties )
    {
        List<ErrorReport> errorReports = new ArrayList<>();
        uniqueProperties.forEach( property -> {
            Object value = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

            if ( value != null )
            {
                String objectIdentifier = uniquenessMap.computeIfAbsent( property.getName(), key -> new HashMap<>() )
                    .get( value );

                if ( objectIdentifier != null )
                {
                    if ( !identifier.getIdentifier( object ).equals( objectIdentifier ) )
                    {
                        String identifiersWithName = identifier.getIdentifiersWithName( object );

                        ErrorReport errorReport = new ErrorReport( HibernateProxyUtils.getRealClass( object ),
                            ErrorCode.E5003,
                            property.getName(),
                            value,
                            identifiersWithName,
                            objectIdentifier );

                        errorReports
                            .add( errorReport.setMainId( objectIdentifier ).setErrorProperty( property.getName() ) );
                    }
                }
                else
                {
                    uniquenessMap.get( property.getName() ).put( value, identifier.getIdentifier( object ) );
                }
            }
        } );

        return errorReports;
    }

}
