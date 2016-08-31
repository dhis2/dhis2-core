package org.hisp.dhis.validation;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 * @version ValidationCriteriaServiceTest.java May 25, 20106:31:15 PM
 */
public class ValidationCriteriaServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ValidationCriteriaService validationCriteriaService;

    private String propertyA;

    private String propertyB;

    private String valueA;

    private String valueB;

    private ValidationCriteria validationCriteriaA;

    private ValidationCriteria validationCriteriaB;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        propertyA = "name";
        propertyB = "name";

        valueA = "A";
        valueB = "B";

        validationCriteriaA = createValidationCriteria( 'A', propertyA, ValidationCriteria.OPERATOR_EQUAL_TO, valueA );
        validationCriteriaB = createValidationCriteria( 'B', propertyB, ValidationCriteria.OPERATOR_EQUAL_TO, valueB );
    }

    // ----------------------------------------------------------------------
    // Business logic tests
    // ----------------------------------------------------------------------

    @Test
    public void testSaveValidationCriteria()
    {
        int id = validationCriteriaService.saveValidationCriteria( validationCriteriaA );

        validationCriteriaA = validationCriteriaService.getValidationCriteria( id );

        assertEquals( validationCriteriaA.getName(), "ValidationCriteriaA" );
        assertEquals( validationCriteriaA.getDescription(), "DescriptionA" );
        assertEquals( validationCriteriaA.getProperty(), propertyA );
        assertEquals( validationCriteriaA.getOperator(), ValidationCriteria.OPERATOR_EQUAL_TO );
        assertEquals( validationCriteriaA.getValue(), valueA );
    }
    
    @Test
    public void testUpdateValidationCriteria()
    {
        int id = validationCriteriaService.saveValidationCriteria( validationCriteriaA );
        validationCriteriaA = validationCriteriaService.getValidationCriteria(id );

        assertEquals( validationCriteriaA.getName(), "ValidationCriteriaA" );
        assertEquals( validationCriteriaA.getDescription(), "DescriptionA" );
        assertEquals( validationCriteriaA.getProperty(), propertyA );
        assertEquals( validationCriteriaA.getOperator(), ValidationCriteria.OPERATOR_EQUAL_TO );
        assertEquals( validationCriteriaA.getValue(), valueA );

        validationCriteriaA.setName( "ValidationCriteriaB" );
        validationCriteriaA.setDescription( "DescriptionB" );
        validationCriteriaA.setProperty( propertyB );
        validationCriteriaA.setOperator( ValidationCriteria.OPERATOR_GREATER_THAN );
        validationCriteriaA.setValue( valueB );

        validationCriteriaService.updateValidationCriteria( validationCriteriaA );
        validationCriteriaA = validationCriteriaService.getValidationCriteria( id );

        assertEquals( validationCriteriaA.getName(), "ValidationCriteriaB" );
        assertEquals( validationCriteriaA.getDescription(), "DescriptionB" );
        assertEquals( validationCriteriaA.getProperty(), propertyB );
        assertEquals( validationCriteriaA.getOperator(), ValidationCriteria.OPERATOR_GREATER_THAN );
        assertEquals( validationCriteriaA.getValue(), valueB );
    }

    @Test
    public void testDeleteValidationCriteria()
    {
        int idA = validationCriteriaService.saveValidationCriteria( validationCriteriaA );
        int idB = validationCriteriaService.saveValidationCriteria( validationCriteriaB );
        
        assertNotNull( validationCriteriaService.getValidationCriteria( idA ) );
        assertNotNull( validationCriteriaService.getValidationCriteria( idB ) );

        validationCriteriaService.deleteValidationCriteria( validationCriteriaA );

        assertNull( validationCriteriaService.getValidationCriteria( idA ) );
        assertNotNull( validationCriteriaService.getValidationCriteria( idB ) );

        validationCriteriaService.deleteValidationCriteria( validationCriteriaB );
        
        assertNull( validationCriteriaService.getValidationCriteria( idA ) );
        assertNull( validationCriteriaService.getValidationCriteria( idB ) );
    }

    @Test
    public void testGetValidationCriteriaByName()
    {
        int idA = validationCriteriaService.saveValidationCriteria( validationCriteriaA );
        validationCriteriaService.saveValidationCriteria( validationCriteriaB );

        ValidationCriteria validationCriteria = validationCriteriaService.getValidationCriteria( idA );

        assertEquals( validationCriteria.getId(), idA );
        assertEquals( validationCriteria.getName(), "ValidationCriteriaA" );
    }

    @Test
    public void testGetAllValidationRules()
    {
        validationCriteriaService.saveValidationCriteria( validationCriteriaA );
        validationCriteriaService.saveValidationCriteria( validationCriteriaB );

        List<ValidationCriteria> validationCriteria = validationCriteriaService.getAllValidationCriterias();

        assertTrue( validationCriteria.size() == 2 );
        assertTrue( validationCriteria.contains( validationCriteriaA ) );
        assertTrue( validationCriteria.contains( validationCriteriaB ) );
    }

}
