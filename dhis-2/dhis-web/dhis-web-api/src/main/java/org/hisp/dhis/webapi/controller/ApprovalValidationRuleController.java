package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRule;
import org.hisp.dhis.schema.descriptors.ApprovalValidationRuleSchemaDescriptor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Mike Nelushi
 */
@Controller
@RequestMapping( value = ApprovalValidationRuleSchemaDescriptor.API_ENDPOINT )
public class ApprovalValidationRuleController
    extends AbstractCrudController<ApprovalValidationRule>
{
}
