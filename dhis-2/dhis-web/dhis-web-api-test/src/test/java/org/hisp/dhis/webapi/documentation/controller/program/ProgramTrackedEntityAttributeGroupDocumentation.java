<<<<<<< HEAD
package org.hisp.dhis.webapi.documentation.controller.program;

=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.webapi.documentation.controller.program;

<<<<<<< HEAD
import org.hisp.dhis.program.Program;

=======
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hisp.dhis.program.Program;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeGroup;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.webapi.documentation.common.TestUtils;
import org.hisp.dhis.webapi.documentation.controller.AbstractWebApiTest;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */

public class ProgramTrackedEntityAttributeGroupDocumentation
    extends AbstractWebApiTest<ProgramTrackedEntityAttributeGroup>
{
    @Test
    public void testAddAndRemoveMember()
        throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        Program pr = createProgram( 'A' );
        manager.save( pr );
        TrackedEntityAttribute tea = createTrackedEntityAttribute( 'A' );
        manager.save( tea );
        ProgramTrackedEntityAttribute attrA = createProgramTrackedEntityAttribute( pr, tea );
        manager.save( attrA );

        ProgramTrackedEntityAttributeGroup group = createProgramTrackedEntityAttributeGroup( 'A' );
        manager.save( group );

        attrA.addGroup( group );

        mvc.perform( post( schema.getRelativeApiEndpoint() + "/" + group.getUid() + "/attributes/" + attrA.getUid() )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( status().isNoContent() );

        mvc.perform( get( schema.getRelativeApiEndpoint() + "/{id}", group.getUid() )
            .session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.attributes.length()" ).value( 1 ) );

        mvc.perform( delete( schema.getRelativeApiEndpoint() + "/" + group.getUid() + "/attributes/" + attrA.getUid() )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( status().isNoContent() );

        mvc.perform( get( schema.getRelativeApiEndpoint() + "/{id}", group.getUid() )
            .session( session )
            .accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.attributes.length()" ).value( 0 ) );

    }
}
