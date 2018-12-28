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

import io.restassured.response.Response;
import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.actions.LoginActions;

import java.util.Map;
import java.util.logging.Logger;

import static io.restassured.RestAssured.when;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TestCleanUp
{
    private Logger logger = Logger.getLogger( TestCleanUp.class.getName() );

    public void deleteCreatedEntities()
    {
        new LoginActions().loginAsDefaultUser();

        Map<String, String> pairs = TestRunStorage.getCreatedEntities();

        for ( Map.Entry entry : pairs.entrySet() )
        {
            boolean isDeleted = deleteEntity( entry );

            if ( isDeleted )
            {
                TestRunStorage.removeEntity( entry.getValue().toString(), entry.getKey().toString() );
            }
        }

        // Sometimes when object is referenced, it can´t be deleted.
        // This ensures that tests doesn't leave a footprint.
        for ( Map.Entry entry : TestRunStorage.getCreatedEntities().entrySet() )
        {
            deleteEntity( entry );
        }

        TestRunStorage.removeAllEntities();
    }

    private boolean deleteEntity( Map.Entry entry )
    {
        Response response = when()
            .delete( entry.getValue() + "/" + entry.getKey() )
            .thenReturn();

        if ( response.statusCode() == 200 )
        {
            logger.info( "Resource " + entry.getValue() + " with id " + entry.getKey() + " deleted." );
            return true;
        }

        logger.warning( "Resource " + entry.getValue() + " with id " + entry.getKey() + " was not deleted. " );
        return false;
    }

}
