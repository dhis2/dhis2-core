package org.hisp.dhis.parsing;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.parsing.generated.ExpressionBaseVisitor;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.pow;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;
import static org.hisp.dhis.parsing.generated.ExpressionParser.*;

/**
 * Common ANTLR parsed expression code, using the ANTLR visitor pattern.
 *
 * @author Jim Grace
 */
public abstract class AbstractVisitor extends ExpressionBaseVisitor<Object>
{
    @Autowired
    protected OrganisationUnitService organisationUnitService;

    @Autowired
    protected IdentifiableObjectManager manager;

    protected OrganisationUnit currentOrgUnit = null;

    protected Period currentPeriod = null;

    protected Map<String, Double> constantMap = null;

    protected Map<String, Integer> orgUnitCountMap = null;

    protected Integer days = null;

    /**
     * Tells whether we are already inside a multi-value orgUnit function,
     * in which case we should filter selected orgUnits instaed of iterating
     * through all of them.
     */
    private boolean filterOrgUnits = false;

    // -------------------------------------------------------------------------
    // Visitor methods that are implemented here
    // -------------------------------------------------------------------------

    @Override
    public final Object visitExpr( ExprContext ctx )
    {
        if ( ctx.fun != null )
        {
            return function( ctx );
        }
        else if ( ctx.expr( 0 ) != null ) // pass through the expression
        {
            return visit( ctx.expr( 0 ) );
        }
        else // pass through the entire subtree
        {
            return visit( ctx.getChild( 0 ) );
        }
    }

    @Override
    public final Object visitNumericLiteral( NumericLiteralContext ctx )
    {
        return Double.valueOf( ctx.getText() );
    }

    @Override
    public final Object visitStringLiteral(StringLiteralContext ctx)
    {
        return unescapeJava( ctx.getText().substring( 1, ctx.getText().length() - 1 ) );
    }

    @Override
    public final Object visitBooleanLiteral(BooleanLiteralContext ctx)
    {
        return Boolean.valueOf( ctx.getText() );
    }

    @Override
    public final Object visitConstant( ConstantContext ctx )
    {
        Double value = constantMap.get( ctx.constantId().getText() );

        if ( value == null )
        {
            throw new ParsingException( "No constant defined for " + ctx.getText() );
        }

        return value;
    }

    // -------------------------------------------------------------------------
    // Visitor methods to override when the syntax is supported.
    // -------------------------------------------------------------------------

    @Override
    public Object visitProgramIndicatorVariable( ProgramIndicatorVariableContext ctx )
    {
        throw new ParsingException( "Program indicator variable is not valid in this expression." );
    }

    @Override
    public Object visitProgramIndicatorFunction( ProgramIndicatorFunctionContext ctx )
    {
        throw new ParsingException( "Program indicator function is not valid in this expression." );
    }

    @Override
    public Object visitReportingRate( ReportingRateContext ctx )
    {
        throw new ParsingException( "Reporting rate is not valid in this expression." );
    }

    // -------------------------------------------------------------------------
    // Visitor methods that subclasses must implement to process the
    // data (for evaluation) or metadata (to find DimensionItemObjects.)
    // -------------------------------------------------------------------------

    @Override
    public abstract Object visitOrgUnitCount( OrgUnitCountContext ctx );

    @Override
    public abstract Object visitDays( DaysContext ctx );

    // -------------------------------------------------------------------------
    // Logical methods that subclasses must implement so that only needed
    // expressions are visited when evaluating, but all expressions are
    // visited when syntax checking and/or finding DimensionItemObjects
    // -------------------------------------------------------------------------

    /**
     * Finds the logical AND of two boolean expressions.
     * <p/>
     * When not evaluating, visit both expressions. When evaluating,
     * visit the first expression and then the second only if necessary.
     *
     * @param ctx the parsing context.
     * @return the logical AND.
     */
    protected abstract Object functionAnd( ExprContext ctx );

    /**
     * Finds the logical OR of two boolean expressions.
     * <p/>
     * When not evaluating, visit both expressions. When evaluating,
     * visit the first expression and then the second only if necessary.
     *
     * @param ctx the parsing context.
     * @return the logical OR.
     */
    protected abstract Object functionOr( ExprContext ctx );

