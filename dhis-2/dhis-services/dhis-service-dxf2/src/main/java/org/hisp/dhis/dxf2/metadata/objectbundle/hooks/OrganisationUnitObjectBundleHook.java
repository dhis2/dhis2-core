package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hibernate.Session;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.comparator.OrganisationUnitParentCountComparator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class OrganisationUnitObjectBundleHook extends AbstractObjectBundleHook
{
    @Override
    public void preImport( ObjectBundle objectBundle )
    {
        sortOrganisationUnits( objectBundle );
    }

    private void sortOrganisationUnits( ObjectBundle bundle )
    {
        List<IdentifiableObject> nonPersistedObjects = bundle.getObjects( OrganisationUnit.class, false );
        List<IdentifiableObject> persistedObjects = bundle.getObjects( OrganisationUnit.class, true );

        Collections.sort( nonPersistedObjects, new OrganisationUnitParentCountComparator() );
        Collections.sort( persistedObjects, new OrganisationUnitParentCountComparator() );
    }

    @Override
    public void postImport( ObjectBundle bundle )
    {
        if ( !bundle.getObjectMap().containsKey( OrganisationUnit.class ) ) return;

        List<IdentifiableObject> objects = bundle.getObjectMap().get( OrganisationUnit.class );
        Map<String, Map<String, Object>> objectReferences = bundle.getObjectReferences( OrganisationUnit.class );

        Session session = sessionFactory.getCurrentSession();

        for ( IdentifiableObject identifiableObject : objects )
        {
            identifiableObject = bundle.getPreheat().get( bundle.getPreheatIdentifier(), identifiableObject );
            Map<String, Object> objectReferenceMap = objectReferences.get( identifiableObject.getUid() );

            if ( objectReferenceMap == null || objectReferenceMap.isEmpty() || !objectReferenceMap.containsKey( "parent" ) )
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
}
