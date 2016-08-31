jQuery( document ).ready( function()
{
    loadAvailableGroups();
    getDataElementsByGroup();
    getAssignedDataElementGroups();

    jQuery( "#addDataElementGroupForm" ).dialog( {
        autoOpen : false,
        modal : true
    } );
    jQuery( "#tabs" ).tabs();
} );

function loadAvailableDataElements()
{
    var filter_1 = jQuery( '#view_1 #availableDataElementsFilter' ).val();
    var filter_2 = jQuery( '#view_2 #availableDataElementsFilter' ).val();
    var list_1 = jQuery( "#view_1 #availableDataElements" );
    var list_2 = jQuery( "#view_2 #availableDataElements2" );
    list_1.empty();
    list_2.empty();

    for ( var id in availableDataElements )
    {
        var text = availableDataElements[id];

        if ( text.toLowerCase().indexOf( filter_1.toLowerCase() ) != -1 )
        {
			if( !checkSelectedDataElement( id ) )
			{
				list_1.append( '<option value="' + id + '" title="' + text + '">' + text + '</option>' );
			}
		}
		
		if ( text.toLowerCase().indexOf( filter_2.toLowerCase() ) != -1 )
        {
			list_2.append( '<option value="' + id + '" title="' + text + '">' + text + '</option>' );
		}
    }

	sortList( 'availableDataElements', 'ASC' );
	sortList( 'availableDataElements2', 'ASC' );
    list_1.find( ":first" ).attr( "selected", "selected" );
    list_2.find( ":first" ).attr( "selected", "selected" );
}

function checkSelectedDataElement( deId )
{
	var list_selected = jQuery( "#selectedDataElements_storage" ).children();	
	var selectedDataElements = jQuery( "#view_1 #selectedDataElements" ).children();

	$.each( selectedDataElements, function( i_de, item_de )
	{
		list_selected.push( item_de );
	});

	var returnStatus = false;
	
	jQuery.each( list_selected, function( i, item )
	{
		if ( item.value == deId )
		{
			returnStatus = true;
			return false;  // same as break;
		}
	} );
	
	return returnStatus;
}

function loadAvailableGroups()
{
    var filter_1 = jQuery( '#view_1 #dataElementGroupsFilter' ).val();
    var filter_2 = jQuery( '#view_2 #dataElementGroupsFilter' ).val();
    var list_1 = jQuery( "#view_1 #dataElementGroups" );
    var list_2 = jQuery( "#view_2 #availableGroups" );
    list_1.empty();
    list_2.empty();

    for ( var id in dataElementGroups )
    {
        var text = dataElementGroups[id];

        if ( text.toLowerCase().indexOf( filter_1.toLowerCase() ) != -1 )
        {
            list_1.append( '<option value="' + id + '" title="' + text + '">' + text + '</option>' );
            list_2.append( '<option value="' + id + '" title="' + text + '">' + text + '</option>' );
        }
    }

    sortList( 'dataElementGroups', 'ASC' );
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

function getDataElementsByGroup()
{
    loadAvailableDataElements();

	var id = jQuery( '#view_1 #dataElementGroups' ).val();
    var filter_1 = jQuery( '#view_1 #selectedDataElementsFilter' ).val();
    var list_1 = jQuery( "#view_1 #selectedDataElements" );
    list_1.empty();

    jQuery.postJSON( "../dhis-web-commons-ajax-json/getDataElements.action", {
        id : ( isNotNull( id ) ? id : -1 )
    }, function( json )
    {
        jQuery.each( json.dataElements, function( i, item )
        {
            var text = item.name;
            if ( text.toLowerCase().indexOf( filter_1.toLowerCase() ) != -1 )
            {
                list_1.append( '<option value="' + item.uid + '" title="' + text + '">' + text + '</option>' );
            }
            jQuery( "#view_1 #availableDataElements" ).children().each( function( k, it )
            {
                if ( item.uid == it.value )
                {
                    jQuery( it ).remove();
                }
            } );
        } );
    } );
}

function showAddGroup()
{
    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'title', i18n_new );
	jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'width', '350px' );
    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'buttons', [ {
        text : i18n_save,
        click : function()
        {
			jQuery( '#addDataElementGroupForm #shortName' ).closest('tr').show();
			jQuery( '#addDataElementGroupForm #code' ).closest('tr').show();
			if( jQuery( '#addDataElementGroupForm #name' ).val() == "" ){
				markValid( "addDataElementGroupForm #shortName" );
				markInvalid( "addDataElementGroupForm #name", i18n_this_field_is_required );
			}
			else if( jQuery( '#addDataElementGroupForm #shortName' ).val() == "" ){
				markValid( "addDataElementGroupForm #name" );
				markInvalid( "addDataElementGroupForm #shortName", i18n_this_field_is_required );
			}
			else
			{
				jQuery.postJSON( "validateDataElementGroup.action", {
					name : jQuery( '#addDataElementGroupForm #name' ).val()
					,shortName : jQuery( '#addDataElementGroupForm #shortName' ).val()
					,code : jQuery( '#addDataElementGroupForm #code' ).val()
				}, function( json )
				{
					if ( json.response == 'success' )
					{
						markValid( "addDataElementGroupForm #name" );				
						markValid( "addDataElementGroupForm #shortName" );
				
						jQuery.postJSON( "addDataElementGroupEditor.action", {
							name : jQuery( '#addDataElementGroupForm #name' ).val(),
							shortName : jQuery( '#addDataElementGroupForm #shortName' ).val(),
							code : jQuery( '#addDataElementGroupForm #code' ).val()
						}, function( json )
						{
							var id = json.dataElementGroup.id;
							dataElementGroups[id] = json.dataElementGroup.name;
							dataElementGroupShortNames[id] = json.dataElementGroup.name;
							dataElementGroupCodes[id] = json.dataElementGroup.shortName;
							loadAvailableGroups();
							jQuery( "#view_1 #selectedDataElements" ).empty();
							$("#dataElementGroups option:contains('" + jQuery( '#addDataElementGroupForm #name' ).val() + "')").attr("selected",true);
							loadAvailableDataElements();
							jQuery( '#addDataElementGroupForm' ).dialog( 'close' );
						} );
					} else
					{
						markInvalid( "addDataElementGroupForm #name", json.message );
					}
				} );
			}
        }
    } ] );

    jQuery( '#addDataElementGroupForm' ).dialog( 'open' );
}

