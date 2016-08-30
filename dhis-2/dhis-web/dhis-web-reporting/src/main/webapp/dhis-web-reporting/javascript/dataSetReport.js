
/**
 * This file is used by dataSetReportForm.vm and dataApprovalForm.vm.
 */
dhis2.util.namespace( 'dhis2.dsr' );

dhis2.dsr.currentPeriodOffset = 0;
dhis2.dsr.currentDataSetReport = null;

//------------------------------------------------------------------------------
// Get and set methods
//------------------------------------------------------------------------------

dhis2.dsr.getDataSetReport = function()
{
	var ds = $( "#dataSetId" ).val();
	
    var dataSetReport = {
        ds: ds,
        periodType: $( "#periodType" ).val(),
        pe: $( "#periodId" ).val(),
        ou: selectionTreeSelection.getSelectedUid()[0],
        selectedUnitOnly: $( "#selectedUnitOnly" ).is( ":checked" ),
        offset: dhis2.dsr.currentPeriodOffset
    };
        
    var dims = [];
    var cps = [];
    
    $( ".dimension" ).each( function( index, value ) {
    	var dim = $( this ).data( "uid" );
    	var item = $( this ).val();
    	
    	if ( dim && item && item != -1 )
    	{
    		var dimQuery = dim + ":" + item;
    		dims.push( dimQuery );
    		cps.push( item );
    	}
    } );
    
    dataSetReport.dimension = dims;
    dataSetReport.cp = cps;
    
    return dataSetReport;
};

dhis2.dsr.setDataSetReport = function( dataSetReport )
{
	$( "#dataSetId" ).val( dataSetReport.dataSet );
	$( "#periodType" ).val( dataSetReport.periodType );
	
	dhis2.dsr.currentPeriodOffset = dataSetReport.offset;
	
	dhis2.dsr.displayPeriods();
	$( "#periodId" ).val( dataSetReport.period );
	
	selectionTreeSelection.setMultipleSelectionAllowed( false );
	selectionTree.buildSelectionTree();
	
	$( "body" ).on( "oust.selected", function() 
	{
		$( "body" ).off( "oust.selected" );
		dhis2.dsr.generateDataSetReport();
	} );
}

//------------------------------------------------------------------------------
// Data set
//------------------------------------------------------------------------------

/**
 * Callback for changes to data set selection.
 */
dhis2.dsr.dataSetSelected = function()
{
	var ds = $( "#dataSetId" ).val();
	
	var html = '';
	
	$.getJSON( "../api/dimensions/dataSet/" + ds + ".json", function( json ) {
		
		if ( !json.dimensions ) {
			$( "#dimensionsDiv" ).hide();
		}
		else
		{
			var rx = [];
			
			$.each( json.dimensions, function( idx, dim ) {
				rx.push( $.get( "../api/dimensions/" + dim.id + ".json?fields=:all,items[id,displayName]" ) );
			} );
			
			$.when.apply( $, rx ).done( function() {
				var html = '';
				var response = dhis2.util.normalizeArguments( arguments );
	
				$.each( response, function( idx, dimension ) {
					var dim = dimension[0];
	
					if ( dim.hasOwnProperty( 'items' ) ) {
						html += '<div class="inputSection">';
						html += '<div><label>' + dim.name + '</label></div>';
						html += '<select class="dimension" data-uid="' + dim.id + '" style="width:330px">';
						html += '<option value="-1">[ ' + i18n_select_option_view_all + ' ]</option>';
						
						dim.items.sort( dhis2.util.nameSort );
						
						$.each( dim.items, function( idx, option ) {
							html += '<option value="' + option.id + '">' + option.displayName + '</option>';
						} );
						
						html += '</select>';
						html += '</div>';
					}
				} );
	
				$( "#dimensionsDiv" ).show().html( html );
			} );
		}
	} ).fail( function() {
		$( "#dimensionsDiv" ).hide();
	} );
};

//------------------------------------------------------------------------------
// Period
//------------------------------------------------------------------------------

dhis2.dsr.displayPeriods = function()
{
    var periodType = $( "#periodType" ).val();
    dhis2.dsr.displayPeriodsInternal( periodType, dhis2.dsr.currentPeriodOffset );
};

dhis2.dsr.displayPeriodsInternal = function( periodType, offset )
{
    var periods = dhis2.period.generator.generateReversedPeriods(periodType, offset);

    $( "#periodId" ).removeAttr( "disabled" );
    clearListById( "periodId" );

    for ( i in periods )
    {
        addOptionById( "periodId", periods[i].iso, periods[i].name );
    }
};

dhis2.dsr.displayNextPeriods = function()
{
    if ( dhis2.dsr.currentPeriodOffset < 0 ) // Cannot display future periods
    {
        dhis2.dsr.currentPeriodOffset++;
        dhis2.dsr.displayPeriods();
    }
};

dhis2.dsr.displayPreviousPeriods = function()
{
    dhis2.dsr.currentPeriodOffset--;
    dhis2.dsr.displayPeriods();
};

//------------------------------------------------------------------------------
// Run report
//------------------------------------------------------------------------------

