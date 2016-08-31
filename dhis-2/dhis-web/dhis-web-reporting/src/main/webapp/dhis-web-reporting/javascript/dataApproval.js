
dhis2.util.namespace( 'dhis2.appr' );

dhis2.appr.currentPeriodOffset = 0;
dhis2.appr.permissions = null;
dhis2.appr.dataSets = {};
dhis2.appr.dataApprovalLevels = {};

/**
 * Object with properties: ds, pe, ou, array of approvals with aoc, ou.
 */
dhis2.appr.uiState = {};

//------------------------------------------------------------------------------
// Report
//------------------------------------------------------------------------------

/**
 * Page init.
 */
$( document ).ready( function() 
{
	$.getJSON( "../api/dataSets.json?fields=id,displayName,periodType,workflow,categoryCombo[id,displayName]", function( json ) {
				
		var dsHtml = "<option value=''>[ Select ]</option>";
		
		$.each( json.dataSets, function( inx, ds ) {
			if ( ds.workflow ) {
				ds.hasCategoryCombo = !!( ds.categoryCombo.displayName !== "default" );
				dsHtml += "<option value='" + ds.id + "'>" + ds.displayName + "</option>";
				dhis2.appr.dataSets[ds.id] = ds;
			}
		} );
		
		$( "#dataSetId" ).html( dsHtml );
	} );
	
	$.getJSON( "../api/dataApprovalLevels.json", function( json ) {
		$.each( json.dataApprovalLevels, function( inx, al ) {
			dhis2.appr.dataApprovalLevels[al.id] = al;
		} );
	} );
} );

/**
 * Callback for changes in data set. Displays a list of period types starting
 * with the data set's period as the shortest type, and including all longer
 * types so that approvals can be made for multiple periods. If there is a
 * current period type selection, and it is still on the new list of period
 * types, keep it. Otherwise choose the period type for the data set.
 */
dhis2.appr.dataSetSelected = function()
{
    var ds = dhis2.appr.dataSets[$( "#dataSetId :selected" ).val()];
    var dataSetPeriodType = dhis2.appr.dataSets[ds.id].periodType;

    if ( $( "#periodType" ).val() != dataSetPeriodType ) {
        var periodTypeToSelect = $( "#periodType" ).val() || dataSetPeriodType;
        var foundDataSetPeriodType = false;
        var html = "<option value=''>[ " + i18n_select_period_type + " ]</option>";

        $.each( dhis2.appr.metaData.periodTypes, function () {
            if ( foundDataSetPeriodType || this == dataSetPeriodType ) {
                var selected = ( this == periodTypeToSelect ) ? " selected" : "";
                html += "<option value='" + this + "'" + selected + ">" + this + "</option>";
                foundDataSetPeriodType = true;
            } 
            else if ( this == periodTypeToSelect ) {
                periodTypeToSelect = dataSetPeriodType;
            }
        } );

        $( "#periodType" ).html( html );
        $( "#periodType" ).removeAttr( "disabled" );
        dhis2.appr.displayPeriods();
    }
    
    dhis2.appr.setCategoryComboDiv( ds );
    dhis2.appr.clearItemsDialog();
};

/**
 * Sets the category combo div HTML content. 
 * @param ds a data set object.
 */
dhis2.appr.setCategoryComboDiv = function( ds )
{
	if ( ds.hasCategoryCombo ) {
		var cc = ds.categoryCombo;
		var html = cc.displayName + "<br><a href='javascript:dhis2.appr.showItemsDialog()'>" + i18n_select_items + "</a>";
		$( "#attributeOptionComboDiv" ).show().html( html );
	}
	else {
		$( "#attributeOptionComboDiv" ).html( "" ).hide();
	}
}

/**
 * Sets the attribute option combo div HTML content.
 */