function showAddGroupView2()
{
    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'title', i18n_new );
    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'width', '350px' );
    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'buttons', [ {
        text : i18n_save,
        click : function()
        {
			if( jQuery( '#addDataElementGroupForm #name' ).val() == "" ){
				markValid( "addDataElementGroupForm #shortName" );
				markInvalid( "addDataElementGroupForm #name", i18n_this_field_is_required );
			}
			else if( jQuery( '#addDataElementGroupForm #shortName' ).val() == "" ){
				markValid( "addDataElementGroupForm #name" );
				markInvalid( "addDataElementGroupForm #shortName", i18n_this_field_is_required );
			}
			else
			{
				jQuery.postJSON( "validateDataElementGroup.action", {
					name : jQuery( '#addDataElementGroupForm #name' ).val(),
					shortName : jQuery( '#addDataElementGroupForm #shortName' ).val(),
					code : jQuery( '#addDataElementGroupForm #code' ).val()
				}, function( json )
				{
					if ( json.response == 'success' )
					{
						jQuery.postJSON( "addDataElementGroupEditor.action", {
							name : jQuery( '#addDataElementGroupForm #name' ).val(),
							shortName : jQuery( '#addDataElementGroupForm #shortName' ).val(),
							code : jQuery( '#addDataElementGroupForm #code' ).val()
						}, function( json )
						{
							dataElementGroups[json.dataElementGroup.id] = json.dataElementGroup.name;
							loadAvailableGroups();
							jQuery( '#addDataElementGroupForm' ).dialog( 'close' );
						} );
					} else
					{
						markInvalid( "addDataElementGroupForm #name", json.message );
					}
				} );
			}
        }
    } ] );
		
    jQuery( '#addDataElementGroupForm' ).dialog( 'open' );
}

function showUpdateGroup()
{
    var id = jQuery( "#view_1 #dataElementGroups" ).val();
    var text = jQuery( "#view_1 #dataElementGroups option[value=" + id + "]" ).text();
	
    jQuery( '#addDataElementGroupForm #name' ).val( text );
    jQuery( '#addDataElementGroupForm #shortName' ).closest('tr').hide();
    jQuery( '#addDataElementGroupForm #code' ).closest('tr').hide();

    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'buttons', [ {
        text : i18n_save,
        click : function()
        {
			if( jQuery( '#addDataElementGroupForm #name' ).val() == "" ){
				markInvalid( "addDataElementGroupForm #name", i18n_this_field_is_required );
			}
			else
			{
				jQuery.postJSON( "validateDataElementGroup.action", {
					id : id,
					name :jQuery( '#addDataElementGroupForm #name' ).val()
				}, function( json )
				{
					if ( json.response == 'success' )
					{
						markValid( "addDataElementGroupForm #name" );
				
						jQuery.postJSON( "renameDataElementGroupEditor.action", {
							name : function()
							{
								return jQuery( '#addDataElementGroupForm #name' ).val();
							},
							id : id
						}, function( json )
						{
							dataElementGroups[json.dataElementGroup.id] = json.dataElementGroup.name;
							loadAvailableGroups();
							jQuery( '#addDataElementGroupForm' ).dialog( 'close' );
							setHeaderDelayMessage( i18n_update_success );
						} );
					} else
					{
						markInvalid( "addDataElementGroupForm #name", json.message );
					}
				} );
			}
        }
    } ] );

    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'title', i18n_rename );
    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'width', '350px' );
    jQuery( '#addDataElementGroupForm' ).dialog( 'open' );
}

