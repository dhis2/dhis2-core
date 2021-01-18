package org.hisp.dhis.indicator;

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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;

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
            for ( LegendSet ls : indicator.getLegendSets() )
            {
                if( legendSet.equals( ls ) )
                {
                    indicator.getLegendSets().remove( ls );
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
            Set<DataElement> daels = expressionService.getExpressionDataElements( indicator.getNumerator(), INDICATOR_EXPRESSION );

            if ( daels != null && daels.contains( dataElement ) )
            {
                return indicator.getName();
            }

            daels = expressionService.getExpressionDataElements( indicator.getDenominator(), INDICATOR_EXPRESSION );

            if ( daels != null && daels.contains( dataElement ) )
            {
                return indicator.getName();
            }
        }

        return null;
    }

    @Override
    public String allowDeleteCategoryCombo( CategoryCombo categoryCombo )
    {
        Set<String> optionComboIds = categoryCombo.getOptionCombos().stream()
            .map( CategoryOptionCombo::getUid ).collect( Collectors.toSet() );

        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            Set<String> comboIds = expressionService.getExpressionOptionComboIds(
                indicator.getNumerator(), INDICATOR_EXPRESSION );
            comboIds.retainAll( optionComboIds );

            if ( !comboIds.isEmpty() )
            {
                return indicator.getName();
            }

            comboIds = expressionService.getExpressionOptionComboIds(
                indicator.getDenominator(), INDICATOR_EXPRESSION );
            comboIds.retainAll( optionComboIds );

            if ( !comboIds.isEmpty() )
            {
                return indicator.getName();
            }
        }

        return null;
    }
}
