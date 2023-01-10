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

import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import java.util.Iterator;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.JdbcDeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Ken Haase
 */
@Component
@RequiredArgsConstructor
public class PredictorDeletionHandler extends JdbcDeletionHandler
{
    private static final DeletionVeto VETO = new DeletionVeto( Predictor.class );

    private final PredictorService predictorService;

    @Override
    protected void register()
    {
        whenDeletingEmbedded( Expression.class, this::deleteExpression );
        whenDeleting( PredictorGroup.class, this::deletePredictorGroup );
        whenVetoing( DataElement.class, this::allowDeleteDataElement );
        whenVetoing( CategoryOptionCombo.class, this::allowDeleteCategoryOptionCombo );
        whenVetoing( CategoryCombo.class, this::allowDeleteCategoryCombo );
    }

    private void deleteExpression( Expression expression )
    {
        Iterator<Predictor> iterator = predictorService.getAllPredictors().iterator();

        while ( iterator.hasNext() )
        {
            Predictor predictor = iterator.next();

            Expression generator = predictor.getGenerator();
            Expression skipTest = predictor.getSampleSkipTest();

            if ( generator != null && generator.equals( expression ) ||
                skipTest != null && skipTest.equals( expression ) )
            {
                iterator.remove();
                predictorService.deletePredictor( predictor );
            }
        }
    }

    private void deletePredictorGroup( PredictorGroup predictorGroup )
    {
        for ( Predictor predictor : predictorGroup.getMembers() )
        {
            predictor.getGroups().remove( predictorGroup );
            predictorService.updatePredictor( predictor );
        }
    }

    private DeletionVeto allowDeleteDataElement( DataElement dataElement )
    {
        String predictorName = firstMatch( "select p.name from predictor p where p.generatoroutput = :dataElementId",
            Map.of( "dataElementId", dataElement.getId() ) );
        return predictorName == null ? ACCEPT : new DeletionVeto( Predictor.class, predictorName );
    }

    private DeletionVeto allowDeleteCategoryOptionCombo( CategoryOptionCombo optionCombo )
    {
        return vetoIfExists( VETO, "select 1 from predictor where generatoroutputcombo=:id limit 1",
            Map.of( "id", optionCombo.getId() ) );
    }

    private DeletionVeto allowDeleteCategoryCombo( CategoryCombo categoryCombo )
    {
        return vetoIfExists( VETO, "select 1 from predictor p where exists ("
            + "select 1 from categorycombos_optioncombos co"
            + " where co.categorycomboid=:id and co.categoryoptioncomboid=p.generatoroutputcombo limit 1"
            + ")", Map.of( "id", categoryCombo.getId() ) );
    }
}
