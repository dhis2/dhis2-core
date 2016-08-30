$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

function showUpdateProgramReminder( context ) {
  location.href = 'showUpdateProgramReminder.action?programId=' + getFieldValue('programId') + "&id=" + context.id;
}

function removeProgramReminder( context ) {
	var result = window.confirm( i18n_confirm_delete + "\n\n" + context.name );
    
    if ( result )
    {
    	setHeaderWaitMessage( i18n_deleting );
    	
    	$.postJSON("removeProgramReminder.action",
    	    {
    	        programId: getFieldValue('programId'),
				reminderId: context.id		
    	    },
    	    function( json )
    	    { 
    	    	if ( json.response == "success" )
    	    	{
					jQuery( "tr#tr" + context.id ).remove();
	                
					jQuery( "table.listTable tbody tr" ).removeClass( "listRow listAlternateRow" );
	                jQuery( "table.listTable tbody tr:odd" ).addClass( "listAlternateRow" );
	                jQuery( "table.listTable tbody tr:even" ).addClass( "listRow" );
					jQuery( "table.listTable tbody" ).trigger("update");
  
					setHeaderDelayMessage( i18n_delete_success );
    	    	}
    	    	else if ( json.response == "error" )
    	    	{ 
					setHeaderMessage( json.message );
    	    	}
    	    }
    	);
    }
}

function showReminderDetails( context ) {
 
  jQuery.getJSON("getReminder.action", {
    id: context.id,
	programId: getFieldValue('programId')
  }, function( json ) {
    setInnerHTML('nameField', json.reminder.name);
	setInnerHTML('idField', json.reminder.uid);
	
	var days = json.reminder.daysAllowedSendMessage;
	if( eval(days)<=0 ){
		setInnerHTML('daysAllowedSendMessageField', -1 * days + " " + i18n_days_before);
	}
	else{
		setInnerHTML('daysAllowedSendMessageField',eval(days) + " " + i18n_days_after);
	}

    setInnerHTML('templateMessageField', json.reminder.templateMessage);
    setInnerHTML('dateToCompareField', json.reminder.dateToCompare);
	
	var map = recipientsMap();
	setInnerHTML( 'sendToField', map[json.reminder.sendTo] ); 
	
	var whenToSend = i18n_days_scheduled;
	if( json.reminder.whenToSend=='3'){
		whenToSend = i18n_complete_program;
	}
	else if( json.reminder.whenToSend=='1'){
		whenToSend = i18n_program_enrollment;
	}
	setInnerHTML('whenToSendField', whenToSend);
    
	var type = i18n_direct_sms;
	if( json.reminder.messageType=='2'){
		whenToSend = i18n_message;
	}
	else if( json.reminder.whenToSend=='3'){
		whenToSend = i18n_both;
	}
	
	map = messageTypeMap();
    setInnerHTML('messageTypeField', map[json.reminder.messageType]);
	
    setInnerHTML('userGroupField', json.reminder.userGroup);
    showDetails();
  });
}

function recipientsMap()
{
	var typeMap = [];
	typeMap['1'] = i18n_tracked_entity_sms_only;
	typeMap['2'] = i18n_orgunit_phone_number_sms_only;
	typeMap['3'] = i18n_attribute_users;
	typeMap['4'] = i18n_all_users_in_orgunit_registered;
	typeMap['5'] = i18n_user_group;
	return typeMap;
}

function messageTypeMap()
{
	var typeMap = [];
	typeMap['1'] = i18n_direct_sms;
	typeMap['2'] = i18n_message;
	typeMap['3'] = i18n_both;
	return typeMap;
}

function showHideUserGroup() {
  jQuery(".sendTo").each(function( i, item ) {
    var numb = i + 1;
    if( item.value == 5 ) {
      showById('tr' + numb);
    }
    else
      hideById('tr' + numb);
  });
}

function insertParams( paramValue ) {
  var templateMessage = paramValue;
  insertTextCommon('templateMessage', templateMessage);
  getMessageLength();
}

function whenToSendOnChange() {
  var whenToSend = getFieldValue('whenToSend' );
  if( whenToSend == "" ) {
    enable('dateToCompare');
    enable('daysAllowedSendMessage' );
    enable('time');
    enable('days');
  }
  else {
    disable('dateToCompare');
    disable('daysAllowedSendMessage');
    disable('time');
    disable('days');
  }
}

function getMessageLength( ) {
  var message = getFieldValue('templateMessage' );
  var length = 0;
  var idx = message.indexOf('{');
  while( idx >= 0 ) {
    length += message.substr(0, idx).length;
    var end = message.indexOf('}');
    if( end >= 0 ) {
      message = message.substr(end + 1, message.length);
      idx = message.indexOf('{');
    }
  }
  length += message.length;
  setInnerHTML('messageLengthTD' , length + " " + i18n_characters_without_params);
  if( length >= 160 ) {
    jQuery('#templateMessage').attr('maxlength', 160);
  }
  else {
    jQuery('#templateMessage').removeAttr('maxlength');
  }
}

function setRealDays( ) {
  var days = jQuery("#days");
  var time = jQuery("#time option:selected ").val();
  jQuery("#daysAllowedSendMessage").val(time * eval(days).val());
}

function onchangeUserGroup() {
  var value = document.getElementById('sendTo').value;
  
  if( value == "1" || value == "3" ) {
    setFieldValue('messageType', '1');
    disable('messageType');
  }
  else {
    enable('messageType');
  }
  
  if( value == "5" ){
	enable('userGroup');
  }
  else{
	disable('userGroup');
  }
}