dhis2.appr.setItemsDialog = function()
{
	dhis2.appr.uiState = dhis2.appr.getUiState();
	ui = dhis2.appr.uiState;
	
	if ( !ui.ds || !ui.pe || !ui.ou ) {
		return false;
	}
	
	var cc = dhis2.appr.dataSets[ui.ds].categoryCombo,
		ccUrl = "../api/categoryCombos/" + cc.id + ".json?fields=id,displayName,categoryOptionCombos[id,displayName]",
		apUrl = "../api/dataApprovals/categoryOptionCombos?ds=" + ui.ds + "&pe=" + ui.pe + "&ou=" + ui.ou,
		cocs = {},
		ccName,
		html;
	
	$.getJSON( ccUrl, function( cc ) {
		
		ccName = cc.displayName.toLowerCase();
		
		$( "#attributeOptionComboHeaderDiv" ).html( "<h4 style='margin: 12px 0'>" + i18n_select_items_for + " " + ccName + "</h4>" );
		
		$.each( cc.categoryOptionCombos, function( inx, coc ) {
			cocs[coc.id] = coc;
		} );
		
		$.getJSON( apUrl, function( approvals ) {
		
			html = "<table id='attributeOptionComboTable'>";
			html += "<col width='2%'><col width='53%'><col width='17%'><col width='15%'><col width='10%'>";
			html += "<tr><th></th><th>" + i18n_item + "</th><th>" + i18n_organisation_unit + "</th><th>" + i18n_approval_level + "</th><th>" + i18n_action + "</th></tr>";
			
			$.each( approvals, function( inx, ap ) {
				
				var cocName = cocs[ap.id].displayName,
					pm = ap.permissions,
					level,
					levelName = i18n_not_yet_approved;
				
				if ( ap.level && ap.level.hasOwnProperty( "level" ) ) {
					level = dhis2.appr.dataApprovalLevels[ap.level.id];
					levelName = level && level.displayName ? level.displayName : levelName;
				}
				
				if ( cocName && pm ) {
					html += "<tr>";
					html += "<td><input type='checkbox' class='itemCheckbox' id='coc-" + ap.id + "-" + ap.ou + "' data-coc='" + ap.id + "' data-ou='" + ap.ou + "' ";
					html += "data-approve='" + pm.mayApprove + "' data-unapprove='" + pm.mayUnapprove + "' data-accept='" + pm.mayAccept + "' data-unaccept='" + pm.mayUnaccept + "'></td>";
					html += "<td><label for='coc-" + ap.id + "-" + ap.ou + "'>" + cocName + "</label></td>";
					html += "<td>" + ap.ouName + "</td>";
					html += "<td>" + levelName + "</td>";
					html += "<td>" + dhis2.appr.getPermissions( ap ) + "</td>";
					html += "</tr>";
				}
			} );
			
			html += "</table>";
			
			$( "#attributeOptionComboItemDiv" ).html( html );
		} );
	} );
};

/**
 * Supportive method for getting the permission text.
 * @param ap a data approval object. 
 */
dhis2.appr.getPermissions = function( ap )
{
	var s = "";
	
	if ( ap.permissions.mayApprove ) {
		s += i18n_approve + ", ";
	}
	if ( ap.permissions.mayUnapprove ) {
		s += i18n_unapprove + ", ";
	}
	if ( ap.permissions.mayAccept ) {
		s += i18n_accept + ", ";
	}
	if ( ap.permissions.mayUnaccept ) {
		s += i18n_unaccept + ", ";
	}
	
	if ( s.length > 2 ) {
		s = s.slice(0,-2);
	}
	else {
		s = "Pending";
	}
	
	return s;
};

/**
 * Checks all attribute option combo checkboxes.
 */
dhis2.appr.selectAllItems = function()
{
	$( ".itemCheckbox" ).prop( "checked", true );
};

/**
 * Unchecks all attribute option combo checkboxes.
 */
dhis2.appr.unselectAllItems = function()
{
	$( ".itemCheckbox" ).prop( "checked", false );
};

/**
 * Opens the attribute option combo dialog. Sets the attribute option
 * combos if necessary.
 */
