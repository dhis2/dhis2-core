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

import static java.lang.Integer.parseInt;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.MONTH;
import static org.hisp.dhis.parser.expression.ParserUtils.DOUBLE_VALUE_IF_NULL;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

/**
 * Expression item [periodInYear]
 *
 * @author Jim Grace
 */
public class ItemPeriodInYear
    implements ExpressionItem
{
    private static final Pattern TRAILING_DIGITS = Pattern.compile( "\\d+$" );

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

        if ( periodType instanceof DailyPeriodType )
        {
            return dayOfYear( period );
        }
        else if ( periodType instanceof MonthlyPeriodType )
        {
            return monthOfYear( period );
        }
        else if ( periodType.getFrequencyOrder() >= 365 )
        {
            return 1.0;
        }

        return trailingDigits( period );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private double dayOfYear( Period period )
    {
        Calendar cal = Calendar.getInstance();

        cal.setTime( period.getStartDate() );

        return cal.get( DAY_OF_YEAR );
    }

    private double monthOfYear( Period period )
    {
        Calendar cal = Calendar.getInstance();

        cal.setTime( period.getStartDate() );

        return 1 + cal.get( MONTH );
    }

    private double trailingDigits( Period period )
    {
        Matcher m = TRAILING_DIGITS.matcher( period.getIsoDate() );

        if ( m.find() )
        {
            return parseInt( m.group() );
        }

        return 0; // Unexpected
    }
}
