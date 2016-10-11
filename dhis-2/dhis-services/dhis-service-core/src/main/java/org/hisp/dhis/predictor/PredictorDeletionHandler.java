package org.hisp.dhis.predictor;

import org.hisp.dhis.dataelement.DataElement;

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

import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.system.deletion.DeletionHandler;

import java.util.Iterator;
import java.util.List;

/**
 * @author Ken Haase
 */
public class PredictorDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private PredictorService predictorService;

    public void setPredictorService( PredictorService predictorService )
    {
        this.predictorService = predictorService;
    }

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return Predictor .class.getSimpleName();
    }

    @Override
    public void deleteExpression( Expression expression )
    {
        Iterator<Predictor> iterator = predictorService.getAllPredictors().iterator();
        
        while ( iterator.hasNext() )
        {
            Predictor predictor = iterator.next();
            
            Expression generator = predictor.getGenerator();
            Expression skipTest= predictor.getSampleSkipTest();

            if ( generator != null && generator.equals( expression ) ||
                 skipTest != null && skipTest.equals( expression ) )
            {
                iterator.remove();
                predictorService.deletePredictor ( predictor );
            }
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
}
