/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.comparator.OrganisationUnitParentCountComparator;
import org.hisp.dhis.system.util.GeoUtils;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class OrganisationUnitObjectBundleHook extends AbstractObjectBundleHook
{
    @Override
    public void preCommit( ObjectBundle objectBundle )
    {
        sortOrganisationUnits( objectBundle );
    }

    private void sortOrganisationUnits( ObjectBundle bundle )
    {
        List<IdentifiableObject> nonPersistedObjects = bundle.getObjects( OrganisationUnit.class, false );
        List<IdentifiableObject> persistedObjects = bundle.getObjects( OrganisationUnit.class, true );

        nonPersistedObjects.sort( new OrganisationUnitParentCountComparator() );
        persistedObjects.sort( new OrganisationUnitParentCountComparator() );
    }

    @Override
    public void postCommit( ObjectBundle bundle )
    {
        if ( !bundle.getObjectMap().containsKey( OrganisationUnit.class ) )
            return;

        List<IdentifiableObject> objects = bundle.getObjectMap().get( OrganisationUnit.class );
        Map<String, Map<String, Object>> objectReferences = bundle.getObjectReferences( OrganisationUnit.class );

        Session session = sessionFactory.getCurrentSession();

        for ( IdentifiableObject identifiableObject : objects )
        {
            identifiableObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), identifiableObject );
            Map<String, Object> objectReferenceMap = objectReferences.get( identifiableObject.getUid() );

            if ( objectReferenceMap == null || objectReferenceMap.isEmpty()
                || !objectReferenceMap.containsKey( "parent" ) )
            {
                continue;
            }

            OrganisationUnit organisationUnit = (OrganisationUnit) identifiableObject;
            OrganisationUnit parentRef = (OrganisationUnit) objectReferenceMap.get( "parent" );
            OrganisationUnit parent = bundle.getPreheat().get( bundle.getPreheatIdentifier(), parentRef );

            organisationUnit.setParent( parent );
            session.update( organisationUnit );
        }
    }

    @Override
    public void preCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        setSRID( object );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        setSRID( object );
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        if ( object == null || !object.getClass().isAssignableFrom( OrganisationUnit.class ) )
        {
            return new ArrayList<>();
        }

        OrganisationUnit organisationUnit = (OrganisationUnit) object;

        List<ErrorReport> errors = new ArrayList<>();

        if ( organisationUnit.getClosedDate() != null
            && organisationUnit.getClosedDate().before( organisationUnit.getOpeningDate() ) )
        {
            errors.add( new ErrorReport( OrganisationUnit.class, ErrorCode.E4013, organisationUnit.getClosedDate(),
                organisationUnit
                    .getOpeningDate() ) );
        }

        return errors;
    }

    private void setSRID( IdentifiableObject object )
    {
        if ( !OrganisationUnit.class.isInstance( object ) )
            return;

        OrganisationUnit organisationUnit = (OrganisationUnit) object;

        if ( organisationUnit.getGeometry() != null )
        {
            organisationUnit.getGeometry().setSRID( GeoUtils.SRID );
        }
    }
}
