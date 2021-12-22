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
import static org.hisp.dhis.common.OrganisationUnitDescendants.DESCENDANTS;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_SKIP_TEST;
import static org.hisp.dhis.parser.expression.ParserUtils.DEFAULT_SAMPLE_PERIODS;
import static org.hisp.dhis.predictor.PredictionFormatter.formatPrediction;
import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.AnalyticsServiceTarget;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.MapMap;
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
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.CurrentUserServiceTarget;
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
@AllArgsConstructor
public class DefaultPredictionService
    implements PredictionService, AnalyticsServiceTarget, CurrentUserServiceTarget
{
    private final PredictorService predictorService;

    private final ConstantService constantService;

    private final ExpressionService expressionService;

    private final DataValueService dataValueService;

    private final CategoryService categoryService;

    private final OrganisationUnitService organisationUnitService;

    private final OrganisationUnitGroupService organisationUnitGroupService;

    private final PeriodService periodService;

    private final IdentifiableObjectManager idObjectManager;

    private final Notifier notifier;

    private final BatchHandlerFactory batchHandlerFactory;

    private AnalyticsService analyticsService;

    private CurrentUserService currentUserService;

    @Override
    public void setAnalyticsService( AnalyticsService analyticsService )
    {
        this.analyticsService = analyticsService;
    }

    @Override
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
                    predictorList.addAll( predictorGroup.getSortedMembers() );
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
        DataType expressionDataType = DataType.fromValueType( outputDataElement.getValueType() );

        Map<DimensionalItemId, DimensionalItemObject> outputPeriodItemMap = new HashMap<>();
        Map<DimensionalItemId, DimensionalItemObject> sampledItemMap = new HashMap<>();
        expressionService.getExpressionDimensionalItemMaps( generator.getExpression(),
            PREDICTOR_EXPRESSION, expressionDataType, outputPeriodItemMap, sampledItemMap );
        Set<String> orgUnitGroupIds = expressionService.getExpressionOrgUnitGroupIds( generator.getExpression(),
            PREDICTOR_EXPRESSION );
        if ( skipTest != null )
        {
            expressionService.getExpressionDimensionalItemMaps( skipTest.getExpression(),
                PREDICTOR_SKIP_TEST, DataType.BOOLEAN, sampledItemMap, sampledItemMap );
            orgUnitGroupIds.addAll( expressionService.getExpressionOrgUnitGroupIds(
                skipTest.getExpression(), PREDICTOR_SKIP_TEST ) );
        }
        Map<String, OrganisationUnitGroup> orgUnitGroupMap = orgUnitGroupIds.stream()
            .map( organisationUnitGroupService::getOrganisationUnitGroup )
            .filter( Objects::nonNull )
            .collect( Collectors.toMap( OrganisationUnitGroup::getUid, g -> g ) );
        Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>( outputPeriodItemMap );
        itemMap.putAll( sampledItemMap );
        Set<DimensionalItemObject> outputPeriodItems = new HashSet<>( outputPeriodItemMap.values() );
        Set<DimensionalItemObject> sampledItems = new HashSet<>( sampledItemMap.values() );
        Set<DimensionalItemObject> items = new HashSet<>( itemMap.values() );
        Map<String, Constant> constantMap = constantService.getConstantMap();
        List<Period> outputPeriods = getPeriodsBetweenDates( predictor.getPeriodType(), startDate, endDate );
        Set<Period> existingOutputPeriods = getExistingPeriods( outputPeriods );
        ListMap<Period, Period> samplePeriodsMap = getSamplePeriodsMap( outputPeriods, predictor );
        Set<Period> allSamplePeriods = samplePeriodsMap.uniqueValues();
        Set<Period> analyticsQueryPeriods = getAnalyticsQueryPeriods( sampledItems, allSamplePeriods,
            outputPeriodItems, existingOutputPeriods );
        Set<Period> dataValueQueryPeriods = getDataValueQueryPeriods( analyticsQueryPeriods, existingOutputPeriods );
        outputPeriods = periodService.reloadPeriods( outputPeriods );
        CategoryOptionCombo defaultCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        CategoryOptionCombo outputOptionCombo = predictor.getOutputCombo() == null
            ? defaultCategoryOptionCombo
            : predictor.getOutputCombo();
        DataElementOperand outputDataElementOperand = new DataElementOperand( outputDataElement, outputOptionCombo );
        Date now = new Date();

        boolean requireData = generator.getMissingValueStrategy() != NEVER_SKIP && (!items.isEmpty());
        DimensionalItemObject forwardReference = addOuputToItems( outputDataElementOperand, items );

        Set<OrganisationUnit> currentUserOrgUnits = new HashSet<>();
        String storedBy = "system-process";
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser != null )
        {
            currentUserOrgUnits = currentUser.getOrganisationUnits();
            storedBy = currentUser.getUsername();
        }

        PredictionDataConsolidator consolidator = new PredictionDataConsolidator( items,
            predictor.getOrganisationUnitDescendants().equals( DESCENDANTS ),
            new PredictionDataValueFetcher( dataValueService, categoryService ),
            new PredictionAnalyticsDataFetcher( analyticsService, categoryService ) );

        PredictionWriter predictionWriter = new PredictionWriter( dataValueService, batchHandlerFactory );

        predictionWriter.init( existingOutputPeriods, predictionSummary );

        predictionSummary.incrementPredictors();

        for ( OrganisationUnitLevel orgUnitLevel : predictor.getOrganisationUnitLevels() )
        {
            List<OrganisationUnit> orgUnits = organisationUnitService
                .getOrganisationUnitsAtOrgUnitLevels( Lists.newArrayList( orgUnitLevel ), currentUserOrgUnits );

            consolidator.init( currentUserOrgUnits, orgUnitLevel.getLevel(), orgUnits,
                dataValueQueryPeriods, analyticsQueryPeriods, existingOutputPeriods, outputDataElementOperand );

            PredictionData data;

            while ( (data = consolidator.getData()) != null )
            {
                List<DataValue> predictions = new ArrayList<>();

                List<PredictionContext> contexts = PredictionContextGenerator.getContexts(
                    outputPeriods, data.getValues(), defaultCategoryOptionCombo );

                for ( PredictionContext c : contexts )
                {
                    List<Period> samplePeriods = new ArrayList<>( samplePeriodsMap.get( c.getOutputPeriod() ) );

                    samplePeriods.removeAll( getSkippedPeriods( allSamplePeriods, itemMap, c.getPeriodValueMap(),
                        skipTest, constantMap, orgUnitGroupMap, data.getOrgUnit() ) );

                    if ( requireData && !dataIsPresent( outputPeriodItems, c.getValueMap(), sampledItems,
                        samplePeriods, c.getPeriodValueMap() ) )
                    {
                        continue;
                    }

                    Object value = expressionService.getExpressionValue( predictor.getGenerator().getExpression(),
                        PREDICTOR_EXPRESSION, itemMap, c.getValueMap(), constantMap, null, orgUnitGroupMap,
                        c.getOutputPeriod().getDaysInPeriod(), generator.getMissingValueStrategy(), data.getOrgUnit(),
                        samplePeriods, c.getPeriodValueMap(), expressionDataType );

                    if ( value != null || generator.getMissingValueStrategy() == NEVER_SKIP )
                    {
                        String valueString = formatPrediction( value, outputDataElement );

                        if ( valueString != null )
                        {
                            DataValue prediction = new DataValue( outputDataElement,
                                c.getOutputPeriod(), data.getOrgUnit(), outputOptionCombo,
                                c.getAttributeOptionCombo(), valueString, storedBy, now, null );

                            carryPredictionForward( prediction, contexts, forwardReference );

                            predictions.add( prediction );
                        }
                    }
                }

                predictionWriter.write( predictions, data.getOldPredictions() );
            }
        }

        predictionWriter.flush();
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Returns any existing periods to be used for querying analytics items (if
     * there are any). Includes sample periods if there are any sample items,
     * and includes output periods if there are any output items.
     */
    private Set<Period> getAnalyticsQueryPeriods(
        Set<DimensionalItemObject> sampleItems, Set<Period> allSamplePeriods,
        Set<DimensionalItemObject> outputPeriodItems, Set<Period> existingOutputPeriods )
    {
        Set<Period> analyticsQueryPeriods = new HashSet<>();

        if ( !sampleItems.isEmpty() )
        {
            analyticsQueryPeriods.addAll( getExistingPeriods( new ArrayList<>( allSamplePeriods ) ) );
        }

        if ( !outputPeriodItems.isEmpty() )
        {
            analyticsQueryPeriods.addAll( existingOutputPeriods );
        }

        return analyticsQueryPeriods;
    }

    /**
     * Returns any existing periods to be used to query data values. This
     * includes all existing periods to be used for querying analytics items
     * plus all existing output periods (if not already included), to find
     * existing predictor values so we know how to process predictor outputs.
     */
    private Set<Period> getDataValueQueryPeriods( Set<Period> analyticsQueryPeriods, Set<Period> existingOutputPeriods )
    {
        return Sets.union( analyticsQueryPeriods, existingOutputPeriods );
    }

    /**
     * Finds sample periods that should be skipped based on the skip test.
     */
    private Set<Period> getSkippedPeriods( Set<Period> allSamplePeriods,
        Map<DimensionalItemId, DimensionalItemObject> itemMap, MapMap<Period, DimensionalItemObject, Object> aocData,
        Expression skipTest, Map<String, Constant> constantMap, Map<String, OrganisationUnitGroup> orgUnitGroupMap,
        OrganisationUnit orgUnit )
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
                    PREDICTOR_SKIP_TEST, itemMap, aocData.get( p ), constantMap, null, orgUnitGroupMap,
                    p.getDaysInPeriod(), skipTest.getMissingValueStrategy(), orgUnit,
                    DEFAULT_SAMPLE_PERIODS, new MapMap<>(), DataType.BOOLEAN ) )
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
     * <p>
     * The periods returned do not need to be in the database.
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
     * from which the sample data is to be drawn. Sample periods returned for
     * each output period are in order from older to newer, so any prediction
     * results can be brought forward if they are to be used in later period
     * predictions.
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
     * Adds the predictor to the list of items. Also, returns the
     * DimensionalItemObject if any to update with the predicted value.
     * <p>
     * Note that we make the simplifying assumption that if the output data
     * element is sampled in an expression without a catOptionCombo, the
     * predicted data value will be used. This is usually what the user wants,
     * but would break if the expression assumes a sum of catOptionCombos
     * including the predicted value and other catOptionCombos.
     */
    private DimensionalItemObject addOuputToItems( DataElementOperand outputDataElementOperand,
        Set<DimensionalItemObject> sampleItems )
    {
        DimensionalItemObject forwardReference = null;

        for ( DimensionalItemObject item : sampleItems )
        {
            if ( item.equals( outputDataElementOperand ) )
            {
                return item;
            }

            if ( item.equals( outputDataElementOperand.getDataElement() ) )
            {
                forwardReference = item;
            }
        }

        sampleItems.add( outputDataElementOperand );

        return forwardReference;
    }

    /**
     * If the predicted value might be used in a future period prediction,
     * insert it into any future context data.
     */
    private void carryPredictionForward( DataValue prediction, List<PredictionContext> contexts,
        DimensionalItemObject forwardReference )
    {
        if ( forwardReference == null )
        {
            return;
        }

        for ( PredictionContext ctx : contexts )
        {
            if ( ctx.getAttributeOptionCombo().equals( prediction.getAttributeOptionCombo() ) )
            {
                ctx.getPeriodValueMap().putEntry( prediction.getPeriod(), forwardReference, prediction.getValue() );

                if ( ctx.getOutputPeriod().equals( prediction.getPeriod() ) )
                {
                    ctx.getValueMap().put( forwardReference, prediction.getValue() );
                }
            }
        }
    }

    /**
     * Returns true if there is data to be used for a prediction in this period.
     * This allows us to save time by evaluating an expression only if there is
     * data. (Expression evaluation can take a non-trivial amount of time.)
     */
    private boolean dataIsPresent( Set<DimensionalItemObject> outputPeriodItems,
        Map<DimensionalItemObject, Object> valueMap,
        Set<DimensionalItemObject> sampledItems, List<Period> samplePeriods,
        MapMap<Period, DimensionalItemObject, Object> periodValueMap )
    {
        if ( presentIn( outputPeriodItems, valueMap ) )
        {
            return true;
        }

        for ( Period p : samplePeriods )
        {
            Map<DimensionalItemObject, Object> pValueMap = periodValueMap.get( p );

            if ( pValueMap != null && presentIn( sampledItems, pValueMap ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if any items are present in the value map.
     */
    private boolean presentIn( Set<DimensionalItemObject> items, Map<DimensionalItemObject, Object> valueMap )
    {
        return !Sets.intersection( items, valueMap.keySet() ).isEmpty();
    }
}