dhis2.appr.showItemsDialog = function()
{
	var c = $( "#attributeOptionComboItemDiv" ).html();
	
	if ( $( "#attributeOptionComboItemDiv" ).html().length == 0 ) {
		dhis2.appr.setItemsDialog();
	}
	
	$( "#attributeOptionComboDialog" ).dialog( {
		modal : true,
		width : 800,
		height : 600,
		title : i18n_select_items
	} );
};

/**
 * Clears the attribute option combo dialog content.
 */
dhis2.appr.clearItemsDialog = function()
{
	$( "#attributeOptionComboItemDiv" ).html( "" );
}

/**
 * Closes the attribute option combo dialog.
 */
dhis2.appr.closeItemsDialog = function()
{
	$( "#attributeOptionComboDialog" ).dialog( "close" );
};

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
	dhis2.appr.clearItemsDialog();
};

dhis2.appr.orgUnitSelected = function()
{
	dhis2.appr.clearItemsDialog();
};

/**
 * Gets the state of the selected UI values.
 */
dhis2.appr.getDataReport = function()
{	
	var ui = dhis2.appr.getUiState(),
		aocs = [],
		aoc;
	
	var dataReport = {
		ds: ui.ds,
		pe: ui.pe,
		ou: ui.ou
	};
        
    $.each( $( ".itemCheckbox:checkbox:checked" ), function() {
		aoc = $( this ).data( "coc" );
		
		if ( aocs.indexOf( aoc ) == -1 ) {
			aocs.push( aoc );
		}
	} );
		
	if ( aocs && aocs.length ) {
		dataReport.dimension = "ao:" + aocs.join( ";" );
	}	
    
    return dataReport;
};

/**
 * Gets the state of the user interface selections.
 */
dhis2.appr.getUiState = function()
{
	var ui = {
		ds: $( "#dataSetId" ).val(),
		pe: $( "#periodId" ).val(),
		ou: selection.getSelected()[0],
		approvals: []
	};
    
    $.each( $( ".itemCheckbox:checkbox:checked" ), function() {		
		ui.approvals.push( {
			aoc: $( this ).data( "coc" ),
			ou: $( this ).data( "ou" ),
			permissions: {
				mayApprove: JSON.parse( $( this ).data( "approve" ) ),
				mayUnapprove: JSON.parse( $( this ).data( "unapprove" ) ),
				mayAccept: JSON.parse( $( this ).data( "accept" ) ),
				mayUnaccept: JSON.parse( $( this ).data( "unaccept" ) )
			}				
		} );
	} );
	    
    return ui;
};

/**
 * Gets an approval payload object based on the given state.
 * @param ui the ui state.
 * @param permissionProperty name of permission property which must be
 *        true for the approval record to be included.
 */
dhis2.appr.getApprovalPayload = function( ui, permissionProperty ) {
	
	var json = {
		ds: [ui.ds],
		pe: [ui.pe],
		approvals: []
	};
	
	$.each( ui.approvals, function( inx, ap ) {
		
		if ( ap.permissions && ap.permissions[permissionProperty] ) {
			json.approvals.push( {
				ou: ap.ou,
				aoc: ap.aoc
			} );
		}
	} );
	
	return json;
};	

/**
 * Generates the data set report for the currently selected parameters.
 */
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
    
    dhis2.appr.uiState = dhis2.appr.getUiState();

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

/**
 * Hides the data criteria div.
 */
dhis2.appr.hideCriteria = function()
{
	$( "#criteria" ).hide( "fast" );
	$( "#dataButton" ).removeAttr( "disabled" );
};

//------------------------------------------------------------------------------
// Approval
//------------------------------------------------------------------------------

/**
 * Sets the state of the approval buttons and notification.
 */
dhis2.appr.setApprovalState = function()
{
	var ui = dhis2.appr.uiState,
		ds = dhis2.appr.dataSets[ui.ds];
	
	if ( ds.hasCategoryCombo ) {
		dhis2.appr.setAttributeOptionComboApprovalState();
	}
	else {
		dhis2.appr.setRegularApprovalState();
	}
};

/**
 * Sets the state of the approval buttons and notification for data sets
 * with category combinations.
 */
