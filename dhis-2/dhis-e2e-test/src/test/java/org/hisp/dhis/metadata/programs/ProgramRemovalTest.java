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

package org.hisp.dhis.metadata.programs;

import org.hisp.dhis.ConcurrentApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.metadata.RelationshipTypeActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ProgramRemovalTest
    extends ConcurrentApiTest
{
    private ProgramActions programActions;

    private RelationshipTypeActions relationshipTypeActions;

    private String programId;

    private String relationshipTypeId;

    @BeforeEach
    public void beforeEach()
        throws Exception
    {
        programActions = new ProgramActions();
        relationshipTypeActions = new RelationshipTypeActions();

        new LoginActions().loginAsSuperUser();
        setupData();
    }

    @Test
    public void shouldRemoveRelationshipTypesWhenProgramIsRemoved()
    {
        programActions.delete( programId )
            .validate().statusCode( 200 );

        relationshipTypeActions.get( relationshipTypeId )
            .validate().statusCode( 404 );
    }

    private void setupData()
    {
        programId = programActions.createTrackerProgram().getId();
        assertNotNull( programId, "Failed to create program" );

        relationshipTypeId = relationshipTypeActions
            .create( "TRACKED_ENTITY_INSTANCE", Constants.TRACKED_ENTITY_TYPE_ID, "PROGRAM_STAGE_INSTANCE", programId );

        assertNotNull( relationshipTypeId, "Failed to create relationshipType" );

    }
}
