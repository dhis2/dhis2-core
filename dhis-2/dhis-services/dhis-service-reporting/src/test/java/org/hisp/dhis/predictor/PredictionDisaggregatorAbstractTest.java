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
package org.hisp.dhis.predictor;

import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.category.CategoryOption.DEFAULT_NAME;
import static org.hisp.dhis.common.DataDimensionType.DISAGGREGATION;

import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.MonthlyPeriodType;

/**
 * Defines test elements for prediction disaggregator tests.
 *
 * @author Jim Grace
 */
abstract class PredictionDisaggregatorAbstractTest
    extends DhisConvenienceTest
{
    // Overall plan:
    //
    // Data Element A -> Cat Combo A -> Category A -> Options A, B
    // Data Element B -> Cat Combo B -> Category B -> Options A, B
    // Data Element C -> Cat Combo C -> Category C -> Option A
    // Data Element D -> Cat Combo D -> Category D -> Options A, C
    // Data Element T -> Default combo -> Default category -> Default option
    //
    // Data Element A is used by output data element and an input d.e.
    // Data Element B will map disaggregations to Cat Combo A (full set)
    // Data Element C will map disaggregations to Cat Combo A (partial subset)
    // Data Element D will not map to disaggregations (invalid option C)
    //
    // Note that Data element C will still map data with option B (such as might
    // be present in legacy data) even though Option B is not at present part of
    // Cat Combo C.

    // Category Options

    protected final CategoryOption coA = createCategoryOption( "coA", "coAaaaaaaaa" );

    protected final CategoryOption coB = createCategoryOption( "coB", "coBbbbbbbbb" );

    protected final CategoryOption coC = createCategoryOption( "coC", "coCcccccccc" );

    protected final CategoryOption coDefault = createCategoryOption( DEFAULT_NAME, "coIsDefault" );

    // Categories

    protected final Category catA = createCategory( "catA", "catAaaaaaaa", coA, coB );

    protected final Category catB = createCategory( "catB", "catBbbbbbbb", coA, coB );

    protected final Category catC = createCategory( "catC", "catCccccccc", coA );

    protected final Category catD = createCategory( "catD", "catDddddddd", coA, coC );

    protected final Category catDefault = createCategory( DEFAULT_NAME, "caIsDefault", coDefault );

    // Category Combos

    protected final CategoryCombo ccA = createCategoryCombo( "ccA", "ccAaaaaaaaa", catA );

    protected final CategoryCombo ccB = createCategoryCombo( "ccB", "ccBbbbbbbbb", catB );

    protected final CategoryCombo ccC = createCategoryCombo( "ccC", "ccCcccccccc", catC );

    protected final CategoryCombo ccD = createCategoryCombo( "ccD", "ccDdddddddd", catD );

    protected final CategoryCombo ccDefault = new CategoryCombo( DEFAULT_CATEGORY_COMBO_NAME, DISAGGREGATION,
        List.of( catDefault ) );

    // Category Option Combos

    protected final CategoryOptionCombo cocAa = newCategoryOptionCombo( "cocAa", "cocAaAaAaAa", ccA, coA );

    protected final CategoryOptionCombo cocAb = newCategoryOptionCombo( "cocAb", "cocAbAbAbAb", ccA, coB );

    protected final CategoryOptionCombo cocBa = newCategoryOptionCombo( "cocBa", "cocBaBaBaBa", ccB, coA );

    protected final CategoryOptionCombo cocBb = newCategoryOptionCombo( "cocBb", "cocBbBbBbBb", ccB, coB );

    protected final CategoryOptionCombo cocCa = newCategoryOptionCombo( "cocCa", "cocCaCaCaCa", ccC, coA );

    // Even though Category combo C does not have option combo B, there may
    // still be legacy data for combo C that has option B.
    protected final CategoryOptionCombo cocCb = createCategoryOptionCombo( "cocCb", "cocCbCbCbCb", ccC, coB );

    protected final CategoryOptionCombo cocDa = newCategoryOptionCombo( "cocDa", "cocDaDaDaDa", ccD, coA );

    protected final CategoryOptionCombo cocDc = newCategoryOptionCombo( "cocDc", "cocDcDcDcDc", ccD, coC );

    protected final CategoryOptionCombo cocDefault = newCategoryOptionCombo( DEFAULT_NAME, "cocDefault", ccDefault,
        coDefault );

    // Data Elements

    protected final DataElement deA = createDataElement( 'A', ccA );

    protected final DataElement deB = createDataElement( 'B', ccB );

    protected final DataElement deC = createDataElement( 'C', ccC );

    protected final DataElement deD = createDataElement( 'D', ccD );

    protected final DataElement deT = createDataElement( 'T', ccDefault );

    // Data Element Operands

    protected final DataElementOperand deoAa = new DataElementOperand( deA, cocAa );

    protected final DataElementOperand deoAb = new DataElementOperand( deA, cocAb );

    protected final DataElementOperand deoBa = new DataElementOperand( deB, cocBa );

    protected final DataElementOperand deoBb = new DataElementOperand( deB, cocBb );

    protected final DataElementOperand deoCa = new DataElementOperand( deC, cocCa );

    protected final DataElementOperand deoCb = new DataElementOperand( deC, cocCb );

    protected final DataElementOperand deoDa = new DataElementOperand( deD, cocDa );

    protected final DataElementOperand deoDc = new DataElementOperand( deD, cocDc );

    protected final DataElementOperand deoAX = new DataElementOperand( deA, null );

    protected final DataElementOperand deoBX = new DataElementOperand( deB, null );

    protected final DataElementOperand deoCX = new DataElementOperand( deC, null );

    // Items as if parsed from an expression

    protected final List<DimensionalItemObject> expressionItems = List.of( deA, deB, deC, deD,
        deoAa, deoAb, deoBa, deoBb, deoCa, deoCb, deoDa, deoDc );

    // Organisation unit level

    protected final OrganisationUnitLevel ouLevelA = new OrganisationUnitLevel( 1, "Top" );

    // Expression

    protected final Expression expA = new Expression( "1", "Description" );

    // Predictors

    protected final Predictor pWithoutDisag = createPredictor( deA, cocAa, "A", expA, null,
        new MonthlyPeriodType(), ouLevelA, 0, 0, 0 );

    protected final Predictor pWithDisag = createPredictor( deA, null, "A", expA, null,
        new MonthlyPeriodType(), ouLevelA, 0, 0, 0 );

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private CategoryOptionCombo newCategoryOptionCombo( String name, String uid, CategoryCombo categoryCombo,
        CategoryOption... categoryOptions )
    {
        CategoryOptionCombo coc = createCategoryOptionCombo( name, uid, categoryCombo, categoryOptions );
        categoryCombo.getOptionCombos().add( coc );

        return coc;
    }
}
