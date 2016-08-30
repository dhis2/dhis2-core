var MODE_REPORT = "report";
var MODE_TABLE = "table";

var TYPE_HTML = "html";

// -----------------------------------------------------------------------------
// Validation
// -----------------------------------------------------------------------------

function validationError()
{
    if ( $( "#selectionTree" ).length && !selectionTreeSelection.isSelected() )
    {
        setHeaderDelayMessage( i18n_please_select_unit );
        return true;
    }

    return false;
}

// -----------------------------------------------------------------------------
// Report
// -----------------------------------------------------------------------------

function viewReport( type )
{
	var reportType = isDefined( type ) && type != "" ? type : "pdf";

    if ( validationError() )
    {
        return false;
    }

    var uid = $( "#uid" ).val();
    var mode = $( "#mode" ).val();
    var type = $( "#type" ).val();

    setHeaderMessage( i18n_process_completed );

    if ( MODE_REPORT == mode )
    {
    	if ( TYPE_HTML == type.toLowerCase() )
    	{
    		window.location.href= "generateHtmlReport.action?uid=" + uid + "&" + getUrlParams();
    	}
    	else // JASPER
    	{
    		window.location.href = "../api/reports/" + uid + "/data." + reportType + "?" + getUrlParams();
    	}
    }
    else // MODE_TABLE
    {
        window.location.href = "exportTable.action?uid=" + uid + "&type=html&" + getUrlParams();
    }
}

function getUrlParams()
{
    var url = "";

    if ( $( "#reportingPeriod" ).length )
    {
        url += "pe=" + $( "#reportingPeriod" ).val() + "&";
    }

    if ( selectionTreeSelection.isSelected() )
    {
        url += "ou=" + selectionTreeSelection.getSelectedUid();
    }

    return url;
}

// -----------------------------------------------------------------------------
// Report table
// -----------------------------------------------------------------------------

function exportReport( type, uid, pe, ou )
{
    var url = "exportTable.action?type=" + type + 
    	"&uid=" + uid + "&pe=" + pe + "&ou=" + ou;

    url += $( "#id" ).length ? ( "&id=" + $( "#id" ).val() ) : "";

    window.location.href = url;
}

function viewShareForm()
{
	$( "#shareForm" ).dialog( {
		modal : true,
		width : 550,
		resizable: false,
		title : i18n_share_your_interpretation
	} );
}

function shareInterpretation( uid, pe, ou )
{
    var text = $( "#interpretationArea" ).val();
    
    if ( text.length && $.trim( text ).length )
    {
    	text = $.trim( text );
    	
	    var url = "../api/interpretations/reportTable/" + uid + "?";
	    
	    url += ( pe && pe.length ) ? "pe=" + pe + "&": "";
	    url += ( ou && ou.length ) ? "ou=" + ou : "";
	    
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
