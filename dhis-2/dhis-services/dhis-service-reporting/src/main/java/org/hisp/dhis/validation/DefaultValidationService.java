package org.hisp.dhis.validation;

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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.*;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.notification.ValidationNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.hisp.dhis.common.DimensionItemType.*;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_ESCAPED_SEP;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.splitSafe;

/**
 * @author Jim Grace
 * @author Stian Sandvold
 */
@Transactional
public class DefaultValidationService
    implements ValidationService
{
    private static final Log log = LogFactory.getLog( DefaultValidationService.class );

    private static final ImmutableSet<DimensionItemType> EVENT_DIM_ITEM_TYPES = ImmutableSet.of(
        PROGRAM_DATA_ELEMENT, PROGRAM_ATTRIBUTE, PROGRAM_INDICATOR );

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private ConstantService constantService;

    @Autowired
    private ValidationNotificationService notificationService;

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ValidationResultService validationResultService;

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // ValidationRule business logic
    // -------------------------------------------------------------------------

    public Collection<ValidationResult> validationAnalysis( ValidationAnalysisParams parameters)
    {
        Clock clock = new Clock( log ).startClock().logTime( "Starting validation analysis." );

        ValidationRunContext context = getValidationContext( parameters );

        clock.logTime( "Initialized validation analysis." );

        if ( context.getSubContexts().isEmpty() )
        {
            clock.logTime( "Finished Validation Analysis - nothing to do." ).stop();

            return new ArrayList<ValidationResult>();
        }

        Collection<ValidationResult> results = Validator.validate( context, applicationContext );

        clock.logTime( "Finished validation analysis." ).stop();

        if ( context.isSendNotifications() )
        {
            notificationService.sendNotifications( Sets.newHashSet( results ) );
        }

        return results;
    }

    @Override
    public List<DataElementOperand> validateRequiredComments( DataSet dataSet, Period period,
        OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        List<DataElementOperand> violations = new ArrayList<>();

        if ( dataSet.isNoValueRequiresComment() )
        {
            for ( DataElement de : dataSet.getDataElements() )
            {
                for ( DataElementCategoryOptionCombo co : de.getCategoryOptionCombos() )
                {
                    DataValue dv = dataValueService
                        .getDataValue( de, period, organisationUnit, co, attributeOptionCombo );

                    boolean missingValue = dv == null || StringUtils.trimToNull( dv.getValue() ) == null;
                    boolean missingComment = dv == null || StringUtils.trimToNull( dv.getComment() ) == null;

                    if ( missingValue && missingComment )
                    {
                        violations.add( new DataElementOperand( de, co ) );
                    }
                }
            }
        }

        return violations;
    }

    // -------------------------------------------------------------------------
    // Methods for creating ValidationAnalysisParams Builder object
    // -------------------------------------------------------------------------

    @Override
    public ValidationAnalysisParams.Builder newParamsBuilder( Collection<ValidationRule> validationRules,
        OrganisationUnit organisationUnit, Collection<Period> periods )
    {
        return new ValidationAnalysisParams.Builder( validationRules, organisationUnit, periods);
    }

    @Override
    public ValidationAnalysisParams.Builder newParamsBuilder( ValidationRuleGroup validationRuleGroup,
        OrganisationUnit organisationUnit, Date startDate, Date endDate )
    {
        Collection<ValidationRule> validationRules = validationRuleGroup != null ? validationRuleGroup.getMembers() : validationRuleService.getAllValidationRules();
        Collection<Period> periods = periodService.getPeriodsBetweenDates( startDate, endDate );

        return new ValidationAnalysisParams.Builder( validationRules, organisationUnit, periods);
    }

    @Override
    public ValidationAnalysisParams.Builder newParamsBuilder( DataSet dataSet, OrganisationUnit organisationUnit,
        Period period )
    {
        Collection<ValidationRule> validationRules = validationRuleService.getValidationRulesForDataElements( dataSet.getDataElements() );
        Collection<Period> periods = Sets.newHashSet(period);

        return new ValidationAnalysisParams.Builder( validationRules, organisationUnit, periods);
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Gets the event dimension item for the validation rules.
     *
     * @param dimensionItemMap map from UIDs to all dimension items.
     * @return Set with all event dimension items.
     */
    private Set<DimensionalItemObject> getEventItems( Map<String, DimensionalItemObject> dimensionItemMap )
    {
        return dimensionItemMap.values().stream()
            .filter( di -> EVENT_DIM_ITEM_TYPES.contains( di.getDimensionItemType() ) )
            .collect( Collectors.toSet() );
    }

    /**
     * Returns a new Builder with basic configuration based on the input parameters.
     *
     * @param parameters        ValidationRuleParameters for creating ValidationRuleContext
     * @return Builder with basic configuration based on input.
     */
    private ValidationRunContext getValidationContext( ValidationAnalysisParams parameters )
    {
        User currentUser = currentUserService.getCurrentUser();

        OrganisationUnit parameterOrgUnit = parameters.getOrgUnit();
        List<OrganisationUnit> orgUnits;
        if ( parameterOrgUnit == null )
        {
            orgUnits = organisationUnitService.getAllOrganisationUnits();
        }
        else if ( parameters.isIncludeOrgUnitDescendants() )
        {
            orgUnits = organisationUnitService.getOrganisationUnitWithChildren( parameterOrgUnit.getUid() );
        }
        else
        {
            orgUnits = Lists.newArrayList( parameterOrgUnit );
        }

        Map<PeriodType, PeriodTypeExtended> periodTypeXMap = new HashMap<>();

        addPeriodsToContext( periodTypeXMap, parameters.getPeriods() );
        addRulesToContext( periodTypeXMap, parameters.getRules() );
        removeAnyUnneededPeriodTypes( periodTypeXMap );
        List<ValidationSubContext> subContexts = createSubContexts( periodTypeXMap, orgUnits );

        ValidationRunContext.Builder builder = ValidationRunContext.newBuilder()
            .withPeriodTypeXMap( periodTypeXMap )
            .withSubContexts( subContexts )
            .withConstantMap( constantService.getConstantMap() )
            .withInitialResults( validationResultService
                .getValidationResults( parameterOrgUnit,
                    parameters.isIncludeOrgUnitDescendants(), parameters.getRules(), parameters.getPeriods()) )
            .withSendNotifications( parameters.isSendNotifications() )
            .withPersistResults( parameters.isPersistResults() )
            .withAttributeCombo( parameters.getAttributeOptionCombo() )
            .withDefaultAttributeCombo( categoryService.getDefaultDataElementCategoryOptionCombo() )
            .withMaxResults( parameters.getMaxResults() );

        if ( currentUser != null )
        {
            builder
                .withCoDimensionConstraints(
                    categoryService.getCoDimensionConstraints( currentUser.getUserCredentials() ) )
                .withCogDimensionConstraints(
                    categoryService.getCogDimensionConstraints( currentUser.getUserCredentials() ) );
        }

        return builder.build();
    }

    /**
     * Adds Periods to the context, grouped by period type.
     *
     * @param periodTypeXMap period type map to extended period types.
     * @param periods               periods to group and add.
     */
    private void addPeriodsToContext( Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
        Collection<Period> periods )
    {
        for ( Period period : periods )
        {
            PeriodTypeExtended periodTypeX = getOrCreatePeriodTypeExtended( periodTypeXMap,
                period.getPeriodType() );
            periodTypeX.getPeriods().add( period );
        }
    }

    /**
     * Adds validation rules to the context.
     *
     * @param periodTypeXMap period type map to extended period types.
     * @param rules                 validation rules to add.
     */
    private void addRulesToContext( Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
        Collection<ValidationRule> rules )
    {
        // 1. Find all dimensional object IDs in the expressions of the validation rules.

        SetMap<Class<? extends DimensionalItemObject>, String> allItemIds = new SetMap<>();

        SetMap<ValidationRule, String> ruleItemIds = new SetMap<>();

        for ( ValidationRule rule : rules )
        {
            if ( periodTypeXMap.get( rule.getPeriodType() ) == null )
            {
                continue; // Don't include rules for which there are no periods.
            }

            SetMap<Class<? extends DimensionalItemObject>, String> dimensionItemIdentifiers = expressionService
                .getDimensionalItemIdsInExpression( rule.getLeftSide().getExpression() );
            dimensionItemIdentifiers.putValues(
                expressionService.getDimensionalItemIdsInExpression( rule.getRightSide().getExpression() ) );

            Set<String> ruleIds = dimensionItemIdentifiers.values().stream()
                .reduce( new HashSet<>(), Sets::union );

            ruleItemIds.putValues( rule, ruleIds );

            allItemIds.putValues( dimensionItemIdentifiers );
        }

        // 2. Get the dimensional objects from the IDs. (Get them all at once for best performance.)

        Map<String, DimensionalItemObject> dimensionItemMap = getDimensionalItemObjects( allItemIds );

        // 3. Save the dimensional objects in the extended validation rules.

        for ( ValidationRule rule : rules )
        {
            PeriodTypeExtended periodTypeX = periodTypeXMap.get( rule.getPeriodType() );

            if ( periodTypeX == null )
            {
                continue;
            }

            ValidationRuleExtended ruleX = new ValidationRuleExtended( rule );

            Set<DimensionalItemObject> ruleDimensionItemObjects = ruleItemIds.get( rule ).stream()
                .map( dimensionItemMap::get )
                .collect( Collectors.toSet() );

            if ( ruleDimensionItemObjects != null )
            {
                for ( DimensionalItemObject o : ruleDimensionItemObjects )
                {
                    if ( o.getDimensionItemType() == DimensionItemType.DATA_ELEMENT )
                    {
                        o = new DataElementOperand( (DataElement) o ); // Convert data elemnets to data element operands.
                    }

                    if ( o.getDimensionItemType() == DimensionItemType.DATA_ELEMENT_OPERAND )
                    {
                        DataElementOperand deo = (DataElementOperand) o;
                        ruleX.getDataElementOperands().add( deo );
                        ruleX.getDataElements().add( deo.getDataElement() );
                    }
                    else if ( hasAttributeOptions( o ) )
                    {
                        ruleX.getEventItems().add( o );
                    }
                    else
                    {
                        ruleX.getEventItemsWithoutAttributeOptions().add( o );
                    }
                }
            }

            periodTypeX.getRuleXs().add( ruleX );
        }
    }

    /**
     * Checks to see if a dimensional item object has values
     * stored in the database by attribute option combo.
     *
     * @param o dimensional item object
     * @return true if values are stored by attribuete option combo.
     */
    private boolean hasAttributeOptions( DimensionalItemObject o )
    {
        return o.getDimensionItemType() != DimensionItemType.PROGRAM_INDICATOR
            || ( (ProgramIndicator)o ).getAnalyticsType() != AnalyticsType.ENROLLMENT;
    }

    /**
     * Gets all required DimensionalItemObjects from their UIDs.
     *
     * @param expressionIdMap UIDs of DimensionalItemObjects to get.
     * @return map of the DimensionalItemObjects.
     */
    private Map<String, DimensionalItemObject> getDimensionalItemObjects(
        SetMap<Class<? extends DimensionalItemObject>, String> expressionIdMap )
    {
        // 1. Get ids for all the individual IdentifiableObjects within the DimensionalItemObjects:

        SetMap<Class<? extends IdentifiableObject>, String> idsToGet = new SetMap<>();

        getIdentifiableObjectIds( idsToGet, expressionIdMap, DataElementOperand.class, DataElement.class,
            DataElementCategoryOptionCombo.class );
        getIdentifiableObjectIds( idsToGet, expressionIdMap, ProgramDataElementDimensionItem.class, Program.class,
            DataElement.class );
        getIdentifiableObjectIds( idsToGet, expressionIdMap, ProgramTrackedEntityAttributeDimensionItem.class,
            Program.class, TrackedEntityAttribute.class );
        getIdentifiableObjectIds( idsToGet, expressionIdMap, ProgramIndicator.class, ProgramIndicator.class );

        // 2. Look up all the IdentifiableObjects (each class all together, for best performance):

        MapMap<Class<? extends IdentifiableObject>, String, IdentifiableObject> idMap = new MapMap<>();

        for ( Map.Entry<Class<? extends IdentifiableObject>, Set<String>> e : idsToGet.entrySet() )
        {
            idMap.putEntries( e.getKey(), idObjectManager.get( e.getKey(), e.getValue() ).stream()
                .collect( Collectors.toMap( IdentifiableObject::getUid, o -> o ) ) );
        }

        // 3. Build the map of DimensionalItemObjects:

        Map<String, DimensionalItemObject> dimObjects = new HashMap<>();

        for ( Map.Entry<Class<? extends DimensionalItemObject>, Set<String>> e : expressionIdMap.entrySet() )
        {
            for ( String id : e.getValue() )
            {
                if ( e.getKey() == DataElementOperand.class )
                {
                    DataElementOperand deo = new DataElementOperand(
                        (DataElement) idMap.getValue( DataElement.class, getIdPart( id, 0 ) ),
                        (DataElementCategoryOptionCombo) idMap
                            .getValue( DataElementCategoryOptionCombo.class, getIdPart( id, 1 ) ) );

                    if ( deo.getDataElement() != null &&
                        (deo.getCategoryOptionCombo() != null || getIdPart( id, 1 ) == null) )
                    {
                        dimObjects.put( id, deo );
                    }
                }
                else if ( e.getKey() == ProgramDataElementDimensionItem.class )
                {
                    ProgramDataElementDimensionItem pde = new ProgramDataElementDimensionItem(
                        (Program) idMap.getValue( Program.class, getIdPart( id, 0 ) ),
                        (DataElement) idMap.getValue( DataElement.class, getIdPart( id, 1 ) ) );

                    if ( pde.getProgram() != null && pde.getDataElement() != null )
                    {
                        dimObjects.put( id, pde );
                    }
                }
                else if ( e.getKey() == ProgramTrackedEntityAttributeDimensionItem.class )
                {
                    ProgramTrackedEntityAttributeDimensionItem pa = new ProgramTrackedEntityAttributeDimensionItem(
                        (Program) idMap.getValue( Program.class, getIdPart( id, 0 ) ),
                        (TrackedEntityAttribute) idMap.getValue( TrackedEntityAttribute.class, getIdPart( id, 1 ) ) );

                    if ( pa.getProgram() != null && pa.getAttribute() != null )
                    {
                        dimObjects.put( id, pa );
                    }
                }
                else if ( e.getKey() == ProgramIndicator.class )
                {
                    ProgramIndicator pi = (ProgramIndicator) idMap.getValue( ProgramIndicator.class, id );

                    if ( pi != null )
                    {
                        dimObjects.put( id, pi );
                    }
                }
            }
        }

        return dimObjects;
    }

    /**
     * Takes all the identifiers within a dimensional object class, and splits
     * them into identifiers for the identifiable objects that make up
     * the dimensional object.
     *
     * @param idsToGet        To add to: identifiable object IDs to look up.
     * @param expressionIdMap Dimensional object IDs from expression.
     * @param dimClass        Class of dimensional object
     * @param idClasses       Component class(es) of identifiable objects
     */
    @SafeVarargs
    private final void getIdentifiableObjectIds( SetMap<Class<? extends IdentifiableObject>, String> idsToGet,
        SetMap<Class<? extends DimensionalItemObject>, String> expressionIdMap,
        Class<? extends DimensionalItemObject> dimClass,
        Class<? extends IdentifiableObject>... idClasses )
    {
        Set<String> expressionIds = expressionIdMap.get( dimClass );

        if ( expressionIds == null )
        {
            return;
        }

        for ( int i = 0; i < idClasses.length; i++ )
        {
            for ( String expressionId : expressionIds )
            {
                String objectId = getIdPart( expressionId, i );

                if ( objectId != null )
                {
                    idsToGet.putValue( idClasses[i], objectId );
                }
            }
        }
    }

    /**
     * Gets part of an object identifier which may be composite.
     *
     * @param id    The identifier to parse.
     * @param index Index of the part to return.
     * @return The identifier part.
     */
    private String getIdPart( String id, int index )
    {
        return splitSafe( id, COMPOSITE_DIM_OBJECT_ESCAPED_SEP, index );
    }

    /**
     * Removes any period types that don't have rules assigned to them.
     *
     * @param periodTypeXMap period type map to extended period types.
     */
    private void removeAnyUnneededPeriodTypes( Map<PeriodType, PeriodTypeExtended> periodTypeXMap )
    {
        Set<PeriodTypeExtended> periodTypeXs = new HashSet<>( periodTypeXMap.values() );

        for ( PeriodTypeExtended periodTypeX : periodTypeXs )
        {
            if ( periodTypeX.getRuleXs().isEmpty() )
            {
                periodTypeXMap.remove( periodTypeX.getPeriodType() );
            }
        }
    }

    /**
     * Create all the validation sub contexts for this run.
     *
     * @param periodTypeXMap
     * @param orgUnits
     * @return
     */
    private List<ValidationSubContext> createSubContexts(
        Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
        List<OrganisationUnit> orgUnits )
    {
        Map<String, ValidationSubContext> subContextMap = new HashMap<>();

        for ( OrganisationUnit orgUnit : orgUnits )
        {
            ListMap<PeriodType, DataSet> periodTypeDataSetMap = new ListMap<>();

            for ( DataSet dataSet : orgUnit.getDataSets() )
            {
                periodTypeDataSetMap.putValue( dataSet.getPeriodType(), dataSet );
            }

            ListMap<PeriodType, PeriodType> allowedPeriodTypes = getAllowedPeriodTypes( periodTypeDataSetMap.keySet() );
            ListMap<PeriodType, DataElement> periodTypeDataElements = getPeriodTypeDataElements( periodTypeDataSetMap );
            ListMap<PeriodType, DataElement> periodTypeAllowedDataElements = getPeriodTypeAllowedDataElements( periodTypeDataElements, allowedPeriodTypes );

            for ( Map.Entry<PeriodType, List<DataElement>> e : periodTypeDataElements.entrySet() )
            {
                PeriodType periodType = e.getKey();
                List<DataElement> dataElements = e.getValue();
                PeriodTypeExtended periodTypeX = periodTypeXMap.get( periodType );

                if ( periodTypeX == null )
                {
                    continue;
                }

                List<DataElement> allowedDataElements = periodTypeAllowedDataElements.get( periodType );

                List<ValidationRuleExtended> ruleXs = getPeriodRules( periodTypeX, dataElements, allowedDataElements );

                if ( ruleXs.isEmpty() )
                {
                    continue;
                }

                String subContextKey = getRulesKey( ruleXs );
                ValidationSubContext subContext = subContextMap.get( subContextKey );

                if ( subContext == null )
                {
                    subContext = createSubContext( periodTypeX, ruleXs, allowedPeriodTypes.get( periodType ) );
                    subContextMap.put( subContextKey, subContext );
                }

                subContext.getOrgUnits().add( orgUnit );
            }
        }

        return new ArrayList<>( subContextMap.values() );
    }

    private ListMap<PeriodType, PeriodType> getAllowedPeriodTypes( Collection<PeriodType> periodTypes )
    {
        ListMap<PeriodType, PeriodType> allowedPeriodTypes = new ListMap<>();

        for ( PeriodType p : periodTypes )
        {
            for ( PeriodType q : periodTypes )
            {
                if ( q.getFrequencyOrder() >= p.getFrequencyOrder() )
                {
                    allowedPeriodTypes.putValue( p, q );
                }
            }
        }

        return allowedPeriodTypes;
    }

    private ListMap<PeriodType, DataElement> getPeriodTypeDataElements( ListMap<PeriodType, DataSet> periodTypeDataSetMap )
    {
        ListMap<PeriodType, DataElement> periodTypeDataElements = new ListMap<>();

        for ( Map.Entry<PeriodType, List<DataSet>> e : periodTypeDataSetMap.entrySet() )
        {
            PeriodType periodType = e.getKey();
            List<DataSet> dataSets = e.getValue();

            for ( DataSet dataSet : dataSets )
            {
                periodTypeDataElements.putValues( dataSet.getPeriodType(), dataSet.getDataElements() );
            }
        }

        return periodTypeDataElements;
    }

    ListMap<PeriodType, DataElement> getPeriodTypeAllowedDataElements( ListMap<PeriodType, DataElement> periodTypeDataElements, ListMap<PeriodType, PeriodType> allowedPeriodTypes )
    {
        ListMap<PeriodType, DataElement> periodTypeDataElementsAllowed = new ListMap<>();

        for ( PeriodType p : periodTypeDataElements.keySet() )
        {
            for ( PeriodType q : allowedPeriodTypes.get( p ) )
            {
                periodTypeDataElementsAllowed.putValues( p, periodTypeDataElements.get( q ) );
            }
        }

        return periodTypeDataElementsAllowed;
    }

    private String getRulesKey( List<ValidationRuleExtended> ruleXs )
    {
        List<ValidationRule> rules = ruleXs.stream().map( r -> r.getRule() ).collect( Collectors.toList() );

        List<String> uids = getUids( rules );

        Collections.sort( uids );

        return String.join("", uids );
    }

    private List<ValidationRuleExtended> getPeriodRules( PeriodTypeExtended periodTypeX,
        List<DataElement> dataElements, List<DataElement> allowedDataElements )
    {
        List<ValidationRuleExtended> rules = new ArrayList<>();

        for ( ValidationRuleExtended ruleX : periodTypeX.getRuleXs() )
        {
            Set<DataElement> ruleElements = ruleX.getDataElements();

            if ( ruleElements.isEmpty() ||
                ( CollectionUtils.containsAny( dataElements, ruleElements ) && allowedDataElements.containsAll( ruleElements ) ) )
            {
                rules.add( ruleX );
            }
        }

        return rules;
    }

    private ValidationSubContext createSubContext( PeriodTypeExtended periodTypeX,
        List<ValidationRuleExtended> ruleXs, List<PeriodType> allowedPeriodTypes )
    {
        List<ValidationRule> rules = new ArrayList<>();
        List<OrganisationUnit> orgUnits = new ArrayList<>();
        Set<DataElementOperand> dataItems = new HashSet<>();
        Set<DimensionalItemObject> eventItems = new HashSet<>();
        Set<DimensionalItemObject> eventItemsWithoutAttributeOptions = new HashSet<>();

        for ( ValidationRuleExtended ruleX : ruleXs )
        {
            rules.add( ruleX.getRule() );
            dataItems.addAll( ruleX.getDataElementOperands() );
            eventItems.addAll( ruleX.getEventItems() );
            eventItemsWithoutAttributeOptions.addAll( ruleX.getDataElementOperands() );
        }

        return ValidationSubContext.newBuilder()
            .withRules( rules )
            .withOrgUnits( orgUnits )
            .withPeriodTypeX( periodTypeX )
            .withAllowedPeriodTypes( allowedPeriodTypes )
            .withDataItems ( dataItems )
            .withEventItems( eventItems )
            .withEventItemsWithoutAttributeOptions( eventItemsWithoutAttributeOptions )
            .build();
    }

    /**
     * Gets the PeriodTypeExtended from the context object. If not found,
     * creates a new PeriodTypeExtended object, puts it into the context object,
     * and returns it.
     *
     * @param periodTypeXMap period type map to extended period types.
     * @param periodType            period type to search for
     * @return period type extended from the context object
     */
    private PeriodTypeExtended getOrCreatePeriodTypeExtended( Map<PeriodType, PeriodTypeExtended> periodTypeXMap,
        PeriodType periodType )
    {
        PeriodTypeExtended periodTypeX = periodTypeXMap.get( periodType );

        if ( periodTypeX == null )
        {
            periodTypeX = new PeriodTypeExtended( periodType );
            periodTypeXMap.put( periodType, periodTypeX );
        }

        return periodTypeX;
    }
}