dhis2.appr.setAttributeOptionComboApprovalState = function() 
{	
    var ui = dhis2.appr.uiState,
		notification = "";
    
    var counts = {
		approve: 0,
		unapprove: 0,
		accept: 0,
		unaccept: 0
	};
    
    var param = {
		ds: ui.ds,
		pe: ui.pe,
		ou: ui.ou
	};
	
	$.each( ui.approvals, function( inx, ap ) {
		if ( ap.permissions.mayApprove ) {
			counts.approve++;
		}
		if ( ap.permissions.mayUnapprove ) {
			counts.unapprove++;
		}
		if ( ap.permissions.mayAccept ) {
			counts.accept++;
		}
		if ( ap.permissions.mayUnaccept ) {
			counts.unaccept++;
		}
	} );
	
	if ( counts.approve > 0 ) {
		notification += i18n_approve + ": " + counts.approve + " items, ";
        $( "#approveButton" ).show();
    }
	if ( counts.unapprove > 0 ) {
		notification += i18n_unapprove + ": " + counts.unapprove + " items, ";
        $( "#unapproveButton" ).show();
    }
	if ( counts.accept > 0 ) {
		notification += i18n_accept + ": " + counts.accept + " items, ";
        $( "#acceptButton" ).show();
    }
	if ( counts.unaccept > 0 ) {
		notification += i18n_unaccept + ": " + counts.unaccept + " items, ";
        $( "#unacceptButton" ).show();
    }
    
    if ( notification && notification.length > 2 ) {
		notification = notification.slice( 0, -2 );
		$( "#approvalNotification" ).html( notification );
	}
	else {
		$( "#approvalNotification" ).html( i18n_nothing_to_do );
	}	
};

dhis2.appr.resetApprovalOptions = function()
{
	$( ".approveButton" ).hide();
	$( "#approvalNotification" ).html( i18n_done + ". " + i18n_please_make_selection );
	dhis2.appr.clearItemsDialog();
}

/**
 * Sets the state of the approval buttons and notification for regular
 * data sets.
 */
