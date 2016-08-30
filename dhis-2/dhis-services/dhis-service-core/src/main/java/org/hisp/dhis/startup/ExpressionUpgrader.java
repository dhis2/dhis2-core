package org.hisp.dhis.startup;

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

import static org.hisp.dhis.dataentryform.DataEntryFormService.DATAELEMENT_TOTAL_PATTERN;
import static org.hisp.dhis.dataentryform.DataEntryFormService.IDENTIFIER_PATTERN;
import static org.hisp.dhis.dataentryform.DataEntryFormService.INDICATOR_PATTERN;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Upgrades indicator formulas, expressions (for validation rules) and custom
 * data entry forms from using identifiers to using uids.
 * 
 * @author Lars Helge Overland
 */
public class ExpressionUpgrader
    extends TransactionContextStartupRoutine
{
    private static final String OLD_OPERAND_EXPRESSION = "\\[(\\d+)\\.?(\\d*)\\]";
    private static final String OLD_CONSTANT_EXPRESSION = "\\[C(\\d+?)\\]";

    private static final Pattern OLD_OPERAND_PATTERN = Pattern.compile( OLD_OPERAND_EXPRESSION );
    private static final Pattern OLD_CONSTANT_PATTERN = Pattern.compile( OLD_CONSTANT_EXPRESSION );

    private static final Log log = LogFactory.getLog( ExpressionUpgrader.class );
    
    @Autowired
    private DataEntryFormService dataEntryFormService;
    
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private DataElementCategoryService categoryService;
    
    @Autowired
    private IndicatorService indicatorService;
    
    @Autowired
    private ConstantService constantService;
    
    @Autowired
    private ExpressionService expressionService;

    @Override
    public void executeInTransaction()
    {
        upgradeIndicators();
        upgradeExpressions();
        upgradeDataEntryForms();
    }
    
    private void upgradeIndicators()
    {
        Collection<Indicator> indicators = indicatorService.getAllIndicators();
        
        for ( Indicator indicator : indicators )
        {
            String numerator = upgradeExpression( indicator.getNumerator() );
            String denominator = upgradeExpression( indicator.getDenominator() );
            
            if ( numerator != null || denominator != null )
            {
                indicator.setNumerator( numerator );
                indicator.setDenominator( denominator );
                indicatorService.updateIndicator( indicator );
            }
        }
    }

    private void upgradeExpressions()
    {
        Collection<Expression> expressions = expressionService.getAllExpressions();
        
        for ( Expression expression : expressions )
        {
            String expr = upgradeExpression( expression.getExpression() );
            
            if ( expr != null )
            {
                expression.setExpression( expr );
                expressionService.updateExpression( expression );
            }
        }
    }
    
    private String upgradeExpression( String expression )
    {
        if ( expression == null || expression.trim().isEmpty() )
        {
            return null;
        }
        
        boolean changes = false;
        
        StringBuffer sb = new StringBuffer();

        try
        {
            // -----------------------------------------------------------------
            // Constants
            // -----------------------------------------------------------------
    
            Matcher matcher = OLD_CONSTANT_PATTERN.matcher( expression );
            
            while ( matcher.find() )
            {
                Constant constant = constantService.getConstant( Integer.parseInt( matcher.group( 1 ) ) );
                String replacement = "C{" + constant.getUid() + "}";
                matcher.appendReplacement( sb, replacement );
                changes = true;
            }
            
            matcher.appendTail( sb );
            expression = sb.toString();

            // -----------------------------------------------------------------
            // Operands
            // -----------------------------------------------------------------
    
            matcher = OLD_OPERAND_PATTERN.matcher( expression );
            sb = new StringBuffer();
            
            while ( matcher.find() )
            {
                DataElement de = dataElementService.getDataElement( Integer.parseInt( matcher.group( 1 ) ) );
                String replacement = "#{" + de.getUid();
                
                if ( matcher.groupCount() == 2 && matcher.group( 2 ) != null && !matcher.group( 2 ).trim().isEmpty() )
                {
                    DataElementCategoryOptionCombo coc = categoryService.getDataElementCategoryOptionCombo( Integer.parseInt( matcher.group( 2 ) ) );
                    replacement += "." + coc.getUid();
                }
                
                replacement += "}";
                matcher.appendReplacement( sb, replacement );                
                changes = true;
            }
    
            matcher.appendTail( sb );
            expression = sb.toString();            
        }
        catch ( Exception ex )
        {
            log.error( "Failed to upgrade expression: " + expression );
            log.error( ex ); // Log and continue
        }
        
        if ( changes )
        {
            log.info( "Upgraded expression: " + expression );
        }
        
        return changes ? expression : null;
    }
    
    private void upgradeDataEntryForms()
    {
        Collection<DataEntryForm> forms = dataEntryFormService.getAllDataEntryForms();
        
        for ( DataEntryForm form : forms )
        {
            if ( DataEntryForm.CURRENT_FORMAT > form.getFormat() && form.getHtmlCode() != null && !form.getHtmlCode().trim().isEmpty() )
            {
                try
                {
                    // ---------------------------------------------------------
                    // Identifiers
                    // ---------------------------------------------------------

                    Matcher matcher = IDENTIFIER_PATTERN.matcher( form.getHtmlCode() );
                    StringBuffer sb = new StringBuffer();
                    
                    while ( matcher.find() )
                    {
                        DataElement de = dataElementService.getDataElement( Integer.parseInt( matcher.group( 1 ) ) );
                        DataElementCategoryOptionCombo coc = categoryService.getDataElementCategoryOptionCombo( Integer.parseInt( matcher.group( 2 ) ) );                        
                        String replacement = "id=\"" + de.getUid() + "-" + coc.getUid() + "-val\"";                        
                        matcher.appendReplacement( sb, replacement );
                    }
                    
                    matcher.appendTail( sb );                    
                    form.setHtmlCode( sb.toString() );

                    // ---------------------------------------------------------
                    // Data element totals
                    // ---------------------------------------------------------

                    matcher = DATAELEMENT_TOTAL_PATTERN.matcher( form.getHtmlCode() );
                    sb = new StringBuffer();
                    
                    while ( matcher.find() )
                    {
                        DataElement de = dataElementService.getDataElement( Integer.parseInt( matcher.group( 1 ) ) );
                        String replacement = "dataelementid=\"" + de.getUid() + "\"";
                        matcher.appendReplacement( sb, replacement );
                    }

                    matcher.appendTail( sb );                    
                    form.setHtmlCode( sb.toString() );

                    // ---------------------------------------------------------
                    // Indicators
                    // ---------------------------------------------------------

                    matcher = INDICATOR_PATTERN.matcher( form.getHtmlCode() );
                    sb = new StringBuffer();
                    
                    while ( matcher.find() )
                    {
                        Indicator in = indicatorService.getIndicator( Integer.parseInt( matcher.group( 1 ) ) );
                        String replacement = "indicatorid=\"" + in.getUid() + "\"";
                        matcher.appendReplacement( sb, replacement );
                    }

                    matcher.appendTail( sb );                    
                    form.setHtmlCode( sb.toString() );

                    // ---------------------------------------------------------
                    // Update format and save
                    // ---------------------------------------------------------

                    form.setFormat( DataEntryForm.CURRENT_FORMAT );                    
                    dataEntryFormService.updateDataEntryForm( form );
                    
                    log.info( "Upgraded custom data entry form: " + form.getName() );
                }
                catch ( Exception ex )
                {
                    log.error( "Upgrading custom data entry form failed: " + form.getName() );
                    log.error( ex ); // Log and continue
                }
            }
        }
    }
}
