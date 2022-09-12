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

import static org.hisp.dhis.predictor.PredictionDisaggregatorUtils.createPredictionDisaggregator;
import static org.hisp.dhis.predictor.PredictionDisaggregatorUtils.getSortedOptions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link PredictionDisaggregatorUtils}.
 *
 * @author Jim Grace
 */
@ExtendWith( MockitoExtension.class )
class PredictionDisaggregatorUtilsTest
    extends PredictionDisaggregatorAbstractTest
{
    @Mock
    PredictionDisaggregator.PredictionDisaggregatorBuilder builder;

    @Test
    void testCreatePredictionDisaggregatorWithoutDisaggregation()
    {
        mockBuilder( pWithoutDisag );

        verify( builder ).predictor( pWithoutDisag );
        verify( builder ).defaultCOC( cocDefault );
        verify( builder ).items( expressionItems );
        verify( builder ).outputCatCombo( ccA );
        verify( builder ).disagPredictions( false );
    }

    @Test
    void testCreatePredictionDisaggregatorWithDisaggregation()
    {
        when( builder.sortedOptionsToCoc( anyMap() ) ).thenReturn( builder );
        when( builder.mappedCategoryCombos( anySet() ) ).thenReturn( builder );

        mockBuilder( pWithDisag );

        verify( builder ).predictor( pWithDisag );
        verify( builder ).defaultCOC( cocDefault );
        verify( builder ).items( expressionItems );
        verify( builder ).outputCatCombo( ccA );
        verify( builder ).disagPredictions( true );

        Map<String, String> sortedOptionsToCoc = Map.of(
            "coAaaaaaaaa", "cocAaAaAaAa",
            "coBbbbbbbbb", "cocAbAbAbAb" );

        Set<String> getMappedCategoryCombos = Set.of(
            "ccAaaaaaaaa",
            "ccBbbbbbbbb",
            "ccCcccccccc" );

        verify( builder ).sortedOptionsToCoc( sortedOptionsToCoc );
        verify( builder ).mappedCategoryCombos( getMappedCategoryCombos );
    }

    @Test
    void testGetSortedOptions()
    {
        assertEquals( "coAaaaaaaaa", getSortedOptions( cocAa ) );

        CategoryOptionCombo cocX = createCategoryOptionCombo( "cocX", "cocXxxxxxxx", ccA, coB, coC, coA );

        assertEquals( "coAaaaaaaaacoBbbbbbbbbcoCcccccccc", getSortedOptions( cocX ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    void mockBuilder( Predictor predictor )
    {
        when( builder.predictor( any( Predictor.class ) ) ).thenReturn( builder );
        when( builder.defaultCOC( any( CategoryOptionCombo.class ) ) ).thenReturn( builder );
        when( builder.items( anyCollection() ) ).thenReturn( builder );
        when( builder.outputCatCombo( any( CategoryCombo.class ) ) ).thenReturn( builder );
        when( builder.disagPredictions( anyBoolean() ) ).thenReturn( builder );

        try ( MockedStatic<PredictionDisaggregator> mocked = mockStatic( PredictionDisaggregator.class ) )
        {
            mocked.when( () -> PredictionDisaggregator.builder() )
                .thenReturn( builder );

            createPredictionDisaggregator( predictor, cocDefault, expressionItems );
        }
    }
}