dhis2.appr.setRegularApprovalState = function( ui ) 
{    
    var ui = dhis2.appr.uiState;
    var param = {
		ds: ui.ds,
		pe: ui.pe,
		ou: ui.ou
	};
	
	$.getJSON( "../api/dataApprovals", param, function( json ) {	
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
		            $( "#approveButton" ).show();
		        }
		        
		        break;

            case "PARTIALLY_APPROVED_HERE":
                $( "#approvalNotification" ).html( i18n_approved_for_part_of_this_period );

                if ( json.mayApprove ) {
                    $( "#approveButton" ).show();
                }

                if ( json.mayUnapprove )  {
                    $( "#unapproveButton" ).show();
                }

                break;

            case "APPROVED_ABOVE":
                $( "#approvalNotification" ).html( i18n_approved_at_a_higher_level );
                break;

		    case "APPROVED_HERE":
		        $( "#approvalNotification" ).html( i18n_approved );
		        
		        if ( json.mayUnapprove )  {
		            $( "#unapproveButton" ).show();
		        }
		        
		        if ( json.mayAccept )  {
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
                    $( "#unapproveButton" ).show();
                }

                if ( json.mayAccept )  {
                    $( "#acceptButton" ).show();
                }

                if ( json.mayUnaccept )  {
                    $( "#unacceptButton" ).show();
                }

                break;

		    case "ACCEPTED_HERE":
		        $( "#approvalNotification" ).html( i18n_approved_and_accepted );
		        
		        if ( json.mayUnapprove )  {
		            $( "#unapproveButton" ).show();
		        }
		        
		        if ( json.mayUnaccept )  {
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

/**
 * Approve data.
 */
dhis2.appr.approveData = function()
{
	if ( !confirm( i18n_confirm_approval ) ) {
		return false;
	}
	
	var ui = dhis2.appr.getUiState(),
		ds = dhis2.appr.dataSets[ui.ds],
		json = dhis2.appr.getApprovalPayload( ui, "mayApprove" );
	
	if ( ds.hasCategoryCombo ) {
		$.ajax( {
			url: "../api/dataApprovals/approvals",
			type: "post",
			contentType: "application/json",
			data: JSON.stringify( json ),
			success: function() {
				dhis2.appr.resetApprovalOptions();
			},
			error: function( xhr, status, error ) {
				alert( xhr.responseText );
			}
		} );			
	}
	else {
		$.ajax( {
			url: "../api/dataApprovals?ds=" + ui.ds + "&pe=" + ui.pe + "&ou=" + ui.ou,
			type: "post",
			success: function() {
				dhis2.appr.setApprovalState();
			},
			error: function( xhr, status, error ) {
				alert( xhr.responseText );
			}
		} );
	}
};

/**
 * Unapprove data.
 */
dhis2.appr.unapproveData = function()
{
	if ( !confirm( i18n_confirm_unapproval ) ) {
		return false;
	}

	var ui = dhis2.appr.getUiState(),
		ds = dhis2.appr.dataSets[ui.ds],
		json = dhis2.appr.getApprovalPayload( ui, "mayUnapprove" );
	
	if ( ds.hasCategoryCombo ) {
		$.ajax( {
			url: "../api/dataApprovals/unapprovals",
			type: "post",
			contentType: "application/json",
			data: JSON.stringify( json ),
			success: function() {
				dhis2.appr.resetApprovalOptions();
			},
			error: function( xhr, status, error ) {
				alert( xhr.responseText );
			}
		} );
	}
	else {
		$.ajax( {
			url: "../api/dataApprovals?ds=" + ui.ds + "&pe=" + ui.pe + "&ou=" + ui.ou,
			type: "delete",
			success: function() {
				dhis2.appr.setApprovalState();
			},
			error: function( xhr, status, error ) {
				alert( xhr.responseText );
			}
		} );
	}
};

/**
 * Accept data.
 */
dhis2.appr.acceptData = function()
{
    if ( !confirm( i18n_confirm_accept ) ) {
        return false;
    }

	var ui = dhis2.appr.getUiState(),
		ds = dhis2.appr.dataSets[ui.ds],
		json = dhis2.appr.getApprovalPayload( ui, "mayAccept" );
	
	if ( ds.hasCategoryCombo ) {
		$.ajax( {
			url: "../api/dataAcceptances/acceptances",
			type: "post",
			contentType: "application/json",
			data: JSON.stringify( json ),
			success: function() {
				dhis2.appr.resetApprovalOptions();
			},
			error: function( xhr, status, error ) {
				alert( xhr.responseText );
			}
		} );
	}
	else {
		$.ajax( {
			url: "../api/dataAcceptances?ds=" + ui.ds + "&pe=" + ui.pe + "&ou=" + ui.ou,
			type: "post",
			success: function() {
				dhis2.appr.setApprovalState();
			},
			error: function( xhr, status, error ) {
				alert( xhr.responseText );
			}
		} );
	}
};

/**
 * Unaccept data.
 */
dhis2.appr.unacceptData = function()
{
    if ( !confirm( i18n_confirm_unaccept ) ) {
        return false;
    }

	var ui = dhis2.appr.getUiState(),
		ds = dhis2.appr.dataSets[ui.ds],
		json = dhis2.appr.getApprovalPayload( ui, "mayUnaccept" );
		
	if ( ds.hasCategoryCombo ) {
		$.ajax( {
			url: "../api/dataAcceptances/unacceptances",
			type: "post",
			contentType: "application/json",
			data: JSON.stringify( json ),
			success: function() {
				dhis2.appr.resetApprovalOptions();
			},
			error: function( xhr, status, error ) {
				alert( xhr.responseText );
			}
		} );
	}
	else {
		$.ajax( {
			url: "../api/dataAcceptances?ds=" + ui.ds + "&pe=" + ui.pe + "&ou=" + ui.ou,
			type: "delete",
			success: function() {
				dhis2.appr.setApprovalState();
			},
			error: function( xhr, status, error ) {
				alert( xhr.responseText );
			}
		} );
	}
};
