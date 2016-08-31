jQuery( document ).ready( function()
{
    loadAvailableGroups();
    getIndicatorsByGroup();
    getAssignedIndicatorGroups();

    jQuery( "#addIndicatorGroupForm" ).dialog( {
        autoOpen : false,
        modal : true
    } );
    jQuery( "#tabs" ).tabs();
} );

function loadAvailableIndicators()
{
    var filter_1 = jQuery( '#view_1 #availableIndicatorsFilter' ).val();
    var filter_2 = jQuery( '#view_2 #availableIndicatorsFilter' ).val();
    var list_1 = jQuery( "#view_1 #availableIndicators" );
    var list_2 = jQuery( "#view_2 #availableIndicators2" );
    list_1.empty();
    list_2.empty();

    for ( var id in availableIndicators )
    {
        var text = availableIndicators[id];
        if ( text.toLowerCase().indexOf( filter_1.toLowerCase() ) != -1 )
        {
            list_1.append( '<option value="' + id + '">' + text + '</option>' );
            list_2.append( '<option value="' + id + '">' + text + '</option>' );
        }
    }

	sortList( 'availableIndicators', 'ASC' );
	sortList( 'availableIndicators2', 'ASC' );
    list_1.find( ":first" ).attr( "selected", "selected" );
    list_2.find( ":first" ).attr( "selected", "selected" );

}

function loadAvailableGroups()
{
    var filter_1 = jQuery( '#view_1 #indicatorGroupsFilter' ).val();
    var filter_2 = jQuery( '#view_2 #indicatorGroupsFilter' ).val();
    var list_1 = jQuery( "#view_1 #indicatorGroups" );
    var list_2 = jQuery( "#view_2 #availableGroups" );
    list_1.empty();
    list_2.empty();

    for ( var id in indicatorGroups )
    {
        var text = indicatorGroups[id];
        if ( text.toLowerCase().indexOf( filter_1.toLowerCase() ) != -1 )
        {
            list_1.append( '<option value="' + id + '">' + text + '</option>' );
            list_2.append( '<option value="' + id + '">' + text + '</option>' );
        }
    }

    sortList( 'indicatorGroups', 'ASC' );
    sortList( 'availableGroups', 'ASC' );
    list_1.find( ":first" ).attr( "selected", "selected" );
    list_2.find( ":first" ).attr( "selected", "selected" );

    var list_3 = jQuery( "#view_2 #assignedGroups" ).children();
    list_2.children().each( function( i, item )
    {
        list_3.each( function( k, it )
        {
            if ( it.value == item.value )
            {
                jQuery( item ).remove();
            }
        } );
    } );
}

function getIndicatorsByGroup()
{
    loadAvailableIndicators();

	var id = jQuery( '#view_1 #indicatorGroups' ).val();
    var filter_1 = jQuery( '#view_1 #selectedIndicatorsFilter' ).val();
    var list_1 = jQuery( "#view_1 #selectedIndicators" );
    list_1.empty();

    jQuery.postJSON( "../dhis-web-commons-ajax-json/getIndicators.action", {
        id : ( isNotNull( id ) ? id : -1 )
    }, function( json )
    {
        jQuery.each( json.indicators, function( i, item )
        {
            var text = item.name;
            if ( text.toLowerCase().indexOf( filter_1.toLowerCase() ) != -1 )
            {
                list_1.append( '<option value="' + item.id + '">' + text + '</option>' );
            }
            jQuery( "#view_1 #availableIndicators" ).children().each( function( k, it )
            {
                if ( item.id == it.value )
                {
                    jQuery( it ).remove();
                }
            } );
        } );
    } );
}

