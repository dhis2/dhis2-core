
// -------------------------------------------------------------------------
// Import
// -------------------------------------------------------------------------

function showAdvancedOptions()
{
	var optionDiv = document.getElementById( "optionDiv" );
	
	optionDiv.style.display = "block";
	
	var optionButton = document.getElementById( "optionButton" );
	
	optionButton.setAttribute( "onclick", "hideAdvancedOptions()" );
	optionButton.value = i18n_hide_advanced_options;
}

function hideAdvancedOptions()
{
	var optionDiv = document.getElementById( "optionDiv" );
	
	optionDiv.style.display = "none";
	
	var optionButton = document.getElementById( "optionButton" );
	
	optionButton.setAttribute( "onclick", "showAdvancedOptions()" );
	optionButton.value = i18n_show_advanced_options;
}

// -------------------------------------------------------------------------
// Preview
// -------------------------------------------------------------------------

function discardObjectAjax()
{
	var objects = document.getElementById( "objects" );
	
	var params = "";
	
	for ( var i = 0; i < objects.options.length; i++ )
	{
		if ( objects.options[ i ].selected )
		{
			params += "id=" + objects.options[ i ].value + "&";
		}
	}
	
	var url = "discardObjectAjax.action";
			
	var request = new Request();
	request.sendAsPost( params );
    request.setResponseTypeXML( 'message' );	    
    request.setCallbackSuccess( discardObjectCallback );
    request.send( url );
}

