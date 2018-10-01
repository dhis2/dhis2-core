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
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.parsing.generated.ExpressionBaseVisitor;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.springframework.beans.factory.annotation.Autowired;

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
public abstract class ExpressionVisitor extends ExpressionBaseVisitor<Object>
{
    @Autowired
    protected OrganisationUnitService organisationUnitService;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected ConstantService constantService;

    protected OrganisationUnit currentOrgUnit = null;

    protected Period currentPeriod = null;

    protected AggregationType currentAggregationType = null;

    protected Map<String, Double> constantMap = null;

    protected Map<String, Integer> orgUnitCountMap = null;

    protected Double days = DUMMY_VALUE;

    protected Map<String, String> itemDescriptions = null;

    /**
     * Dummy values to use when the value is not yet known.
     */
    protected final static int DUMMY_INT = 1;

    protected final static Double DUMMY_VALUE = 1.0;

    protected final static Period DUMMY_PERIOD = ( new MonthlyPeriodType() ).createPeriod( "20010101" );

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
        if ( ctx.fun != null ) // Invoke a function
        {
            return function( ctx );
        }
        else if ( ctx.expr( 0 ) != null ) // Pass through the expression
        {
            return visit( ctx.expr( 0 ) );
        }
        else // Visit the type of expression defined
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
        String constantId = ctx.constantId().getText();

        Double value = constantMap.get( constantId );

        if ( value == null )
        {
            throw new ParsingException( "No constant defined for " + constantId );
        }

        if ( itemDescriptions != null )
        {
            Constant constant = constantService.getConstant( constantId );

            itemDescriptions.put( ctx.getText(), constant.getDisplayName() );
        }