    /**
     * If the test expression is true, returns the first expression value,
     * else returns the second expression value.
     * <p/>
     * When not evaluating, visit both result expressions. When evaluating,
     * visit only the expression required.
     *
     * @param ctx the parsing context.
     * @return the first or second expression value, depending on the test.
     */
    protected abstract Object functionIf( ExprContext ctx );

    /**
     * If the test expression is true, skips this value (returns null).
     * <p/>
     * When not evaluating, always visit the value expression. When evaluating,
     * only visit it if the test expression is false.
     *
     * @param ctx the parsing context.
     * @return the expression, or null, depending on the test.
     */
    protected abstract Object functionExcept( ExprContext ctx );

    /**
     * Returns the first non-null argument.
     * <p/>
     * When not evaluating, always visit every argument. When evaluating,
     * only visit arguments until a non-null is found.
     *
     * @param ctx the parsing context.
     * @return the first non-null argument.
     */
    protected abstract Object functionCoalesce( ExprContext ctx );

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Evaluates common functions and operators.
     *
     * @param ctx expression context
     * @return evaluated expression object
     */
    protected Object function( ExprContext ctx )
    {
        switch ( ctx.fun.getType() )
        {
            // -----------------------------------------------------------------
            // Arithmetic Operators (return Double)
            // -----------------------------------------------------------------

            case MINUS:
                if ( ctx.expr( 1 ) != null ) // Subtract operator
                {
                    return castDouble( visit( ctx.expr( 0 ) ) )
                        - castDouble( visit( ctx.expr( 1 ) ) );
                }
                else // Unary Negative operator
                {
                    return -castDouble( visit( ctx.expr( 0 ) ) );
                }

            case PLUS: // String concatenation or numeric addition
                Object left = visit( ctx.expr( 0 ) );
                Object right = visit( ctx.expr( 1 ) );

                if ( left.getClass() == String.class )
                {
                    return castString( left )
                        + castString( right );
                }

                return castDouble( left )
                    + castDouble( right );

            case POWER:
                return pow( castDouble( visit( ctx.expr( 0 ) ) ),
                    castDouble( visit( ctx.expr( 1 ) ) ) );

            case MUL:
                return castDouble( visit( ctx.expr( 0 ) ) )
                    * castDouble( visit( ctx.expr( 1 ) ) );

            case DIV:
                return castDouble( visit( ctx.expr( 0 ) ) )
                    / castDouble( visit( ctx.expr( 1 ) ) );

            case MOD:
                return castDouble( visit( ctx.expr( 0 ) ) )
                    % castDouble( visit( ctx.expr( 1 ) ) );

            // -----------------------------------------------------------------
            // Logical Operators (return Boolean)
            // -----------------------------------------------------------------

            case NOT:
                return !castBoolean( visit( ctx.expr( 0 ) ) );

            case LEQ:
                return compare( ctx ) <= 0;

            case GEQ:
                return compare( ctx ) >= 0;

            case LT:
                return compare( ctx ) < 0;

            case GT:
                return compare( ctx ) > 0;

            case EQ:
                return compare( ctx ) == 0;

            case NE:
                return compare( ctx ) != 0;

            case AND:
                return functionAnd( ctx );

            case OR:
                return functionOr( ctx );

            // -----------------------------------------------------------------
            // Logical functions
            // -----------------------------------------------------------------

            case IF:
                return functionIf( ctx );

            case EXCEPT:
                return functionExcept( ctx );

            case IS_NULL:
                return visit( ctx.expr( 0 ) ) == null;

            case COALESCE:
                return functionCoalesce( ctx );

            // -----------------------------------------------------------------
            // Aggregation functions
            // -----------------------------------------------------------------

            case FIRST:
                return firstOrLast( ctx, true );

            case LAST:
                return firstOrLast( ctx, false );

            case COUNT:
                return evalAll( ctx ).size();

            case SUM:
                return aggregate( StatUtils::sum, ctx );

            case MAX:
                return aggregate( StatUtils::max, ctx );

            case MIN:
                return aggregate( StatUtils::min, ctx );

            case AVERAGE:
                return aggregate( StatUtils::mean, ctx );

            case STDDEV:
                return aggregate( (new StandardDeviation())::evaluate, ctx );

            case VARIANCE:
                return aggregate( StatUtils::variance, ctx );

            case MEDIAN:
                return aggregate2( StatUtils::percentile, 50.0, ctx );

            case PERCENTILE:
                return aggregate2( StatUtils::percentile, castDouble( visit( ctx.a1().expr() ) ), ctx );

            case RANK_HIGH:
                return rankHigh( getDoubles( ctx ), ctx );

            case RANK_LOW:
                return rankLow( getDoubles( ctx ), ctx );

            case RANK_PERCENTILE:
                double[] vals = getDoubles( ctx );
                return vals.length == 0 ? 0 : (int)Math.round( 100.0 * rankHigh( vals, ctx ) / vals.length );

            // -----------------------------------------------------------------
            // Aggregation scope functions
            // -----------------------------------------------------------------

            case PERIOD:
                return period( ctx );

            case OU_ANCESTOR:
                return ouAncestor( ctx );

            case OU_DESCENDANT:
                return ouDescendant( ctx );

            case OU_LEVEL:
                return ouLevel( ctx );

            case OU_PEER:
                return ouPeer( ctx );

            case OU_GROUP:
                return ouGroup( ctx );

            case OU_DATA_SET:
                return ouDataSet( ctx );

            default: // (Shouldn't happen, mismatch between expression grammer and here.)
                throw new ParsingException( "fun=" + ctx.fun.getType() + " not recognized." );
        }
    }

