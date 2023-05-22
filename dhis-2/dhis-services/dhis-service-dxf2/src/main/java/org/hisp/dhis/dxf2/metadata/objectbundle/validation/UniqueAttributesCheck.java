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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class UniqueAttributesCheck implements ObjectValidationCheck
{
    @Override
    public <T extends IdentifiableObject> void check( ObjectBundle bundle, Class<T> klass,
        List<T> persistedObjects, List<T> nonPersistedObjects,
        ImportStrategy importStrategy, ValidationContext ctx, Consumer<ObjectReport> addReports )
    {
        List<T> objects = selectObjects( persistedObjects, nonPersistedObjects, importStrategy );

        if ( objects.isEmpty()
            || !ctx.getSchemaService().getDynamicSchema( klass ).hasPersistedProperty( "attributeValues" ) )
        {
            return;
        }

        for ( T object : objects )
        {
            List<ErrorReport> errorReports = checkUniqueAttributes( klass, object, bundle.getPreheat(),
                bundle.getPreheatIdentifier() );

            if ( !errorReports.isEmpty() )
            {

                addReports.accept( createObjectReport( errorReports, object, bundle ) );
                ctx.markForRemoval( object );
            }
        }
    }

    private List<ErrorReport> checkUniqueAttributes( Class<? extends IdentifiableObject> klass,
        IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier )
    {
        if ( object == null || preheat.isDefault( object ) || !preheat.getUniqueAttributes().containsKey( klass ) )
        {
            return emptyList();
        }
        if ( preheat.getUniqueAttributes().get( klass ).isEmpty() )
        {
            return emptyList();
        }

        Map<String, Map<String, String>> uniqueAttributeValues = preheat.getUniqueAttributeValues()
            .computeIfAbsent( klass, key -> new HashMap<>() );

        List<ErrorReport> errorReports = new ArrayList<>();
        object.getAttributeValues().forEach( attributeValue -> {
            Attribute attribute = preheat.get( identifier, attributeValue.getAttribute() );

            if ( attribute == null || !attribute.isUnique() || StringUtils.isEmpty( attributeValue.getValue() ) )
            {
                return;
            }

            if ( uniqueAttributeValues.containsKey( attribute.getUid() ) )
            {
                Map<String, String> values = uniqueAttributeValues.get( attribute.getUid() );

                if ( values.containsKey( attributeValue.getValue() )
                    && !values.get( attributeValue.getValue() ).equals( object.getUid() ) )
                {
                    errorReports.add( new ErrorReport( Attribute.class, ErrorCode.E4009,
                        IdentifiableObjectUtils.getDisplayName( attribute ), attributeValue.getValue() )
                        .setMainId( attribute.getUid() ).setErrorProperty( "value" ) );
                }
                else
                {
                    uniqueAttributeValues.get( attribute.getUid() ).put( attributeValue.getValue(), object.getUid() );
                }
            }
            else
            {
                uniqueAttributeValues.put( attribute.getUid(), new HashMap<>() );
                uniqueAttributeValues.get( attribute.getUid() ).put( attributeValue.getValue(), object.getUid() );
            }
        } );

        return errorReports;
    }
}
