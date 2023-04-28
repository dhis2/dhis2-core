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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.badRequest;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.system.util.CodecUtils.filenameEncode;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dataanalysis.DataAnalysisParams;
import org.hisp.dhis.dataanalysis.DataAnalysisService;
import org.hisp.dhis.dataanalysis.FollowupAnalysisRequest;
import org.hisp.dhis.dataanalysis.FollowupAnalysisResponse;
import org.hisp.dhis.dataanalysis.FollowupAnalysisService;
import org.hisp.dhis.dataanalysis.FollowupParams;
import org.hisp.dhis.dataanalysis.MinMaxOutlierAnalysisService;
import org.hisp.dhis.dataanalysis.StdDevOutlierAnalysisService;
import org.hisp.dhis.dataanalysis.UpdateFollowUpForDataValuesRequest;
import org.hisp.dhis.dataanalysis.ValidationRuleExpressionDetails;
import org.hisp.dhis.dataanalysis.ValidationRulesAnalysisParams;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.validation.Importance;
import org.hisp.dhis.validation.ValidationAnalysisParams;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.ValidationRuleService;
import org.hisp.dhis.validation.ValidationService;
import org.hisp.dhis.validation.comparator.ValidationResultComparator;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.ValidationResultView;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Joao Antunes
 */