    // -------------------------------------------------------------------------
    // Iterate through periods
    // -------------------------------------------------------------------------

    /**
     * Iterates through periods according to arguments. There must be at least
     * one argument and there may be any greater number. The arguments are:
     * <ul>
     *     <li>1. The number of periods before (negative) or after (positive)
     *     the current period. If there is only one argument, then only a
     *     single value is returned, not multiple values</li>
     *     <li>2. If a second argument is present, the first argument is the
     *     offset (negative for past, positive for future) for the start of
     *     a range of periods, and the second argument is the offset for the
     *     end of the period range. Multiple values are returned and must be
     *     processed by an aggregtion funciton (even if the start and end
     *     offsets are the same.) </li>
     *     <li>3. If a third argument is present, it is an offset in years
     *     (negative for past, positive for future). The period(s) specified by
     *     the first two arguments are shifted this number of years into the
     *     past or the future.</li>
     *     <li>4. If a fourth argument is present, the third and forth
     *     arguments define the offsets for the start and end of a range of
     *     years in which the periods from the first two arguments are
     *     evaluated.</li>
     *     <li>5+. If more than four arguments are present, each subsequent
     *     four arguments specify an additional range of periods to evaluate
     *     in addition to the range specified by arguments 1-4.</li>
     * </ul>
     * @param ctx the parsing context.
     * @return a single value (1 argument) or multiple values (>1 argument).
     */
    private Object period( ExprContext ctx )
    {
        Period savedPeriod = currentPeriod;

        Object returnValue;

        int argumentCount = ctx.a1_n().expr().size();

        if ( argumentCount == 1 ) // Single period shift returns single value.
        {
            int periodShift = evalIntDefault( ctx.a1_n().expr( 0 ), 0 );

            if ( savedPeriod == null ) // No period, syntax check only.
            {
                return visit( ctx.expr( 0 ) );
            }

            currentPeriod = getPreviousOrNextPeriod( savedPeriod, periodShift );

            returnValue = visit( ctx.expr( 0 ) );
        }
        else
        {
            MultiValues values = new MultiValues();

            for ( int i = 0; i < argumentCount; i += 4 )
            {
                int periodShiftFrom = evalIntDefault( ctx.a1_n().expr( i ), 0 );
                int periodShiftTo = evalIntDefault( ctx.a1_n().expr( i + 1 ), periodShiftFrom );
                int yearShiftFrom = evalIntDefault( ctx.a1_n().expr( i + 2 ), 0 );
                int yearShiftTo = evalIntDefault( ctx.a1_n().expr( i + 3 ), yearShiftFrom );

                if ( savedPeriod == null ) // No period, syntax check only.
                {
                    values.addPeriodValue( currentPeriod, visit( ctx.expr( 0 ) ) );
                }
                else
                {
                    PeriodType periodType = savedPeriod.getPeriodType();

                    for ( int yearShift = yearShiftFrom; yearShift <= yearShiftTo; yearShift++ )
                    {
                        Period yearShiftPeriod = periodType.getPreviousYearsPeriod( savedPeriod, -yearShift );

                        for ( int periodShift = periodShiftFrom; periodShift <= periodShiftTo; periodShift++ )
                        {
                            currentPeriod = getPreviousOrNextPeriod( yearShiftPeriod, periodShift );

                            values.addPeriodValue( currentPeriod, visit( ctx.expr( 0 ) ) );
                        }
                    }
                }
            }

            returnValue = values;
        }

        currentPeriod = savedPeriod;

        return returnValue;
    }

