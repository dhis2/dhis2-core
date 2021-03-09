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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_SKIP_TEST;
import static org.hisp.dhis.parser.expression.ParserUtils.DEFAULT_SAMPLE_PERIODS;
import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsZeroAndInsignificant;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.*;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Jim Grace
 */
@Slf4j
@Service( "org.hisp.dhis.predictor.PredictionService" )
@Transactional
public class DefaultPredictionService
    implements PredictionService
{
    private final PredictorService predictorService;

    private final ConstantService constantService;

    private final ExpressionService expressionService;

    private final DataValueService dataValueService;

    private final CategoryService categoryService;

    private final OrganisationUnitService organisationUnitService;

    private final PeriodService periodService;

    private final IdentifiableObjectManager idObjectManager;

    private AnalyticsService analyticsService;

    private final Notifier notifier;

    private final BatchHandlerFactory batchHandlerFactory;

    private CurrentUserService currentUserService;

    public DefaultPredictionService( PredictorService predictorService, ConstantService constantService,
        ExpressionService expressionService, DataValueService dataValueService, CategoryService categoryService,
        OrganisationUnitService organisationUnitService, PeriodService periodService,
        IdentifiableObjectManager idObjectManager, AnalyticsService analyticsService, Notifier notifier,
        BatchHandlerFactory batchHandlerFactory, CurrentUserService currentUserService )
    {
        checkNotNull( predictorService );
        checkNotNull( constantService );
        checkNotNull( expressionService );
        checkNotNull( dataValueService );
        checkNotNull( categoryService );
        checkNotNull( periodService );
        checkNotNull( idObjectManager );
        checkNotNull( analyticsService );
        checkNotNull( notifier );
        checkNotNull( batchHandlerFactory );
        checkNotNull( currentUserService );

        this.predictorService = predictorService;
        this.constantService = constantService;
        this.expressionService = expressionService;
        this.dataValueService = dataValueService;
        this.categoryService = categoryService;
        this.organisationUnitService = organisationUnitService;
        this.periodService = periodService;
        this.idObjectManager = idObjectManager;
        this.analyticsService = analyticsService;
        this.notifier = notifier;
        this.batchHandlerFactory = batchHandlerFactory;
        this.currentUserService = currentUserService;
    }

    /**
     * Used only for testing, remove when test is refactored
     */
    @Deprecated
    public void setAnalyticsService( AnalyticsService analyticsService )
    {
        this.analyticsService = analyticsService;
    }

    /**
     * Used only for testing, remove when test is refactored
     */
    @Deprecated
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // Prediction business logic
    // -------------------------------------------------------------------------

    /**
     * String that is not an Attribute Option Combo UID. This is used for
     * holding analytics data that is not stored by AOC, and therefore is used
     * in expressions for every AOC value.
     */
    private final static String NON_AOC = "x";

    @Override
    public PredictionSummary predictJob( PredictorJobParameters params, JobConfiguration jobId )
    {
        Date startDate = DateUtils.getDateAfterAddition( new Date(), params.getRelativeStart() );
        Date endDate = DateUtils.getDateAfterAddition( new Date(), params.getRelativeEnd() );

        return predictTask( startDate, endDate, params.getPredictors(), params.getPredictorGroups(), jobId );
    }

    @Override
    public PredictionSummary predictTask( Date startDate, Date endDate,
        List<String> predictors, List<String> predictorGroups, JobConfiguration jobId )
    {
        PredictionSummary predictionSummary;

        try
        {
            notifier.notify( jobId, NotificationLevel.INFO, "Making predictions", false );

            predictionSummary = predictInternal( startDate, endDate, predictors, predictorGroups );

            notifier.update( jobId, NotificationLevel.INFO, "Prediction done", true )
                .addJobSummary( jobId, predictionSummary, PredictionSummary.class );
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );

            predictionSummary = new PredictionSummary( PredictionStatus.ERROR,
                "Predictions failed: " + ex.getMessage() );

            notifier.update( jobId, ERROR, predictionSummary.getDescription(), true );
        }

        return predictionSummary;
    }

    private PredictionSummary predictInternal( Date startDate, Date endDate, List<String> predictors,
        List<String> predictorGroups )
    {
        List<Predictor> predictorList = new ArrayList<>();

        if ( CollectionUtils.isEmpty( predictors ) && CollectionUtils.isEmpty( predictorGroups ) )
        {
            predictorList = predictorService.getAllPredictors();
        }
        else
        {
            if ( !CollectionUtils.isEmpty( predictors ) )
            {
                predictorList = idObjectManager.get( Predictor.class, predictors );
            }

            if ( !CollectionUtils.isEmpty( predictorGroups ) )
            {
                List<PredictorGroup> predictorGroupList = idObjectManager.get( PredictorGroup.class, predictorGroups );

                for ( PredictorGroup predictorGroup : predictorGroupList )
                {
                    predictorList.addAll( predictorGroup.getMembers() );
                }
            }
        }

        PredictionSummary predictionSummary = new PredictionSummary();

        log.info( "Running " + predictorList.size() + " predictors from " + startDate.toString() + " to "
            + endDate.toString() );

        for ( Predictor predictor : predictorList )
        {
            predict( predictor, startDate, endDate, predictionSummary );
        }

        log.info( "Finished predictors from " + startDate.toString() + " to " + endDate.toString() + ": "
            + predictionSummary.toString() );

        return predictionSummary;
    }

    @Override
    public void predict( Predictor predictor, Date startDate, Date endDate, PredictionSummary predictionSummary )
    {
        Expression generator = predictor.getGenerator();
        Expression skipTest = predictor.getSampleSkipTest();
        DataElement outputDataElement = predictor.getOutput();

        // Note that data is collected for the output (predicted) period based
        // on items that are not enclosed within vector functions (like sum,
        // stddev, etc.) For items that are within vector functions, and
        // for skip tests, data is collected separately for the set of sample
        // periods defined by the sequential and annual sample counts. These
        // two types of data are fetched and stored in different collections.

        Set<DimensionalItemObject> items = new HashSet<>(); // Non-sampled
        // items.
        Set<DimensionalItemObject> sampleItems = new HashSet<>(); // Sampled
        // items.
        expressionService.getExpressionDimensionalItemObjects( generator.getExpression(), PREDICTOR_EXPRESSION, items,
            sampleItems );
        if ( skipTest != null )
        {
            expressionService.getExpressionDimensionalItemObjects( skipTest.getExpression(), PREDICTOR_SKIP_TEST,
                sampleItems, new HashSet<>() );
        }
        Map<String, Constant> constantMap = constantService.getConstantMap();
        List<Period> outputPeriods = getPeriodsBetweenDates( predictor.getPeriodType(), startDate, endDate );
        Set<Period> existingOutputPeriods = getExistingPeriods( outputPeriods );
        ListMap<Period, Period> samplePeriodsMap = getSamplePeriodsMap( outputPeriods, predictor );
        Set<Period> allSamplePeriods = samplePeriodsMap.uniqueValues();
        Set<Period> existingSamplePeriods = getExistingPeriods( new ArrayList<>( allSamplePeriods ) );
        outputPeriods = periodService.reloadPeriods( outputPeriods );
        Set<Period> outputPeriodSet = new HashSet<>( outputPeriods );
        CategoryOptionCombo defaultCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        Set<String> defaultOptionComboAsSet = Sets.newHashSet( defaultCategoryOptionCombo.getUid() );
        CategoryOptionCombo outputOptionCombo = predictor.getOutputCombo() == null ? defaultCategoryOptionCombo
            : predictor.getOutputCombo();
        CachingMap<String, CategoryOptionCombo> cocMap = new CachingMap<>();
        Date now = new Date();
        boolean requireData = generator.getMissingValueStrategy() != NEVER_SKIP
            && (!items.isEmpty() || !sampleItems.isEmpty());
        DimensionalItemObject predictionReference = getPredictionReference( outputDataElement, outputOptionCombo,
            sampleItems );

        Set<OrganisationUnit> currentUserOrgUnits = new HashSet<>();
        String storedBy = "system-process";
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser != null )
        {
            currentUserOrgUnits = currentUser.getOrganisationUnits();
            storedBy = currentUser.getUsername();
        }

        predictionSummary.incrementPredictors();

        // Do separate predictor processing for each organisation unit level
        // selected. This is because at each level, predictions might be based
        // on data aggregated from all descendant org units. So to prevent
        // confusion, data for different levels are fetched independently.

        for ( OrganisationUnitLevel orgUnitLevel : predictor.getOrganisationUnitLevels() )
        {
            List<OrganisationUnit> orgUnitsAtLevel = organisationUnitService.getOrganisationUnitsAtOrgUnitLevels(
                Lists.newArrayList( orgUnitLevel ), currentUserOrgUnits );

            if ( orgUnitsAtLevel.size() == 0 )
            {
                continue;
            }

            // For performance, fetch the data from a bunch of orgUnits at once.

            List<List<OrganisationUnit>> orgUnitLists = Lists.partition( orgUnitsAtLevel, 500 );

            for ( List<OrganisationUnit> orgUnits : orgUnitLists )
            {
                Map4<OrganisationUnit, String, Period, DimensionalItemObject, Double> sampleMap4 = sampleItems.isEmpty()
                    ? new Map4<>()
                    : getDataValues( sampleItems, allSamplePeriods, existingSamplePeriods, orgUnits );

                Map4<OrganisationUnit, String, Period, DimensionalItemObject, Double> valueMap4 = items.isEmpty()
                    ? new Map4<>()
                    : getDataValues( items, outputPeriodSet, existingOutputPeriods, orgUnits );

                List<DataValue> predictions = new ArrayList<>();

                // For each org unit, find its sample data and its non-sample
                // data values.
                //
                // We will make independent predictions for each attribute
                // option combination, but some analytics data that is not
                // stored by AOC must be evaluated with every AOC found.

                for ( OrganisationUnit orgUnit : orgUnits )
                {
                    MapMapMap<String, Period, DimensionalItemObject, Double> sampleMap3 = firstNonNull(
                        sampleMap4.get( orgUnit ), new MapMapMap<>() );
                    MapMapMap<String, Period, DimensionalItemObject, Double> valueMap3 = firstNonNull(
                        valueMap4.get( orgUnit ), new MapMapMap<>() );

                    MapMap<Period, DimensionalItemObject, Double> sampleMapNonAoc = firstNonNull(
                        sampleMap3.get( NON_AOC ), new MapMap<>() );
                    MapMap<Period, DimensionalItemObject, Double> valueMapNonAoc = firstNonNull(
                        valueMap3.get( NON_AOC ), new MapMap<>() );

                    Set<String> attributeOptionCombos = getAttributeOptionCombos( sampleMap3, valueMap3,
                        defaultOptionComboAsSet );

                    // Predict independently for each AOC, adding in the data,
                    // if any, that is stored without an AOC.

                    for ( String aoc : attributeOptionCombos )
                    {
                        MapMap<Period, DimensionalItemObject, Double> sampleMap2 = firstNonNull( sampleMap3.get( aoc ),
                            new MapMap<>() );
                        MapMap<Period, DimensionalItemObject, Double> valueMap2 = firstNonNull( valueMap3.get( aoc ),
                            new MapMap<>() );

                        sampleMap2.putMap( sampleMapNonAoc );
                        valueMap2.putMap( valueMapNonAoc );

                        MapMap<Period, DimensionalItemObject, Double> periodValueMap = applySkipTest( sampleMap2,
                            skipTest, constantMap );

                        // Predict for each output period.

                        for ( Period outputPeriod : outputPeriods )
                        {
                            Map<DimensionalItemObject, Double> valueMap = firstNonNull( valueMap2.get( outputPeriod ),
                                new HashMap<>() );

                            if ( requireData
                                && dataIsAbsent( outputPeriod, valueMap, samplePeriodsMap, periodValueMap ) )
                            {
                                continue;
                            }

                            Double value = castDouble( expressionService.getExpressionValue( generator.getExpression(),
                                PREDICTOR_EXPRESSION, valueMap, constantMap, null,
                                outputPeriod.getDaysInPeriod(), generator.getMissingValueStrategy(),
                                samplePeriodsMap.get( outputPeriod ), periodValueMap ) );

                            carryPredictionForward( value, outputPeriod, predictionReference, periodValueMap );

                            if ( value != null && !value.isNaN() && !value.isInfinite() &&
                                !dataValueIsZeroAndInsignificant( Double.toString( value ), outputDataElement ) )
                            {
                                String valueString = outputDataElement.getValueType().isInteger()
                                    ? Long.toString( Math.round( value ) )
                                    : Double.toString( MathUtils.roundFraction( value, 4 ) );

                                predictions.add( new DataValue( outputDataElement,
                                    outputPeriod, orgUnit, outputOptionCombo,
                                    cocMap.get( aoc, () -> categoryService.getCategoryOptionCombo( aoc ) ),
                                    valueString, storedBy, now, null ) );
                            }
                        }
                    }
                }

                writePredictions( predictions, outputDataElement, outputOptionCombo,
                    outputPeriodSet, existingOutputPeriods, orgUnits, storedBy, predictionSummary );
            }
        }
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * For a predictor and orgUnit, determines the set of attribute option
     * combos for which predictions will be generated.
     *
     * @param sampleMap3 other-period sample data for an orgUnit
     * @param valueMap3 current-period sample data for an orgUnit
     * @param defaultOptionComboAsSet system default category option combo
     * @return set of attribute option combos to use for an orgUnit
     */
    Set<String> getAttributeOptionCombos(
        MapMapMap<String, Period, DimensionalItemObject, Double> sampleMap3,
        MapMapMap<String, Period, DimensionalItemObject, Double> valueMap3,
        Set<String> defaultOptionComboAsSet )
    {
        Set<String> attributeOptionCombos = new HashSet<>(
            Sets.union( sampleMap3.keySet(), valueMap3.keySet() ) );

        attributeOptionCombos.remove( NON_AOC );

        if ( attributeOptionCombos.isEmpty() )
        {
            return defaultOptionComboAsSet;
        }

        return attributeOptionCombos;
    }

    /**
     * Checks to see if a dimensional item object has values stored in the
     * database by attribute option combo.
     *
     * @param o dimensional item object
     * @return true if values are stored by attribuete option combo.
     */
    private boolean hasAttributeOptions( DimensionalItemObject o )
    {
        return o.getDimensionItemType() != DimensionItemType.PROGRAM_INDICATOR
            || ((ProgramIndicator) o).getAnalyticsType() != AnalyticsType.ENROLLMENT;
    }

    /**
     * Evaluates the skip test expression for any sample periods in which skip
     * test data occurs. For any combination of period and attribute option
     * combo where the skip test is true, removes all sample data with that
     * combination of period and attribute option combo.
     *
     * @param sampleMap2 data values from sampled periods.
     * @param skipTest the skip test expression.
     * @param constantMap constants to use in skip expression if needed.
     * @return the sample period map to use.
     */
    private MapMap<Period, DimensionalItemObject, Double> applySkipTest(
        MapMap<Period, DimensionalItemObject, Double> sampleMap2,
        Expression skipTest, Map<String, Constant> constantMap )
    {
        if ( skipTest == null || StringUtils.isEmpty( skipTest.getExpression() ) )
        {
            return sampleMap2;
        }

        MapMap<Period, DimensionalItemObject, Double> periodValueMap = new MapMap<>();

        for ( Period p : sampleMap2.keySet() )
        {
            if ( sampleMap2.get( p ) != null
                && Boolean.TRUE != expressionService.getExpressionValue( skipTest.getExpression(),
                    PREDICTOR_SKIP_TEST, sampleMap2.get( p ), constantMap, null,
                    p.getDaysInPeriod(), skipTest.getMissingValueStrategy(),
                    DEFAULT_SAMPLE_PERIODS, new MapMap<>() ) )
            {
                periodValueMap.put( p, sampleMap2.get( p ) );
            }
        }

        return periodValueMap;
    }

    /**
     * Returns all Periods of the specified PeriodType with start date after or
     * equal the specified start date and end date before or equal the specified
     * end date. Periods are returned in ascending date order.
     *
     * The periods returned do not need to be in the database.
     *
     * @param periodType the PeriodType.
     * @param startDate the ultimate start date.
     * @param endDate the ultimate end date.
     * @return a list of all Periods with start date after or equal the
     *         specified start date and end date before or equal the specified
     *         end date, or an empty list if no Periods match.
     */
    private List<Period> getPeriodsBetweenDates( PeriodType periodType, Date startDate, Date endDate )
    {
        List<Period> periods = new ArrayList<>();

        Period period = periodType.createPeriod( startDate );

        if ( !period.getStartDate().before( startDate ) && !period.getEndDate().after( endDate ) )
        {
            periods.add( period );
        }

        period = periodType.getNextPeriod( period );

        while ( !period.getEndDate().after( endDate ) )
        {
            periods.add( period );
            period = periodType.getNextPeriod( period );
        }

        return periods;
    }

    /**
     * Creates a map relating each output period to a list of sample periods
     * from which the sample data is to be drawn.
     *
     * @param outputPeriods the output periods
     * @param predictor the predictor
     * @return map from output periods to sample periods
     */
    private ListMap<Period, Period> getSamplePeriodsMap( List<Period> outputPeriods, Predictor predictor )
    {
        int sequentialCount = predictor.getSequentialSampleCount();
        int annualCount = predictor.getAnnualSampleCount();
        int skipCount = firstNonNull( predictor.getSequentialSkipCount(), 0 );
        PeriodType periodType = predictor.getPeriodType();

        ListMap<Period, Period> samplePeriodsMap = new ListMap<>();

        for ( Period outputPeriod : outputPeriods )
        {
            samplePeriodsMap.put( outputPeriod, new ArrayList<>() );

            Period p = periodType.getPreviousPeriod( outputPeriod, skipCount );

            for ( int i = skipCount; i < sequentialCount; i++ )
            {
                p = periodType.getPreviousPeriod( p );

                samplePeriodsMap.putValue( outputPeriod, p );
            }

            for ( int year = 1; year <= annualCount; year++ )
            {
                Period pPrev = periodType.getPreviousYearsPeriod( outputPeriod, year );
                Period pNext = pPrev;

                samplePeriodsMap.putValue( outputPeriod, pPrev );

                for ( int i = 0; i < sequentialCount; i++ )
                {
                    pPrev = periodType.getPreviousPeriod( pPrev );
                    pNext = periodType.getNextPeriod( pNext );

                    samplePeriodsMap.putValue( outputPeriod, pPrev );
                    samplePeriodsMap.putValue( outputPeriod, pNext );
                }
            }
        }
        return samplePeriodsMap;
    }

    /**
     * Finds the set of periods that exist, from a list of periods.
     *
     * Only adds the period if it is found in the database, because: (a) We will
     * need the period id, and (b) If the period does not exist in the database,
     * then there is no data in the database to look for.
     *
     * @param periods the periods to look for
     * @return the set of periods that exist, with ids.
     */
    private Set<Period> getExistingPeriods( List<Period> periods )
    {
        Set<Period> existingPeriods = new HashSet<>();

        for ( Period period : periods )
        {
            Period existingPeriod = period.getId() != 0 ? period
                : periodService.getPeriod( period.getStartDate(), period.getEndDate(), period.getPeriodType() );

            if ( existingPeriod != null )
            {
                existingPeriods.add( existingPeriod );
            }
        }
        return existingPeriods;
    }

    /**
     * Checks to see if the output predicted value should be used as input to
     * subsequent (later period) predictions. If so, returns the
     * DimensionalItemObject that should be updated with the predicted value.
     *
     * Note that we make the simplifying assumption that if the output data
     * element is sampled in an expression without a catOptionCombo, the
     * predicted data value will be used. This is usually what the user wants,
     * but would break if the expression assumes a sum of catOptionCombos
     * including the predicted value and other catOptionCombos.
     *
     * @param outputDataElement the data element to output predicted value to.
     * @param outputOptionCombo the option combo to output predicted value to.
     * @param sampleItems the sample items used in future predictions.
     * @return the DimensionalItemObject, if any, for the predicted value.
     */
    private DimensionalItemObject getPredictionReference( DataElement outputDataElement,
        CategoryOptionCombo outputOptionCombo, Set<DimensionalItemObject> sampleItems )
    {
        for ( DimensionalItemObject item : sampleItems )
        {
            if ( item == outputDataElement )
            {
                return item;
            }

            if ( item.getDimensionItemType() == DimensionItemType.DATA_ELEMENT_OPERAND
                && ((DataElementOperand) item).getDataElement() == outputDataElement
                && ((DataElementOperand) item).getCategoryOptionCombo() == outputOptionCombo )
            {
                return item;
            }
        }

        return null;
    }

    /**
     * If the predicted value might be used in a future period prediction,
     * insert it into the period value map.
     *
     * @param value the predicted value.
     * @param outputPeriod the period the value is predicted for.
     * @param predictionReference the item for the prediction, if any.
     * @param periodValueMap the period value map.
     */
    private void carryPredictionForward( Double value, Period outputPeriod,
        DimensionalItemObject predictionReference,
        MapMap<Period, DimensionalItemObject, Double> periodValueMap )
    {
        if ( value != null && predictionReference != null )
        {
            periodValueMap.putEntry( outputPeriod, predictionReference, value );
        }
    }

    /**
     * Returns true if there is no data for this period and no sample data to be
     * used for a prediction in this period. This allows us to save time by not
     * evaluating an expression where there is no data.
     *
     * @param outputPeriod the current output period.
     * @param valueMap the current output period value map.
     * @param samplePeriodsMap sample periods for each output period.
     * @param periodValueMap sample data by period.
     * @return
     */
    private boolean dataIsAbsent( Period outputPeriod, Map<DimensionalItemObject, Double> valueMap,
        ListMap<Period, Period> samplePeriodsMap, MapMap<Period, DimensionalItemObject, Double> periodValueMap )
    {
        if ( !valueMap.isEmpty() )
        {
            return false;
        }

        for ( Period p : samplePeriodsMap.get( outputPeriod ) )
        {
            Map<DimensionalItemObject, Double> periodValues = periodValueMap.get( p );

            if ( periodValues != null && !periodValues.isEmpty() )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets data values for a set of DimensionalItemObjects over a set of
     * Periods for an organisation unit and/or any of the organisation unit's
     * descendants.
     *
     * DimensionalItemObjects may reference aggregate and/or event data.
     *
     * Returns the values mapped by attribute option combo UID, then Period,
     * then DimensionalItemObject.
     *
     * @param dimensionItems the dimensionItems.
     * @param allPeriods all data Periods (to fetch event data).
     * @param existingPeriods existing data Periods (to fetch aggregate data).
     * @param orgUnits the roots of the OrganisationUnit trees to include.
     * @return the map of values
     */
    private Map4<OrganisationUnit, String, Period, DimensionalItemObject, Double> getDataValues(
        Set<DimensionalItemObject> dimensionItems, Set<Period> allPeriods, Set<Period> existingPeriods,
        List<OrganisationUnit> orgUnits )
    {
        Set<DataElement> dataElements = new HashSet<>();
        Set<DataElementOperand> dataElementOperands = new HashSet<>();
        Set<DimensionalItemObject> eventAttributeOptionObjects = new HashSet<>();
        Set<DimensionalItemObject> eventNonAttributeOptionObjects = new HashSet<>();
        Map4<OrganisationUnit, String, Period, DimensionalItemObject, Double> dataValues = new Map4<>();

        for ( DimensionalItemObject o : dimensionItems )
        {
            if ( o instanceof DataElement )
            {
                dataElements.add( (DataElement) o );
            }
            else if ( o instanceof DataElementOperand )
            {
                dataElementOperands.add( (DataElementOperand) o );
            }
            else if ( hasAttributeOptions( o ) )
            {
                eventAttributeOptionObjects.add( o );
            }
            else
            {
                eventNonAttributeOptionObjects.add( o );
            }
        }

        if ( (!dataElements.isEmpty() || !dataElementOperands.isEmpty()) && !existingPeriods.isEmpty() )
        {
            dataValues = fetchDataValues( dataElements, dataElementOperands, existingPeriods, orgUnits );
        }

        if ( !eventAttributeOptionObjects.isEmpty() && !allPeriods.isEmpty() )
        {
            dataValues.putMap( getEventDataValues( eventAttributeOptionObjects, true, allPeriods, orgUnits ) );
        }

        if ( !eventNonAttributeOptionObjects.isEmpty() && !allPeriods.isEmpty() )
        {
            dataValues.putMap( getEventDataValues( eventNonAttributeOptionObjects, false, allPeriods, orgUnits ) );
        }

        return dataValues;
    }

    private Map4<OrganisationUnit, String, Period, DimensionalItemObject, Double> fetchDataValues(
        Set<DataElement> dataElements, Set<DataElementOperand> dataElementOperands, Set<Period> periods,
        List<OrganisationUnit> orgUnits )
    {
        DataExportParams params = new DataExportParams();
        params.setDataElements( dataElements );
        params.setDataElementOperands( dataElementOperands );
        params.setPeriods( periods );
        params.setOrganisationUnits( new HashSet<>( orgUnits ) );
        params.setReturnParentOrgUnit( true );

        List<DeflatedDataValue> deflatedDataValues = dataValueService.getDeflatedDataValues( params );

        Map<Long, DataElement> dataElementLookup = dataElements.stream()
            .collect( Collectors.toMap( DataElement::getId, de -> de ) );
        Map<String, DataElementOperand> dataElementOperandLookup = dataElementOperands.stream().collect(
            Collectors.toMap( deo -> deo.getDataElement().getId() + "." + deo.getCategoryOptionCombo().getId(),
                deo -> deo ) );
        Map<Long, Period> periodLookup = periods.stream().collect( Collectors.toMap( Period::getId, p -> p ) );
        Map<Long, OrganisationUnit> orgUnitLookup = orgUnits.stream()
            .collect( Collectors.toMap( OrganisationUnit::getId, ou -> ou ) );
        Map<Long, CategoryOptionCombo> aocLookup = new HashMap<>();

        Map4<OrganisationUnit, String, Period, DimensionalItemObject, Double> dataValues = new Map4<>();

        for ( DeflatedDataValue dv : deflatedDataValues )
        {
            DataElement dataElement = dataElementLookup.get( dv.getDataElementId() );
            DataElementOperand dataElementOperand = dataElementOperandLookup
                .get( dv.getDataElementId() + "." + dv.getCategoryOptionComboId() );
            Period p = periodLookup.get( dv.getPeriodId() );
            OrganisationUnit orgUnit = orgUnitLookup.get( dv.getSourceId() );
            CategoryOptionCombo attributeOptionCombo = aocLookup.get( dv.getAttributeOptionComboId() );
            String stringValue = dv.getValue();

            if ( stringValue == null )
            {
                continue;
            }

            if ( attributeOptionCombo == null )
            {
                attributeOptionCombo = categoryService.getCategoryOptionCombo( dv.getAttributeOptionComboId() );

                aocLookup.put( dv.getAttributeOptionComboId(), attributeOptionCombo );
            }

            if ( dataElement != null )
            {
                putDataValue( dataValues, orgUnit, attributeOptionCombo, p, dataElement, stringValue );
            }

            if ( dataElementOperand != null )
            {
                putDataValue( dataValues, orgUnit, attributeOptionCombo, p, dataElementOperand, stringValue );
            }
        }

        return dataValues;
    }

    private void putDataValue( Map4<OrganisationUnit, String, Period, DimensionalItemObject, Double> dataValues,
        OrganisationUnit orgUnit, CategoryOptionCombo attributeOptionCombo, Period p,
        DimensionalItemObject dimensionItem,
        String stringValue )
    {
        Double value;

        try
        {
            value = Double.parseDouble( stringValue );
        }
        catch ( NumberFormatException e )
        {
            return; // Ignore any non-numeric values.
        }

        Double valueSoFar = dataValues.getValue( orgUnit, attributeOptionCombo.getUid(), p, dimensionItem );

        if ( valueSoFar != null )
        {
            value += valueSoFar;
        }

        dataValues.putEntry( orgUnit, attributeOptionCombo.getUid(), p, dimensionItem, value );
    }

    /**
     * Gets data values for a set of Event dimensionItems over a set of Periods
     * for a list of organisation units and/or any of the organisation units'
     * descendants.
     *
     * Returns the values mapped by OrganisationUnit, Period, attribute option
     * combo UID, and DimensionalItemObject.
     *
     * @param dimensionItems the dimensionItems.
     * @param periods the Periods of the DataValues.
     * @param orgUnits the roots of the OrganisationUnit trees to include.
     * @return the map of values
     */
    private Map4<OrganisationUnit, String, Period, DimensionalItemObject, Double> getEventDataValues(
        Set<DimensionalItemObject> dimensionItems, boolean hasAttributeOptions, Set<Period> periods,
        List<OrganisationUnit> orgUnits )
    {
        Map4<OrganisationUnit, String, Period, DimensionalItemObject, Double> eventDataValues = new Map4<>();

        DataQueryParams.Builder paramsBuilder = DataQueryParams.newBuilder()
            .withPeriods( new ArrayList<>( periods ) )
            .withDataDimensionItems( Lists.newArrayList( dimensionItems ) )
            .withOrganisationUnits( orgUnits );

        if ( hasAttributeOptions )
        {
            paramsBuilder.withAttributeOptionCombos( Lists.newArrayList() );
        }

        Grid grid = analyticsService.getAggregatedDataValues( paramsBuilder.build() );

        int peInx = grid.getIndexOfHeader( DimensionalObject.PERIOD_DIM_ID );
        int dxInx = grid.getIndexOfHeader( DimensionalObject.DATA_X_DIM_ID );
        int ouInx = grid.getIndexOfHeader( DimensionalObject.ORGUNIT_DIM_ID );
        int aoInx = hasAttributeOptions ? grid.getIndexOfHeader( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID ) : 0;
        int vlInx = grid.getWidth() - 1;

        Map<String, Period> periodLookup = periods.stream().collect( Collectors.toMap( Period::getIsoDate, p -> p ) );
        Map<String, DimensionalItemObject> dimensionItemLookup = dimensionItems.stream()
            .collect( Collectors.toMap( DimensionalItemObject::getDimensionItem, d -> d ) );
        Map<String, OrganisationUnit> orgUnitLookup = orgUnits.stream()
            .collect( Collectors.toMap( BaseIdentifiableObject::getUid, o -> o ) );

        for ( List<Object> row : grid.getRows() )
        {
            String pe = (String) row.get( peInx );
            String dx = (String) row.get( dxInx );
            String ou = (String) row.get( ouInx );
            String ao = hasAttributeOptions ? (String) row.get( aoInx ) : NON_AOC;
            Double vl = ((Number) row.get( vlInx )).doubleValue();

            Period period = periodLookup.get( pe );
            DimensionalItemObject dimensionItem = dimensionItemLookup.get( dx );
            OrganisationUnit orgUnit = orgUnitLookup.get( ou );

            eventDataValues.putEntry( orgUnit, ao, period, dimensionItem, vl );
        }

        return eventDataValues;
    }

    /**
     * Writes the predicted values to the database. Also updates the prediction
     * summmary per-record counts.
     *
     * @param predictions Predictions to write to the database.
     * @param outputDataElement Predictor output data elmeent.
     * @param outputOptionCombo Predictor output category option commbo.
     * @param periods Periods to predict for.
     * @param existingPeriods Those periods to predict for already in DB.
     * @param orgUnits Organisation units to predict for.
     * @param summary Prediction summary to update.
     */
    private void writePredictions( List<DataValue> predictions, DataElement outputDataElement,
        CategoryOptionCombo outputOptionCombo, Set<Period> periods, Set<Period> existingPeriods,
        List<OrganisationUnit> orgUnits, String storedBy, PredictionSummary summary )
    {
        DataExportParams params = new DataExportParams();
        params.setDataElementOperands(
            Sets.newHashSet( new DataElementOperand( outputDataElement, outputOptionCombo ) ) );
        params.setPeriods( periods );
        params.setOrganisationUnits( new HashSet<>( orgUnits ) );
        params.setIncludeDeleted( true );

        List<DeflatedDataValue> oldValueList = dataValueService.getDeflatedDataValues( params );

        Map<String, DeflatedDataValue> oldValues = oldValueList.stream().collect( Collectors.toMap(
            d -> d.getPeriodId() + "-" + d.getSourceId() + "-" + d.getAttributeOptionComboId(), d -> d ) );

        BatchHandler<DataValue> dataValueBatchHandler = batchHandlerFactory
            .createBatchHandler( DataValueBatchHandler.class ).init();

        for ( DataValue newValue : predictions )
        {
            boolean zeroInsignificant = dataValueIsZeroAndInsignificant( newValue.getValue(),
                newValue.getDataElement() );

            String key = newValue.getPeriod().getId() + "-" + newValue.getSource().getId() + "-"
                + newValue.getAttributeOptionCombo().getId();

            DeflatedDataValue oldValue = oldValues.get( key );

            if ( oldValue == null )
            {
                if ( zeroInsignificant )
                {
                    continue;
                }

                summary.incrementInserted();

                /*
                 * Note: BatchHandler can be used for inserts only when the
                 * period previously existed. To insert values into new periods
                 * (just added to the database within this transaction), the
                 * dataValueService must be used.
                 */
                if ( existingPeriods.contains( newValue.getPeriod() ) )
                {
                    dataValueBatchHandler.addObject( newValue );
                }
                else
                {
                    dataValueService.addDataValue( newValue );
                }
            }
            else
            {
                if ( newValue.getValue().equals( oldValue.getValue() ) && !oldValue.isDeleted() )
                {
                    summary.incrementUnchanged();
                }
                else
                {
                    if ( zeroInsignificant )
                    {
                        continue; // Leave the old value to be deleted because
                        // the new value, insigificant, won't be
                        // stored.
                    }

                    summary.incrementUpdated();

                    dataValueBatchHandler.updateObject( newValue );
                }

                oldValues.remove( key );
            }
        }

        Map<Long, OrganisationUnit> orgUnitLookup = orgUnits.stream()
            .collect( Collectors.toMap( OrganisationUnit::getId, o -> o ) );

        for ( DeflatedDataValue oldValue : oldValues.values() )
        {
            summary.incrementDeleted();

            DataValue toDelete = new DataValue( outputDataElement, oldValue.getPeriod(),
                orgUnitLookup.get( oldValue.getSourceId() ), outputOptionCombo,
                categoryService.getCategoryOptionCombo( oldValue.getAttributeOptionComboId() ),
                oldValue.getValue(), storedBy, null, null );

            dataValueBatchHandler.deleteObject( toDelete );
        }

        dataValueBatchHandler.flush();
    }
}