function discardObjectCallback( xmlElement )
{
	var type = xmlElement.getAttribute( "type" );
    var discardedElements = xmlElement.firstChild.nodeValue;
    
    var elementType = document.getElementById( "type" ).value;
	var elementStatus = document.getElementById( "status" ).value;
	
	if ( type == "success" )
    {
    	if ( elementType == "CONSTANT" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newConstantSpan", "newConstantTd" );
		}
    	else if ( elementType == "CONSTANT" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateConstantSpan", "updateConstantTd" );
		}
		else if ( elementType == "DATAELEMENT" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newDataElementSpan", "newDataElementTd" );
		}
    	else if ( elementType == "DATAELEMENT" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateDataElementSpan", "updateDataElementTd" );
		}
		else if ( elementType == "EXTENDEDDATAELEMENT" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newExtendedDataElementSpan", "newExtendedDataElementTd" );
		}
    	else if ( elementType == "EXTENDEDDATAELEMENT" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateExtendedDataElementSpan", "updateExtendedDataElementTd" );
		}
		else if ( elementType == "DATAELEMENTGROUP" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newDataElementGroupSpan", "newDataElementGroupTd" );
		}
    	else if ( elementType == "DATAELEMENTGROUP" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateDataElementGroupSpan", "updateDataElementGroupTd" );
		}
        else if ( elementType == "DATAELEMENTGROUPSET" && elementStatus == "NEW" )
        {
            handleField( discardedElements, "newDataElementGroupSetSpan", "newDataElementGroupSetTd" );
        }
        else if ( elementType == "DATAELEMENTGROUPSET" && elementStatus == "UPDATE" )
        {
            handleField( discardedElements, "updateDataElementGroupSetSpan", "updateDataElementGroupSetTd" );
        }
		else if ( elementType == "INDICATORTYPE" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newIndicatorTypeSpan", "newIndicatorTypeTd" );
		}
		else if ( elementType == "INDICATORTYPE" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateIndicatorTypeSpan", "updateIndicatorTypeTd" );
		}
		else if ( elementType == "INDICATOR" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newIndicatorSpan", "newIndicatorTd" );
		}
		else if ( elementType == "INDICATOR" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateIndicatorSpan", "updateIndicatorTd" );
		}
		else if ( elementType == "INDICATORGROUP" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newIndicatorGroupSpan", "newIndicatorGroupTd" );
		}
		else if ( elementType == "INDICATORGROUP" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateIndicatorGroupSpan", "updateIndicatorGroupTd" );
		}
        else if ( elementType == "INDICATORGROUPSET" && elementStatus == "NEW" )
        {
            handleField( discardedElements, "newIndicatorGroupSetSpan", "newIndicatorGroupSetTd" );
        }
        else if ( elementType == "INDICATORGROUPSET" && elementStatus == "UPDATE" )
        {
            handleField( discardedElements, "updateIndicatorGroupSetSpan", "updateIndicatorGroupSetTd" );
        }
		else if ( elementType == "DATASET" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newDataSetSpan", "newDataSetTd" );
		}
    	else if ( elementType == "DATASET" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateDataSetSpan", "updateDataSetTd" );
		}
		else if ( elementType == "ORGANISATIONUNIT" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newOrganisationUnitSpan", "newOrganisationUnitTd" );
		}
    	else if ( elementType == "ORGANISATIONUNIT" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateOrganisationUnitSpan", "updateOrganisationUnitTd" );
		}
		else if ( elementType == "ORGANISATIONUNITGROUP" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newOrganisationUnitGroupSpan", "newOrganisationUnitGroupTd" );
		}
    	else if ( elementType == "ORGANISATIONUNITGROUP" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateOrganisationUnitGroupSpan", "updateOrganisationUnitGroupTd" );
		}
		else if ( elementType == "ORGANISATIONUNITGROUPSET" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newOrganisationUnitGroupSetSpan", "newOrganisationUnitGroupSetTd" );
		}
    	else if ( elementType == "ORGANISATIONUNITGROUPSET" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateOrganisationUnitGroupSetSpan", "updateOrganisationUnitGroupSetTd" );
		}
        else if ( elementType == "ORGANISATIONUNITLEVEL" && elementStatus == "NEW" )
        {
            handleField( discardedElements, "newOrganisationUnitLevelSpan", "newOrganisationUnitLevelTd" );
        }
        else if ( elementType == "ORGANISATIONUNITLEVEL" && elementStatus == "UPDATE" )
        {
            handleField( discardedElements, "updateOrganisationUnitLevelSpan", "updateOrganisationUnitLevelTd" );
        }
		else if ( elementType == "VALIDATIONRULE" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newValidationRuleSpan", "newValidationRuleTd" );
		}
		else if ( elementType == "VALIDATIONRULE" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateValidationRuleSpan", "updateValidationRuleTd" );
		}
		else if ( elementType == "REPORT" && elementStatus == "NEW" )
		{
		    handleField( discardedElements, "newReportSpan", "newReportTd" );
		}
		else if ( elementType == "REPORT" && elementStatus == "UPDATE" )
		{
		    handleField( discardedElements, "updateReportTableSpan", "updateReportTableTd" );
		}
		else if ( elementType == "REPORTTABLE" && elementStatus == "NEW" )
		{
		    handleField( discardedElements, "newReportTableSpan", "newReportTableTd" );
		}
		else if ( elementType == "REPORTTABLE" && elementStatus == "UPDATE" )
		{
		    handleField( discardedElements, "updateReportTableSpan", "updateReportTableTd" );
		}
		else if ( elementType == "CHART" && elementStatus == "NEW" )
		{
		    handleField( discardedElements, "newChartSpan", "newChartTd" );
		}
		else if ( elementType == "CHART" && elementStatus == "UPDATE" )
		{
		    handleField( discardedElements, "updateChartSpan", "updateChartTd" );
		}
		else if ( elementType == "DATAVALUE" && elementStatus == "NEW" )
		{
			handleField( discardedElements, "newDataValueSpan", "newDataValueTd" );
		}
		else if ( elementType == "DATAVALUE" && elementStatus == "UPDATE" )
		{
			handleField( discardedElements, "updateDataValueSpan", "updateDataValueTd" );
		}
		
		// Remove selected options from list
		
		removeSelectedOptions( document.getElementById( "objects" ) );
    }
}

function discardObjectsOfTypeAjax()
{
	var type = document.getElementById( "type" ).value;
	
	if ( type != null )
	{
		var url = "discardObjectsOfTypeAjax.action?type=" + type;
		
		var request = new Request();
	    request.setResponseTypeXML( 'message' );
	    request.setCallbackSuccess( discardObjectsOfTypeCallback );
	    request.send( url );
	}
}

