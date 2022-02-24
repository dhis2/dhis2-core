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
package org.hisp.dhis.predictor;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Ken Haase
 */
@Component( "org.hisp.dhis.predictor.PredictorDeletionHandler" )
public class PredictorDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final PredictorService predictorService;

    private final JdbcTemplate jdbcTemplate;

    public PredictorDeletionHandler( PredictorService predictorService, JdbcTemplate jdbcTemplate )
    {
        checkNotNull( predictorService );

        this.predictorService = predictorService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return Predictor.class.getSimpleName();
    }

    @Override
    public void deleteExpression( Expression expression )
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

    @Override
    public void deletePredictorGroup( PredictorGroup predictorGroup )
    {
        for ( Predictor predictor : predictorGroup.getMembers() )
        {
            predictor.getGroups().remove( predictorGroup );
            predictorService.updatePredictor( predictor );
        }
    }

    @Override
    public String allowDeleteDataElement( DataElement dataElement )
    {
        List<Predictor> predictors = predictorService.getAllPredictors();

        for ( Predictor predictor : predictors )
        {
            if ( dataElement.typedEquals( predictor.getOutput() ) )
            {
                return predictor.getName();
            }
        }

        return null;
    }

    @Override
    public String allowDeleteCategoryOptionCombo( CategoryOptionCombo coc )
    {
        return exists( "SELECT COUNT(*) FROM predictor where generatoroutputcombo=" + coc.getId() ) ? ERROR : null;
    }

    @Override
    public String allowDeleteCategoryCombo( CategoryCombo categoryCombo )
    {
        return exists( "SELECT COUNT(*) FROM predictor p where exists ("
            + "select 1 from categorycombos_optioncombos co"
            + " where co.categorycomboid=" + categoryCombo.getId()
            + " and co.categoryoptioncomboid=p.generatoroutputcombo"
            + ")" ) ? ERROR : null;
    }

    private boolean exists( String sql )
    {
        Integer count = jdbcTemplate.queryForObject( sql, Integer.class );
        return count != null && count > 0;
    }
}
