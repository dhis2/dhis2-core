package org.hisp.dhis.notification;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.notifications.DataSetNotificationTemplateVariables;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by zubair on 04.07.17.
 */
public class DataSetNotificationMessageRenderer
    extends BaseNotificationMessageRenderer<CompleteDataSetRegistration>
{
    private final ImmutableMap<TemplateVariable, Function<CompleteDataSetRegistration, String>> VARIABLE_RESOLVERS =
        new ImmutableMap.Builder<TemplateVariable, Function<CompleteDataSetRegistration, String>>()
            .put( DataSetNotificationTemplateVariables.DATASET_NAME, cdsr -> cdsr.getDataSet().getName() )
            .put( DataSetNotificationTemplateVariables.DATASET_DESCRIPTION, cdsr -> cdsr.getDataSet().getDescription() )
            .put( DataSetNotificationTemplateVariables.COMPLETE_REG_OU, cdsr -> cdsr.getSource().getName() )
            .put( DataSetNotificationTemplateVariables.COMPLETE_REG_PERIOD, CompleteDataSetRegistration::getPeriodName )
            .put( DataSetNotificationTemplateVariables.COMPLETE_REG_USER, CompleteDataSetRegistration::getStoredBy )
            .put( DataSetNotificationTemplateVariables.COMPLETE_REG_TIME, cdsr -> DateUtils.getMediumDateString() )
            .put( DataSetNotificationTemplateVariables.COMPLETE_REG_ATT_OPT_COMBO, cdsr -> getAttributeOptionCombo( cdsr ) )
            .put( DataSetNotificationTemplateVariables.CURRENT_DATE, cdsr -> formatDate( new Date() ) )
            .build();

    private static final ImmutableSet<ExpressionType> SUPPORTED_EXPRESSION_TYPES = ImmutableSet.of( ExpressionType.VARIABLE );

    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    public DataSetNotificationMessageRenderer()
    {
    }

    @Override
    protected Map<TemplateVariable, Function<CompleteDataSetRegistration, String>> getVariableResolvers()
    {
        return VARIABLE_RESOLVERS;
    }

    @Override
    protected Map<String, String> resolveTrackedEntityAttributeValues( Set<String> attributeKeys, CompleteDataSetRegistration entity )
    {
        // Attributes are not supported for dataset notifications
        return Collections.emptyMap();
    }

    @Override
    protected TemplateVariable fromVariableName( String name )
    {
        return DataSetNotificationTemplateVariables.fromVariableName( name );
    }

    @Override
    protected Map<String, String> resolveDataElementValues( Set<String> elementKeys, CompleteDataSetRegistration entity )
    {
        // DataElement is not supported for dataset notifications
        return Collections.emptyMap();
    }

    @Override
    protected Set<ExpressionType> getSupportedExpressionTypes()
    {
        return SUPPORTED_EXPRESSION_TYPES;
    }

    private String getAttributeOptionCombo( CompleteDataSetRegistration registration )
    {
        if ( registration.getAttributeOptionCombo() != null )
        {
            return registration.getAttributeOptionCombo().getName();
        }

        return dataElementCategoryService.getDefaultDataElementCategoryOptionCombo().getName();
    }
}
