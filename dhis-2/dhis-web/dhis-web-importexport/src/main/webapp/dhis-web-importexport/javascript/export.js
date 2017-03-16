
// -----------------------------------------------------------------------------
// Export
// -----------------------------------------------------------------------------

function exportDataValue()
{
    selectAllById( "selectedDataSets" );
	
	if ( validateDataValueExportForm() )
	{
	   document.getElementById( "exportForm" ).submit();
	}
}

function toggleOptions()
{
	$( ".moreOptions" ).toggle();
}

// -----------------------------------------------------------------------------
// MetaDataExport
// -----------------------------------------------------------------------------

function submitMetaDataExportForm()
{
    if ( validateMetaDataExportForm() )
    {
       document.getElementById( "exportForm" ).submit();
    }
}

function toggle( knob )
{
    var toggle = (knob == "all" ? true : false);
	
	jQuery.each( jQuery("input[type=checkbox]"), function(i, item){
		item.checked = toggle;
	});
}


// -----------------------------------------------------------------------------
// DataExport
// -----------------------------------------------------------------------------

function submitDataExportForm()
{
    if ( validateDataExportForm() )
    {
       document.getElementById( "exportForm" ).submit();
    }
}

// -----------------------------------------------------------------------------
// Validation
// -----------------------------------------------------------------------------

function validateMetaDataExportForm()
{
	if ( jQuery("input:checked").length == 0 )
	{
		setMessage( i18n_select_one_or_more_object_types );
		return false;
	}
	
	hideHeaderMessage();
	return true;
}

function validateDataValueExportForm()
{    
    if ( !selectionTreeSelection.isSelected() )
    {
        setHeaderDelayMessage( i18n_select_organisation_unit );
        return false;
    }
    if ( !hasText( "startDate" ) )
    {
        setHeaderDelayMessage( i18n_select_startdate );
        return false;
    }
    if ( !hasText( "endDate" ) )
    {
        setHeaderDelayMessage( i18n_select_enddate );
        return false;
    }
    if ( !hasElements( "selectedDataSets" ) )
    {
        setHeaderDelayMessage( i18n_select_datasets );
        return false;
    }
    
    hideHeaderMessage();
    return true;
}

function validateDataExportForm()
{    

	if ( jQuery("input:checked").length == 0 )
	{
		setMessage( i18n_select_one_or_more_object_types );
		return false;
	}
	
    if ( !hasText( "startDate" ) )
    {
        setHeaderDelayMessage( i18n_select_startdate );
        return false;
    }
    if ( !hasText( "endDate" ) )
    {
        setHeaderDelayMessage( i18n_select_enddate );
        return false;
    }
    
    hideHeaderMessage();
    return true;
}
