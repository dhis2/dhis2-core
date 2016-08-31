
dhis2.util.namespace( 'dhis2.appr' );

dhis2.appr.currentPeriodOffset = 0;
dhis2.appr.permissions = null;

//------------------------------------------------------------------------------
// Report
//------------------------------------------------------------------------------

/**
 * Callback for changes in data set. Displays a list of period types starting
 * with the data set's period as the shortest type, and including all longer
 * types so that approvals can be made for multiple periods. If there is a
 * current period type selection, and it is still on the new list of period
 * types, keep it. Otherwise choose the period type for the data set.
 */
dhis2.appr.dataSetSelected = function()
{
    var dataSetPeriodType = $( "#dataSetId :selected" ).data( "pt" );

    if ( $( "#periodType" ).val() != dataSetPeriodType ) {
        var periodTypeToSelect = $( "#periodType" ).val() || dataSetPeriodType;
        var foundDataSetPeriodType = false;
        var html = "<option value=''>[ " + i18n_select_period_type + " ]</option>";

        $.each( dhis2.appr.metaData.periodTypes, function () {
            if ( foundDataSetPeriodType || this == dataSetPeriodType ) {
                var selected = ( this == periodTypeToSelect ) ? " selected" : "";
                html += "<option value='" + this + "'" + selected + ">" + this + "</option>";
                foundDataSetPeriodType = true;
            } else if ( this == periodTypeToSelect ) {
                periodTypeToSelect = dataSetPeriodType;
            }
        } );

        $( "#periodType" ).html( html );
        $( "#periodType" ).removeAttr( "disabled" );
        dhis2.appr.displayPeriods();
    }
}

dhis2.appr.displayPeriods = function()
{
    var periodType = $( "#periodType" ).val();
    dhis2.dsr.displayPeriodsInternal( periodType, dhis2.appr.currentPeriodOffset );
};

dhis2.appr.displayNextPeriods = function()
{	
    if ( dhis2.appr.currentPeriodOffset < 0 ) // Cannot display future periods
    {
        dhis2.appr.currentPeriodOffset++;
        dhis2.appr.displayPeriods();
    }
};

dhis2.appr.displayPreviousPeriods = function()
{
    dhis2.appr.currentPeriodOffset--;
    dhis2.appr.displayPeriods();
};

dhis2.appr.periodSelected = function()
{
};

dhis2.appr.getDataReport = function()
{	
    var dataReport = {
        ds: $( "#dataSetId" ).val(),
        pe: $( "#periodId" ).val(),
        ou: selection.getSelected()[0]
    };
        
    return dataReport;
};

dhis2.appr.generateDataReport = function()
{
	var dataReport = dhis2.appr.getDataReport();
	
	if ( !dataReport.ds )
    {
		setHeaderDelayMessage( i18n_select_data_set );
        return false;
    }
    if ( !dataReport.pe )
    {
    	setHeaderDelayMessage( i18n_select_period );
        return false;
    }
    if ( !selection.isSelected() )
    {
    	setHeaderDelayMessage( i18n_select_organisation_unit );
        return false;
    }

    hideHeaderMessage();
    dhis2.appr.hideCriteria();
	$( "#content" ).hide( "fast" );
    showLoader();
    
    $.get( "generateDataSetReport.action", dataReport, function( data ) {
    	$( "#content" ).html( data );
    	$( "#shareForm" ).hide();
    	hideLoader();
    	$( "#content" ).show( "fast" );
    	setTableStyles();
    	dhis2.appr.setApprovalState();
    } );
};

dhis2.appr.hideCriteria = function()
{
	$( "#criteria" ).hide( "fast" );
	$( "#dataButton" ).removeAttr( "disabled" );
};

//------------------------------------------------------------------------------
// Approval
//------------------------------------------------------------------------------

