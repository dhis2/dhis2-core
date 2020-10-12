package org.hisp.dhis.approval.dataapproval.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.approvalvalidationrule.ApprovalValidation;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRule;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRuleService;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationService;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.utils.InputUtils;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Mike Nelushi
 */
public class GetApprovalValidationRuleAction
    implements Action
{
    private static final String SEPERATOR = " - ";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ApprovalValidationRuleService approvalValidationRuleService;

    public void setApprovalValidationRuleService( ApprovalValidationRuleService approvalValidationRuleService )
    {
        this.approvalValidationRuleService = approvalValidationRuleService;
    }

    @Autowired
    private ApprovalValidationService approvalValidationService;

    public void setApprovalValidationService( ApprovalValidationService approvalValidationService )
    {
        this.approvalValidationService = approvalValidationService;
    }

    @Autowired
    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    @Autowired
    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    @Autowired
    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    @Autowired
    private CategoryService categoryService;

    public void setCategoryService( CategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private I18nFormat format;

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String ds;

    public void setDs( String ds )
    {
        this.ds = ds;
    }

    private String pe;

    public void setPe( String pe )
    {
        this.pe = pe;
    }

    private String ou;

    public void setOu( String ou )
    {
        this.ou = ou;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private List<ApprovalValidation> approvalValidations;

    public List<ApprovalValidation> getApprovalValidations()
    {
        return approvalValidations;
    }

    private List<ApprovalValidationRule> approvalValidationRules;

    public List<ApprovalValidationRule> getApprovalValidationRules()
    {
        return approvalValidationRules;
    }

    private Map<Long, Boolean> approvalValidationMap = new HashMap<>();

    public Map<Long, Boolean> getApprovalValidationMap()
    {
        return approvalValidationMap;
    }

    private DataSet selectedDataSet;

    public DataSet getSelectedDataSet()
    {
        return selectedDataSet;
    }

    private OrganisationUnit selectedOrgunit;

    public OrganisationUnit getSelectedOrgunit()
    {
        return selectedOrgunit;
    }

    private Period selectedPeriod;

    public Period getSelectedPeriod()
    {
        return selectedPeriod;
    }

    private String titleName;

    public String getTitleName()
    {
        return titleName;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( pe != null )
        {
            selectedPeriod = PeriodType.getPeriodFromIsoString( pe );
            selectedPeriod = periodService.reloadPeriod( selectedPeriod );
        }

        selectedDataSet = dataSetService.getDataSetNoAcl( ds );

        selectedOrgunit = organisationUnitService.getOrganisationUnit( ou );

        titleName = selectedDataSet.getName() + SEPERATOR + selectedOrgunit.getName() + SEPERATOR
            + format.formatPeriod( selectedPeriod );

        List<Period> periods = new ArrayList<>();
        periods.add( selectedPeriod );

        approvalValidationRules = approvalValidationRuleService.getApprovalValidationRules( false ).stream()
            .filter( approvalValidationRule -> approvalValidationRule.getOrganisationUnitLevels()
                .contains( selectedOrgunit.getLevel() ) )
            .collect( Collectors.toList() );

        approvalValidations = approvalValidationService.getApprovalValidations( selectedDataSet, selectedOrgunit, false,
            approvalValidationRules, periods );

        addApprovalValidation( approvalValidationMap, approvalValidations );

        addMissingApprovalValidation( approvalValidationMap, approvalValidationRules );

        return SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void addApprovalValidation( Map<Long, Boolean> approvalMap,
        List<ApprovalValidation> approvalValidations )
    {
        approvalValidations.forEach( av -> {
            approvalMap.put( av.getApprovalValidationRule().getId(), true );
        } );
    }

    private void addMissingApprovalValidation( Map<Long, Boolean> approvalMap,
        List<ApprovalValidationRule> approvalValidationRules )
    {
        approvalValidationRules.forEach( avr -> {
            if ( !approvalMap.containsKey( avr.getId() ) )
            {
                approvalMap.put( avr.getId(), false );
            }
        } );
    }

}
