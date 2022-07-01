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
package org.hisp.dhis.dataapproval;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
class DataApprovalWorkflowServiceTest extends TransactionalIntegrationTest
{

    @Autowired
    private DataApprovalService dataApprovalService;

    @Autowired
    private DataApprovalLevelService dataApprovalLevelService;

    @Autowired
    private UserService _userService;

    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------
    private DataApprovalWorkflow workflowA;

    private DataApprovalWorkflow workflowB;

    private DataApprovalWorkflow workflowC;

    private DataApprovalLevel level1;

    private DataApprovalLevel level2;

    private DataApprovalLevel level3;

    PeriodType periodType;

    // -------------------------------------------------------------------------
    // Set up/tear down
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
        throws Exception
    {
        userService = _userService;
        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------
        level1 = new DataApprovalLevel( "1", 1, null );
        level2 = new DataApprovalLevel( "2", 2, null );
        level3 = new DataApprovalLevel( "3", 3, null );
        dataApprovalLevelService.addDataApprovalLevel( level1 );
        dataApprovalLevelService.addDataApprovalLevel( level2 );
        dataApprovalLevelService.addDataApprovalLevel( level3 );
        periodType = PeriodType.getPeriodTypeByName( "Monthly" );
        workflowA = new DataApprovalWorkflow( "A", periodType, newHashSet( level1, level2 ) );
        workflowB = new DataApprovalWorkflow( "B", periodType, newHashSet( level2, level3 ) );
        workflowC = new DataApprovalWorkflow( "C", periodType, newHashSet( level1, level3 ) );
    }

    // -------------------------------------------------------------------------
    // Basic DataApprovalWorkflow
    // -------------------------------------------------------------------------
    @Test
    void testAddDataApprovalWorkflow()
    {
        long id = dataApprovalService.addWorkflow( workflowA );
        assertTrue( id != 0 );
        DataApprovalWorkflow workflow = dataApprovalService.getWorkflow( id );
        assertEquals( "A", workflow.getName() );
        Set<DataApprovalLevel> members = workflow.getLevels();
        assertEquals( 2, members.size() );
        assertTrue( members.contains( level1 ) );
        assertTrue( members.contains( level2 ) );
    }

    @Test
    void testUpdateDataApprovalWorkflow()
    {
        long id = dataApprovalService.addWorkflow( workflowA );
        DataApprovalWorkflow workflow = dataApprovalService.getWorkflow( id );
        workflow.setName( "workflowB" );
        workflow.setPeriodType( periodType );
        workflow.setLevels( newHashSet( level2, level3 ) );
        dataApprovalService.updateWorkflow( workflow );
        workflow = dataApprovalService.getWorkflow( id );
        assertEquals( "workflowB", workflow.getName() );
        assertEquals( "Monthly", workflow.getPeriodType().getName() );
        Set<DataApprovalLevel> members = workflow.getLevels();
        assertEquals( 2, members.size() );
        assertTrue( members.contains( level2 ) );
        assertTrue( members.contains( level3 ) );
    }

    @Test
    void testDeleteDataApprovalWorkflow()
    {
        long id = dataApprovalService.addWorkflow( workflowA );
        dataApprovalService.deleteWorkflow( workflowA );
        DataApprovalWorkflow workflow = dataApprovalService.getWorkflow( id );
        assertNull( workflow );
        List<DataApprovalWorkflow> workflows = dataApprovalService.getAllWorkflows();
        assertEquals( 0, workflows.size() );
    }

    @Test
    void testGetDataApprovalWorkflow()
    {
        long idA = dataApprovalService.addWorkflow( workflowA );
        long idB = dataApprovalService.addWorkflow( workflowB );
        long idC = dataApprovalService.addWorkflow( workflowC );
        assertEquals( workflowA, dataApprovalService.getWorkflow( idA ) );
        assertEquals( workflowB, dataApprovalService.getWorkflow( idB ) );
        assertEquals( workflowC, dataApprovalService.getWorkflow( idC ) );
        assertNull( dataApprovalService.getWorkflow( 0 ) );
        assertNull( dataApprovalService.getWorkflow( idA + idB + idC ) );
    }

    @Test
    void testGetAllDataApprovalWorkflows()
    {
        List<DataApprovalWorkflow> workflows = dataApprovalService.getAllWorkflows();
        assertEquals( 0, workflows.size() );
        dataApprovalService.addWorkflow( workflowA );
        workflows = dataApprovalService.getAllWorkflows();
        assertEquals( 1, workflows.size() );
        assertTrue( workflows.contains( workflowA ) );
        dataApprovalService.addWorkflow( workflowB );
        workflows = dataApprovalService.getAllWorkflows();
        assertEquals( 2, workflows.size() );
        assertTrue( workflows.contains( workflowA ) );
        assertTrue( workflows.contains( workflowB ) );
        dataApprovalService.addWorkflow( workflowC );
        workflows = dataApprovalService.getAllWorkflows();
        assertEquals( 3, workflows.size() );
        assertTrue( workflows.contains( workflowA ) );
        assertTrue( workflows.contains( workflowB ) );
        assertTrue( workflows.contains( workflowC ) );
    }

    @Test
    void testSaveWorkFlowWithAuthority()
    {
        createUserAndInjectSecurityContext( false, "F_DATA_APPROVAL_WORKFLOW" );
        long idA = dataApprovalService
            .addWorkflow( new DataApprovalWorkflow( "H", periodType, newHashSet( level1, level2 ) ) );
        assertEquals( "H", dataApprovalService.getWorkflow( idA ).getName() );
    }

    @Test
    void testSaveWorkFlowWithoutAuthority()
    {
        createUserAndInjectSecurityContext( false, null );
        assertThrows( CreateAccessDeniedException.class, () -> dataApprovalService
            .addWorkflow( new DataApprovalWorkflow( "F", periodType, newHashSet( level1, level2 ) ) ) );
    }

    @Test
    void testSaveLevelWithAuthority()
    {
        createUserAndInjectSecurityContext( false, "F_DATA_APPROVAL_LEVEL" );
        long idA = dataApprovalLevelService.addDataApprovalLevel( new DataApprovalLevel( "4", 1, null ) );
        assertEquals( "4", dataApprovalLevelService.getDataApprovalLevel( idA ).getName() );
    }

    @Test
    void testSaveLevelWithoutAuthority()
    {
        createUserAndInjectSecurityContext( false, null );
        assertThrows( UpdateAccessDeniedException.class,
            () -> dataApprovalLevelService.addDataApprovalLevel( new DataApprovalLevel( "7", 1, null ) ) );
    }
}