@OpenApi.Tags( "data" )
@Controller
@RequestMapping( value = DataAnalysisController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@Slf4j
@PreAuthorize( "hasRole('ALL') or hasRole('F_RUN_VALIDATION')" )
public class DataAnalysisController
{
    public static final String RESOURCE_PATH = "/dataAnalysis";

    private static final String KEY_ANALYSIS_DATA_VALUES = "analysisDataValues";

    private static final String KEY_VALIDATION_RESULT = "validationResult";

    private static final String KEY_ORG_UNIT = "orgUnit";

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private StdDevOutlierAnalysisService stdDevOutlierAnalysisService;

    @Autowired
    private MinMaxOutlierAnalysisService minMaxOutlierAnalysisService;

    @Autowired
    private FollowupAnalysisService followupAnalysisService;

    @PostMapping( value = "/validationRules", consumes = APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.OK )
    public @ResponseBody List<ValidationResultView> performValidationRulesAnalysis(
        @RequestBody ValidationRulesAnalysisParams validationRulesAnalysisParams,
        HttpSession session )
        throws WebMessageException
    {
        I18nFormat format = i18nManager.getI18nFormat();

        ValidationRuleGroup group = null;
        if ( validationRulesAnalysisParams.getVrg() != null )
        {
            group = validationRuleService
                .getValidationRuleGroup( validationRulesAnalysisParams.getVrg() );
        }

        OrganisationUnit organisationUnit = organisationUnitService
            .getOrganisationUnit( validationRulesAnalysisParams.getOu() );
        if ( organisationUnit == null )
        {
            throw new WebMessageException( badRequest( "No organisation unit defined" ) );
        }

        ValidationAnalysisParams params = validationService.newParamsBuilder( group, organisationUnit,
            format.parseDate( validationRulesAnalysisParams.getStartDate() ),
            format.parseDate( validationRulesAnalysisParams.getEndDate() ) )
            .withIncludeOrgUnitDescendants( true )
            .withPersistResults( validationRulesAnalysisParams.isPersist() )
            .withSendNotifications( validationRulesAnalysisParams.isNotification() )
            .withMaxResults( ValidationService.MAX_INTERACTIVE_ALERTS )
            .build();

        List<ValidationResult> validationResults = validationService.validationAnalysis( params,
            NoopJobProgress.INSTANCE );

        validationResults.sort( new ValidationResultComparator() );

        session.setAttribute( KEY_VALIDATION_RESULT, validationResults );
        session.setAttribute( KEY_ORG_UNIT, organisationUnit );

        return validationResultsListToResponse( validationResults );
    }

    @GetMapping( "validationRulesExpression" )
    @ResponseStatus( HttpStatus.OK )
    public @ResponseBody ValidationRuleExpressionDetails getValidationRuleExpressionDetials(
        @OpenApi.Param( { UID.class, ValidationRule.class } ) @RequestParam String validationRuleId,
        @OpenApi.Param( Period.class ) @RequestParam String periodId,
        @OpenApi.Param( { UID.class, OrganisationUnit.class } ) @RequestParam String organisationUnitId,
        @OpenApi.Param( { UID.class,
            CategoryOptionCombo.class } ) @RequestParam( required = false ) String attributeOptionComboId )
        throws WebMessageException
    {
        ValidationRule validationRule = validationRuleService.getValidationRule( validationRuleId );
        if ( validationRule == null )
        {
            throw new WebMessageException(
                notFound( "Can't find ValidationRule with id =" + validationRuleId ) );
        }

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( organisationUnitId );
        if ( organisationUnit == null )
        {
            throw new WebMessageException(
                notFound( "Can't find OrganisationUnit with id =" + organisationUnitId ) );
        }

        Period period = periodService.getPeriod( periodId );
        if ( period == null )
        {
            throw new WebMessageException( notFound( "Can't find Period with id =" + periodId ) );
        }

        CategoryOptionCombo attributeOptionCombo;
        if ( attributeOptionComboId == null )
        {
            attributeOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        }
        else
        {
            attributeOptionCombo = categoryService.getCategoryOptionCombo( attributeOptionComboId );
            if ( attributeOptionCombo == null )
            {
                throw new WebMessageException(
                    notFound( "Can't find AttributeOptionCombo with id = " + attributeOptionComboId ) );
            }
        }

        ValidationAnalysisParams params = validationService.newParamsBuilder(
            Lists.newArrayList( validationRule ), organisationUnit, Lists.newArrayList( period ) )
            .withAttributeOptionCombo( attributeOptionCombo )
            .build();

        return validationService.getValidationRuleExpressionDetails( params );
    }

    @PostMapping( value = "/stdDevOutlier", consumes = APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.OK )
    public @ResponseBody List<DeflatedDataValue> performStdDevOutlierAnalysis(
        @RequestBody DataAnalysisParams stdDevOutlierAnalysisParams,
        HttpSession session )
        throws WebMessageException
    {
        I18nFormat format = i18nManager.getI18nFormat();

        OrganisationUnit organisationUnit = organisationUnitService
            .getOrganisationUnit( stdDevOutlierAnalysisParams.getOu() );
        if ( organisationUnit == null )
        {
            throw new WebMessageException( badRequest( "No organisation unit defined" ) );
        }

        Collection<Period> periods = periodService
            .getPeriodsBetweenDates( format.parseDate( stdDevOutlierAnalysisParams.getStartDate() ),
                format.parseDate( stdDevOutlierAnalysisParams.getEndDate() ) );

        Set<DataElement> dataElements = new HashSet<>();

        for ( String uid : stdDevOutlierAnalysisParams.getDs() )
        {
            dataElements.addAll( dataSetService.getDataSet( uid ).getDataElements() );
        }

        Date from = new DateTime( format.parseDate( stdDevOutlierAnalysisParams.getStartDate() ) ).minusYears( 2 )
            .toDate();

        log.info( "From date: " + stdDevOutlierAnalysisParams.getStartDate() + ", To date: " +
            stdDevOutlierAnalysisParams.getEndDate() + ", Organisation unit: " + organisationUnit
            + ", Std dev: " + stdDevOutlierAnalysisParams.getStandardDeviation() );

        log.info( "Nr of data elements: " + dataElements.size() + " Nr of periods: " + periods.size() +
            "for Standard Deviation Outlier Analysis" );

        List<DeflatedDataValue> dataValues = new ArrayList<>( stdDevOutlierAnalysisService
            .analyse( Sets.newHashSet( organisationUnit ), dataElements, periods,
                stdDevOutlierAnalysisParams.getStandardDeviation(), from ) );

        session.setAttribute( KEY_ANALYSIS_DATA_VALUES, dataValues );
        session.setAttribute( KEY_ORG_UNIT, organisationUnit );

        return deflatedValuesListToResponse( dataValues );
    }

    @PostMapping( value = "/minMaxOutlier", consumes = APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.OK )
    public @ResponseBody List<DeflatedDataValue> performMinMaxOutlierAnalysis(
        @RequestBody DataAnalysisParams params,
        HttpSession session )
        throws WebMessageException
    {
        I18nFormat format = i18nManager.getI18nFormat();

        OrganisationUnit organisationUnit = organisationUnitService
            .getOrganisationUnit( params.getOu() );
        if ( organisationUnit == null )
        {
            throw new WebMessageException( badRequest( "No organisation unit defined" ) );
        }

        Collection<Period> periods = periodService
            .getPeriodsBetweenDates( format.parseDate( params.getStartDate() ),
                format.parseDate( params.getEndDate() ) );

        Set<DataElement> dataElements = new HashSet<>();

        if ( params.getDs() != null )
        {
            for ( String uid : params.getDs() )
            {
                dataElements.addAll( dataSetService.getDataSet( uid ).getDataElements() );
            }
        }

        Date from = new DateTime( format.parseDate( params.getStartDate() ) ).minusYears( 2 )
            .toDate();

        log.info( "From date: " + params.getStartDate() + ", To date: " +
            params.getEndDate() + ", Organisation unit: " + organisationUnit );

        log.info( "Nr of data elements: " + dataElements.size() + " Nr of periods: " + periods.size() +
            " for Min Max Outlier Analysis" );

        List<DeflatedDataValue> dataValues = new ArrayList<>( minMaxOutlierAnalysisService
            .analyse( Sets.newHashSet( organisationUnit ), dataElements, periods, null, from ) );

        session.setAttribute( KEY_ANALYSIS_DATA_VALUES, dataValues );
        session.setAttribute( KEY_ORG_UNIT, organisationUnit );

        return deflatedValuesListToResponse( dataValues );
    }

    @PostMapping( value = "/followup", consumes = APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.OK )
    public @ResponseBody List<DeflatedDataValue> performFollowupAnalysis( @RequestBody DataAnalysisParams params,
        HttpSession session )
        throws WebMessageException
    {
        I18nFormat format = i18nManager.getI18nFormat();

        OrganisationUnit organisationUnit = organisationUnitService
            .getOrganisationUnit( params.getOu() );
        if ( organisationUnit == null )
        {
            throw new WebMessageException( badRequest( "No organisation unit defined" ) );
        }

        Collection<Period> periods = periodService
            .getPeriodsBetweenDates( format.parseDate( params.getStartDate() ),
                format.parseDate( params.getEndDate() ) );

        Set<DataElement> dataElements = new HashSet<>();

        if ( params.getDs() != null )
        {
            for ( String uid : params.getDs() )
            {
                dataElements.addAll( dataSetService.getDataSet( uid ).getDataElements() );
            }
        }

        List<DeflatedDataValue> dataValues = new ArrayList<>( followupAnalysisService
            .getFollowupDataValues( Sets.newHashSet( organisationUnit ), dataElements,
                periods, DataAnalysisService.MAX_OUTLIERS + 1 ) );
        // +1 to detect overflow

        session.setAttribute( KEY_ANALYSIS_DATA_VALUES, dataValues );
        session.setAttribute( KEY_ORG_UNIT, organisationUnit );

        return deflatedValuesListToResponse( dataValues );
    }

    @GetMapping( value = "/followup", produces = APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.OK )
    public @ResponseBody FollowupAnalysisResponse performFollowupAnalysis( HttpSession session,
        FollowupAnalysisRequest request )
    {
        FollowupAnalysisResponse results = followupAnalysisService.getFollowupDataValues( request );
        session.setAttribute( KEY_ANALYSIS_DATA_VALUES, results );
        return results;
    }

    @PostMapping( value = "/followup/mark", consumes = APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public @ResponseBody void markDataValues( @RequestBody UpdateFollowUpForDataValuesRequest params )
    {
        log.info( "markDataValues from DataAnalysisController input " + params );

        List<DataValue> dataValues = new ArrayList<>();
        for ( FollowupParams followup : params.getFollowups() )
        {
            DataElement dataElement = dataElementService.getDataElement( followup.getDataElementId() );
            Period period = periodService.getPeriod( followup.getPeriodId() );
            OrganisationUnit source = organisationUnitService.getOrganisationUnit( followup.getOrganisationUnitId() );
            CategoryOptionCombo categoryOptionCombo = categoryService
                .getCategoryOptionCombo( followup.getCategoryOptionComboId() );
            CategoryOptionCombo attributeOptionCombo = categoryService
                .getCategoryOptionCombo( followup.getAttributeOptionComboId() );

            DataValue dataValue = dataValueService
                .getDataValue( dataElement, period, source, categoryOptionCombo, attributeOptionCombo );

            if ( dataValue != null )
            {
                dataValue.setFollowup( followup.isFollowup() );
                dataValues.add( dataValue );
            }
        }

        if ( dataValues.size() > 0 )
        {
            dataValueService.updateDataValues( dataValues );
        }
    }

    @GetMapping( "/report.pdf" )
    public void getPdfReport( HttpSession session, HttpServletResponse response )
        throws Exception
    {
        Grid grid = getGridFromAnalysisResult( session.getAttribute( KEY_ANALYSIS_DATA_VALUES ),
            (OrganisationUnit) session.getAttribute(
                KEY_ORG_UNIT ) );

        String filename = filenameEncode( grid.getTitle() ) + ".pdf";
        contextUtils
            .configureResponse( response, ContextUtils.CONTENT_TYPE_PDF, CacheStrategy.RESPECT_SYSTEM_SETTING, filename,
                false );

        GridUtils.toPdf( grid, response.getOutputStream() );
    }

    @GetMapping( "/report.xls" )
    public void getXlsReport( HttpSession session, HttpServletResponse response )
        throws Exception
    {
        Grid grid = getGridFromAnalysisResult( session.getAttribute( KEY_ANALYSIS_DATA_VALUES ),
            (OrganisationUnit) session.getAttribute(
                KEY_ORG_UNIT ) );

        String filename = filenameEncode( grid.getTitle() ) + ".xls";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, false );

        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @GetMapping( "/report.csv" )
    public void getCSVReport( HttpSession session, HttpServletResponse response )
        throws Exception
    {
        Grid grid = getGridFromAnalysisResult( session.getAttribute( KEY_ANALYSIS_DATA_VALUES ),
            (OrganisationUnit) session.getAttribute(
                KEY_ORG_UNIT ) );

        String filename = filenameEncode( grid.getTitle() ) + ".csv";
        contextUtils
            .configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING, filename,
                false );

        GridUtils.toCsv( getGridFromAnalysisResult( session.getAttribute( KEY_ANALYSIS_DATA_VALUES ),
            (OrganisationUnit) session.getAttribute(
                KEY_ORG_UNIT ) ),
            response.getWriter() );
    }

    @GetMapping( "validationRules/report.pdf" )
    public void getValidationRulesPdfReport( HttpSession session, HttpServletResponse response )
        throws Exception
    {
        @SuppressWarnings( "unchecked" )
        List<ValidationResult> results = (List<ValidationResult>) session.getAttribute( KEY_VALIDATION_RESULT );
        Grid grid = generateValidationRulesReportGridFromResults( results, (OrganisationUnit) session.getAttribute(
            KEY_ORG_UNIT ) );

        String filename = filenameEncode( grid.getTitle() ) + ".pdf";
        contextUtils
            .configureResponse( response, ContextUtils.CONTENT_TYPE_PDF, CacheStrategy.RESPECT_SYSTEM_SETTING, filename,
                false );

        GridUtils.toPdf( grid, response.getOutputStream() );
    }

    @GetMapping( "validationRules/report.xls" )
    public void getValidationRulesXlsReport( HttpSession session, HttpServletResponse response )
        throws Exception
    {
        @SuppressWarnings( "unchecked" )
        List<ValidationResult> results = (List<ValidationResult>) session.getAttribute( KEY_VALIDATION_RESULT );
        Grid grid = generateValidationRulesReportGridFromResults( results, (OrganisationUnit) session.getAttribute(
            KEY_ORG_UNIT ) );

        String filename = filenameEncode( grid.getTitle() ) + ".xls";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, false );

        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @GetMapping( "validationRules/report.csv" )
    public void getValidationRulesCSVReport( HttpSession session, HttpServletResponse response )
        throws Exception
    {
        @SuppressWarnings( "unchecked" )
        List<ValidationResult> results = (List<ValidationResult>) session.getAttribute( KEY_VALIDATION_RESULT );
        Grid grid = generateValidationRulesReportGridFromResults( results, (OrganisationUnit) session.getAttribute(
            KEY_ORG_UNIT ) );

        String filename = filenameEncode( grid.getTitle() ) + ".csv";
        contextUtils
            .configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING, filename,
                false );

        GridUtils.toCsv( grid, response.getWriter() );
    }

    private Grid generateAnalysisReportGridFromResults( List<DeflatedDataValue> results, OrganisationUnit orgUnit )
    {
        Grid grid = new ListGrid();
        if ( results != null )
        {
            I18nFormat format = i18nManager.getI18nFormat();
            I18n i18n = i18nManager.getI18n();

            grid.setTitle( i18n.getString( "data_analysis_report" ) );

            if ( orgUnit != null )
            {
                grid.setSubtitle( orgUnit.getName() );
            }

            grid.addHeader( new GridHeader( i18n.getString( "dataelement" ), false, true ) );
            grid.addHeader( new GridHeader( i18n.getString( "source" ), false, true ) );
            grid.addHeader( new GridHeader( i18n.getString( "period" ), false, true ) );
            grid.addHeader( new GridHeader( i18n.getString( "min" ), false, false ) );
            grid.addHeader( new GridHeader( i18n.getString( "value" ), false, false ) );
            grid.addHeader( new GridHeader( i18n.getString( "max" ), false, false ) );

            for ( DeflatedDataValue dataValue : results )
            {
                Period period = dataValue.getPeriod();

                grid.addRow();
                grid.addValue( dataValue.getDataElementName() );
                grid.addValue( dataValue.getSourceName() );
                grid.addValue( format.formatPeriod( period ) );
                grid.addValue( dataValue.getMin() );
                grid.addValue( dataValue.getValue() );
                grid.addValue( dataValue.getMax() );
            }
        }

        return grid;
    }

    private Grid generateValidationRulesReportGridFromResults( List<ValidationResult> results,
        OrganisationUnit orgUnit )
    {
        Grid grid = new ListGrid();
        if ( results != null )
        {
            I18nFormat format = i18nManager.getI18nFormat();
            I18n i18n = i18nManager.getI18n();

            grid.setTitle( i18n.getString( "data_quality_report" ) );

            if ( orgUnit != null )
            {
                grid.setSubtitle( orgUnit.getName() );
            }

            grid.addHeader( new GridHeader( i18n.getString( "source" ), false, true ) );
            grid.addHeader( new GridHeader( i18n.getString( "period" ), false, true ) );
            grid.addHeader( new GridHeader( i18n.getString( "validation_rule" ), false, true ) );
            grid.addHeader( new GridHeader( i18n.getString( "importance" ), false, true ) );
            grid.addHeader( new GridHeader( i18n.getString( "left_side_description" ), false, true ) );
            grid.addHeader( new GridHeader( i18n.getString( "value" ), false, false ) );
            grid.addHeader( new GridHeader( i18n.getString( "operator" ), false, false ) );
            grid.addHeader( new GridHeader( i18n.getString( "value" ), false, false ) );
            grid.addHeader( new GridHeader( i18n.getString( "right_side_description" ), false, true ) );

            for ( ValidationResult validationResult : results )
            {
                OrganisationUnit unit = validationResult.getOrganisationUnit();
                Period period = validationResult.getPeriod();
                Importance importance = validationResult.getValidationRule().getImportance();
                Operator operator = validationResult.getValidationRule().getOperator();

                grid.addRow();
                grid.addValue( unit.getName() );
                grid.addValue( format.formatPeriod( period ) );
                grid.addValue( validationResult.getValidationRule().getName() );
                grid.addValue( i18n.getString( importance.toString().toLowerCase() ) );
                grid.addValue( validationResult.getValidationRule().getLeftSide().getDescription() );
                grid.addValue( String.valueOf( validationResult.getLeftsideValue() ) );
                grid.addValue( i18n.getString( operator.toString() ) );
                grid.addValue( String.valueOf( validationResult.getRightsideValue() ) );
                grid.addValue( validationResult.getValidationRule().getRightSide().getDescription() );
            }
        }

        return grid;
    }

    private List<DeflatedDataValue> deflatedValuesListToResponse( List<DeflatedDataValue> deflatedDataValues )
    {
        I18nFormat format = i18nManager.getI18nFormat();
        if ( deflatedDataValues == null )
        {
            return Collections.emptyList();
        }

        if ( deflatedDataValues.size() > DataAnalysisService.MAX_OUTLIERS )
        {
            return deflatedDataValues.subList( 0, DataAnalysisService.MAX_OUTLIERS );
        }

        for ( DeflatedDataValue dataValue : deflatedDataValues )
        {
            dataValue.getPeriod().setName( format.formatPeriod( dataValue.getPeriod() ) );
        }

        return deflatedDataValues;
    }

    private List<ValidationResultView> validationResultsListToResponse( List<ValidationResult> validationResults )
    {
        I18nFormat format = i18nManager.getI18nFormat();
        if ( validationResults == null )
        {
            return Collections.emptyList();
        }

        List<ValidationResultView> validationResultViews = new ArrayList<>( validationResults.size() );
        for ( ValidationResult validationResult : validationResults )
        {
            ValidationResultView validationResultView = new ValidationResultView();
            ValidationRule validationRule = validationResult.getValidationRule();
            if ( validationRule != null )
            {
                validationResultView.setValidationRuleId( validationRule.getUid() );
                validationResultView.setValidationRuleDescription( validationRule.getDescription() );
                validationResultView.setImportance( validationRule.getImportance().toString() );
                validationResultView.setOperator( validationRule.getOperator().getMathematicalOperator() );
            }

            OrganisationUnit organisationUnit = validationResult.getOrganisationUnit();
            if ( organisationUnit != null )
            {
                validationResultView.setOrganisationUnitId( organisationUnit.getUid() );
                validationResultView.setOrganisationUnitDisplayName( organisationUnit.getDisplayName() );
                validationResultView.setOrganisationUnitPath( organisationUnit.getPath() );
                validationResultView.setOrganisationUnitAncestorNames( organisationUnit.getAncestorNames() );
            }

            Period period = validationResult.getPeriod();
            if ( period != null )
            {
                validationResultView.setPeriodId( period.getIsoDate() );
                validationResultView.setPeriodDisplayName( format.formatPeriod( period ) );
            }

            CategoryOptionCombo attributeOptionCombo = validationResult.getAttributeOptionCombo();
            if ( attributeOptionCombo != null )
            {
                validationResultView.setAttributeOptionComboId( attributeOptionCombo.getUid() );
                validationResultView.setAttributeOptionComboDisplayName( attributeOptionCombo.getDisplayName() );
            }

            validationResultView.setLeftSideValue( validationResult.getLeftsideValue() );
            validationResultView.setRightSideValue( validationResult.getRightsideValue() );

            validationResultViews.add( validationResultView );
        }

        return validationResultViews;
    }

    /**
     * Generate Grid response from analysis result.
     *
     * @param result
     * @param organisationUnit
     * @return {@link Grid} to be returned to the client
     */
    private Grid getGridFromAnalysisResult( Object result, OrganisationUnit organisationUnit )
    {
        Grid grid;
        if ( result instanceof FollowupAnalysisResponse )
        {
            grid = followupAnalysisService.generateAnalysisReport( (FollowupAnalysisResponse) result );
        }
        else
        {
            grid = generateAnalysisReportGridFromResults( (List<DeflatedDataValue>) result, organisationUnit );
        }

        return grid;
    }
}