dhis2.appr.setApprovalState = function()
{
	var data = dhis2.appr.getDataReport();
	
    $( "#approvalDiv" ).hide();
		
	$.getJSON( "../api/dataApprovals", data, function( json ) {	
		if ( !json || !json.state ) {
			return;
		}
	
		dhis2.appr.permissions = json;
		
	    $( ".approveButton" ).hide();
	
	    switch ( json.state ) {
	        case "UNAPPROVABLE":
		        $( "#approvalNotification" ).html( i18n_approval_not_relevant );
		        break;

            case "UNAPPROVED_ABOVE":
                $( "#approvalNotification" ).html( i18n_ready_for_approval_at_a_higher_level );
                break;

		    case "UNAPPROVED_WAITING":
		        $( "#approvalNotification" ).html( i18n_waiting_for_lower_level_approval );
		        break;
		
		    case "UNAPPROVED_ELSEWHERE":
		        $( "#approvalNotification" ).html( i18n_waiting_for_approval_elsewhere );
		        break;		    	
		        
		    case "UNAPPROVED_READY":
		        $( "#approvalNotification" ).html( i18n_ready_for_approval );

		        if ( json.mayApprove ) {
		            $( "#approvalDiv" ).show();
		            $( "#approveButton" ).show();
		        }
		        
		        break;

            case "PARTIALLY_APPROVED_HERE":
                $( "#approvalNotification" ).html( i18n_approved_for_part_of_this_period );

                if ( json.mayApprove ) {
                    $( "#approvalDiv" ).show();
                    $( "#approveButton" ).show();
                }

                if ( json.mayUnapprove )  {
                    $( "#approvalDiv" ).show();
                    $( "#unapproveButton" ).show();
                }

                break;

            case "APPROVED_ABOVE":
                $( "#approvalNotification" ).html( i18n_approved_at_a_higher_level );
                break;

		    case "APPROVED_HERE":
		        $( "#approvalNotification" ).html( i18n_approved );
		        
		        if ( json.mayUnapprove )  {
		            $( "#approvalDiv" ).show();
		            $( "#unapproveButton" ).show();
		        }
		        
		        if ( json.mayAccept )  {
		            $( "#approvalDiv" ).show();
		            $( "#acceptButton" ).show();
		        }
		        
		        break;

            case "PARTIALLY_APPROVED_ELSEWHERE":
                $( "#approvalNotification" ).html( i18n_approved_elsewhere_for_part_of_this_period );
                break;

		    case "APPROVED_ELSEWHERE":
		        $( "#approvalNotification" ).html( i18n_approved_elsewhere );
		        break;

            case "PARTIALLY_ACCEPTED_HERE":
                $( "#approvalNotification" ).html( i18n_accepted_for_part_of_this_period );

                if ( json.mayUnapprove )  {
                    $( "#approvalDiv" ).show();
                    $( "#unapproveButton" ).show();
                }

                if ( json.mayAccept )  {
                    $( "#approvalDiv" ).show();
                    $( "#acceptButton" ).show();
                }

                if ( json.mayUnaccept )  {
                    $( "#approvalDiv" ).show();
                    $( "#unacceptButton" ).show();
                }

                break;

		    case "ACCEPTED_HERE":
		        $( "#approvalNotification" ).html( i18n_approved_and_accepted );
		        
		        if ( json.mayUnapprove )  {
		            $( "#approvalDiv" ).show();
		            $( "#unapproveButton" ).show();
		        }
		        
		        if ( json.mayUnaccept )  {
		            $( "#approvalDiv" ).show();
		            $( "#unacceptButton" ).show();
		        }
		        
		        break;

            case "PARTIALLY_ACCEPTED_ELSEWHERE":
                $( "#approvalNotification" ).html( i18n_accepted_elsewhere_for_part_of_this_period );
                break;

	        case "ACCEPTED_ELSEWHERE":
		        $( "#approvalNotification" ).html( i18n_accepted_elsewhere );
		        break;
		    }
	           	
		} );	
};

dhis2.appr.approveData = function()
{
	if ( !confirm( i18n_confirm_approval ) ) {
		return false;
	}
	
	$.ajax( {
		url: dhis2.appr.getApprovalUrl(),
		type: "post",
		success: function() {
            dhis2.appr.setApprovalState();
        },
		error: function( xhr, status, error ) {
			alert( xhr.responseText );
		}
	} );
};

dhis2.appr.unapproveData = function()
{
	if ( !confirm( i18n_confirm_unapproval ) ) {
		return false;
	}

	$.ajax( {
		url: dhis2.appr.getApprovalUrl(),
		type: "delete",
		success: function() {
            dhis2.appr.setApprovalState();
        },
		error: function( xhr, status, error ) {
			alert( xhr.responseText );
		}
	} );
};

dhis2.appr.acceptData = function()
{
    if ( !confirm( i18n_confirm_accept ) ) {
        return false;
    }

    $.ajax( {
		url: dhis2.appr.getAcceptanceUrl(),
        type: "post",
        success: function() {
            dhis2.appr.setApprovalState();
        },
        error: function( xhr, status, error ) {
            alert( xhr.responseText );
        }
    } );
};

dhis2.appr.unacceptData = function()
{
    if ( !confirm( i18n_confirm_unaccept ) ) {
        return false;
    }

    $.ajax( {
		url: dhis2.appr.getAcceptanceUrl(),
        type: "delete",
        success: function() {
            dhis2.appr.setApprovalState();
        },
        error: function( xhr, status, error ) {
            alert( xhr.responseText );
        }
  } );
};

dhis2.appr.getApprovalUrl = function()
{
	var data = dhis2.appr.getDataReport();
	var url = "../api/dataApprovals?ds=" + data.ds + "&pe=" + data.pe + "&ou=" + data.ou;
	return url;
};

dhis2.appr.getAcceptanceUrl = function()
{
	var data = dhis2.appr.getDataReport();
	var url = "../api/dataAcceptances?ds=" + data.ds + "&pe=" + data.pe + "&ou=" + data.ou;
	return url;
};