    private Period getPreviousOrNextPeriod( Period period, int periodShift )
    {
        return periodShift < 0
            ? period.getPeriodType().getPreviousPeriod( period, -periodShift )
            : period.getPeriodType().getNextPeriod( period, periodShift );
    }

    // -------------------------------------------------------------------------
    // Iterate through organistion units
    // -------------------------------------------------------------------------

    /**
     * Returns the expression value, evaluated at the orgUnit's ancestor.
     * <p/>
     * If a multi-value function is chained to our left, returns multiple
     * values. Otherwise returns a single value.
     *
     * @param ctx the parsing context, containing the ancestor level
     *            (1=parent, 2=grandparent, etc.)
     * @return the value from the ancestor.
     */
    private Object ouAncestor( ExprContext ctx )
    {
        int parentLevels = castInteger( visit( ctx.a1().expr() ) );

        if ( currentOrgUnit == null )
        {
            return visit( ctx.expr( 0 ) );
        }

        OrganisationUnit savedOrgUnit = currentOrgUnit;

        for ( int i = 0; i < parentLevels && currentOrgUnit.getParent() != null; i++ )
        {
            currentOrgUnit = currentOrgUnit.getParent();
        }

        Object value = visit( ctx.expr( 0 ) );

        currentOrgUnit = savedOrgUnit;

        return value;
    }

    /**
     * Returns the expression value, evaluated at the orgUnit's descendants.
     *
     * @param ctx the parsing context, containing the descendant level(s)
     *            (1=children, 2=grandchildren, etc.)
     * @return the single value from the ancestor.
     */
    private Object ouDescendant( ExprContext ctx )
    {
        Set<Integer> levels = new HashSet<>();

        for ( int i = 0; i < ctx.a1_n().expr().size(); i++ )
        {
            levels.add( castInteger( visit( ctx.a1_n().expr( i ) ) ) );
        }

        if ( currentOrgUnit == null )
        {
            return visit( ctx.expr( 0 ) );
        }

        Set<OrganisationUnit> descendants = new HashSet<>();

        for ( Integer i : levels )
        {
            descendants.addAll ( organisationUnitService.getOrganisationUnitsAtLevel( currentOrgUnit.getLevel() + i, currentOrgUnit ) );
        }

        return ouVisitOrFilter( ctx, descendants );

    }

    /**
     * Interates over all orgUnits in the system at given level(s).
     *
     * @param ctx the parsing context, containing the orgUnit level.
     * @return the multiple values from the orgUnits.
     */
    private Object ouLevel( ExprContext ctx )
    {
        Set<Integer> levels = new HashSet<>();

        for ( int i = 0; i < ctx.a1_n().expr().size(); i++ )
        {
            levels.add( castInteger( visit( ctx.a1_n().expr( i ) ) ) );
        }

        if ( currentOrgUnit == null )
        {
            return visit( ctx.expr( 0 ) );
        }

        Set<OrganisationUnit> orgUnitsAtLevels = new HashSet<>();

        for ( Integer i : levels )
        {
            orgUnitsAtLevels.addAll ( organisationUnitService.getOrganisationUnitsAtLevel( i ) );
        }

        return ouVisitOrFilter( ctx, orgUnitsAtLevels );
    }