function showAddGroup()
{
    jQuery( '#addIndicatorGroupForm' ).dialog( 'option', 'title', i18n_new );
    jQuery( '#addIndicatorGroupForm' ).dialog( 'option', 'buttons', [ {
        text : i18n_save,
        click : function()
        {
            jQuery.postJSON( "validateIndicatorGroup.action", {
                name : function()
                {
                    return jQuery( '#addIndicatorGroupForm #name' ).val();
                }
            }, function( json )
            {
                if ( json.response == 'success' )
                {
                    jQuery.postJSON( "addIndicatorGroupEditor.action", {
                        name : function()
                        {
                            return jQuery( '#addIndicatorGroupForm #name' ).val();
                        }
                    }, function( json )
                    {
                        indicatorGroups[json.indicatorGroup.id] = json.indicatorGroup.name;
                        loadAvailableGroups();
                        loadAvailableIndicators();

                        jQuery( "#view_1 #selectedIndicators" ).empty();
                        jQuery( '#addIndicatorGroupForm' ).dialog( 'close' );
                    } );
                } else
                {
                    markInvalid( "addIndicatorGroupForm #name", json.message );
                }
            } );
        }
    } ] );

    jQuery( '#addIndicatorGroupForm' ).dialog( 'open' );

}

function showUpdateGroup()
{
    var id = jQuery( "#view_1 #indicatorGroups" ).val();
    var text = jQuery( "#view_1 #indicatorGroups option[value=" + id + "]" ).text();
    jQuery( '#addIndicatorGroupForm #name' ).val( text );

    jQuery( '#addIndicatorGroupForm' ).dialog( 'option', 'buttons', [ {
        text : i18n_save,
        click : function()
        {
            jQuery.postJSON( "validateIndicatorGroup.action", {
                id : id,
                name : function()
                {
                    return jQuery( '#addIndicatorGroupForm #name' ).val();
                }
            }, function( json )
            {
                if ( json.response == 'success' )
                {
                    jQuery.postJSON( "renameIndicatorGroupEditor.action", {
                        name : function()
                        {
                            return jQuery( '#addIndicatorGroupForm #name' ).val();
                        },
                        id : id
                    }, function( json )
                    {
                        indicatorGroups[id] = jQuery( '#addIndicatorGroupForm #name' ).val();
                        loadAvailableGroups();
                        jQuery( '#addIndicatorGroupForm' ).dialog( 'close' );
                        setHeaderDelayMessage( i18n_update_success );
                    } );
                } else
                {
                    markInvalid( "addIndicatorGroupForm #name", json.message );
                }
            } );
        }
    } ] );

    jQuery( '#addIndicatorGroupForm' ).dialog( 'option', 'title', i18n_rename );
    jQuery( '#addIndicatorGroupForm' ).dialog( 'open' );

}

function deleteIndicatorGroup()
{
    var id = jQuery( "#view_1 #indicatorGroups" ).val();
    var name = jQuery( "#view_1 #indicatorGroups option[value=" + id + "]" ).text();

    if ( window.confirm( i18n_confirm_delete + '\n\n' + name ) )
    {

        jQuery.postJSON( "deleteIndicatorGroupEditor.action", {
            id : id
        }, function( json )
        {
            if ( json.response == 'success' )
            {
                indicatorGroups.splice( id, 1 );
                loadAvailableGroups();
                setHeaderDelayMessage( json.message );
            } else
            {
            	setHeaderDelayMessage( json.message );
            }
        } );
    }
}

function updateGroupMembers()
{
    var id = jQuery( "#view_1 #indicatorGroups" ).val();

    jQuery.getJSON( "updateIndicatorGroupEditor.action?id=" + id + "&"
            + toQueryString( '#view_1 #selectedIndicators', 'groupMembers' ), function( json )
    {
    	setHeaderDelayMessage( i18n_update_success );
    } );
}

function toQueryString( jQueryString, paramName )
{
    var p = "";
    jQuery( jQueryString ).children().each( function( i, item )
    {
        item.selected = "selected";
        p += paramName + "=" + item.value + "&";
    } );
    return p;
}

// View2
function getAssignedIndicatorGroups()
{
    loadAvailableGroups();

    var id = jQuery( "#view_2 #availableIndicators2" ).val();
    var list_2 = jQuery( "#view_2 #assignedGroups" );
    list_2.empty();

    jQuery.postJSON( "getAssignedIndicatorGroups.action", {
        indicatorId : ( isNotNull( id ) ? id : -1 )
    }, function( json )
    {
        var availabelGroups_2 = jQuery( "#view_2 #availableGroups" );
        availabelGroups_2.empty();
        for ( var index in indicatorGroups )
        {
            availabelGroups_2.append( '<option value="' + index + '" selected="selected">' + indicatorGroups[index]
                    + '</option>' );
        }

        jQuery.each( json.indicatorGroups, function( i, item )
        {
            list_2.append( '<option value="' + item.id + '">' + item.name + '</option>' );

            jQuery( "#view_2 #availableGroups" ).children().each( function( k, it )
            {
                if ( item.id == it.value )
                {
                    jQuery( it ).remove();
                }
            } );
        } );
    } );
}

