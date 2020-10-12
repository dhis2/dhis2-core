package org.hisp.dhis.approvalvalidationrule;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRule;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRuleService;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRuleStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Mike Nelushi
 */
@Transactional
@Service( "org.hisp.dhis.approvalvalidationrule.ApprovalValidationRuleService" )
public class DefaultApprovalValidationRuleService
    implements ApprovalValidationRuleService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ApprovalValidationRuleStore approvalValidationRuleStore;
    
    public DefaultApprovalValidationRuleService(ApprovalValidationRuleStore approvalValidationRuleStore) {
    	checkNotNull( approvalValidationRuleStore  );
    	
		this.approvalValidationRuleStore = approvalValidationRuleStore;
	}
    

    // -------------------------------------------------------------------------
    // ApprovalValidationRule CRUD operations
    // -------------------------------------------------------------------------

    @Override
    public long saveApprovalValidationRule( ApprovalValidationRule approvalValidationRule )
    {
        approvalValidationRuleStore.save( approvalValidationRule );

        return approvalValidationRule.getId();
    }

	@Override
    public void updateApprovalValidationRule( ApprovalValidationRule approvalValidationRule )
    {
        approvalValidationRuleStore.update( approvalValidationRule );
    }

    @Override
    public void deleteApprovalValidationRule( ApprovalValidationRule approvalValidationRule )
    {
        approvalValidationRuleStore.delete( approvalValidationRule );
    }

    @Override
    public ApprovalValidationRule getApprovalValidationRule( long id )
    {
        return approvalValidationRuleStore.get( id );
    }

    @Override
    public ApprovalValidationRule getApprovalValidationRule( String uid )
    {
        return approvalValidationRuleStore.getByUid( uid );
    }

    @Override
    public List<ApprovalValidationRule> getAllApprovalValidationRules()
    {
        return approvalValidationRuleStore.getAllApprovalValidationRules();
    }

    @Override
    public ApprovalValidationRule getApprovalValidationRuleByName( String name )
    {
        return approvalValidationRuleStore.getByName( name );
    }

    @Override
    public List<ApprovalValidationRule> getApprovalValidationRules( boolean skipApprovalValidation )
    {
        return approvalValidationRuleStore.getApprovalValidationRules( skipApprovalValidation );
    }

}
