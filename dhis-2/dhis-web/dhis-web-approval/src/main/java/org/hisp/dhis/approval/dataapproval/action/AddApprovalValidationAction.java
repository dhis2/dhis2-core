package org.hisp.dhis.approval.dataapproval.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidation;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRule;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRuleService;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationService;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Mike Nelushi
 */
public class AddApprovalValidationAction implements Action
{
    private static final Log log = LogFactory.getLog( AddApprovalValidationAction.class );
    private static final String APRROVALRULE_SEP = ";";
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    
    @Autowired
    private ApprovalValidationRuleService approvalValidationRuleService;  

	public void setApprovalValidationRuleService(ApprovalValidationRuleService approvalValidationRuleService) {
		this.approvalValidationRuleService = approvalValidationRuleService;
	}	

	@Autowired
    private ApprovalValidationService approvalValidationService;  
    
    public void setApprovalValidationService(ApprovalValidationService approvalValidationService) {
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
    
    @Autowired
    private CurrentUserService currentUserService;
    public void setCurrentUserService(CurrentUserService currentUserService) {
		this.currentUserService = currentUserService;
	}

    
    // -----------------------------------------------------------------------
    // I18n
    // -----------------------------------------------------------------------

	protected I18n i18n;
    
    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
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
    
    /**
     * ApprovalValidationRuleIds parameters
     * <approvalvalidation-id>;<approvalvalidation-id>
     */    
    /*private String approvalValidationRuleIds;

    public void setApprovalValidationRuleIds(String approvalValidationRuleIds) {
		this.approvalValidationRuleIds = approvalValidationRuleIds;
	}*/
    
    private Map<Long, Boolean> approvalValidationMap = new HashMap<>();

    public Map<Long, Boolean> getApprovalValidationMap() {
		return approvalValidationMap;
	}


    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private String message;
    
    public String getMessage()
    {
        return message;
    }
    
    private OrganisationUnit selectedOrgunit;

    public OrganisationUnit getSelectedOrgunit()
    {
        return selectedOrgunit;
    }
    
    private DataSet selectedDataSet;

    public DataSet getSelectedDataSet()
    {
        return selectedDataSet;
    }

    private Period selectedPeriod;

    public Period getSelectedPeriod()
    {
        return selectedPeriod;
    }
    
    private Collection<ApprovalValidationRule> approvalValidationRules;
    
    public Collection<ApprovalValidationRule> getApprovalValidationRules() {
		return approvalValidationRules;
	}

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
    	// ---------------------------------------------------------------------
        // New approvalValidation or update existing object?
        // ---------------------------------------------------------------------

    	selectedDataSet = dataSetService.getDataSetNoAcl( ds );
    	
    	if ( pe != null )
        {
            selectedPeriod = PeriodType.getPeriodFromIsoString( pe );
            selectedPeriod = periodService.reloadPeriod( selectedPeriod );
        }
    	
    	selectedDataSet = dataSetService.getDataSetNoAcl( ds );

        selectedOrgunit = organisationUnitService.getOrganisationUnit( ou );
    	
    	CategoryOptionCombo attributeOptionCombo = categoryService.getDefaultCategoryOptionCombo();
    	
    	String storedBy = currentUserService.getCurrentUsername();
    	
    	//get checked approval validation rules
    	List<ApprovalValidationRule> approvalValidationRulesNew = new ArrayList<>();
    	
    	/*List<String> approvalValidationRuleIdItems = getApprovalValidationRuleIds( approvalValidationRuleIds );
    	
    	if ( approvalValidationRuleIdItems != null )
    	{
    		for ( String approvalValidationRuleId : approvalValidationRuleIdItems )
    		{
    			approvalValidationRules.add( getApprovalValidationRule( approvalValidationRuleId ) );
    		}
    	}*/
    	
    	approvalValidationMap.forEach((approvalValidationRuleId, checked) -> {
            if(checked){
            	approvalValidationRulesNew.add( getApprovalValidationRule( approvalValidationRuleId ) );
            }
        });
    	
    	
    	//get existing validations
    	List<Period> periods = new ArrayList<>();
    	periods.add(selectedPeriod);
    	approvalValidationRules = approvalValidationRuleService.getAllApprovalValidationRules();
    	List<ApprovalValidation> approvalValidations = approvalValidationService.getApprovalValidations(selectedDataSet, selectedOrgunit, false, approvalValidationRules, periods);
    	
    	Iterator<ApprovalValidation> iterator = approvalValidations.iterator();
        
        while ( iterator.hasNext() )
        {
        	ApprovalValidation approvalValidation = iterator.next();
            iterator.remove();
            approvalValidationService.deleteApprovalValidation(approvalValidation);
        }
    	
    	List<ApprovalValidation> approvalValidationslist = new ArrayList<>();
    	
    	if ( approvalValidationRulesNew != null )
        {
            for ( ApprovalValidationRule approvalValidationRule : approvalValidationRulesNew )
            {
            	
            	ApprovalValidation approvalValidation = new ApprovalValidation();
            	
            	approvalValidation.setApprovalValidationRule(approvalValidationRule);
            	approvalValidation.setDataSet(selectedDataSet);
            	approvalValidation.setPeriod(selectedPeriod);
            	approvalValidation.setOrganisationUnit(selectedOrgunit);;
            	approvalValidation.setAttributeOptionCombo(attributeOptionCombo); 
            	approvalValidation.setStoredBy(storedBy);
            	
            	approvalValidationslist.add(approvalValidation);
            }
        }

    	approvalValidationService.saveApprovalValidations(approvalValidationslist);
        
        return SUCCESS;
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------
    
    public static List<String> getApprovalValidationRuleIds( String param )
    {
        if ( param == null )
        {
            return null;
        }
        
        if ( param.split( APRROVALRULE_SEP ).length > 1 )
        {
            return new ArrayList<>( Arrays.asList( param.split( APRROVALRULE_SEP ) ) );
        }
        
        return new ArrayList<>();
    }
    
    public ApprovalValidationRule getApprovalValidationRule( Long approvalValidationRuleId )
    {		
		return approvalValidationRuleService.getApprovalValidationRule(approvalValidationRuleId);
	}
}
