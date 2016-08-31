var isSave;
var interval = 60000;

$( document ).ready( function() 
{
	$(":button").button();
	$(":submit").button();
	$("#saveButton").button("option", "icons", { primary: "ui-icon-disk" });
	$("#cancelButton").button("option", "icons", { primary: "ui-icon-cancel" });
	$("#deleteButton").button("option", "icons", { primary: "ui-icon-trash" });
	$("#insertButton").button("option", "icons", { primary: "ui-icon-plusthick" });
	$("#propertiesButton").button("option", "icons", { primary: "ui-icon-newwin" });
	$("#insertImagesButton").button("option", "icons", { primary: "ui-icon-newwin" });
	$("#insertImageButton").button("option", "icons", { primary: "ui-icon-plusthick" });
	
	$("#imageDialog").bind("dialogopen", function(event, ui) {
		$("#insertImagesButton").button("disable");
	})
	$("#imageDialog").bind("dialogclose", function(event, ui) {
		$("#insertImagesButton").button("enable");
	})
	
	$("#insertImagesButton").click(function() {
		$("#imageDialog").dialog({
			overlay:{background:'#000000', opacity:0.1},
			minWidth: 595,
			minHeight: 263,
			position: [($("body").width() - 555) - 50, 50],
		});
	});
	
	if( autoSave )
	{
		timeOut = window.setTimeout( "validateRegistrationFormTimeout( false );", interval );
	}
});
	
function openPropertiesSelector()
{	
	$("#propertiesButton").addClass("ui-state-active2");
	$('#selectionDialog' ).dialog(
		{
			title:i18n_properties,
			maximize:true, 
			closable:true,
			modal:false,
			overlay:{background:'#000000', opacity:0.1},
			minWidth: 595,
			minHeight: 263,
			position: [($("body").width() - 555) - 50, 50],
			close: function(ev, ui) { 
				$("#propertiesButton").removeClass("ui-state-active2"); 
			}
		});
}

function fixAttrOnClick()
{
	$("#insertButton").click(function() {
		insertElement( 'fixedAttr' );
	});	
	
	$("#fixAttrButton").addClass("ui-state-active2");
	$("#attributesButton").removeClass("ui-state-active2");
	$("#programAttrButton").removeClass("ui-state-active2");
	hideById('attributeTab');
	hideById('programAttrTab');
	showById('fixedAttrTab');
}

function attributesOnClick()
{
	$("#insertButton").click(function() {
		insertElement( 'attr' );
	});	
	
	$("#fixAttrButton").removeClass("ui-state-active2");
	$("#attributesButton").addClass("ui-state-active2");
	$("#programAttrButton").removeClass("ui-state-active2");
	hideById('fixedAttrTab');
	hideById('programAttrTab');
	showById('attributeTab');
}

function programAttrOnClick()
{
	$("#insertButton").click(function() {
		insertElement( 'prg' );
	});	
	
	$("#fixAttrButton").removeClass("ui-state-active2");
	$("#attributesButton").removeClass("ui-state-active2");
	$("#programAttrButton").addClass("ui-state-active2");
	hideById('attributeTab');
	hideById('fixedAttrTab');
	showById('programAttrTab');
}

function getDefaultRequiredFields()
{
	var requiredFields = {};
	if( getFieldValue("disableRegistrationFields")!='true' )
	{
		requiredFields['fixedattributeid=fullName'] = i18n_full_name;
		
		jQuery('#attributesSelector option').each(function() {
			var item = jQuery(this);
			if( item.attr('mandatory')=='true'){
				requiredFields['attributeid=' + item.val()] = item.text();
			}
		});
		
		jQuery('#programAttrSelector option').each(function() {
			var item = jQuery(this);
			if( item.attr('mandatory')=='true'){
				requiredFields['programid=' + item.val()] = item.text();
			}
		});
		
		var html = jQuery("#designTextarea").ckeditor().editor.getData();
		var input = jQuery( html ).find("input");
		if( input.length > 0 )
		{
			input.each( function(i, item){	
				var key = "";
				var inputKey = jQuery(item).attr('fixedattributeid');
				if( inputKey!=undefined)
				{
					key = 'fixedattributeid=' + inputKey
				}
				else if( jQuery(item).attr('attributeid')!=undefined ){
					inputKey = jQuery(item).attr('attributeid');
					key = 'attributeid=' + inputKey
				}
				else if( jQuery(item).attr('programid')!=undefined ){
					inputKey = jQuery(item).attr('programid');
					key = 'programid=' + inputKey
				}
					
				for (var idx in requiredFields){
					if( key == idx)
					{
						delete requiredFields[idx];
					}
				}
			});
		}
	
	}
	return requiredFields;
}

