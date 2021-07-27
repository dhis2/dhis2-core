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

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
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
                predictorList = idObjectManager.getByUid( Predictor.class, predictors );
            }

            if ( !CollectionUtils.isEmpty( predictorGroups ) )
            {
                List<PredictorGroup> predictorGroupList = idObjectManager.getByUid( PredictorGroup.class,
                    predictorGroups );

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

        Set<DimensionalItemObject> outputPeriodItems = new HashSet<>();
        Set<DimensionalItemObject> sampledItems = new HashSet<>();
        expressionService.getExpressionDimensionalItemObjects( generator.getExpression(), PREDICTOR_EXPRESSION,
            outputPeriodItems, sampledItems );
        if ( skipTest != null )
        {
            expressionService.getExpressionDimensionalItemObjects( skipTest.getExpression(), PREDICTOR_SKIP_TEST,
                sampledItems, sampledItems );
        }
        Set<DimensionalItemObject> items = Sets.union( outputPeriodItems, sampledItems );
        Map<String, Constant> constantMap = constantService.getConstantMap();
        List<Period> outputPeriods = getPeriodsBetweenDates( predictor.getPeriodType(), startDate, endDate );
        Set<Period> existingOutputPeriods = getExistingPeriods( outputPeriods );
        ListMap<Period, Period> samplePeriodsMap = getSamplePeriodsMap( outputPeriods, predictor );
        Set<Period> allSamplePeriods = samplePeriodsMap.uniqueValues();
        Set<Period> existingSamplePeriods = getExistingPeriods( new ArrayList<>( allSamplePeriods ) );
        outputPeriods = periodService.reloadPeriods( outputPeriods );
        Set<Period> outputPeriodSet = new HashSet<>( outputPeriods );
        CategoryOptionCombo defaultCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        CategoryOptionCombo outputOptionCombo = predictor.getOutputCombo() == null
            ? defaultCategoryOptionCombo
            : predictor.getOutputCombo();
        CachingMap<String, CategoryOptionCombo> cocMap = new CachingMap<>();
        Date now = new Date();

        Set<Period> queryPeriods = getPeriodsFrom( sampledItems, allSamplePeriods, outputPeriodItems, outputPeriods );
        Set<Period> existingQueryPeriods = getPeriodsFrom( sampledItems, existingSamplePeriods, outputPeriodItems,
            existingOutputPeriods );

        boolean requireData = generator.getMissingValueStrategy() != NEVER_SKIP && (!items.isEmpty());
        DimensionalItemObject forwardReference = getForwardReference( outputDataElement, outputOptionCombo, items );
        Set<DataElementOperand> predictionDeoSet = Sets.newHashSet(
            new DataElementOperand( outputDataElement, outputOptionCombo ) );

        Set<DataElement> dataElements = new HashSet<>();
        Set<DataElementOperand> dataElementOperands = new HashSet<>();
        Set<DimensionalItemObject> analyticsAttributeOptionItems = new HashSet<>();
        Set<DimensionalItemObject> analyticsNonAttributeOptionItems = new HashSet<>();
        categorizeItems( items, dataElements, dataElementOperands,
            analyticsAttributeOptionItems, analyticsNonAttributeOptionItems );

        Set<OrganisationUnit> currentUserOrgUnits = new HashSet<>();
        String storedBy = "system-process";
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser != null )
        {
            currentUserOrgUnits = currentUser.getOrganisationUnits();
            storedBy = currentUser.getUsername();
        }

        PredictionDataValueFetcher oldPredictionFetcher = new PredictionDataValueFetcher(
            dataValueService, categoryService ).setIncludeDeleted( true );
        PredictionDataValueFetcher dataValueFetcher = new PredictionDataValueFetcher(
            dataValueService, categoryService ).setIncludeChildren( true );
        PredictionAnalyticsDataFetcher analyticsFetcher = new PredictionAnalyticsDataFetcher( analyticsService );
        PredictionWriter predictionWriter = new PredictionWriter( dataValueService, batchHandlerFactory );

        predictionWriter.init( existingOutputPeriods, predictionSummary );

        predictionSummary.incrementPredictors();

        // Do separate predictor processing for each organisation unit level
        // selected. This is because at each level, predictions might be based
        // on data aggregated from all descendant org units. So to prevent
        // confusion, data for different levels are fetched independently.

        for ( OrganisationUnitLevel orgUnitLevel : predictor.getOrganisationUnitLevels() )
        {
            List<OrganisationUnit> orgUnits = organisationUnitService.getOrganisationUnitsAtOrgUnitLevels(
                Lists.newArrayList( orgUnitLevel ), currentUserOrgUnits );

            orgUnits.sort( Comparator.comparing( OrganisationUnit::getPath ) );

            oldPredictionFetcher.init( currentUserOrgUnits, orgUnitLevel.getLevel(), orgUnits,
                outputPeriodSet, new HashSet<>(), predictionDeoSet );

            dataValueFetcher.init( currentUserOrgUnits, orgUnitLevel.getLevel(), orgUnits,
                existingQueryPeriods, dataElements, dataElementOperands );

            analyticsFetcher.init( orgUnits, queryPeriods, analyticsAttributeOptionItems,
                analyticsNonAttributeOptionItems );

            for ( OrganisationUnit orgUnit : orgUnits )
            {
                MapMap<Period, DimensionalItemObject, Double> nonAocData = analyticsFetcher.getNonAocData( orgUnit );

                MapMapMap<String, Period, DimensionalItemObject, Double> aocData = analyticsFetcher
                    .getAocData( orgUnit );

                List<DataValue> dataValues = dataValueFetcher.getDataValues( orgUnit );

                addDataValuesToAocData( dataValues, aocData, items );

                Set<String> attributeOptionCombos = getAttributeOptionCombos( aocData, defaultCategoryOptionCombo );

                List<DataValue> predictions = new ArrayList<>();

                // Predict independently for each AOC, adding in the data,
                // if any, that is stored without an AOC.

                for ( String aoc : attributeOptionCombos )
                {
                    MapMap<Period, DimensionalItemObject, Double> periodValueMap = firstNonNull( aocData.get( aoc ),
                        new MapMap<>() );

                    periodValueMap.putMap( nonAocData );

                    Set<Period> skippedPeriods = getSkippedPeriods( allSamplePeriods, periodValueMap, skipTest,
                        constantMap );

                    // Predict for each output period.

                    for ( Period outputPeriod : outputPeriods )
                    {
                        List<Period> samplePeriods = new ArrayList<>( samplePeriodsMap.get( outputPeriod ) );

                        samplePeriods.removeAll( skippedPeriods );

                        Map<DimensionalItemObject, Double> valueMap = firstNonNull( periodValueMap.get( outputPeriod ),
                            new HashMap<>() );

                        if ( requireData && !dataIsPresent( outputPeriodItems, valueMap, sampledItems, samplePeriods,
                            periodValueMap ) )
                        {
                            continue;
                        }

                        Double value = castDouble( expressionService.getExpressionValue( generator.getExpression(),
                            PREDICTOR_EXPRESSION, valueMap, constantMap, null,
                            outputPeriod.getDaysInPeriod(), generator.getMissingValueStrategy(),
                            samplePeriods, periodValueMap ) );

                        carryPredictionForward( value, outputPeriod, forwardReference, periodValueMap );

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
                predictionWriter.write( predictions, oldPredictionFetcher.getDataValues( orgUnit ) );
            }
        }
        predictionWriter.flush();
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Returns a Set of periods. Includes sample periods if there are any sample
     * items, and includes output periods if there are any output items.
     *
     * @param sampleItems sample items, if any.
     * @param samplePeriods sample periods.
     * @param outputPeriodItems output items, if any.
     * @param outputPeriods output periods.
     * @return periods needed for the requested items.
     */
    private Set<Period> getPeriodsFrom(
        Set<DimensionalItemObject> sampleItems, Collection<Period> samplePeriods,
        Set<DimensionalItemObject> outputPeriodItems, Collection<Period> outputPeriods )
    {
        Set<Period> periods = new HashSet<>();

        if ( !sampleItems.isEmpty() )
        {
            periods.addAll( samplePeriods );
        }

        if ( !outputPeriodItems.isEmpty() )
        {
            periods.addAll( outputPeriods );
        }

        return periods;
    }

    /**
     * Categories DimensionalItemObjects found in the predictor expression (and
     * skip test) according to how their values will be fetched from either the
     * datavalue table or analytics.
     *
     * @param items DimensionalItemObjects to be categorized.
     * @param dataElements datavalue: data elements.
     * @param dataElementOperands datavalue: data element operands.
     * @param analyticsAttributeOptionItems analytics: items stored with AOCs.
     * @param analyticsNonAttributeOptionItems analytics: items without AOCs.
     */
    private void categorizeItems( Set<DimensionalItemObject> items,
        Set<DataElement> dataElements, Set<DataElementOperand> dataElementOperands,
        Set<DimensionalItemObject> analyticsAttributeOptionItems,
        Set<DimensionalItemObject> analyticsNonAttributeOptionItems )
    {
        for ( DimensionalItemObject i : items )
        {
            if ( i instanceof DataElement )
            {
                dataElements.add( (DataElement) i );
            }
            else if ( i instanceof DataElementOperand )
            {
                dataElementOperands.add( (DataElementOperand) i );
            }
            else if ( hasAttributeOptions( i ) )
            {
                analyticsAttributeOptionItems.add( i );
            }
            else
            {
                analyticsNonAttributeOptionItems.add( i );
            }
        }
    }

    /**
     * Add the non-deleted, numeric values from the datavalue table to the map
     * of data by attributeOptionCombo.
     * <p>
     * The two types of dimensional item object that are needed from the data
     * value table are DataElement (the sum of all category option combos for
     * that data element) and DataElementOperand (a particular combination of
     * DataElement and CategoryOptionCombo).
     *
     * @param dataValues List of data values.
     * @param aocData Map of attributeOptionCombo-keyed data.
     * @param items the items we will need for expression evaluation.
     */
    private void addDataValuesToAocData( List<DataValue> dataValues,
        MapMapMap<String, Period, DimensionalItemObject, Double> aocData,
        Set<DimensionalItemObject> items )
    {
        for ( DataValue dv : dataValues )
        {
            Double value = getDoubleValue( dv );

            if ( value != null )
            {
                DataElementOperand dataElementOperand = new DataElementOperand(
                    dv.getDataElement(), dv.getCategoryOptionCombo() );

                addToData( dataElementOperand, items, dv, value, aocData );

                addToData( dv.getDataElement(), items, dv, value, aocData );

            }
        }
    }

    /**
     * Returns the value of a datavalue as a Double, if it can be done.
     *
     * @param dv the datavalue
     * @return the Double value
     */
    private Double getDoubleValue( DataValue dv )
    {
        if ( dv.isDeleted() || dv.getValue() == null )
        {
            return null;
        }

        Double value;

        try
        {
            value = Double.parseDouble( dv.getValue() );
        }
        catch ( NumberFormatException e )
        {
            value = null;
        }

        return value;
    }

    /**
     * Add the DataElementOperand or the DataElement value to the existing data.
     * <p>
     * This is needed because we may get multiple data values that need to be
     * aggregated to the same item value. In the case of a
     * DimensionalItemObject, this may be multiple values from children
     * organisation units. In the case of a DataElement, this may be multiple
     * value from children organisation units and/or it may be multiple
     * disaggregated values that need to be summed for the data element.
     * <p>
     * Note that a single data value may contribute to a DataElementOperand
     * value, a DataElement value, or both.
     *
     * @param item the item to add (the DataElementOperand or DataElement).
     * @param items the set of items to be added (only if item is in set).
     * @param dv the data value to be added.
     * @param value the numeric value to be added.
     * @param aocData the datavalue map by attribute option combo to add to.
     */
    private void addToData( DimensionalItemObject item, Set<DimensionalItemObject> items,
        DataValue dv, double value, MapMapMap<String, Period, DimensionalItemObject, Double> aocData )
    {
        if ( !items.contains( item ) )
        {
            return;
        }

        Double valueSoFar = aocData.getValue( dv.getAttributeOptionCombo().getUid(), dv.getPeriod(), item );

        Double valueToStore = (valueSoFar == null) ? value : value + valueSoFar;

        aocData.putEntry( dv.getAttributeOptionCombo().getUid(), dv.getPeriod(), item, valueToStore );
    }

    /**
     * For a predictor and orgUnit, determines the set of attribute option
     * combos for which predictions will be generated.
     *
     * @param itemMap item data map for an orgUnit.
     * @param defaultCategoryOptionCombo system default category option combo.
     * @return set of attribute option combos to use for an orgUnit.
     */
    Set<String> getAttributeOptionCombos(
        MapMapMap<String, Period, DimensionalItemObject, Double> itemMap,
        CategoryOptionCombo defaultCategoryOptionCombo )
    {
        Set<String> attributeOptionCombos = new HashSet<>( itemMap.keySet() );

        if ( attributeOptionCombos.isEmpty() )
        {
            attributeOptionCombos.add( defaultCategoryOptionCombo.getUid() );
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
     * Finds sample periods that should be skipped based on the skip test.
     *
     * @param allSamplePeriods all the sample periods.
     * @param aocData data for this attribute option combo.
     * @param skipTest the skip test.
     * @param constantMap constants that may be in the skip expression.
     * @return the sample periods to be skipped.
     */
    Set<Period> getSkippedPeriods( Set<Period> allSamplePeriods,
        MapMap<Period, DimensionalItemObject, Double> aocData,
        Expression skipTest, Map<String, Constant> constantMap )
    {
        Set<Period> skippedPeriods = new HashSet<>();

        if ( skipTest == null || StringUtils.isEmpty( skipTest.getExpression() ) )
        {
            return skippedPeriods;
        }

        for ( Period p : allSamplePeriods )
        {
            if ( aocData.get( p ) != null
                && Boolean.TRUE == expressionService.getExpressionValue( skipTest.getExpression(),
                    PREDICTOR_SKIP_TEST, aocData.get( p ), constantMap, null,
                    p.getDaysInPeriod(), skipTest.getMissingValueStrategy(),
                    DEFAULT_SAMPLE_PERIODS, new MapMap<>() ) )
            {
                skippedPeriods.add( p );
            }
        }

        return skippedPeriods;
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
     * Finds periods that exist in the DB, from a list of periods.
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
    private DimensionalItemObject getForwardReference( DataElement outputDataElement,
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
     * @param periodValueMap the value map according to period.
     */
    private void carryPredictionForward( Double value, Period outputPeriod,
        DimensionalItemObject predictionReference,
        MapMap<Period, DimensionalItemObject, Double> periodValueMap )
    {
        if ( predictionReference != null )
        {
            periodValueMap.putEntry( outputPeriod, predictionReference, value );
        }
    }

    /**
     * Returns true if there is data to be used for a prediction in this period.
     * This allows us to save time by evaluating an expression only if there is
     * data. (Expression evaluation can take a non-trivial amount of time.)
     *
     * @param outputPeriodItems items for output period.
     * @param valueMap values for output period.
     * @param sampledItems items for sampled periods.
     * @param samplePeriods the sampled periods.
     * @param periodValueMap values for output periods.
     * @return true if there is data, else false.
     */

    private boolean dataIsPresent( Set<DimensionalItemObject> outputPeriodItems,
        Map<DimensionalItemObject, Double> valueMap,
        Set<DimensionalItemObject> sampledItems, List<Period> samplePeriods,
        MapMap<Period, DimensionalItemObject, Double> periodValueMap )
    {
        if ( presentIn( outputPeriodItems, valueMap ) )
        {
            return true;
        }

        for ( Period p : samplePeriods )
        {
            Map<DimensionalItemObject, Double> pValueMap = periodValueMap.get( p );

            if ( pValueMap != null && presentIn( sampledItems, pValueMap ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if any items are present in the value map.
     *
     * @param items items to look for.
     * @param valueMap map of values.
     * @return true if any items in map, else false.
     */
    private boolean presentIn( Set<DimensionalItemObject> items, Map<DimensionalItemObject, Double> valueMap )
    {
        return !Sets.intersection( items, valueMap.keySet() ).isEmpty();
    }
}