    /**
     * Interates over the current orgUnit's peers.
     * TODO: Update JavaDoc for only 1 argument.
     * <ol>
     *     <li>The number of times removed from the current orgUnit
     *     (0=self, 1=siblings, 2=cousins but not siblings, etc.)</li>
     *     <li>If two arguments are present, the two are a starting
     *     and ending range of times removed. For example, (0,1) is
     *     self plus all siblings (all the parent's children), and (0,2)
     *     is self, all siblings and all cousins (all the grandparent's
     *     grandchildren).</li>
     * </ol>
     *
     * @param ctx the parsing context, one or two arguments.
     * @return the multiple values from the orgUnits.
     */
    protected Object ouPeer( ExprContext ctx )
    {
        if ( currentOrgUnit == null )
        {
            return visit( ctx.expr( 0 ) );
        }

        OrganisationUnit ancestor = getOuAncestor( castInteger( visit( ctx.a1().expr() ) ) );

        List<OrganisationUnit> orgUnits = organisationUnitService.getOrganisationUnitsAtLevel( currentOrgUnit.getLevel(), ancestor );

        return ouVisitOrFilter( ctx, new HashSet<OrganisationUnit>( orgUnits ) );
    }

    /**
     * Iterates over one or more orgUnit groups.
     *
     * @param ctx the parsing context, with the group(s) to iterate over.
     * @return the multiple values from the orgUnits.
     */
    protected Object ouGroup( ExprContext ctx )
    {
        Set<OrganisationUnit> orgUnitsInGroups = new HashSet<>();

        for ( int i = 0; i < ctx.a1_n().expr().size(); i++ )
        {
            String groupName = castString( visit( ctx.a1_n().expr( i ) ) );

            OrganisationUnitGroup group = getIdentifiableObject( OrganisationUnitGroup.class, groupName );

            if ( group == null )
            {
                throw new ParsingException( "Can't find organisation unit group '" + groupName + "'" );
            }

            orgUnitsInGroups.addAll( group.getMembers() );
        }

        if ( currentOrgUnit == null )
        {
            return visit( ctx.expr( 0 ) );
        }

        return ouVisitOrFilter( ctx, new HashSet<OrganisationUnit>( orgUnitsInGroups ) );
    }

    /**
     * Iterates over one or more orgUnits to which a data set has been assigned.
     *
     * @param ctx the parsing context, with the group(s) to iterate over.
     * @return the multiple values from the orgUnits.
     */
    protected Object ouDataSet( ExprContext ctx )
    {
        Set<OrganisationUnit> orgUnitsInDataSets = new HashSet<>();

        for ( int i = 0; i < ctx.a1_n().expr().size(); i++ )
        {
            String dataSetName = castString( visit( ctx.a1_n().expr( i ) ) );

            DataSet dataSet = getIdentifiableObject( DataSet.class, dataSetName );

            if ( dataSet == null )
            {
                throw new ParsingException( "Can't find data set '" + dataSetName + "'" );
            }

            orgUnitsInDataSets.addAll( dataSet.getSources() );
        }

        if ( currentOrgUnit == null )
        {
            return visit( ctx.expr( 0 ) );
        }

        return ouVisitOrFilter( ctx, new HashSet<OrganisationUnit>( orgUnitsInDataSets ) );
    }

    private OrganisationUnit getOuAncestor( int levels )
    {
        OrganisationUnit orgUnit = currentOrgUnit;

        for ( int i = 0; i < levels && orgUnit.getParent() != null; i++ )
        {
            orgUnit = orgUnit.getParent();
        }

        return orgUnit;
    }

    private <T extends IdentifiableObject> T getIdentifiableObject( Class<T> clazz, String identifier )
    {
        T identifiableObject = manager.get( clazz, identifier );

        if ( identifiableObject != null )
        {
            return identifiableObject;
        }

        identifiableObject = manager.getByCode( clazz, identifier );

        if ( identifiableObject != null )
        {
            return identifiableObject;
        }

        return manager.getByName( clazz, identifier );
    }

    private Object ouVisitOrFilter( ExprContext ctx, Set<OrganisationUnit> orgUnits )
    {
        if ( filterOrgUnits )
        {
            if ( orgUnits.contains( currentOrgUnit ) )
            {
                return visit( ctx.expr( 0 ) );
            }
            else
            {
                return null;
            }
        }

        OrganisationUnit savedOrgUnit = currentOrgUnit;

        MultiValues values = new MultiValues();

        filterOrgUnits = true;

        for ( OrganisationUnit orgUnit : orgUnits )
        {
            currentOrgUnit = orgUnit;

            values.addValue( visit( ctx.expr( 0 ) ) );
        }

        filterOrgUnits = false;

        currentOrgUnit = savedOrgUnit;

        return values;
    }

