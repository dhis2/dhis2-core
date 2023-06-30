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
import static org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationUtils.joinObjects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class ReferencesCheck implements ValidationCheck
{
    @Override
    public <T extends IdentifiableObject> TypeReport check( ObjectBundle bundle, Class<T> klass,
        List<T> persistedObjects, List<T> nonPersistedObjects,
        ImportStrategy importStrategy, ValidationContext ctx )
    {
        if ( persistedObjects.isEmpty() && nonPersistedObjects.isEmpty() )
        {
            return TypeReport.empty( klass );
        }

        TypeReport typeReport = new TypeReport( klass );

        for ( IdentifiableObject object : joinObjects( persistedObjects, nonPersistedObjects ) )
        {
            List<PreheatErrorReport> errorReports = checkReferences( bundle, object, ctx );

            if ( !errorReports.isEmpty() && object != null )
            {
                ObjectReport objectReport = new ObjectReport( object, bundle );
                objectReport.setDisplayName( object.getDisplayName() );
                objectReport.addErrorReports( errorReports );
                typeReport.addObjectReport( objectReport );
            }
        }

        if ( typeReport.hasErrorReports() && AtomicMode.ALL == bundle.getAtomicMode() )
        {
            typeReport.getStats().incIgnored();
        }

        return typeReport;
    }

    private List<PreheatErrorReport> checkReferences( ObjectBundle bundle, IdentifiableObject object,
        ValidationContext ctx )
    {
        if ( object == null )
        {
            return emptyList();
        }

        List<PreheatErrorReport> preheatErrorReports = new ArrayList<>();

        Schema schema = ctx.getSchemaService().getDynamicSchema( HibernateProxyUtils.getRealClass( object ) );

        schema.getProperties().stream().filter( p -> p.isPersisted() && p.isOwner()
            && (PropertyType.REFERENCE == p.getPropertyType() || PropertyType.REFERENCE == p.getItemPropertyType()) )
            .forEach( p -> {
                if ( skipCheck( p.getKlass() ) || skipCheck( p.getItemKlass() ) )
                {
                    return;
                }

                if ( p.isEmbeddedObject() )
                {
                    checkEmbeddedObjects( p, object, ctx, bundle, preheatErrorReports );
                    return;
                }

                if ( !p.isCollection() )
                {
                    checkReference( ctx, bundle, object, preheatErrorReports, p );
                }
                else
                {
                    checkCollection( ctx, bundle, object, preheatErrorReports, p );
                }
            } );

        if ( schema.hasPersistedProperty( "attributeValues" ) )
        {
            checkAttributeValues( object, bundle.getPreheat(), bundle.getPreheatIdentifier(), preheatErrorReports );
        }

        if ( schema.hasPersistedProperty( "sharing" ) && !bundle.isSkipSharing() && object.getSharing() != null )
        {
            checkSharing( object, bundle.getPreheat(), preheatErrorReports );
        }

        return preheatErrorReports;
    }

    private void checkEmbeddedObject( ValidationContext ctx, ObjectBundle bundle,
        List<PreheatErrorReport> preheatErrorReports, Property p,
        EmbeddedObject embeddedObject )
    {
        Schema embeddedSchema = ctx.getSchemaService()
            .getDynamicSchema( p.isCollection() ? p.getItemKlass() : p.getKlass() );
        embeddedSchema.getProperties().stream().filter( ep -> ep.isPersisted() && ep.isOwner()
            && (PropertyType.REFERENCE == ep.getPropertyType() || PropertyType.REFERENCE == ep.getItemPropertyType()) )
            .forEach( property -> {
                if ( skipCheck( property.getKlass() ) || skipCheck( property.getItemKlass() ) )
                {
                    return;
                }

                if ( !property.isCollection() )
                {
                    checkReference( ctx, bundle, embeddedObject, preheatErrorReports, property );
                }
                else
                {
                    checkCollection( ctx, bundle, embeddedObject, preheatErrorReports, property );
                }
            } );
    }

    private void checkReference( ValidationContext ctx, ObjectBundle bundle, Object object,
        List<PreheatErrorReport> preheatErrorReports, Property property )
    {
        IdentifiableObject refObject = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );
        IdentifiableObject ref = bundle.getPreheat().get( bundle.getPreheatIdentifier(), refObject );

        // HACK this needs to be redone when the move to using
        // uuid as user identifiers is ready
        boolean isUserReference = User.class.isAssignableFrom( property.getKlass() ) &&
            ("user".equals( property.getName() ) || "lastUpdatedBy".equals( property.getName() )
                || "createdBy".equals( property.getName() ));

        if ( ref == null && refObject != null && !bundle.getPreheat().isDefault( refObject )
            && !(isUserReference && bundle.isSkipSharing()) )
        {
            preheatErrorReports
                .add( createError( bundle.getPreheatIdentifier(), ErrorCode.E5002, object, refObject, property ) );
        }

        if ( ref != null && ctx.getAclService().isShareable( ref )
            && !ctx.getAclService().canRead( bundle.getUser(), ref ) )
        {
            preheatErrorReports
                .add( createError( bundle.getPreheatIdentifier(), ErrorCode.E5008, object, refObject, property ) );
        }

    }

    private void checkCollection( ValidationContext ctx, ObjectBundle bundle, Object object,
        List<PreheatErrorReport> preheatErrorReports, Property property )
    {
        Collection<IdentifiableObject> objects = ReflectionUtils.newCollectionInstance( property.getKlass() );
        Collection<IdentifiableObject> refObjects = ReflectionUtils.invokeMethod( object,
            property.getGetterMethod() );
        if ( CollectionUtils.isEmpty( refObjects ) )
        {
            return;
        }

        boolean isShareable = ctx.getAclService().isShareable( refObjects.iterator().next() );

        for ( IdentifiableObject refObject : refObjects )
        {
            if ( bundle.getPreheat().isDefault( refObject ) )
                continue;

            IdentifiableObject ref = bundle.getPreheat().get( bundle.getPreheatIdentifier(), refObject );

            if ( ref == null && refObject != null )
            {
                preheatErrorReports.add(
                    createError( bundle.getPreheatIdentifier(), ErrorCode.E5002, object, refObject, property ) );
            }
            else if ( refObject != null && isShareable && !ctx.getAclService().canRead( bundle.getUser(), ref ) )
            {
                preheatErrorReports.add(
                    createError( bundle.getPreheatIdentifier(), ErrorCode.E5008, object, refObject, property ) );
            }
            else
            {
                objects.add( refObject );
            }
        }

        CollectionUtils.findDuplicates( refObjects )
            .forEach( refObject -> preheatErrorReports
                .add(
                    createError( bundle.getPreheatIdentifier(), ErrorCode.E5007, object, refObject, property ) ) );

        ReflectionUtils.invokeMethod( object, property.getSetterMethod(), objects );
    }

    private void checkAttributeValues( IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier,
        List<PreheatErrorReport> preheatErrorReports )
    {
        object.getAttributeValues().stream()
            .filter( attributeValue -> attributeValue.getAttribute() != null
                && preheat.get( identifier, attributeValue.getAttribute() ) == null )
            .forEach(
                attributeValue -> preheatErrorReports.add( new PreheatErrorReport( identifier, object.getClass(),
                    ErrorCode.E5002, identifier.getIdentifiersWithName( attributeValue.getAttribute() ),
                    identifier.getIdentifiersWithName( object ), "attributeValues" ) ) );
    }

    private void checkSharing( IdentifiableObject object, Preheat preheat,
        List<PreheatErrorReport> preheatErrorReports )
    {
        Sharing sharing = object.getSharing();
        if ( sharing.hasUserGroupAccesses() )
        {
            sharing.getUserGroups().values().stream()
                .filter( userGroupAccess -> preheat.get( PreheatIdentifier.UID,
                    userGroupAccess.getUserGroup() ) == null )
                .forEach(
                    userGroupAccess -> preheatErrorReports
                        .add( new PreheatErrorReport( PreheatIdentifier.UID, object.getClass(),
                            ErrorCode.E5002,
                            PreheatIdentifier.UID
                                .getIdentifiersWithName( userGroupAccess.getUserGroup() ),
                            PreheatIdentifier.UID.getIdentifiersWithName( object ), "userGroupAccesses" ) ) );
        }

        if ( sharing.hasUserAccesses() )
        {
            sharing.getUsers().values().stream()
                .filter( userAccess -> preheat.get( PreheatIdentifier.UID,
                    userAccess.getUser() ) == null )
                .forEach(
                    userAccesses -> preheatErrorReports.add( new PreheatErrorReport( PreheatIdentifier.UID,
                        object.getClass(), ErrorCode.E5002,
                        PreheatIdentifier.UID.getIdentifiersWithName( userAccesses.getUser() ),
                        PreheatIdentifier.UID.getIdentifiersWithName( object ), "userAccesses" ) ) );
        }
    }

    private boolean skipCheck( Class<?> klass )
    {
        return klass != null
            && (Period.class.isAssignableFrom( klass ) || PeriodType.class.isAssignableFrom( klass ));
    }

    private PreheatErrorReport createError( PreheatIdentifier identifier, ErrorCode errorCode, Object object,
        IdentifiableObject refObject,
        Property property, String... args )
    {
        if ( object instanceof IdentifiableObject identifiableObject )
        {
            return new PreheatErrorReport( identifier,
                errorCode, identifiableObject, property, identifier.getIdentifiersWithName( refObject ),
                identifier.getIdentifiersWithName( identifiableObject ), property.getName() );
        }

        return new PreheatErrorReport( identifier, object.getClass(), errorCode, property, args );
    }

    private void checkEmbeddedObjects( Property property, Object object, ValidationContext ctx, ObjectBundle bundle,
        List<PreheatErrorReport> preheatErrorReports )
    {
        if ( property.isCollection() )
        {
            Collection<EmbeddedObject> collection = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );
            if ( collection != null )
            {
                collection
                    .forEach( embeddedObject -> checkEmbeddedObject( ctx, bundle,
                        preheatErrorReports, property, embeddedObject ) );
            }
        }
        else
        {
            EmbeddedObject embeddedObject = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );
            if ( embeddedObject != null )
            {
                checkEmbeddedObject( ctx, bundle,
                    preheatErrorReports, property, embeddedObject );
            }
        }
    }
}
