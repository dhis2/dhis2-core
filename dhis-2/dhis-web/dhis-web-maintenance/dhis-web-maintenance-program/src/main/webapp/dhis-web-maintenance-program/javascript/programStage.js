$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

function programStageNotification(context) {
	location.href = 'programStageNotification.action?id=' + context.id;
}

function removeProgramStage( context ) {
  removeItem( context.id, context.name, i18n_confirm_delete , 'removeProgramStage.action' );
}

function showUpdateProgramStageForm( context ) {
  location.href = 'showUpdateProgramStageForm.action?id=' + context.id;
}

function viewDataEntryForm( context ) {
  location.href = 'viewDataEntryForm.action?programStageId=' + context.id;
}

function programStageSectionList( context ) {
  location.href = 'programStageSectionList.action?id=' + context.id;
}

function getStageByProgram( programId )
{
	window.location.href = "programStage.action?id=" + programId;
}

function addProgramStage()
{
	var programId = document.getElementById('id').value;

	if( programId == "null" || programId == "" )
	{
		showWarningMessage( i18n_please_select_program );
	}
	else
	{
		window.location.href="showAddProgramStageForm.action?id=" + programId;
	}
}

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showProgramStageDetails( context )
{
	jQuery.getJSON( 'getProgramStage.action', { id: context.id }, function ( json ) {
		setInnerHTML( 'nameField', json.programStage.name );	
		setInnerHTML( 'descriptionField', json.programStage.description );
		setInnerHTML( 'scheduledDaysFromStartField', json.programStage.minDaysFromStart ); 
		setInnerHTML( 'idField', json.programStage.uid ); 
		
		var repeatable = (json.programStage.repeatable=='true') ? i18n_yes : i18n_no;
		setInnerHTML( 'repeatableField', repeatable );
		
		var autoGenerateEvent = (json.programStage.autoGenerateEvent=='true') ? i18n_yes : i18n_no;
		setInnerHTML( 'autoGenerateEventField', autoGenerateEvent );  
		
		var displayGenerateEventBox = (json.programStage.displayGenerateEventBox=='true') ? i18n_yes : i18n_no;
		setInnerHTML( 'displayGenerateEventBoxField', displayGenerateEventBox );  
		
		var validCompleteOnly = (json.programStage.validCompleteOnly=='true') ? i18n_yes : i18n_no;
		setInnerHTML( 'validCompleteOnlyField', validCompleteOnly );  
		
		var captureCoordinates = (json.programStage.captureCoordinates=='true') ? i18n_yes : i18n_no;
		setInnerHTML( 'captureCoordinatesField', captureCoordinates );
		
		setInnerHTML( 'standardIntervalField', json.programStage.standardInterval );  
		setInnerHTML( 'dataElementCountField', json.programStage.dataElementCount );   
		setInnerHTML( 'excecutionDateLabelField', json.programStage.excecutionDateLabel );
		
		var displayProvidedOtherFacility = ( json.programStage.displayProvidedOtherFacility == 'true') ? i18n_yes : i18n_no;
		setInnerHTML( 'displayProvidedOtherFacilityField', displayProvidedOtherFacility );   	
		
		var blockEntryForm = ( json.programStage.blockEntryForm == 'true') ? i18n_yes : i18n_no;
		setInnerHTML( 'blockEntryFormField', blockEntryForm );   	
		
		var generatedByEnrollmentDate = ( json.programStage.generatedByEnrollmentDate == 'true') ? i18n_yes : i18n_no;
		setInnerHTML( 'generatedByEnrollmentDateField', generatedByEnrollmentDate );   	
		
		var remindCompleted = ( json.programStage.remindCompleted == 'true') ? i18n_yes : i18n_no;
		setInnerHTML( 'remindCompletedField', remindCompleted );   	
		
		var allowGenerateNextVisit = ( json.programStage.allowGenerateNextVisit == 'true') ? i18n_yes : i18n_no;
		setInnerHTML( 'allowGenerateNextVisitField', allowGenerateNextVisit );   	
		
		var openAfterEnrollment = ( json.programStage.openAfterEnrollment == 'true') ? i18n_yes : i18n_no;
		setInnerHTML( 'openAfterEnrollmentField', openAfterEnrollment );
		
		var preGenerateUID = ( json.programStage.preGeneateUID == 'true') ? i18n_yes : i18n_no;
		setInnerHTML( 'preGenerateUIDField', preGenerateUID );    	
		
		setInnerHTML( 'reportDateToUseField', json.programStage.reportDateToUse );   	
		
		showDetails();
	});
}

// -----------------------------------------------------------------------------
// select data-elements
// -----------------------------------------------------------------------------

function selectDataElements()
{
	var selectedList = jQuery("#selectedList");
	jQuery("#availableList").children().each(function(i, item){
		if( item.selected ){
			html = "<tr class='selected' id='" + item.value + "' ondblclick='unSelectDataElement( this )'><td onmousedown='select(event,this)'>" + item.text + "</td>";
			html += "<td align='center'><input type='checkbox' name='compulsory'></td>";
			html += "<td align='center'><input type='checkbox' name='allowProvided'></td>";
			html += "<td align='center'><input type='checkbox' name='displayInReport'></td>";
			if( jQuery(item).attr('valuetype') =='DATE'){
				html += "<td align='center'><input type='checkbox' name='allowFutureDate'></td>";
			}
			else{
				html += "<td align='center'><input type='hidden' name='allowFutureDate'></td>";
			}
			
			html += "</tr>";
			selectedList.append( html );
			jQuery( item ).remove();
		}
	});
}


