package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Luciano Fiandesio
 */
public class ReferencesCheck
    implements
    ValidationCheck
{
    @Override
    public TypeReport check( ObjectBundle bundle, Class<? extends IdentifiableObject> klass,
        List<IdentifiableObject> persistedObjects, List<IdentifiableObject> nonPersistedObjects,
        ImportStrategy importStrategy, ValidationContext ctx )
    {
        TypeReport typeReport = new TypeReport( klass );

        List<IdentifiableObject> objects = ValidationUtils.joinObjects( persistedObjects, nonPersistedObjects );

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        for ( IdentifiableObject object : objects )
        {
            List<PreheatErrorReport> errorReports = checkReferences( object, bundle.getPreheat(),
                bundle.getPreheatIdentifier(), bundle.isSkipSharing(), ctx );

            if ( errorReports.isEmpty() )
            {
                continue;
            }

            if ( object != null )
            {
                ObjectReport objectReport = new ObjectReport( object, bundle );
                objectReport.addErrorReports( errorReports );
                typeReport.addObjectReport( objectReport );
            }
        }

        if ( !typeReport.getErrorReports().isEmpty() && AtomicMode.ALL == bundle.getAtomicMode() )
        {
            typeReport.getStats().incIgnored();
        }

        return typeReport;
    }

    private List<PreheatErrorReport> checkReferences( IdentifiableObject object, Preheat preheat,
        PreheatIdentifier identifier, boolean skipSharing, ValidationContext ctx )
    {
        List<PreheatErrorReport> preheatErrorReports = new ArrayList<>();

        if ( object == null )
        {
            return preheatErrorReports;
        }

        Schema schema = ctx.getSchemaService().getDynamicSchema( object.getClass() );
        schema.getProperties().stream().filter( p -> p.isPersisted() && p.isOwner()
            && (PropertyType.REFERENCE == p.getPropertyType() || PropertyType.REFERENCE == p.getItemPropertyType()) )
            .forEach( p -> {
                if ( skipCheck( p.getKlass() ) || skipCheck( p.getItemKlass() ) )
                {
                    return;
                }

                if ( !p.isCollection() )
                {
                    IdentifiableObject refObject = ReflectionUtils.invokeMethod( object, p.getGetterMethod() );
                    IdentifiableObject ref = preheat.get( identifier, refObject );

                    if ( ref == null && refObject != null && !preheat.isDefault( refObject ) )
                    {
                        // HACK this needs to be redone when the move to using uuid as user identifiers is ready
                        boolean isUserReference = User.class.isAssignableFrom( p.getKlass() ) &&
                            ("user".equals( p.getName() ) || "lastUpdatedBy".equals( p.getName() ));

                        if ( !(isUserReference && skipSharing) )
                        {
                            preheatErrorReports.add( new PreheatErrorReport( identifier, object.getClass(),
                                ErrorCode.E5002, identifier.getIdentifiersWithName( refObject ),
                                identifier.getIdentifiersWithName( object ), p.getName() ) );
                        }
                    }
                }
                else
                {
                    Collection<IdentifiableObject> objects = ReflectionUtils.newCollectionInstance( p.getKlass() );
                    Collection<IdentifiableObject> refObjects = ReflectionUtils.invokeMethod( object,
                        p.getGetterMethod() );

                    for ( IdentifiableObject refObject : refObjects )
                    {
                        if ( preheat.isDefault( refObject ) )
                            continue;

                        IdentifiableObject ref = preheat.get( identifier, refObject );

                        if ( ref == null && refObject != null )
                        {
                            preheatErrorReports.add( new PreheatErrorReport( identifier, object.getClass(),
                                ErrorCode.E5002, identifier.getIdentifiersWithName( refObject ),
                                identifier.getIdentifiersWithName( object ), p.getCollectionName() ) );
                        }
                        else
                        {
                            objects.add( refObject );
                        }
                    }

                    ReflectionUtils.invokeMethod( object, p.getSetterMethod(), objects );
                }
            } );

        if ( schema.havePersistedProperty( "attributeValues" ) )
        {
            object.getAttributeValues().stream()
                .filter( attributeValue -> attributeValue.getAttribute() != null
                    && preheat.get( identifier, attributeValue.getAttribute() ) == null )
                .forEach(
                    attributeValue -> preheatErrorReports.add( new PreheatErrorReport( identifier, object.getClass(),
                        ErrorCode.E5002, identifier.getIdentifiersWithName( attributeValue.getAttribute() ),
                        identifier.getIdentifiersWithName( object ), "attributeValues" ) ) );
        }

        if ( schema.havePersistedProperty( "userGroupAccesses" ) )
        {
            object.getUserGroupAccesses().stream()
                .filter( userGroupAccess -> !skipSharing && userGroupAccess.getUserGroup() != null
                    && preheat.get( identifier, userGroupAccess.getUserGroup() ) == null )
                .forEach(
                    userGroupAccesses -> preheatErrorReports.add( new PreheatErrorReport( identifier, object.getClass(),
                        ErrorCode.E5002, identifier.getIdentifiersWithName( userGroupAccesses.getUserGroup() ),
                        identifier.getIdentifiersWithName( object ), "userGroupAccesses" ) ) );
        }

        if ( schema.havePersistedProperty( "userAccesses" ) )
        {
            object.getUserAccesses().stream()
                .filter( userGroupAccess -> !skipSharing && userGroupAccess.getUser() != null
                    && preheat.get( identifier, userGroupAccess.getUser() ) == null )
                .forEach( userAccesses -> preheatErrorReports.add( new PreheatErrorReport( identifier,
                    object.getClass(), ErrorCode.E5002, identifier.getIdentifiersWithName( userAccesses.getUser() ),
                    identifier.getIdentifiersWithName( object ), "userAccesses" ) ) );
        }


        return preheatErrorReports;
    }

    private boolean skipCheck( Class<?> klass )
    {
        return klass != null
            && (UserCredentials.class.isAssignableFrom( klass ) || EmbeddedObject.class.isAssignableFrom( klass )
            || Period.class.isAssignableFrom( klass ) || PeriodType.class.isAssignableFrom( klass ));
    }
}