function validateProgramFields()
{
	var requiredFields = {};
	jQuery('#programAttrSelector option').each(function() {
		var item = jQuery(this);
		if( item.attr('mandatory')=='true'){
			requiredFields['programid=' + item.val()] = item.text();
		}
	});
	
	var html = jQuery("#designTextarea").ckeditor().editor.getData();
	var input = jQuery( html ).find("input");
	if( input.length > 0 )
	{
		input.each( function(i, item){	
			var key = "";
			var inputKey = jQuery(item).attr('fixedattributeid');
			if( jQuery(item).attr('programid')!=undefined ){
				inputKey = jQuery(item).attr('programid');
				key = 'programid=' + inputKey
			}
			
			for (var idx in requiredFields){
				if( key == idx){
					delete requiredFields[idx];
				}
			}
		});
	}
	
	var violate = "";
	if( Object.keys(requiredFields).length > 0 )
	{
		violate = '<h3>' + i18n_please_insert_all_required_fields + '<h3>';
		for (var idx in requiredFields){
			violate += " - " + requiredFields[idx] + '<br>';
		}
		jQuery('#validateDiv').html(violate).dialog({
			title:i18n_required_fields_valivation,
			maximize:true, 
			closable:true,
			modal:false,
			overlay:{background:'#000000', opacity:0.1},
			width:500,
			height:300
		});
		return false;
	}
	
	return true;
}

function validateFormOnclick()
{
	var requiredFields = getRequiredFields();
	var violate = "";
	if( Object.keys(requiredFields).length > 0 )
	{
		violate = '<h3>' + i18n_please_insert_all_required_fields + '<h3>';
		for (var idx in requiredFields){
			violate += " - " + requiredFields[idx] + '<br>';
		}
	}
	else
	{
		violate = '<h3>' + i18n_validate_success + '<h3>';
	}
	
	jQuery('#validateDiv').html(violate).dialog({
		title:i18n_required_fields_valivation,
		maximize:true, 
		closable:true,
		modal:false,
		overlay:{background:'#000000', opacity:0.1},
		width:500,
		height:300
	});
}

function validateForm( checkViolate )
{
	requiredFields = getRequiredFields();
	
	if( Object.keys(requiredFields).length > 0 )
	{
		if ( byId('autoSave').checked )
		{
			setHeaderMessage( i18n_save_unsuccess_please_insert_all_required_fields );
		}
		else
		{
			var violate = '<h3>' + i18n_please_insert_all_required_fields + '<h3>';
			for (var idx in requiredFields){
				violate += " - " + requiredFields[idx] + '<br>';
			}
			
			setInnerHTML('validateDiv', violate);
			jQuery('#validateDiv').dialog({
				title:i18n_required_fields_valivation,
				maximize:true, 
				closable:true,
				modal:false,
				overlay:{background:'#000000', opacity:0.1},
				width:500,
				height:300
			});
			
		}
		return false;
	}
	else
	{
		return true;
	}
}

function checkLabelAssigned( id )
{	
	var assigned = false;
	var assignedTo = "";
	
	var html = jQuery("#designTextarea").ckeditor().editor.getData();
	var labels = jQuery( html ).find("span[d2-input-label]");
	
	labels.each(function(i, item){		
		var labelAtt = jQuery(item).attr('d2-input-label');
		if( labelAtt != undefined && labelAtt == id){
			assigned = true;
			assignedTo = jQuery(item).text();
			return false;
		}
	});
	
	return {assigned: assigned, assignedTo: assignedTo};
}

function checkExisted( id )
{	
	var result = false;
	var html = jQuery("#designTextarea").ckeditor().editor.getData();
	var input = jQuery( html ).find("input");

	input.each( function(i, item){		
		var key = "";
		var inputKey = jQuery(item).attr('fixedattributeid');
		if( inputKey!=undefined)
		{
			key = 'fixedattributeid="' + inputKey + '"';
		}
		else if( jQuery(item).attr('attributeid')!=undefined ){
			inputKey = jQuery(item).attr('attributeid');
			key = 'attributeid="' + inputKey + '"';
		}
		else if( jQuery(item).attr('programid')!=undefined ){
			inputKey = jQuery(item).attr('programid');
			key = 'programid="' + inputKey + '"';
		}
		
		if( id == key ) result = true;		
		
	});

	return result;
}