dhis2.dsr.drillDownDataSetReport = function( orgUnitId, orgUnitUid )
{
	selectionTreeSelection.select( orgUnitId );
	
	var dataSetReport = dhis2.dsr.getDataSetReport();
	dataSetReport["ou"] = orgUnitUid;
	dhis2.dsr.displayDataSetReport( dataSetReport );
}

dhis2.dsr.generateDataSetReport = function()
{
	var dataSetReport = dhis2.dsr.getDataSetReport();
	dhis2.dsr.displayDataSetReport( dataSetReport );
}

dhis2.dsr.displayDataSetReport = function( dataSetReport )
{	
    if ( !dataSetReport.ds )
    {
    	setHeaderDelayMessage( i18n_select_data_set );
        return false;
    }
    if ( !dataSetReport.pe )
    {
    	setHeaderDelayMessage( i18n_select_period );
        return false;
    }
    if ( !selectionTreeSelection.isSelected() )
    {
    	setHeaderDelayMessage( i18n_select_organisation_unit );
        return false;
    }
    
    dhis2.dsr.currentDataSetReport = dataSetReport;
    
    hideHeaderMessage();
    dhis2.dsr.hideCriteria();
    dhis2.dsr.hideContent();
    showLoader();
	    
    var url = dhis2.dsr.getDataSetReportUrl( dataSetReport );
    
    $.get( url, function( data ) {
    	$( '#content' ).html( data );
    	hideLoader();
    	dhis2.dsr.showContent();
    	setTableStyles();
    	dhis2.dsr.registerViewEvent();
    } );
}

/**
 * Generates the URL for the given data set report.
 */
dhis2.dsr.getDataSetReportUrl = function( dataSetReport )
{
    var url = "generateDataSetReport.action" +
    	"?ds=" + dataSetReport.ds + 
    	"&pe=" + dataSetReport.pe + 
    	"&ou=" + dataSetReport.ou +
    	"&selectedUnitOnly=" + dataSetReport.selectedUnitOnly;
    
    $.each( dataSetReport.dimension, function( inx, val ) {
    	url += "&dimension=" + val;
    } );
    
    return url;
}

dhis2.dsr.exportDataSetReport = function( type )
{
	var dataSetReport = dhis2.dsr.currentDataSetReport;
	
	var url = dhis2.dsr.getDataSetReportUrl( dataSetReport ) + "&type=" + type;
	    
	window.location.href = url;
}

dhis2.dsr.setUserInfo = function( username )
{
	$( "#userInfo" ).load( "../dhis-web-commons-ajax-html/getUser.action?username=" + username, function() {
		$( "#userInfo" ).dialog( {
	        modal : true,
	        width : 350,
	        height : 350,
	        title : "User"
	    } );
	} );	
}

dhis2.dsr.registerViewEvent = function()
{
	var ds = $( "#dataSetId" ).val();
	
	$.post( "../api/dataStatistics?eventType=DATA_SET_REPORT_VIEW&favorite=" + ds );
}

dhis2.dsr.showCriteria = function()
{
	$( "#criteria" ).show( "fast" );
	$( "#dataButton" ).attr( "disabled", "disabled" );
}

dhis2.dsr.hideCriteria = function()
{
	$( "#criteria" ).hide( "fast" );
	$( "#dataButton" ).removeAttr( "disabled" );
}

dhis2.dsr.showContent = function()
{
	$( "#content" ).show( "fast" );
	$( ".downloadButton" ).show();
	$( "#interpretationArea" ).autogrow();
}

dhis2.dsr.hideContent = function()
{
	$( "#content" ).hide( "fast" );
	$( ".downloadButton" ).hide();
}

dhis2.dsr.showMoreOptions = function()
{
	$( "#moreOptionsLink" ).hide();
	$( "#fewerOptionsLink" ).show();
	$( "#advancedOptions" ).show();
}

dhis2.dsr.showFewerOptions = function()
{
	$( "#moreOptionsLink" ).show();
	$( "#fewerOptionsLink" ).hide();
	$( "#advancedOptions" ).hide();
}

//------------------------------------------------------------------------------
// Share
//------------------------------------------------------------------------------

dhis2.dsr.shareInterpretation = function()
{
	var dataSetReport = dhis2.dsr.getDataSetReport();
    var text = $( "#interpretationArea" ).val();
    
    if ( text.length && $.trim( text ).length )
    {
    	text = $.trim( text );
    	
	    var url = "../api/interpretations/dataSetReport/" + $( "#currentDataSetId" ).val() +
	    	"?pe=" + dataSetReport.pe +
	    	"&ou=" + dataSetReport.ou;
	    	    
	    $.ajax( url, {
	    	type: "POST",
	    	contentType: "text/html",
	    	data: text,
	    	success: function() {	    		
	    		$( "#interpretationArea" ).val( "" );
	    		setHeaderDelayMessage( i18n_interpretation_was_shared );
	    	}    	
	    } );
    }
}

//------------------------------------------------------------------------------
// Hooks in custom forms - must be present to avoid errors in forms
//------------------------------------------------------------------------------

function onValueSave( fn )
{
	// Do nothing
}

function onFormLoad( fn )
{
	// Do nothing
}