function showAddGroup2()
{
    jQuery( '#addIndicatorGroupForm' ).dialog( 'option', 'title', i18n_new );
    jQuery( '#addIndicatorGroupForm' ).dialog( 'option', 'buttons', [ {
        text : i18n_save,
        click : function()
        {
            jQuery.postJSON( "validateIndicatorGroup.action", {
                name : function()
                {
                    return jQuery( '#addIndicatorGroupForm #name' ).val();
                }
            }, function( json )
            {
                if ( json.response == 'success' )
                {
                    jQuery.postJSON( "addIndicatorGroupEditor.action", {
                        name : function()
                        {
                            return jQuery( '#addIndicatorGroupForm #name' ).val();
                        }
                    }, function( json )
                    {
                        indicatorGroups[json.indicatorGroup.id] = json.indicatorGroup.name;
                        loadAvailableGroups();
                        jQuery( '#addIndicatorGroupForm' ).dialog( 'close' );
                    } );
                } else
                {
                    markInvalid( "addIndicatorGroupForm #name", json.message );
                }
            } );
        }
    } ] );
    jQuery( '#addIndicatorGroupForm' ).dialog( 'open' );
}

function showUpdateGroup2()
{
    var id = jQuery( "#view_2 #availableGroups" ).val()[0];
    var text = jQuery( "#view_2 #availableGroups option[value=" + id + "]" ).text();
    jQuery( '#addIndicatorGroupForm #name' ).val( text );

    jQuery( '#addIndicatorGroupForm' ).dialog( 'option', 'buttons', [ {
        text : i18n_save,
        click : function()
        {
            jQuery.postJSON( "validateIndicatorGroup.action", {
                id : id,
                name : function()
                {
                    return jQuery( '#addIndicatorGroupForm #name' ).val();
                }
            }, function( json )
            {
                if ( json.response == 'success' )
                {
                    jQuery.postJSON( "renameIndicatorGroupEditor.action", {
                        name : function()
                        {
                            return jQuery( '#addIndicatorGroupForm #name' ).val();
                        },
                        id : id
                    }, function( json )
                    {
                        indicatorGroups[id] = jQuery( '#addIndicatorGroupForm #name' ).val();
                        loadAvailableGroups();
                        jQuery( '#addIndicatorGroupForm' ).dialog( 'close' );
                        setHeaderDelayMessage( i18n_update_success );
                    } );
                } else
                {
                    markInvalid( "addIndicatorGroupForm #name", json.message );
                }
            } );
        }
    } ] );

    jQuery( '#addIndicatorGroupForm' ).dialog( 'option', 'title', i18n_rename );
    jQuery( '#addIndicatorGroupForm' ).dialog( 'open' );
}

function deleteIndicatorGroup2()
{
    var id = jQuery( "#view_2 #availableGroups" ).val()[0];
    var name = jQuery( "#view_2 #availableGroups option[value=" + id + "]" ).text();

    if ( window.confirm( i18n_confirm_delete + '\n\n' + name ) )
    {

        jQuery.postJSON( "deleteIndicatorGroupEditor.action", {
            id : id
        }, function( json )
        {
            if ( json.response == 'success' )
            {
                indicatorGroups.splice( id, 1 );
                loadAvailableGroups();
                setHeaderDelayMessage( json.message );
            } else
            {
            	setHeaderDelayMessage( json.message );
            }
        } );
    }
}

function assignGroupsForIndicator()
{
    var id = jQuery( "#view_2 #availableIndicators2" ).val();

    jQuery.getJSON( "asignGroupsForIndicator.action?indicatorId=" + id + "&"
            + toQueryString( '#view_2 #assignedGroups', 'indicatorGroups' ), function( json )
    {
    	setHeaderDelayMessage( i18n_update_success );
    } );
}
