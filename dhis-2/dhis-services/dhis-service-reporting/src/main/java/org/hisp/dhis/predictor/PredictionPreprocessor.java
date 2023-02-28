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

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_EXPRESSION;
import static org.hisp.dhis.expression.PreprocessorExpression.PREPROCESSOR_SEPARATOR;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.PredictorExpression;
import org.springframework.stereotype.Component;

/**
 * Preprocessor for predictor expression syntax.
 *
 * @author Jim Grace
 */
@Component
@RequiredArgsConstructor
public class PredictionPreprocessor
{
    private final IdentifiableObjectManager idObjectManager;

    private final ExpressionService expressionService;

    private static final String FOR_EACH = "forEach";

    /**
     * Preprocesses a predictor, possibly splitting it into multiple predictors.
     *
     * @param predictor the predictor to preprocess
     * @return the predictors to run
     */
    public List<Predictor> preprocess( Predictor predictor )
    {
        PredictorExpression pe = new PredictorExpression( predictor.getGenerator().getExpression() );

        return (pe.isSimple())
            ? List.of( predictor )
            : expand( predictor, pe );
    }

    /**
     * Gets a predictor expression description, possibly with preprocessing.
     * Throws a runtime exception if the expression is not valid.
     *
     * @param expression the predictor exprssion to describe
     * @return the description
     */
    public String getDescription( String expression )
    {
        PredictorExpression pe = new PredictorExpression( expression );

        return (pe.isSimple())
            ? expressionService.getExpressionDescription( expression, PREDICTOR_EXPRESSION )
            : describe( pe );
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Expands this predictor to a list of simple predictors.
     */
    private List<Predictor> expand( Predictor predictor, PredictorExpression pe )
    {
        DataElementGroup deg = getDeg( pe );

        return deg.getMembers().stream()
            .map( de -> clonePredictor( predictor, pe, de ) )
            .collect( toList() );
    }

    /**
     * Clones a predictor for a particular data element.
     * <p>
     * Note: The .toBuilder().build() pattern makes a shallow clone. In the
     * first attempt at writing this method, SerializationUtils.clone() was used
     * to make a deep clone, but that didn't work because some nested objects
     * were Hibernate proxies and the resulting objects triggered unable to
     * initialize proxy exceptions.
     */
    private Predictor clonePredictor( Predictor predictor, PredictorExpression pe,
        DataElement dataElement )
    {
        Predictor clone = predictor.toBuilder().build();

        clone.setOutput( dataElement );

        String mainExpression = pe.getMain();
        String variable = pe.getVariable();
        String clonedExpression = mainExpression.replace( variable, dataElement.getUid() );
        Expression clonedGenerator = new Expression( clonedExpression, clone.getGenerator().getDescription(),
            clone.getGenerator().getMissingValueStrategy() );
        clone.setGenerator( clonedGenerator );

        return clone;
    }

    /**
     * Gets the description for a predictor.
     */
    private String describe( PredictorExpression pe )
    {
        // Get prefix description (resolve data element group)
        DataElementGroup deg = getDeg( pe );
        String prefixDescription = pe.getPrefix().replace( pe.getTaggedDegUid(), deg.getName() );

        // Find item descriptions for the main expression with a dummy
        // expression resolving the data element variable to some DE in the DEG
        DataElement de = getOneDataElement( deg, pe );
        String mainExpressionWithOneDataElement = pe.getMain().replace( pe.getVariable(), de.getUid() );
        Map<String, String> itemDescriptions = expressionService.getExpressionItemDescriptions(
            mainExpressionWithOneDataElement, PREDICTOR_EXPRESSION );

        // Get the main expression description
        String mainDescription = pe.getMain();
        for ( Map.Entry<String, String> entry : itemDescriptions.entrySet() )
        {
            mainDescription = mainDescription.replace( entry.getKey(), entry.getValue() );
        }

        // Return the complete description
        return prefixDescription + PREPROCESSOR_SEPARATOR + mainDescription;
    }

    /**
     * Gets the data element group for preprocessing.
     */
    private DataElementGroup getDeg( PredictorExpression pe )
    {
        DataElementGroup deg = idObjectManager.get( DataElementGroup.class, pe.getDegUid() );

        if ( deg == null )
        {
            throw new IllegalStateException(
                format( "Can't find data element group %s in %s", pe.getDegUid(), pe.getExpression() ) );
        }
        return deg;
    }

    /**
     * Gets one data element from the data element group.
     */
    private DataElement getOneDataElement( DataElementGroup deg, PredictorExpression pe )
    {
        Set<DataElement> dataElements = deg.getMembers();

        if ( dataElements.isEmpty() )
        {
            throw new IllegalStateException(
                format( "DataElementGroup '%s' is empty when evaluating '%s'", deg.getName(), pe.getExpression() ) );
        }

        return dataElements.iterator().next();
    }
}
