$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

function programStageReminder(context){
	 location.href = 'programStageReminder.action?id=' + context.id;
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
// --------------------------------------------------------------------
// Generate template message form
// --------------------------------------------------------------------

function generateTemplateMessageForm()
{
	var rowId = jQuery('.daysAllowedSendMessage').length + 1;
	
	var contend = '<tr name="tr' + rowId + '" class="listAlternateRow" >'
				+ 	'<td colspan="2">' + i18n_reminder + ' ' + rowId + '<a href="javascript:removeTemplateMessageForm('+ rowId +')"> ( '+ i18n_remove_reminder + ' )</a></td>'
				+ '</tr>'
				+ '<tr name="tr' + rowId + '">'
				+ 	'<td><label>' + i18n_send_when_to + '</label></td>'
				+ 	'<td>'
				+ 		'<select id="whenToSend' + rowId + '" name="whenToSend' + rowId + '" class="whenToSend" onchange="whenToSendOnChange(' + rowId + ')">'
				+ 			'<option value="">' + i18n_days_scheduled + '</option>'
				+ 			'<option value="2">' + i18n_complete_event + '</option>'
				+ 		'</select>'
				+	'</td>'
				+ '</tr>'
				+ '<tr name="tr' + rowId + '">'
				+ 	'<td><label>' + i18n_send_message + '</label></td>'
				+ 	'<td>'
				+		'<input type="text" onchange="setRealDays(' + rowId + ')" style="width:100px;" realvalue="" id="daysAllowedSendMessage' + rowId + '" name="daysAllowedSendMessage' + rowId + '" class="daysAllowedSendMessage {validate:{required:true,number:true}}"/> '
				+ 		i18n_days
				+		' <select id="time' + rowId + '" name="time' + rowId + '" style="width:100px;" onchange="setRealDays(' + rowId + ')" >'
				+			'<option value="1">' + i18n_before + '</option>'
				+			'<option value="-1">' + i18n_after + '</option>'
				+		'</select> '
				+		i18n_scheduled_date
				+   ' </td>'
				+ '</tr>'
				+ '<tr name="tr' + rowId + '">'
				+ 	'<td><label>' + i18n_recipients + '</label></td>'
				+ 	'<td>'
				+ 		'<select id="sendTo' + rowId + '" name="sendTo' + rowId + '" class="sendTo" onchange="onchangeUserGroup('+ rowId +')">'
				+ 			'<option value="1">' + i18n_tracked_entity_sms_only + '</option>'
				+ 			'<option value="3">' + i18n_orgunit_phone_number_sms_only + '</option>'
				+ 			'<option value="2">' + i18n_attribute_users + '</option>'
				+ 			'<option value="4">' + i18n_all_users_at_orgunit + '</option>'
				+ 			'<option value="5">' + i18n_user_group + '</option>'
				+ 		'</select>'
				+	'</td>'
				+ '/<tr>'
				+ '<tr name="tr' + rowId + '" id="tr' + rowId + '">'
				+ 	'<td><label>' + i18n_user_group + '</label></td>'
				+ 	'<td>'
				+	program_stage_SMS_reminder_form
				+	'</td>'
				+ '/<tr>'
				+ '<tr name="tr' + rowId + '">'
				+ '	<td><label>' + i18n_message_type + '</label></td>'
				+ '	<td>'
				+ '		<select type="text" id="messageType' + rowId + '" name="messageType' + rowId + '" class="messageType {validate:{required:true,number:true}}" >'
				+ '			<option value="1">' + i18n_direct_sms + '</option>'
				+ '			<option value="2">' + i18n_message + '</option>'
				+ '			<option value="3">' + i18n_both + '</option>'
				+ '		</select>'
				+ '	</td>'
				+ '</tr>'
				+ '<tr name="tr' + rowId + '">'
				+	'<td>' + i18n_params + '</td>'
				+	'<td>'
				+		'<select multiple size="4" id="params' + rowId +'" name="params" ondblclick="insertParams(this.value, ' + rowId + ');">'
				+			'<option value="{program-name}">' + i18n_program_name + '</option>'
				+			'<option value="{program-stage-name}">' + i18n_program_stage_name + '</option>'
				+			'<option value="{due-date}">' + i18n_due_date + '</option>'
				+			'<option value="{days-since-due-date}">' + i18n_days_since_due_date + '</option>'
				+			'<option value="{orgunit-name}">' + i18n_orgunit_name + '</option>'
				+			attrOptions
				+		'</select>'
				+	'</td>'
				+ '</tr>'
				+ '<tr name="tr' + rowId + '">'
				+	'<td><label>' + i18n_message + '</label></td>'
				+	'<td><textarea id="templateMessage' + rowId + '" name="templateMessage' + rowId + '" style="width:320px" class="templateMessage {validate:{required:true}}"></textarea></td>'
				+ '</tr>'
				+ '<tr>'
				+	'<td></td>'
				+ 	'<td id="messageLengthTD' + rowId + '"></td>'
				+ '</tr>';

	jQuery('#programStageMessage').append( contend );
	showHideUserGroup();
}

function removeTemplateMessageForm( rowId )
{
	jQuery("[name=tr" + rowId + "]").remove();
}

function whenToSendOnChange(index)
{
	var whenToSend = getFieldValue('whenToSend' + index );
	if(whenToSend==2){
		disable('daysAllowedSendMessage' + index );
		disable('time' + index );
	}
	else{
		enable('daysAllowedSendMessage' + index );
		enable('time' + index );
	}
}
function showHideUserGroup()
{
	jQuery(".sendTo").each( function( i, item ){
		var numb = i+1;
		if( item.value == 5){
			showById( 'tr'+numb );
		}
		else
			hideById ( 'tr'+numb );
	});
}

function onchangeUserGroup( id )
{
	var value = document.getElementById( 'sendTo' + id ).value;
	hideById( 'tr'+id );
	
	if( value=="1" || value=="3" ){
		setFieldValue('messageType' + id , '1');
		disable('messageType' + id );
	}
	else{
		if ( value == "5") {
			showById( 'tr' + id );
		}
		enable ('messageType' + id );
	}
}

function getMessageLength(rowId)
{
	var message = getFieldValue( 'templateMessage' + rowId );
	var length = 0;
	var idx = message.indexOf('{');
	while( idx >=0 ){
		length += message.substr(0,idx).length;
		var end = message.indexOf('}');
		if(end>=0){
			message = message.substr(end + 1, message.length);
			idx = message.indexOf('{');
		}
	}
	length += message.length;
	setInnerHTML('messageLengthTD' + rowId, length + " " + i18n_characters_without_params);
	if( length>=160 )
	{
		jQuery('#templateMessage' + rowId ).attr('maxlength', 160);
	}
	else
	{
		jQuery('#templateMessage' + rowId ).removeAttr('maxlength');
	}
}

function setRealDays(rowId)
{
	var daysAllowedSendMessage = jQuery("#daysAllowedSendMessage" + rowId);
	var time = jQuery("#time" + rowId + " option:selected ").val();
	daysAllowedSendMessage.attr("realvalue", time * eval(daysAllowedSendMessage).val());
	var aasdf= 0;
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