function showUpdateGroup2()
{
    var id = jQuery( "#view_2 #availableGroups option:selected" ).val();
    var text = jQuery( "#view_2 #availableGroups option[value=" + id + "]" ).text();
    jQuery( '#addDataElementGroupForm #name' ).val( text );

    jQuery( '#addDataElementGroupForm #shortName' ).closest('tr').hide();
    jQuery( '#addDataElementGroupForm #code' ).closest('tr').hide();
	
    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'buttons', [ {
        text : i18n_save,
        click : function()
        {
			if( jQuery( '#addDataElementGroupForm #name' ).val() == "" ){
				markInvalid( "addDataElementGroupForm #name", i18n_this_field_is_required );
			}
			else
			{
				jQuery.postJSON( "validateDataElementGroup.action", {
					id : id,
					name : jQuery( '#addDataElementGroupForm #name' ).val()
				}, function( json )
				{
					if ( json.response == 'success' )
					{
						markValid( "addDataElementGroupForm #name" );
				
						jQuery.postJSON( "renameDataElementGroupEditor.action", {
							name :jQuery( '#addDataElementGroupForm #name' ).val(),
							id : id
						}, function( json )
						{
							dataElementGroups[json.dataElementGroup.id] = json.dataElementGroup.name;
							loadAvailableGroups();
							jQuery( '#addDataElementGroupForm' ).dialog( 'close' );
							setHeaderDelayMessage( i18n_update_success );
						} );
					} else
					{
						markInvalid( "addDataElementGroupForm #name", json.message );
					}
				} );
			}
        }
    } ] );

    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'title', i18n_rename );
    jQuery( '#addDataElementGroupForm' ).dialog( 'option', 'width', '350px' );
    jQuery( '#addDataElementGroupForm' ).dialog( 'open' );
}

function deleteDataElemenGroup()
{
    if ( window.confirm( i18n_confirm_delete + '\n\n' + name ) )
    {
        var id = jQuery( "#view_1 #dataElementGroups" ).val();

        jQuery.postJSON( "deleteDataElemenGroupEditor.action", {
            id : id
        }, function( json )
        {
            if ( json.response == 'success' )
            {
                dataElementGroups.splice( id, 1 );
				clearListById('view_1 #selectedDataElements');
                loadAvailableGroups();
                setHeaderDelayMessage( json.message );
            } else
            {
            	setHeaderDelayMessage( json.message );
            }
        } );
    }
}

function deleteDataElemenGroupView2()
{
    if ( window.confirm( i18n_confirm_delete + '\n\n' + name ) )
    {
        var id = jQuery( "#view_2 #availableGroups" ).val()[0];

        jQuery.postJSON( "deleteDataElemenGroupEditor.action", {
            id : id
        }, function( json )
        {
            if ( json.response == 'success' )
            {
                dataElementGroups.splice( id, 1 );
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
    var id = jQuery( "#view_1 #dataElementGroups" ).val();
	var name = dataElementGroups[id];
	var shortName = dataElementGroupShortNames[id];
	var code = dataElementGroupCodes[id];

    jQuery.getJSON( "updateDataElementGroupEditor.action?id=" + id
			+ "&name=" + name + "&shortName=" + shortName + "&code=" + code + "&"
            + toQueryString( '#view_1 #selectedDataElements', 'deSelected' ), function( json )
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

// View 2

function getAssignedDataElementGroups()
{
    loadAvailableGroups();

    var id = jQuery( "#view_2 #availableDataElements2" ).val();
    var list_2 = jQuery( "#view_2 #assignedGroups" );
    
    jQuery.postJSON( "getAssignedDataElementGroups.action", {
        dataElementId : ( isNotNull( id ) ? id : -1 )
    }, function( json )
    { 
		clearListById('view_2 [id="assignedGroups"]');
	
        jQuery.each( json.dataElementGroups, function( i, item )
        {
            list_2.append( '<option value="' + item.id + '" title="' + item.name + '">' + item.name + '</option>' );

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

function assignGroupsForDataElement()
{
    var dataElementId = jQuery( "#view_2 #availableDataElements2" ).val();

    jQuery.getJSON( "asignGroupsForDataElement.action?dataElementId=" + dataElementId + "&"
            + toQueryString( '#view_2 #assignedGroups', 'dataElementGroups' ), function( json )
    {
    	setHeaderDelayMessage( i18n_update_success );
    } );
}
