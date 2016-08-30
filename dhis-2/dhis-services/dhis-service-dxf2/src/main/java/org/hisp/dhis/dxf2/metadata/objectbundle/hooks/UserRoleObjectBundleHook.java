package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.UserAuthorityGroup;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class UserRoleObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Override
    @SuppressWarnings( "unchecked" )
    public void postImport( ObjectBundle bundle )
    {
        if ( !bundle.getObjectMap().containsKey( UserAuthorityGroup.class ) ) return;
        List<IdentifiableObject> objects = bundle.getObjectMap().get( UserAuthorityGroup.class );
        Map<String, Map<String, Object>> userRoleReferences = bundle.getObjectReferences( UserAuthorityGroup.class );

        if ( userRoleReferences == null || userRoleReferences.isEmpty() )
        {
            return;
        }

        for ( IdentifiableObject object : objects )
        {
            object = bundle.getPreheat().get( bundle.getPreheatIdentifier(), object );
            Map<String, Object> userRoleReferenceMap = userRoleReferences.get( object.getUid() );

            if ( userRoleReferenceMap == null || userRoleReferenceMap.isEmpty() )
            {
                continue;
            }

            UserAuthorityGroup userRole = (UserAuthorityGroup) object;
            userRole.setDataSets( (Set<DataSet>) userRoleReferenceMap.get( "dataSets" ) );
            userRole.setPrograms( (Set<Program>) userRoleReferenceMap.get( "programs" ) );

            preheatService.connectReferences( userRole, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            sessionFactory.getCurrentSession().update( userRole );
        }
    }
}