function insertElement( type )
{ 
	var id = '';
	var value = '';
	
	if(type == 'lbl') {
		var selectedText = jQuery("#designTextarea").ckeditor().editor.getSelection().getSelectedText();
		var element = jQuery('#attributeTab option:selected');
		
		if( element.length == 0 ) return;		
		
		id = 'attributeId.' + element.attr('value');
		
		var assigned = checkLabelAssigned( id );
		if( assigned && assigned.assigned ){
			setHeaderDelayMessage( i18n_label_is_assigned_to + ': ' + assigned.assignedTo );
			return;
		}
		else{
			var label = '<span title="' + selectedText + '" d2-input-label="' + id + '">' + selectedText + '</span>';		
			var lEditor = jQuery("#designTextarea").ckeditor().editor;
			lEditor.insertHtml( label );
		}
	}
	else{
		if( type == 'fixedAttr' ){
			var element = jQuery('#fixedAttrSelector option:selected');
			if( element.length == 0 ) return;		
			id = 'fixedattributeid="' + element.attr('value') + '"';
			value = element.text();
		}
		else if( type == 'attr' ){
			var element = jQuery('#attributesSelector option:selected');
			if( element.length == 0 ) return;
			
			id = 'attributeid="' + element.attr('value') + '"';
			value = element.text();
		}
		else if( type == 'prg' ){
			var element = jQuery('#programAttrSelector option:selected');
			if( element.length == 0 ) return;
			
			id = 'programid="' + element.attr('value') + '"';
			value = element.text();
		}		
		var htmlCode = "<input " + id + " value=\"[" + value + "]\" title=\"" + value + "\" ";
		
		suggestedValue = getFieldValue('suggestedField');
		if( jQuery('#suggestedField').is(":visible") )
		{
			htmlCode += " suggested='" + suggestedValue + "' ";
		}
		
		var isHidden = jQuery('#hiddenField').attr('checked');
		if(isHidden)
		{
			htmlCode += " class='hidden' ";
		}
		htmlCode += " >";
		
		if( checkExisted( id ) ){		
			// setMessage( "<span class='bold'>" + i18n_property_is_inserted + "</span>" );
			setHeaderDelayMessage(i18n_property_is_inserted);
			return;
		}else{
			var oEditor = jQuery("#designTextarea").ckeditor().editor;
			oEditor.insertHtml( htmlCode );
			setMessage("");
		}
	}
}

function deleteProgramEntryForm( id, name )
{
	var result = window.confirm( i18n_confirm_delete + '\n\n' + name );
	if ( result )
	{
		window.location.href = 'removeProgramEntryForm.action?programId=' + id;
	}
}

function insertImage() {
	var image = $("#imageDialog :selected").val();
	var html = "<img src=\"" + image + "\" title=\"" + $("#imageDialog :selected").text() + "\">";
	var oEditor = $("#designTextarea").ckeditor().editor;
	oEditor.insertHtml( html );
}

// -------------------------------------------------------
// Auto-save data entry form
// -------------------------------------------------------

function setAutoSaveRegistrationSetting(_autoSave)
{
	jQuery.postJSON("setAutoSaveProgramEntryFormSetting.action", {autoSave:_autoSave}, function(json) {
		autoSave = _autoSave;
		if (_autoSave) {
			window.setTimeout( "validateProgramEntryFormTimeout( false );", 6000 );
		}
		else{
			window.clearTimeout(timeOut);
		}
	});
}

function validateRegistrationFormTimeout()
{
	validateDataEntryForm();
	timeOut = window.setTimeout( "validateRegistrationFormTimeout();", interval );
}

function validateDataEntryForm(form)
{
	var name = getFieldValue('name');
	if( name =='' || name.length<4 || name.length > 150 )
	{
		setHeaderDelayMessage( i18n_enter_a_name );
		return false;
	}
	else if(validateProgramFields())
	{
		$.postUTF8( 'validateDataEntryForm.action',
		{
			name: getFieldValue('name'),
			dataEntryFormId: getFieldValue('dataEntryFormId')
		}, 
		function( json )
		{
			if ( json.response == 'success' )
			{
				if( form != undefined)
				{
					form.submit();
				}
				else
				{
					autoSaveProgramEntryForm();
				}
			}
			else if ( json.response = 'error' )
			{
				setHeaderDelayMessage( json.message );
			}
		} );
	}
}

function autoSaveProgramEntryForm()
{
	$.postUTF8( 'autoSaveProgramEntryForm.action',
	{
		name: getFieldValue('name'),
		designTextarea: jQuery("#designTextarea").ckeditor().editor.getData(),
		programId: getFieldValue('programId'),
		id: getFieldValue('id')
	},
	function( json ) 
	{
		setFieldValue('dataEntryFormId', json.message);
		showById('deleteButton');
		setHeaderDelayMessage( i18n_save_success ); 
	} );
}

function deleteRegistrationFormFromView()
{
	var result = window.confirm( i18n_confirm_delete + '\n\n' + name );
	if ( result )
	{
		window.location.href = 'delRegistrationEntryFormAction.action?id=' + getFieldValue('id');
	}
}