function discardObjectsOfTypeCallback( xmlElement )
{
	var type = xmlElement.getAttribute( "type" );
	
	var elementType = document.getElementById( "type" ).value;
		
	if ( type == "success" )
	{
		// Set value count to none
		
		if ( elementType == "CONSTANT" )
		{
			clearField( "newConstantTd" );
			clearField( "updateConstantTd" );
		}
		else if ( elementType == "DATAELEMENT" )
		{
			clearField( "newDataElementTd" );
			clearField( "updateDataElementTd" );
			clearField( "newIndicatorTd" );
			clearField( "updateIndicatorTd" );
		}
		else if ( elementType == "EXTENDEDDATAELEMENT" )
		{
			clearField( "newExtendedDataElementTd" );
			clearField( "updateExtendedDataElementTd" );
		}
		else if ( elementType == "DATAELEMENTGROUP" )
		{
			clearField( "newDataElementGroupTd" );
			clearField( "updateDataElementGroupTd" );
		}
        else if ( elementType == "DATAELEMENTGROUPSET" )
        {
            clearField( "newDataElementGroupSetTd" );
            clearField( "updateDataElementGroupSetTd" );
        }
		else if ( elementType == "INDICATORTYPE")
		{
			clearField( "newIndicatorTypeTd" );
			clearField( "updateIndicatorTypeTd" );
			clearField( "newIndicatorTd" );
			clearField( "updateIndicatorTd" );
		}
		else if ( elementType == "INDICATOR" )
		{
			clearField( "newIndicatorTd" );
			clearField( "updateIndicatorTd" );
		}
		else if ( elementType == "INDICATORGROUP" )
		{
			clearField( "newIndicatorGroupTd" );
			clearField( "updateIndicatorGroupTd" );
		}
        else if ( elementType == "INDICATORGROUPSET" )
        {
            clearField( "newIndicatorGroupSetTd" );
            clearField( "updateIndicatorGroupSetTd" );
        }
		else if ( elementType == "DATASET" )
		{
			clearField( "newDataSetTd" );
			clearField( "updateDataSetTd" );
		}
		else if ( elementType == "ORGANISATIONUNIT" )
		{
			clearField( "newOrganisationUnitTd" );
			clearField( "updateOrganisationUnitTd" );
		}
		else if ( elementType == "ORGANISATIONUNITGROUP" )
		{
			clearField( "newOrganisationUnitGroupTd" );
			clearField( "updateOrganisationUnitGroupTd" );
		}
		else if ( elementType == "ORGANISATIONUNITGROUPSET" )
		{
			clearField( "newOrganisationUnitGroupSetTd" );
			clearField( "updateOrganisationUnitGroupSetTd" );
		}
        else if ( elementType == "ORGANISATIONUNITLEVEL" )
        {
            clearField( "newOrganisationUnitLevelTd" );
            clearField( "updateOrganisationUnitLevelTd" );
        }
        else if ( elementType == "VALIDATIONRULE" )
        {
            clearField( "newValidationRuleTd" );
            clearField( "updateValidationRuleTd" );
        }
		else if ( elementType == "REPORT" )
		{
		    clearField( "newReportTd" );
		    clearFIeld( "updateReportTd" );
		}
		else if ( elementType == "REPORTTABLE" )
		{
		    clearField( "newReportTableTd" );
		    clearFIeld( "updateReportTableTd" );
		}
		else if ( elementType == "CHART" )
		{
		    clearField( "newChartTd" );
		    clearFIeld( "updateChartTd" );
		}
		else if ( elementType == "DATAVALUE" )
		{
			clearField( "newDataValueTd" );
			clearField( "updateDataValueTd" );
		}
		
		// Remove objects from list
		
		document.getElementById( "objects" ).options.length = 0;
	}	
}

// -------------------------------------------------------------------------
// Match / Compare
// -------------------------------------------------------------------------

