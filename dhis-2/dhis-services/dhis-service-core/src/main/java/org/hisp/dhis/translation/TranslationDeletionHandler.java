package org.hisp.dhis.translation;

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

import java.util.Collection;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.i18n.I18nService;
import org.hisp.dhis.i18n.locale.I18nLocale;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.system.util.LocaleUtils;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;

/**
 * @author Lars Helge Overland
 */
public class TranslationDeletionHandler
    extends DeletionHandler
{
    private I18nService i18nService;

    public void setI18nService( I18nService service )
    {
        i18nService = service;
    }

    private TranslationService translationService;

    public void setTranslationService( TranslationService translationService )
    {
        this.translationService = translationService;
    }
    
    @Override
    protected String getClassName()
    {
        return Translation.class.getSimpleName();
    }
    
    @Override
    public void deleteDataElement( DataElement dataElement )
    {
        i18nService.removeObject( dataElement );
    }
    
    @Override
    public void deleteDataElementGroup( DataElementGroup dataElementGroup )
    {
        i18nService.removeObject( dataElementGroup );
    }

    @Override
    public void deleteDataElementGroupSet( DataElementGroupSet dataElementGroupSet )
    {
        i18nService.removeObject( dataElementGroupSet );
    }

    @Override
    public void deleteDataSet( DataSet dataSet )
    {
        i18nService.removeObject( dataSet );
    }
    
    @Override
    public void deleteSection( Section section )
    {
        i18nService.removeObject( section );
    }

    @Override
    public void deleteIndicator( Indicator indicator )
    {
        i18nService.removeObject( indicator );
    }

    @Override
    public void deleteIndicatorGroup( IndicatorGroup indicatorGroup )
    {
        i18nService.removeObject( indicatorGroup );
    }

    @Override
    public void deleteIndicatorGroupSet( IndicatorGroupSet indicatorGroupSet )
    {
        i18nService.removeObject( indicatorGroupSet );
    }

    @Override
    public void deleteIndicatorType( IndicatorType indicatorType )
    {
        i18nService.removeObject( indicatorType );
    }

    @Override
    public void deleteValidationRule( ValidationRule validationRule )
    {
        i18nService.removeObject( validationRule );
    }

    @Override
    public void deleteValidationRuleGroup( ValidationRuleGroup validationRuleGroup )
    {
        i18nService.removeObject( validationRuleGroup );
    }

    @Override
    public void deleteOrganisationUnitGroup( OrganisationUnitGroup group )
    {
        i18nService.removeObject( group );
    }

    @Override
    public void deleteOrganisationUnitGroupSet( OrganisationUnitGroupSet groupSet )
    {
        i18nService.removeObject( groupSet );
    }
    
    @Override
    public String allowDeleteI18nLocale( I18nLocale i18nLocale )
    {
        Collection<Translation> translations = translationService.getTranslations( LocaleUtils.getLocale( i18nLocale.getLocale() ) );

        return translations.isEmpty() ? null : translations.iterator().next().getLocale();
    }
}