    // -------------------------------------------------------------------------
    // Other supportive functions
    // -------------------------------------------------------------------------

    private Object aggregate( Function<double[], Double> func, ExprContext ctx )
    {
        double[] doubles = getDoubles( ctx );

        if ( doubles.length == 0 )
        {
            return null;
        }

        return func.apply( doubles );
    }

    private Object aggregate2( BiFunction<double[], Double, Double> func, Double arg2, ExprContext ctx )
    {
        double[] doubles = getDoubles( ctx );

        if ( doubles.length == 0 )
        {
            return null;
        }

        return func.apply( doubles, arg2 );
    }

    /**
     * Returns the high rank of the argument within the set of multiple values.
     * n is the rank of the highest value, where n is the number of values.
     * <p/>
     * For example, if the multiple values are 60, 50, 50, 40, then the rankHigh
     * value of 60, 50, 50, or 40 would be 4, 3, 3, or 1, respectively.
     *
     * @param values the values to rank amongst.
     * @param ctx parsing context with a single argument of the value to rank.
     * @return the rank of the argument within the multiple values.
     */
    private Integer rankHigh( double[] values, ExprContext ctx )
    {
        Double test = castDouble( visit( ctx.a1().expr() ) );

        if ( test == null || values.length == 0 )
        {
            return null;
        }

        Integer rankHigh = 0;

        for ( double d : values )
        {
            if ( d <= test )
            {
                rankHigh++;
            }
        }

        return rankHigh;
    }

    /**
     * Returns the low rank of the argument within the set of multiple values.
     * 1 is the rank of the highest value.
     * <p/>
     * For example, if the multiple values are 60, 50, 50, 40, then the rankLow
     * value of 60, 50, 50, or 40 would be 1, 2, 2, or 4, respectively.
     *
     * @param values the values to rank amongst.
     * @param ctx parsing context with a single argument of the value to rank.
     * @return the rank of the argument within the multiple values.
     */
    private Integer rankLow( double[] values, ExprContext ctx )
    {
        Double test = castDouble( visit( ctx.a1().expr() ) );

        if ( test == null || values.length == 0 )
        {
            return null;
        }

        Integer rankLow = 1;

        for ( double d : values )
        {
            if ( d > test )
            {
                rankLow++;
            }
        }

        return rankLow;
    }

    /**
     * Casts object as Integer, or throw exception if we can't.
     * <p/>
     * The object must not be null.
     *
     * @param object the value to cast as an Integer.
     * @return Integer value.
     */
    protected Integer castInteger( Object object )
    {
        Double d = castDouble( object );

        if ( d == null )
        {
            throw new ParsingException( "integer value missing" );
        }

        Integer i = (int) (double) d;

        if ( (double) d != i )
        {
            throw new ParsingException( "integer expected at: '" + object.toString() + "'" );
        }

        return i;
    }

    /**
     * Casts object as Double, or throw exception if we can't.
     * <p/>
     * If the object is null, return null.
     *
     * @param object the value to cast as a Double.
     * @return Double value.
     */
    protected Double castDouble( Object object )
    {
        if ( object == null )
        {
            return null;
        }

        try
        {
            if ( object.getClass() == String.class )
            {
                return Double.valueOf( (String) object );
            }

            return (Double) object;
        }
        catch ( Exception ex )
        {
            throw new ParsingException( "number expected at: '" + object.toString() + "'" );
        }
    }

    /**
     * Casts object as Boolean, or throw exception if we can't.
     *
     * @param object the value to cast as a Boolean.
     * @return Boolean value.
     */
    protected Boolean castBoolean( Object object )
    {
        if ( object == null )
        {
            return null;
        }

        try
        {
            return (Boolean) object;
        }
        catch ( Exception ex )
        {
            throw new ParsingException( "boolean expected at: '" + object.toString() + "'" );
        }
    }

