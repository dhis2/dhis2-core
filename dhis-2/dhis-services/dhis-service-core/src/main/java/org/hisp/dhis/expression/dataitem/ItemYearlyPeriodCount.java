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
package org.hisp.dhis.expression.dataitem;

import static org.hisp.dhis.calendar.DateTimeUnit.fromJdkDate;
import static org.hisp.dhis.parser.expression.ParserUtils.DOUBLE_VALUE_IF_NULL;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;

import java.util.List;

import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.period.BiWeeklyAbstractPeriodType;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyAbstractPeriodType;

/**
 * Expression item [yearlyPeriodCount]
 *
 * @author Jim Grace
 */
public class ItemYearlyPeriodCount
    implements ExpressionItem
{
    @Override
    public Object getDescription( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        return DOUBLE_VALUE_IF_NULL;
    }

    @Override
    public Double evaluate( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        List<Period> periods = visitor.getParams().getPeriods();

        if ( periods.size() != 1 )
        {
            return 0.0; // Not applicable
        }

        Period period = periods.get( 0 );
        PeriodType periodType = period.getPeriodType();

        if ( periodType instanceof WeeklyAbstractPeriodType )
        {
            return weeksInYear( period );
        }
        else if ( periodType instanceof BiWeeklyAbstractPeriodType )
        {
            return biWeeksInYear( period );
        }
        else if ( periodType instanceof CalendarPeriodType && periodType.getFrequencyOrder() < 365 )
        {
            return Double.valueOf( ((CalendarPeriodType) periodType).generatePeriods( period ).size() );
        }

        return 1.0;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private double weeksInYear( Period period )
    {
        String isoDate = period.getIsoDate();
        String testIsoString = isoDate.replaceAll( "\\d+$", "52" );
        Period testPeriod = getPeriodFromIsoString( testIsoString );
        DateTimeUnit testEndDate = fromJdkDate( testPeriod.getEndDate() );

        if ( testEndDate.getMonth() == 12 && testEndDate.getDay() < 28 )
        {
            return 53;
        }

        return 52;
    }

    private double biWeeksInYear( Period period )
    {
        String isoDate = period.getIsoDate();
        String testIsoString = isoDate.replaceAll( "\\d+$", "26" );
        Period testPeriod = getPeriodFromIsoString( testIsoString );
        DateTimeUnit testEndDate = fromJdkDate( testPeriod.getEndDate() );

        if ( testEndDate.getMonth() == 12 && testEndDate.getDay() < 28 )
        {
            return 27;
        }

        return 26;
    }
}
