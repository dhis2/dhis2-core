package org.hisp.dhis.dataset.notifications;

import org.hisp.dhis.notification.TemplateVariable;

import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by zubair on 04.07.17.
 */
public enum DataSetNotificationTemplateVariables
    implements TemplateVariable
{
    DATASET_NAME( "data_name" ),
    DATASET_DESCRIPTION( "data_description" ),
    COMPLETE_REG_OU( "complete_registration_ou" ),
    COMPLETE_REG_PERIOD( "complete_registration_period" ),
    COMPLETE_REG_USER( "complete_registration_user" ),
    COMPLETE_REG_TIME( "complete_registration_time" ),
    COMPLETE_REG_ATT_OPT_COMBO( "complete_registration_att_opt_combo" );

    private static final Map<String, DataSetNotificationTemplateVariables> variableNameMap =
        EnumSet.allOf( DataSetNotificationTemplateVariables.class ).stream()
            .collect( Collectors.toMap( DataSetNotificationTemplateVariables::getVariableName, e -> e ) );

    private final String variableName;

    DataSetNotificationTemplateVariables( String variableName )
    {
        this.variableName = variableName;
    }

    @Override
    public String getVariableName()
    {
        return variableName;
    }

    public static boolean isValidVariableName( String expressionName )
    {
        return variableNameMap.keySet().contains( expressionName );
    }

    public static DataSetNotificationTemplateVariables fromVariableName( String variableName )
    {
        return variableNameMap.get( variableName );
    }
}
