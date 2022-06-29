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
package org.hisp.dhis.program;

import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramStageSectionServiceTest extends TransactionalIntegrationTest
{

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    @Autowired
    private AclService aclService;

    @Autowired
    private ProgramStageSectionService programStageSectionService;

    @Autowired
    private CategoryService _categoryService;

    @BeforeEach
    void init()
    {
        userService = _userService;
        categoryService = _categoryService;
    }

    @Test
    void testUpdateWithAuthority()
    {
        Program program = createProgram( 'A' );
        manager.save( program );
        ProgramStage programStage = createProgramStage( 'A', program );
        manager.save( programStage );
        ProgramStageSection programStageSection = createProgramStageSection( 'A', 0 );
        programStageSection.setProgramStage( programStage );
        manager.save( programStageSection );
        User userA = createUserWithAuth( "A", "F_PROGRAMSTAGE_ADD" );
        Assertions.assertTrue( aclService.canUpdate( userA, programStageSection ) );
        User userB = createUserWithAuth( "B" );
        Assertions.assertFalse( aclService.canUpdate( userB, programStageSection ) );
    }

    @Test
    void testSaveWithoutAuthority()
    {
        Program program = createProgram( 'A' );
        manager.save( program );
        ProgramStage programStage = createProgramStage( 'A', program );
        manager.save( programStage );
        createUserAndInjectSecurityContext( false );
        ProgramStageSection programStageSection = createProgramStageSection( 'A', 0 );
        programStageSection.setProgramStage( programStage );
        programStageSectionService.saveProgramStageSection( programStageSection );
        Assertions.assertNotNull( programStageSectionService.getProgramStageSection( programStageSection.getId() ) );
    }
}