function displayMatchForm()
{
	var list = document.getElementById( "objects" );
	
	var id = list.options[ list.selectedIndex ].value;
	
	var type = document.getElementById( "type" ).value;
	
	window.location.href = "displayMatchForm.action?objectType=" + type + "&objectId=" + id;
}

function displayCompareForm()
{
	var list = document.getElementById( "objects" );
	
	var id = list.options[ list.selectedIndex ].value;
	
	var type = document.getElementById( "type" ).value;
	
	window.location.href = "displayCompareForm.action?objectType=" + type + "&objectId=" + id;
}

function matchObject()
{
	var importObjectId = document.getElementById( "objectId" ).value;
	
	var existingObjects = document.getElementById( "existingObjects" );
	
	var selectedIndex = existingObjects.selectedIndex;
	
	if ( selectedIndex == -1 )
	{
		setHeaderDelayMessage( i18n_no_item_to_match );
	}
	else
	{
		var existingObjectId = existingObjects.options[ selectedIndex ].value;
		
		if ( importObjectId != null && existingObjectId != null )
		{
			window.location.href = "matchObject.action?importObjectId=" + importObjectId + "&existingObjectId=" + existingObjectId;
		}
	}
}

function matchUpdateObject()
{
	var importObjectId = document.getElementById( "objectId" ).value;
	
	var existingObjectId = document.getElementById( "existingObjectId" ).value;
	
    if ( importObjectId != null && existingObjectId != null )
    {
        window.location.href = "matchObject.action?importObjectId=" + importObjectId + "&existingObjectId=" + existingObjectId;
    }	
}

function discardObject()
{
	var objectId = document.getElementById( "objectId" ).value;
	
	if ( objectId != null )
	{
		window.location.href = "discardObject.action?id=" + objectId;
	}
}

function cancelObject()
{
	var type = document.getElementById( "objectType" ).value;
	
	window.location.href = "displayPreviewForm.action?type=" + type;
}

function viewAllExistingObjects()
{
	var objectType = document.getElementById( "objectType" ).value;
	var objectId = document.getElementById( "objectId" ).value;
	
	window.location.href="displayMatchForm.action?objectType=" + objectType + "&objectId=" + objectId;
}

function filterExistingObjects()
{
	var existingList = document.getElementById( "existingObjects" );
	
	var filteredList = document.getElementById( "filteredObjects" );
	
	var filterText = document.getElementById( "filterField" ).value.toLowerCase();
	
	// Move non-matching options to holding list
	
	for ( var i = existingList.options.length-1; i >= 0; i-- )
	{
		var string = existingList.options[ i ].text.toLowerCase();
		
		if ( string.indexOf( filterText ) == -1 )
		{
			option = existingList.options[ i ];
			existingList.remove( i );
			filteredList.add( option, null );
		}
	}
	
	// Move matching options to main list
	
	for ( var i = filteredList.options.length-1; i >= 0; i-- )
	{
		var string = filteredList.options[ i ].text.toLowerCase();
		
		if ( string.indexOf( filterText ) != -1 )
		{
			option = filteredList.options[ i ];
			filteredList.remove( i );
			existingList.add( option, null );
		}
	}
}

// -------------------------------------------------------------------------
// Supportive methods
// -------------------------------------------------------------------------

function handleField( discardedElements, spanName, tdName )
{
	var existingElements = document.getElementById( spanName ).innerHTML;
			
	existingElements = existingElements - discardedElements;
	
	if ( existingElements > 0 )
	{
		document.getElementById( spanName ).innerHTML = existingElements;
	}
	else
	{
		clearField( tdName );
	}
}

function clearField( fieldName )
{
	if ( document.getElementById( fieldName ) != null )
	{
		document.getElementById( fieldName ).style.backgroundColor = "#FFFFFF";	
		document.getElementById( fieldName ).innerHTML = "None";
	}
}

function removeSelectedOptions( list )
{
	for ( var i = list.options.length-1; i >= 0; i-- )
	{
		if ( list.options[ i ].selected )
		{
			list.remove( i );
		}
	}
}
