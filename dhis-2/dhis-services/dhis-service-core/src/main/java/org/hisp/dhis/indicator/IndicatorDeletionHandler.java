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
package org.hisp.dhis.indicator;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.collections4.CollectionUtils.containsAny;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Component( "org.hisp.dhis.indicator.IndicatorDeletionHandler" )
public class IndicatorDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final IndicatorService indicatorService;

    private final ExpressionService expressionService;

    public IndicatorDeletionHandler( IndicatorService indicatorService, ExpressionService expressionService )
    {
        checkNotNull( indicatorService );
        checkNotNull( expressionService );

        this.indicatorService = indicatorService;
        this.expressionService = expressionService;
    }

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return Indicator.class.getSimpleName();
    }

    @Override
    public String allowDeleteIndicatorType( IndicatorType indicatorType )
    {
        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            if ( indicator.getIndicatorType().equals( indicatorType ) )
            {
                return indicator.getName();
            }
        }

        return null;
    }

    @Override
    public void deleteIndicatorGroup( IndicatorGroup group )
    {
        for ( Indicator indicator : group.getMembers() )
        {
            indicator.getGroups().remove( group );
            indicatorService.updateIndicator( indicator );
        }
    }

    @Override
    public void deleteDataSet( DataSet dataSet )
    {
        for ( Indicator indicator : dataSet.getIndicators() )
        {
            indicator.getDataSets().remove( dataSet );
            indicatorService.updateIndicator( indicator );
        }
    }

    @Override
    public void deleteLegendSet( LegendSet legendSet )
    {
        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            for ( Iterator<LegendSet> itr = indicator.getLegendSets().iterator(); itr.hasNext(); )
            {
                if ( legendSet.equals( itr.next() ) )
                {
                    itr.remove();
                    indicatorService.updateIndicator( indicator );
                }
            }
        }
    }

    @Override
    public String allowDeleteDataElement( DataElement dataElement )
    {
        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            if ( getElementIds( indicator.getNumerator() ).contains( dataElement.getUid() ) ||
                getElementIds( indicator.getDenominator() ).contains( dataElement.getUid() ) )
            {
                return indicator.getName();
            }
        }

        return null;
    }

    private Set<String> getElementIds( String expression )
    {
        return expressionService.getExpressionDataElementIds( expression, INDICATOR_EXPRESSION );
    }

    @Override
    public String allowDeleteCategoryCombo( CategoryCombo categoryCombo )
    {
        Set<String> optionComboIds = categoryCombo.getOptionCombos().stream()
            .map( CategoryOptionCombo::getUid ).collect( Collectors.toSet() );

        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            if ( containsAny( getOptionComboIds( indicator.getNumerator() ), optionComboIds ) ||
                containsAny( getOptionComboIds( indicator.getDenominator() ), optionComboIds ) )
            {
                return indicator.getName();
            }
        }

        return null;
    }

    private Set<String> getOptionComboIds( String expression )
    {
        return expressionService.getExpressionOptionComboIds( expression, INDICATOR_EXPRESSION );
    }
}