function selectAllDataElements()
{
	var selectedList = jQuery("#selectedList");
	jQuery("#availableList").children().each(function(i, item){
		html = "<tr class='selected' id='" + item.value + "' ondblclick='unSelectDataElement( this )'><td onmousedown='select(this)'>" + item.text + "</td>";
		html += "<td align='center'><input type='checkbox' name='compulsory'></td>";
		html += "<td align='center'><input type='checkbox' name='allowProvided'></td>";
		html += "<td align='center'><input type='checkbox' name='displayInReport'></td>";
		
		if( jQuery(item).attr('valuetype') =='DATE'){
			html += "<td align='center'><input type='checkbox' name='allowFutureDate'></td>";
		}
		else{
			html += "<td align='center'><input type='hidden' name='allowFutureDate'></td>";
		}
		
		html += "</tr>";
		selectedList.append( html );
		jQuery( item ).remove();
	});
}

function unSelectDataElements()
{
	var availableList = jQuery("#availableList");
	jQuery("#selectedList").find("tr").each( function( i, item ){
		item = jQuery(item);
		if( item.hasClass("selected") )
		{		
			availableList.append( "<option value='" + item.attr( "id" ) + "' selected='true' valuetype='" + item.valuetype + "'>" + item.find("td:first").text() + "</option>" );
			item.remove();
		}
	});
}


function unSelectAllDataElements()
{
	var availableList = jQuery("#availableList");
	jQuery("#selectedList").find("tr").each( function( i, item ){
		item = jQuery(item);
		availableList.append( "<option value='" + item.attr( "id" ) + "' selected='true' valuetype='" + item.valuetype + "'>" + item.find("td:first").text() + "</option>" );
		item.remove();
	});
}

//-----------------------------------------------------------------------------
//Move Table Row Up and Down
//-----------------------------------------------------------------------------

function moveUpDataElement()
{
	var selectedList = jQuery("#selectedList");

	jQuery("#selectedList").find("tr").each( function( i, item ){
		item = jQuery(item);
		if( item.hasClass("selected") )
		{
			var prev = item.prev('#selectedList tr');
			if (prev.length == 1) 
			{ 
				prev.before(item);
			}
		}
	});
}

function moveDownDataElement()
{
	var selectedList = jQuery("#selectedList");
	var items = new Array();
	jQuery("#selectedList").find("tr").each( function( i, item ){
		items.push(jQuery(item));
	});
	
	for( var i=items.length-1;i>=0;i--)
	{	
		var item = items[i];
		if( item.hasClass("selected") )
		{
			var next = item.next('#selectedList tr');
			if (next.length == 1) 
			{ 
				next.after(item);
			}
		}
	}
}

function unSelectDataElement( element )
{
	element = jQuery(element);	
	jQuery("#availableList").append( "<option value='" + element.attr( "id" ) + "' selected='true'>" + element.find("td:first").text() + "</option>" );
	element.remove();
}

function select( event, element )
{
	if ( !getKeyCode( event ) )// Ctrl
	{
		jQuery("#selectedList .selected").removeClass( 'selected' );
	}
	
	element = jQuery( element ).parent();
	if( element.hasClass( 'selected') ) element.removeClass( 'selected' );
	else element.addClass( 'selected' );
}

function getKeyCode(e)
{
	var ctrlPressed=0;

	if (parseInt(navigator.appVersion)>3) {

		var evt = e ? e:window.event;

		if (document.layers && navigator.appName=="Netscape"
		&& parseInt(navigator.appVersion)==4) {
			// NETSCAPE 4 CODE
			var mString =(e.modifiers+32).toString(2).substring(3,6);
			ctrlPressed =(mString.charAt(1)=="1");
		}
		else {
			// NEWER BROWSERS [CROSS-PLATFORM]
			ctrlPressed=evt.ctrlKey;
		}
	}
	return ctrlPressed;
}

function repeatableOnChange()
{
	var checked = byId('repeatable').checked;
	if( checked )
	{
		enable('standardInterval');
		enable('periodTypeName');
		enable('displayGenerateEventBox');
	}
	else
	{
		disable('standardInterval');
		disable('periodTypeName');
		disable('displayGenerateEventBox');
	}
}

function periodTypeOnChange(){
	var periodType = byId('periodTypeName').value;	
	if( periodType != ''){
		disable('standardInterval');
	}
	else{
		enable('standardInterval');
	}
}

function openAfterEnrollmentOnchange()
{
	if(byId('openAfterEnrollment').checked){
		enable('reportDateToUse');
	}
	else{
		disable('reportDateToUse');
	}
}

function autoGenerateEventOnChange(openAfterEnrollment)
{
	if(openAfterEnrollment==''){
		if( byId('autoGenerateEvent').checked ){
			enable('openAfterEnrollment');
		}
		else{
			disable('openAfterEnrollment');
			disable('reportDateToUse');
		}
	}
}