    /**
     * Casts object as String, or throw exception if we can't.
     *
     * @param object the value to cast as a String.
     * @return String value.
     */
    protected String castString( Object object )
    {
        if ( object == null )
        {
            return null;
        }

        try
        {
            return (String) object;
        }
        catch ( Exception ex )
        {
            throw new ParsingException( "string expected at: '" + object.toString() + "'" );
        }
    }

    /**
     * Casts object as Multivalues, or throws exception if we can't.
     *
     * @param object the value to cast as a Multivalues.
     * @return Multivalues object.
     */
    protected MultiValues castMultiValues( Object object )
    {
        if ( ! ( object instanceof MultiValues ) )
        {
            throw new ParsingException( "multiple values expected at: '" +
                ( object == null ? "" : object.toString() ) + "'" );
        }

        return (MultiValues) object;
    }

    /**
     * Casts object as MultiValues with periods, or throws exception if we can't.
     *
     * @param object the value to cast as a MultiValues.
     * @return MultiValues object with periods.
     */
    protected MultiValues castMultiPeriodvalues( Object object )
    {
        MultiValues multiValues = castMultiValues( object );

        if ( ! multiValues.hasPeriods() )
        {
            throw new ParsingException( "multiple period values expected at: '" +
                ( object == null ? "" : object.toString() ) + "'" );
        }
        return multiValues;
    }

    /**
     * Gets from the object a Double array
     */
    protected Double[] castDoubleArray( Object object )
    {
        return castMultiValues( object ).getValues().stream()
            .map( v -> castDouble( v ) ).collect( Collectors.toList() ).toArray( new Double[0] );
    }

    private int evalIntDefault( ExprContext ctx, int defaultValue )
    {
        if ( ctx == null )
        {
            return defaultValue;
        }
        else
        {
            return coalesce( castInteger( visit( ctx ) ), defaultValue );
        }
    }

    private Integer coalesce( Integer... integers )
    {
        for ( Integer i : integers )
        {
            if ( i != null )
            {
                return i;
            }
        }

        return null;
    }

    /**
     * Returns an array of double values for aggregate function processing.
     *
     * @param ctx the parsing context.
     * @return the array of double values.
     */
    private double[] getDoubles( ExprContext ctx )
    {
        return ArrayUtils.toPrimitive( castMultiValues( visit( ctx.expr( 0 ) ) )
            .getValues().stream()
            .filter( o -> o != null )
            .map( o -> castDouble( o ) ).collect( Collectors.toList() )
            .toArray( new Double[0] ) );
    }

    private List<Object> evalAll( ExprContext ctx )
    {
        return evalAll( ctx, 1 );
    }

    private List<Object> evalAll( ExprContext ctx, int periodOrder )
    {
        int limit = 0; // Unlimited

        //TODO: iterate through periods and orgUnits in scope

        return Arrays.asList( visit( ctx.expr( 0 ) ) );
    }

    private Object firstOrLast( ExprContext ctx, boolean isFirst )
    {
        MultiValues multiValues = castMultiPeriodvalues( visit( ctx.expr( 0 ) ) );

        if ( ctx.a0_1().expr() == null )
        {
            List<Object> values = multiValues.firstOrLast( 1, isFirst ).getValues();

            return values.isEmpty() ? null : values.get( 0 );
        }
        else
        {
            return multiValues.firstOrLast( castInteger( visit( ctx.a0_1().expr() ) ), isFirst );
        }
    }

    private int compare( ExprContext ctx )
    {
        Object o1 = visit( ctx.expr( 0 ) );
        Object o2 = visit( ctx.expr( 1 ) );

        if ( o1.getClass() == Double.class )
        {
            return ( (Double) o1).compareTo( castDouble( o2 ) );
        }
        else if ( o1.getClass() == String.class )
        {
            return ( (String) o1).compareTo( castString( o2 ) );
        }
        else if ( o1.getClass() == Boolean.class )
        {
            return ( (Boolean) o1).compareTo( castBoolean( o2 ) );
        }
        else // (Shouldn't happen)
        {
            throw new ParsingException( "magnitude of " + o1.getClass().getSimpleName() + " cannot be compared at: '" + o2.toString() + "'" );
        }
    }
}
