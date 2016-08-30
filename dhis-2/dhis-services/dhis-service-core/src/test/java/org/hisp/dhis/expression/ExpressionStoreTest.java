package org.hisp.dhis.expression;

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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class ExpressionStoreTest
    extends DhisSpringTest
{
    @Resource( name = "org.hisp.dhis.expression.ExpressionStore" )
    private GenericStore<Expression> expressionStore;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService categoryService;

    private int dataElementIdA;

    private int dataElementIdB;

    private int dataElementIdC;

    private int dataElementIdD;

    private String expressionA;

    private String expressionB;

    private String descriptionA;

    private String descriptionB;

    private Set<DataElement> dataElements = new HashSet<>();

    private Set<DataElement> sampleElements = new HashSet<>();

    private Set<DataElementCategoryOptionCombo> optionCombos;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );

        dataElementIdA = dataElementService.addDataElement( dataElementA );
        dataElementIdB = dataElementService.addDataElement( dataElementB );
        dataElementIdC = dataElementService.addDataElement( dataElementC );
        dataElementIdD = dataElementService.addDataElement( dataElementD );

        DataElementCategoryCombo categoryCombo = categoryService
            .getDataElementCategoryComboByName( DataElementCategoryCombo.DEFAULT_CATEGORY_COMBO_NAME );
        DataElementCategoryOptionCombo categoryOptionCombo = categoryCombo.getOptionCombos().iterator().next();

        optionCombos = new HashSet<>();
        optionCombos.add( categoryOptionCombo );

        expressionA = "[" + dataElementIdA + "] + [" + dataElementIdB + "]";
        expressionB = "[" + dataElementIdC + "] - [" + dataElementIdD + "]";

        descriptionA = "Expression A";
        descriptionB = "Expression B";

        dataElements.add( dataElementA );
        dataElements.add( dataElementB );
        dataElements.add( dataElementC );
        dataElements.add( dataElementD );

        sampleElements.add( dataElementA );
        sampleElements.add( dataElementB );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddGetExpression()
    {
        Expression expr = new Expression( expressionA, descriptionA, dataElements, sampleElements );

        int id = expressionStore.save( expr );

        expr = expressionStore.get( id );

        assertEquals( expr.getExpression(), expressionA );
        assertEquals( expr.getDescription(), descriptionA );
        assertEquals( expr.getDataElementsInExpression(), dataElements );
        assertEquals( expr.getSampleElementsInExpression(), sampleElements );
    }

    @Test
    public void testUpdateExpression()
    {
        Expression expr = new Expression( expressionA, descriptionA, dataElements );

        int id = expressionStore.save( expr );

        expr = expressionStore.get( id );

        assertEquals( expr.getExpression(), expressionA );
        assertEquals( expr.getDescription(), descriptionA );

        expr.setExpression( expressionB );
        expr.setDescription( descriptionB );

        expressionStore.update( expr );

        expr = expressionStore.get( id );

        assertEquals( expr.getExpression(), expressionB );
        assertEquals( expr.getDescription(), descriptionB );
    }

    @Test
    public void testDeleteExpression()
    {
        Expression exprA = new Expression( expressionA, descriptionA, dataElements );
        Expression exprB = new Expression( expressionB, descriptionB, dataElements );

        int idA = expressionStore.save( exprA );
        int idB = expressionStore.save( exprB );

        assertNotNull( expressionStore.get( idA ) );
        assertNotNull( expressionStore.get( idB ) );

        expressionStore.delete( exprA );

        assertNull( expressionStore.get( idA ) );
        assertNotNull( expressionStore.get( idB ) );

        expressionStore.delete( exprB );

        assertNull( expressionStore.get( idA ) );
        assertNull( expressionStore.get( idB ) );
    }

    @Test
    public void testGetAllExpressions()
    {
        Expression exprA = new Expression( expressionA, descriptionA, dataElements );
        Expression exprB = new Expression( expressionB, descriptionB, dataElements );

        expressionStore.save( exprA );
        expressionStore.save( exprB );

        List<Expression> expressions = expressionStore.getAll();

        assertTrue( expressions.size() == 2 );
        assertTrue( expressions.contains( exprA ) );
        assertTrue( expressions.contains( exprB ) );
    }
}
