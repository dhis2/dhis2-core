package org.hisp.dhis.validation;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.notification.ValidationNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Margrethe Store
 * @author Lars Helge Overland
 * @author Jim Grace
 */
@Transactional
public class DefaultValidationRuleService
    implements ValidationRuleService
{
    private static final Log log = LogFactory.getLog( DefaultValidationRuleService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ValidationRuleStore validationRuleStore;

    public void setValidationRuleStore( ValidationRuleStore validationRuleStore )
    {
        this.validationRuleStore = validationRuleStore;
    }

    private GenericIdentifiableObjectStore<ValidationRuleGroup> validationRuleGroupStore;

    public void setValidationRuleGroupStore( GenericIdentifiableObjectStore<ValidationRuleGroup> validationRuleGroupStore )
    {
        this.validationRuleGroupStore = validationRuleGroupStore;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private DataValueService dataValueService;

    public void setDataValueService( DataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private ConstantService constantService;

    public void setConstantService( ConstantService constantService )
    {
        this.constantService = constantService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    private ExpressionService expressionService;
    
    public void setExpressionService( ExpressionService expressionService )
    {
        this.expressionService = expressionService;
    }

    private ValidationNotificationService notificationService;

    public void setNotificationService( ValidationNotificationService notificationService )
    {
        this.notificationService = notificationService;
    }

    @Autowired
    private ApplicationContext applicationContext;

    // -------------------------------------------------------------------------
    // ValidationRule business logic
    // -------------------------------------------------------------------------

    @Override
    public Collection<ValidationResult> validate( Date startDate, Date endDate, Collection<OrganisationUnit> sources,
        DataElementCategoryOptionCombo attributeCombo, ValidationRuleGroup group, boolean sendAlerts, I18nFormat format )
    {
        log.debug( "Validate start:" + startDate + " end: " + endDate + " sources: " + sources.size() + " group: " + group );

        List<Period> periods = periodService.getPeriodsBetweenDates( startDate, endDate );
        
        Collection<ValidationRule> rules = group != null ? group.getMembers() : getAllValidationRules();
        
        Map<ValidationRule, Set<DataElement>> ruleDataElementsMap = rules.stream().collect( 
            Collectors.toMap( Function.identity(), v -> getDataElements( v ) ) );

        User user = currentUserService.getCurrentUser();
        
        Collection<ValidationResult> results = Validator.validate( ValidationRunContext.getNewContext(
            sources, periods, rules, attributeCombo, 
            null, ValidationRunType.SCHEDULED, constantService.getConstantMap(), ruleDataElementsMap,
            categoryService.getCogDimensionConstraints( user.getUserCredentials() ),
            categoryService.getCoDimensionConstraints( user.getUserCredentials() ) ), applicationContext );

        formatPeriods( results, format );

        if ( sendAlerts )
        {
            notificationService.sendNotifications( new HashSet<>( results ) );
        }

        return results;
    }

    @Override
    public Collection<ValidationResult> validate( DataSet dataSet, Period period, OrganisationUnit source,
        DataElementCategoryOptionCombo attributeCombo )
    {
        log.debug( "Validate data set: " + dataSet.getName() + " period: " + period.getPeriodType().getName() + " "
            + period.getStartDate() + " " + period.getEndDate() + " source: " + source.getName()
            + " attribute combo: " + ( attributeCombo == null ? "[none]" : attributeCombo.getName() ) );

        Collection<ValidationRule> rules = getValidationRulesForDataElements( dataSet.getDataElements() );
        
        Map<ValidationRule, Set<DataElement>> ruleDataElementsMap = rules.stream().collect( 
            Collectors.toMap( Function.identity(), v -> getDataElements( v ) ) );

        User user = currentUserService.getCurrentUser();
        
        return Validator.validate( ValidationRunContext.getNewContext( 
            Sets.newHashSet( source ), Sets.newHashSet( period ), rules, attributeCombo, null,
            ValidationRunType.SCHEDULED, constantService.getConstantMap(), ruleDataElementsMap,
            categoryService.getCogDimensionConstraints( user.getUserCredentials() ),
            categoryService.getCoDimensionConstraints( user.getUserCredentials() ) ), applicationContext );
    }

    @Override
    public void scheduledRun()
    {
        log.info( "Starting scheduled monitoring task" );

        List<OrganisationUnit> sources = organisationUnitService.getAllOrganisationUnits();

        // Find all rules which might generate notifications
        Set<ValidationRule> rules = getValidationRulesWithNotificationTemplates();

        Set<Period> periods = getAlertPeriodsFromRules( rules );

        Map<ValidationRule, Set<DataElement>> ruleDataElementsMap = rules.stream().collect( 
            Collectors.toMap( Function.identity(), v -> getDataElements( v ) ) );

        Date lastScheduledRun = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_MONITORING_RUN );

        // Any database changes after this moment will contribute to the next run.

        Date thisRun = new Date();

        log.info( "Scheduled monitoring run sources: " + sources.size() + ", periods: " + periods.size() + ", rules:" + rules.size()
            + ", last run: " + (lastScheduledRun == null ? "[none]" : lastScheduledRun) );

        User user = currentUserService.getCurrentUser();
        
        Collection<ValidationResult> results = Validator.validate( ValidationRunContext.getNewContext( 
            sources, periods, rules, null, lastScheduledRun,
            ValidationRunType.SCHEDULED, constantService.getConstantMap(), ruleDataElementsMap,
            categoryService.getCogDimensionConstraints( user.getUserCredentials() ),
            categoryService.getCoDimensionConstraints( user.getUserCredentials() ) ), applicationContext );

        log.info( "Validation run result count: " + results.size() );

        notificationService.sendNotifications( new HashSet<>( results ) );

        log.info( "Sent notifications, monitoring task done" );

        systemSettingManager.saveSystemSetting( SettingKey.LAST_MONITORING_RUN, thisRun );
    }

    @Override
    public List<DataElementOperand> validateRequiredComments( DataSet dataSet, Period period, OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        List<DataElementOperand> violations = new ArrayList<>();

        if ( dataSet.isNoValueRequiresComment() )
        {
            for ( DataElement de : dataSet.getDataElements() )
            {
                for ( DataElementCategoryOptionCombo co : de.getCategoryOptionCombos() )
                {
                    DataValue dv = dataValueService.getDataValue( de, period, organisationUnit, co, attributeOptionCombo );

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

    @Override
    public Collection<ValidationRule> getValidationRulesForDataElements( Set<DataElement> dataElements )
    {
        Set<ValidationRule> rulesForDataElements = new HashSet<>();

        for ( ValidationRule validationRule : getAllValidationRules() )
        {
            Set<DataElement> validationRuleElements = getDataElements( validationRule );

            if ( dataElements.containsAll( validationRuleElements ) )
            {
                rulesForDataElements.add( validationRule );
            }
        }

        return rulesForDataElements;
    }

    @Override
    public Set<DataElement> getDataElements( ValidationRule validationRule )
    {
        Set<DataElement> elements = new HashSet<>();
        elements.addAll( expressionService.getDataElementsInExpression( validationRule.getLeftSide().getExpression() ) );
        elements.addAll( expressionService.getDataElementsInExpression( validationRule.getRightSide().getExpression() ) );
        return elements;
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods - scheduled run
    // -------------------------------------------------------------------------

    private Set<ValidationRule> getValidationRulesWithNotificationTemplates()
    {
        return Sets.newHashSet( validationRuleStore.getValidationRulesWithNotificationTemplates() );
    }

    /**
     * Gets the current and most recent periods to search, based on
     * the period types from the rules to run.
     * <p>
     * For each period type, return the period containing the current date
     * (if any), and the most recent previous period. Add whichever of
     * these periods actually exist in the database.
     * <p>
     * TODO If the last successful daily run was more than one day ago, we might
     * add some additional periods of type DailyPeriodType not to miss any
     * alerts.
     *
     * @param rules the ValidationRules to be evaluated on this run
     * @return periods to search for new alerts
     */
    private Set<Period> getAlertPeriodsFromRules( Set<ValidationRule> rules )
    {
        Set<Period> periods = new HashSet<>();

        Set<PeriodType> rulePeriodTypes = getPeriodTypesFromRules( rules );

        for ( PeriodType periodType : rulePeriodTypes )
        {
            Period currentPeriod = periodType.createPeriod();
            Period previousPeriod = periodType.getPreviousPeriod( currentPeriod );
            
            periods.addAll( periodService.getIntersectingPeriodsByPeriodType( periodType,
                previousPeriod.getStartDate(), currentPeriod.getEndDate() ) );
        }

        return periods;
    }

    /**
     * Gets the Set of period types found in a set of rules.
     * <p>
     * Note that that we have to get periodType from periodService,
     * otherwise the ID will not be present.)
     *
     * @param rules validation rules of interest
     * @return period types contained in those rules
     */
    private Set<PeriodType> getPeriodTypesFromRules( Collection<ValidationRule> rules )
    {
        Set<PeriodType> rulePeriodTypes = new HashSet<>();

        for ( ValidationRule rule : rules )
        {
            rulePeriodTypes.add( periodService.getPeriodTypeByName( rule.getPeriodType().getName() ) );
        }

        return rulePeriodTypes;
    }

    // -------------------------------------------------------------------------
    // Supportive methods - monitoring
    // -------------------------------------------------------------------------

    /**
     * Formats and sets name on the period of each result.
     *
     * @param results the collection of validation results.
     * @param format  the i18n format.
     */
    private void formatPeriods( Collection<ValidationResult> results, I18nFormat format )
    {
        if ( format != null )
        {
            for ( ValidationResult result : results )
            {
                if ( result != null && result.getPeriod() != null )
                {
                    result.getPeriod().setName( format.formatPeriod( result.getPeriod() ) );
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // ValidationRule CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public int saveValidationRule( ValidationRule validationRule )
    {
        return validationRuleStore.save( validationRule );
    }

    @Override
    public void updateValidationRule( ValidationRule validationRule )
    {
        validationRuleStore.update( validationRule );
    }

    @Override
    public void deleteValidationRule( ValidationRule validationRule )
    {
        validationRuleStore.delete( validationRule );
    }

    @Override
    public ValidationRule getValidationRule( int id )
    {
        return validationRuleStore.get( id );
    }

    @Override
    public ValidationRule getValidationRule( String uid )
    {
        return validationRuleStore.getByUid( uid );
    }

    @Override
    public ValidationRule getValidationRuleByName( String name )
    {
        return validationRuleStore.getByName( name );
    }

    @Override
    public List<ValidationRule> getAllValidationRules()
    {
        return validationRuleStore.getAll();
    }

    @Override
    public List<ValidationRule> getValidationRulesByDataElements( Collection<DataElement> dataElements )
    {
        return validationRuleStore.getValidationRulesByDataElements( dataElements );
    }

    @Override
    public int getValidationRuleCount()
    {
        return validationRuleStore.getCount();
    }

    @Override
    public int getValidationRuleCountByName( String name )
    {
        return validationRuleStore.getCountLikeName( name );
    }

    @Override
    public List<ValidationRule> getValidationRulesBetween( int first, int max )
    {
        return validationRuleStore.getAllOrderedName( first, max ) ;
    }

    @Override
    public List<ValidationRule> getValidationRulesBetweenByName( String name, int first, int max )
    {
        return validationRuleStore.getAllLikeName( name, first, max ) ;
    }

    // -------------------------------------------------------------------------
    // ValidationRuleGroup CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public int addValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        return validationRuleGroupStore.save( validationRuleGroup );
    }

    @Override
    public void deleteValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        validationRuleGroupStore.delete( validationRuleGroup );
    }

    @Override
    public void updateValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        validationRuleGroupStore.update( validationRuleGroup );
    }

    @Override
    public ValidationRuleGroup getValidationRuleGroup( int id )
    {
        return validationRuleGroupStore.get( id );
    }

    @Override
    public ValidationRuleGroup getValidationRuleGroup( String uid )
    {
        return validationRuleGroupStore.getByUid( uid );
    }

    @Override
    public List<ValidationRuleGroup> getAllValidationRuleGroups()
    {
        return validationRuleGroupStore.getAll();
    }

    @Override
    public ValidationRuleGroup getValidationRuleGroupByName( String name )
    {
        return validationRuleGroupStore.getByName( name );
    }

    @Override
    public int getValidationRuleGroupCount()
    {
        return validationRuleGroupStore.getCount();
    }

    @Override
    public int getValidationRuleGroupCountByName( String name )
    {
        return validationRuleGroupStore.getCountLikeName( name ) ;
    }

    @Override
    public List<ValidationRuleGroup> getValidationRuleGroupsBetween( int first, int max )
    {
        return validationRuleGroupStore.getAllOrderedName( first, max );
    }

    @Override
    public List<ValidationRuleGroup> getValidationRuleGroupsBetweenByName( String name, int first, int max )
    {
        return validationRuleGroupStore.getAllLikeName( name, first, max ) ;
    }
}