        return value;
    }

    @Override
    public final Object visitDays( DaysContext ctx )
    {
        return days;
    };

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

    // -------------------------------------------------------------------------
    // Subclasses must implement the following logical methods.
    //
    // When getting ExpresssionItems and description, visit all the items
    // beause we don't know what the data values will be. Assume that all
    // values will be dummy and no values will be null.
    //
    // When finding the expression value based on data values, only evaluate
    // those exprssions we need to. This not only saves time but is important
    // when implementing skip strategy. For example, if a value is explicitly
    // tested to see if it is null, then it doesn't apply against the skip
    // strategy if it is missing. Also, assume that any value returned might
    // be null, and protect yourself accordingly.
    // -------------------------------------------------------------------------

    /**
     * Finds the logical AND of two boolean expressions.
     *
     * @param ctx the parsing context.
     * @return the logical AND.
     */
    protected abstract Object functionAnd( ExprContext ctx );

    /**
     * Finds the logical OR of two boolean expressions.
     *
     * @param ctx the parsing context.
     * @return the logical OR.
     */
    protected abstract Object functionOr( ExprContext ctx );

    /**
     * If the test expression is true, returns the first expression value,
     * else returns the second expression value.
     *
     * @param ctx the parsing context.
     * @return the first or second expression value, depending on the test.
     */
    protected abstract Object functionIf( ExprContext ctx );

    /**
     * If the test expression is true, skips this value (returns null).
     *
     * @param ctx the parsing context.
     * @return the expression, or null, depending on the test.
     */
    protected abstract Object functionExcept( ExprContext ctx );

    /**
     * Returns the first non-null argument.
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
                if ( ctx.expr().size() > 1 ) // Subtract operator
                {
                    return double2( ctx, (Double a, Double b) -> a - b );
                }
                else // Unary Negative operator
                {
                    return double1( ctx, (Double a) -> - a );
                }

            case PLUS: // String concatenation or numeric addition
                Object arg1 = visit( ctx.expr( 0 ) );
                Object arg2 = visit( ctx.expr( 1 ) );

                if ( arg1 instanceof String )
                {
                    return arg2 == null ? null : (String) arg1 + castString( arg2 );
                }

                return double2( castDouble( arg1 ), castDouble( arg2 ), (Double a, Double b) -> a + b );

            case POWER:
                return double2( ctx, (Double a, Double b) -> pow(a, b) );

            case MUL:
                return double2( ctx, (Double a, Double b) -> a * b );

            case DIV:
                return double2( ctx, (Double a, Double b) -> a / b );

            case MOD:
                return double2( ctx, (Double a, Double b) -> a % b );

            // -----------------------------------------------------------------
            // Logical Operators (return Boolean)
            // -----------------------------------------------------------------

            case NOT:
                return boolean1( ctx, (Boolean a) -> ! a );

            case LEQ:
                return compare( ctx, (Integer a) -> a <= 0 );

            case GEQ:
                return compare( ctx, (Integer a) -> a >= 0 );

            case LT:
                return compare( ctx, (Integer a) -> a < 0 );

            case GT:
                return compare( ctx, (Integer a) -> a > 0 );

            case EQ:
                return compare( ctx, (Integer a) -> a == 0 );

            case NE:
                return compare( ctx, (Integer a) -> a != 0 );

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
                return visit( ctx.a1().expr() ) == null;

            case COALESCE:
                return functionCoalesce( ctx );

            // -----------------------------------------------------------------
            // Aggregation functions
            // -----------------------------------------------------------------

            case FIRST:
                return firstOrLast( null, ctx, true );

            case LAST:
                return firstOrLast( AggregationType.LAST, ctx, false );

            case COUNT:
                return aggregate( (double[] a) -> (double) a.length, AggregationType.COUNT, ctx );

            case SUM:
                return aggregate( StatUtils::sum, AggregationType.SUM, ctx );

            case MAX:
                return aggregate( StatUtils::max, AggregationType.MAX,  ctx );

            case MIN:
                return aggregate( StatUtils::min, AggregationType.MIN, ctx );

            case AVERAGE:
                return aggregate( StatUtils::mean, AggregationType.AVERAGE, ctx );

            case STDDEV:
                return aggregate( (new StandardDeviation())::evaluate, AggregationType.STDDEV, ctx );

            case VARIANCE:
                return aggregate( StatUtils::variance, AggregationType.VARIANCE, ctx );

            case MEDIAN:
                return aggregate2( StatUtils::percentile, 50.0, ctx );

            case PERCENTILE:
                return aggregate2( StatUtils::percentile, castDouble( visit( ctx.a1().expr() ) ), ctx );

            case RANK_HIGH:
                return rankHigh( getDoubles( null, ctx ), ctx );

            case RANK_LOW:
                return rankLow( getDoubles( null, ctx ), ctx );

            case RANK_PERCENTILE:
                double[] vals = getDoubles( null, ctx );
                return vals.length == 0 ? null : (double) Math.round( 100.0 * rankHigh( vals, ctx ) / vals.length );

            case AVERAGE_SUM_ORG_UNIT:
                return visitAggType( AggregationType.AVERAGE_SUM_ORG_UNIT, ctx );

            case LAST_AVERAGE_ORG_UNIT:
                return visitAggType( AggregationType.LAST_AVERAGE_ORG_UNIT, ctx );

            case NO_AGGREGATION:
                return visitAggType( AggregationType.NONE, ctx );

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
                    values.addPeriodValue( DUMMY_PERIOD, visit( ctx.expr( 0 ) ) );
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

    /**
     * Gets previous periods (negative shift value) or next periods (positive
     * shift value)
     *
     * @param period starting period
     * @param periodShift how many periods to shift
     * @return the shifted period
     */
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
     * @return the single value from the ancestor.
     */
    private Object ouAncestor( ExprContext ctx )
    {
        int parentLevels = castNonNullInteger( visit( ctx.a1().expr() ) );

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
     * @param ctx the parsing context, containing the descendant level
     *            (1=children, 2=grandchildren, etc.)
     * @return the single value from the ancestor.
     */
    private Object ouDescendant( ExprContext ctx )
    {
        int level = castNonNullInteger( visit( ctx.a1().expr() ) );

        if ( currentOrgUnit == null )
        {
            return ouVisitOrFilter( ctx, new HashSet<>() );
        }

        List<OrganisationUnit> descendants = organisationUnitService.getOrganisationUnitsAtLevel( currentOrgUnit.getLevel() + level, currentOrgUnit );

        return ouVisitOrFilter( ctx, new HashSet<>( descendants ) );
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
            levels.add( castNonNullInteger( visit( ctx.a1_n().expr( i ) ) ) );
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
     * <p/>
     * Expects one argument which is the number of times removed from the
     * current orgUnit (0=self, 1=self and siblings, 2=self and siblings
     * and cousins, etc.)
     *
     * @param ctx the parsing context.
     * @return the multiple values from the orgUnits.
     */
    protected Object ouPeer( ExprContext ctx )
    {
        if ( currentOrgUnit == null )
        {
            return ouVisitOrFilter( ctx, new HashSet<>() );
        }

        int ancestorLevel = castNonNullInteger( visit( ctx.a1().expr() ) );

        OrganisationUnit ancestor = getOuAncestor( ancestorLevel );

        List<OrganisationUnit> orgUnits = organisationUnitService.getOrganisationUnitsAtLevel( currentOrgUnit.getLevel(), ancestor );

        return ouVisitOrFilter( ctx, new HashSet<OrganisationUnit>( orgUnits ) );
    }

    /**
     * Iterates over one or more organisation unit groups.
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

        return ouVisitOrFilter( ctx, new HashSet<OrganisationUnit>( orgUnitsInGroups ) );
    }

    /**
     * Iterates over organisation units assigned to one or more data sets.
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

        return ouVisitOrFilter( ctx, new HashSet<OrganisationUnit>( orgUnitsInDataSets ) );
    }

    /**
     * Moves up in the organisation unit hierchy a specified number of times.
     *
     * @param levels The number of levels to move up.
     * @return The nth ancestor (or root if n is too high).
     */
    private OrganisationUnit getOuAncestor( int levels )
    {
        OrganisationUnit orgUnit = currentOrgUnit;

        for ( int i = 0; i < levels && orgUnit.getParent() != null; i++ )
        {
            orgUnit = orgUnit.getParent();
        }

        return orgUnit;
    }

    /**
     * Looks up an identifiable object first by UID, then code, then name.
     *
     * @param clazz the class of the identifiable object
     * @param identifier the identifier to look up
     * @return the object
     */
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

    /**
     * If not iterating through a higher set of organisation units, then
     * iterate through the given set.
     * <p/>
     * If already iterating through a set of organisation units at a higher
     * level, then just pass through this call if the organisation unit is
     * in the given set, else return null. This has the effect of visiting
     * only those organisation units that are in the intersection of the
     * higher-level set and the current set.
     *
     * @param ctx the parting context
     * @param orgUnits
     * @return
     */
    private Object ouVisitOrFilter( ExprContext ctx, Set<OrganisationUnit> orgUnits )
    {
        if ( filterOrgUnits )
        {
            if ( currentOrgUnit == null || orgUnits.contains( currentOrgUnit ) )
            {
                return visit( ctx.expr( 0 ) );
            }
            else
            {
                return null;
            }
        }

        MultiValues values = new MultiValues();

        if ( currentOrgUnit == null )
        {
            values.addValue( visit( ctx.expr( 0 ) ) );

            return values;
        }

        OrganisationUnit savedOrgUnit = currentOrgUnit;

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
    // Aggregation functions
    // -------------------------------------------------------------------------

    /**
     * Applies an aggregation function, or returns null if there are no inputs.
     *
     * @param func the function to apply.
     * @param aggType the aggregation type, if any, to set for values below.
     * @param ctx the parsing context.
     * @return the function value, or null if no inputs.
     */
    private Object aggregate( Function<double[], Double> func, AggregationType aggType, ExprContext ctx )
    {
        double[] doubles = getDoubles( aggType, ctx );

        if ( doubles.length == 0 )
        {
            return null;
        }

        return func.apply( doubles );
    }

    /**
     * Applies an aggreegation function that takes a second argument, or returns null
     * if there are no inputs or no second argument.
     *
     * @param func the function to apply.
     * @param arg2 the second argument to the aggregation function.
     * @param ctx the parsing context.
     * @return the function value, or null if no inputs or no second argument.
     */
    private Object aggregate2( BiFunction<double[], Double, Double> func, Double arg2, ExprContext ctx )
    {
        double[] doubles = getDoubles( null, ctx );

        if ( doubles.length == 0 || arg2 == null )
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
    private Double rankHigh( double[] values, ExprContext ctx )
    {
        Double test = castDouble( visit( ctx.a1().expr() ) );

        if ( test == null || values.length == 0 )
        {
            return null;
        }

        Double rankHigh = 0.0;

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
    private Double rankLow( double[] values, ExprContext ctx )
    {
        Double test = castDouble( visit( ctx.a1().expr() ) );

        if ( test == null || values.length == 0 )
        {
            return null;
        }

        Double rankLow = 1.0;

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
     * Processes the first or last aggregation functions. Must be used on a
     * set of multiple values iterated over periods. Without an argument,
     * returns a single value of either the first or last value. With an
     * argument, returns the first or last n values as a multi-value set.
     *
     * @param aggType aggregation type, if any, to set for data values.
     * @param ctx parsing context with zero or one arguments.
     * @param isFirst whether this is first (or last).
     * @return the first or last value(s).
     */
    private Object firstOrLast( AggregationType aggType, ExprContext ctx, boolean isFirst )
    {
        MultiValues multiValues = castMultiPeriodvalues( visitAggType( aggType, ctx ) );

        if ( ctx.a0_1().expr() == null )
        {
            List<Object> values = multiValues.firstOrLast( 1, isFirst ).getValues();

            return values.isEmpty() ? null : values.get( 0 );
        }
        else
        {
            return multiValues.firstOrLast( castNonNullInteger( visit( ctx.a0_1().expr() ) ), isFirst );
        }
    }

    /**
     * Returns an array of double values for aggregate function processing.
     *
     * @param ctx the parsing context.
     * @return the array of double values.
     */
    private double[] getDoubles( AggregationType aggType, ExprContext ctx )
    {
        return ArrayUtils.toPrimitive( castMultiValues( visitAggType( aggType, ctx ) )
            .getValues().stream()
            .filter( o -> o != null )
            .map( o -> castDouble( o ) ).collect( Collectors.toList() )
            .toArray( new Double[0] ) );
    }

    /**
     * Visits the tree nodes below with the specified aggregation type.
     *
     * @param aggType the aggregation type to use for lower-level nodes.
     * @param ctx the parsing context.
     * @return the value from the tree below.
     */
    private Object visitAggType( AggregationType aggType, ExprContext ctx )
    {
        AggregationType savedAggregationType = currentAggregationType;

        currentAggregationType = aggType;

        Object object = visit( ctx.expr( 0 ) );

        currentAggregationType = savedAggregationType;

        return object;
    }

    // -------------------------------------------------------------------------
    // Other supportive functions
    // -------------------------------------------------------------------------

    /**
     * Evaluates a one-argument double function, but returns null if the
     * input value is null.
     *
     * @param ctx the parsing context with one expression.
     * @param fn the function to evaluate.
     * @return the function result, or null if input was null.
     */
    protected Double double1( ExprContext ctx, Function<Double, Double> fn )
    {
        Double d1 = castDouble( visit( ctx.expr( 0 ) ) );

        return d1 == null
            ? null
            : fn.apply( d1 );
    }

    /**
     * Evaluates a two-argument double function, but returns null if either of
     * the input values is null.
     *
     * @param ctx the parsing context with two expressions.
     * @param fn the function to evaluate.
     * @return the function result, or null if either input was null.
     */
    protected Double double2( ExprContext ctx, BiFunction<Double, Double, Double> fn )
    {
        Double d1 = castDouble( visit( ctx.expr( 0 ) ) );
        Double d2 = castDouble( visit( ctx.expr( 1 ) ) );

        return double2( d1, d2, fn);
    }

    /**
     * Evaluates a two-argument double function, but returns null if either of
     * the input values is null.
     *
     * @param d1 the first function argument (could be null).
     * @param d2 the second function argument (could be null).
     * @param fn the function to evaluate.
     * @return the function result, or null if either input was null.
     */
    protected Double double2( Double d1, Double d2, BiFunction<Double, Double, Double> fn )
    {
        return d1 == null || d2 == null
            ? null
            : fn.apply( d1, d2 );
    }

    /**
     * Evaluates a two-argument boolean function, but returns null if either of
     * the input values is null.
     *
     * @param ctx the parsing context with two expressions.
     * @param fn the function to evaluate.
     * @return the function result, or null if either input was null.
     */
    protected Boolean boolean1( ExprContext ctx, Function<Boolean, Boolean> fn )
    {
        Boolean b1 = castBoolean( visit( ctx.expr( 0 ) ) );

        return b1 == null
            ? null
            : fn.apply( b1 );
    }

    /**
     * Casts object as Integer, or throws an exception if we can't.
     * @param object
     * @return
     */
    protected Integer castInteger( Object object )
    {
        Double d = castDouble( object );

        Integer i = null;

        if ( d != null )
        {
            i = (int) (double) d;

            if ( (double) d != i )
            {
                throw new ParsingException( "integer expected at: '" + object.toString() + "'" );
            }
        }

        return i;
    }

    /**
     * Casts object as Integer, or throws exception if we can't or if the
     * object is null.
     *
     * @param object the value to cast as an Integer.
     * @return Integer value.
     */
    protected Integer castNonNullInteger( Object object )
    {
        Integer i = castInteger( object );

        if ( i == null )
        {
            throw new ParsingException( "integer value missing" );
        }

        return i;
    }

    /**
     * Casts argument as Integer, but returns a default value if either the
     * argument was missing, or it evaluates to null.
     *
     * @param ctx the parsing context of the argument.
     * @param defaultValue the default value to use if null.
     * @return the integer value.
     */
    private int evalIntDefault( ExprContext ctx, int defaultValue )
    {
        if ( ctx == null )
        {
            return defaultValue;
        }
        else
        {
            Integer i = castInteger( visit( ctx ) );

            return i == null ? defaultValue : i;
        }
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

        if ( object instanceof MultiValues )
        {
            throw new ParsingException( "Multiple values must be aggregated." );
        }

        try
        {
            if ( object instanceof String )
            {
                return Double.valueOf( (String) object );
            }

            return (Double) object;
        }
        catch ( Exception ex )
        {
            throw new ParsingException( "number expected at: '" + object.toString() + "', found " + object.getClass().getName() );
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
     * Compares two Doubles, Strings or Booleans applying the given function
     * to the integer result of the comparison. Returns null if either value
     * is null.
     *
     * @param ctx the expression context with two sub-expressions.
     * @param fn the function to evaluate the compare result.
     * @return the results of the comparision.
     */
    private Boolean compare( ExprContext ctx, Function<Integer, Boolean> fn )
    {
        Object o1 = visit( ctx.expr( 0 ) );
        Object o2 = visit( ctx.expr( 1 ) );

        if ( o1 == null || o2 == null )
        {
            return null;
        }

        int compare;

        if ( o1 instanceof Double )
        {
            compare = ( (Double) o1).compareTo( castDouble( o2 ) );
        }
        else if ( o1 instanceof String )
        {
            compare = ( (String) o1).compareTo( castString( o2 ) );
        }
        else if ( o1 instanceof Boolean )
        {
            compare = ( (Boolean) o1).compareTo( castBoolean( o2 ) );
        }
        else // (Shouldn't happen)
        {
            throw new ParsingException( "magnitude of " + o1.getClass().getSimpleName() + " '" + o1.toString() +
                "' cannot be compared to: " + o2.getClass().getSimpleName() + " '" + o1.toString() + "'" );
        }

        return fn.apply( compare );
    }
}
