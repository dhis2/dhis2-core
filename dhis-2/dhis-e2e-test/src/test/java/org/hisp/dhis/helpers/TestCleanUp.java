/*
 * Copyright (c) 2004-2019, University of Oslo
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

/*
 * Copyright (c) 2004-2018, University of Oslo
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

package org.hisp.dhis.helpers;

import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TestCleanUp
{
    private Logger logger = Logger.getLogger( TestCleanUp.class.getName() );

    /**
     * Deletes entities created during test run. Entities deleted one by one starting from last created one.
     */
    public void deleteCreatedEntities()
    {
        new LoginActions().loginAsDefaultUser();

        LinkedHashMap createdEntities = (LinkedHashMap) TestRunStorage.getCreatedEntities();
        List<String> reverseOrderedKeys = new ArrayList<>( createdEntities.keySet() );
        Collections.reverse( reverseOrderedKeys );

        Iterator iterator = reverseOrderedKeys.iterator();

        while ( iterator.hasNext() )
        {
            String key = (String) iterator.next();
            boolean deleted = deleteEntity( (String) createdEntities.get( key ), key );
            if ( deleted )
            {
                createdEntities.remove( key );
            }
        }

        TestRunStorage.removeAllEntities();
    }

    /**
     * Deletes entities created during test run.
     *
     * @param resources I.E /organisationUnits to delete created OU's.
     */
    public void deleteCreatedEntities( String... resources )
    {
        new LoginActions().loginAsDefaultUser();

        for ( String resource : resources
        )
        {
            List<String> entityIds = TestRunStorage.getCreatedEntities( resource );

            Iterator iterator = entityIds.iterator();

            while ( iterator.hasNext() )
            {
                boolean deleted = deleteEntity( resource, (String) iterator.next() );
                if ( deleted )
                {
                    iterator.remove();
                }
            }
        }

    }

    private boolean deleteEntity( String resource, String id )
    {
        ApiResponse response = new RestApiActions( resource ).delete( id );

        if ( response.statusCode() == 200 )
        {
            if ( response.containsImportSummaries() )
            {
                return response.extract( "response.importCount.deleted" ).equals( 1 );
            }

            logger.info( String.format( "Entity from resource %s with id %s deleted", resource, id ) );
            return true;
        }

        logger.warning( String.format( "Entity from resource %s with id %s was not deleted", resource, id ) );
        return false;
    }

}
