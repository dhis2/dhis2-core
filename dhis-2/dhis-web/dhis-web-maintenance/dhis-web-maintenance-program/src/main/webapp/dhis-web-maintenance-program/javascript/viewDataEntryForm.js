
var dataElementSelector;
var existedDataEntry;
var timeout;

jQuery(function(){
	dataElementSelector = jQuery("#dataElementDiv").dialog({
		title: i18n_dataelement,
		minWidth: 595,
		minHeight: 263,
		position: [($("body").width() - 555) - 50, 50],
		autoOpen: false,
		zIndex:99999
	});
	
	existedDataEntry = jQuery("#existedDataEntry").dialog({
		title: i18n_choose_existing_dataentry,
		height: 350,
		width:350,
		autoOpen: false,
		zIndex:99999
	});	
	
	$(":button").button();
	$(":submit").button();
	$("#saveButton").button("option", "icons", { primary: "ui-icon-disk" });
	$("#saveAndCloseButton").button("option", "icons", { primary: "ui-icon-disk" });
	$("#cancelButton").button("option", "icons", { primary: "ui-icon-cancel" });
	$("#deleteButton").button("option", "icons", { primary: "ui-icon-trash" });
	$("#insertButton").button("option", "icons", { primary: "ui-icon-plusthick" });
	$("#insertImagesButton").button("option", "icons", { primary: "ui-icon-newwin" });
	$("#insertImageButton").button("option", "icons", { primary: "ui-icon-plusthick" });
	$("#loadExistForms").button("option", "icons", { primary: "ui-icon-newwin" });
	$("#insertDataElements").button("option", "icons", { primary: "ui-icon-newwin" });
	$("#insertOtherDataElements").button("option", "icons", { primary: "ui-icon-newwin" });
	
	$("#imageDialog").bind("dialogopen", function(event, ui) {
		$("#insertImagesButton").button("disable");
	})
	$("#imageDialog").bind("dialogclose", function(event, ui) {
		$("#insertImagesButton").button("enable");
	})
	
	$("#insertImagesButton").click(function() {
		$("#imageDialog").dialog({
			minWidth: 350,
			minheight: 263,
			position: [$("body").width()- 50, 0],
			zIndex: 10000,
			resizable: false
		});
	});
	
	if( autoSave )
	{
		timeOut = window.setTimeout( "validateDataEntryFormTimeout( false );", 60000 );
	}
});
	
function openDataElementSelector()
{
	dataElementSelector.dialog("open");
}	

function openloadExistedForm()
{
	existedDataEntry.dialog("open");
}

function loadExistedForm()
{
	jQuery.get("showDataEntryForm.action",{
		dataEntryFormId: getFieldValue( 'existedDataEntryId' )
	}, function( html ){
		jQuery("#designTextarea").ckeditor().editor.setData( html );

		var dataEntryFormField = byId('existedDataEntryId');
		var optionField = dataEntryFormField.options[dataEntryFormField.selectedIndex];

		setFieldValue('dataEntryFormId', optionField.value );
		setFieldValue('name', optionField.text );
		
		checkValueIsExist('name', 'validateDataEntryForm.action', {dataEntryFormId:getFieldValue('dataEntryFormId')});
	});
}

function deleteDataEntryForm( dataEntryFormId, programStageId )
{
	if( window.confirm( i18n_delete_program_data_entry_confirm ) )
	{
		window.location.href = 'delDataEntryForm.action?id=' + dataEntryFormId + "&programStageId=" + programStageId;
	}
}

function getProgramStageDataElements( id )
{
	var dataElements = jQuery( "#otherProgramStageDataElements #dataElementIds" );
	dataElements.empty();
	var dataElementIdsStore = jQuery( "#otherProgramStageDataElements #dataElementIdsStore" );
	dataElementIdsStore.empty();
	
	if( id != '' ){
		jQuery.post("getSelectedDataElements.action",{
			associationId: id
		}, function( xml ){			
			jQuery( xml ).find( 'dataElement' ).each( function(i, item ){
				dataElements.append("<option value='" + jQuery( item ).find( "json" ).text() + "' dename='" + jQuery( item ).find( "name" ).text() + "' decode='" + jQuery( item ).find( "code" ).text() + "' >" + jQuery( item ).find( "name" ).text() + "</option>");
				dataElementIdsStore.append("<option value='" + jQuery( item ).find( "json" ).text() + "' dename='" + jQuery( item ).find( "name" ).text() + "' decode='" + jQuery( item ).find( "code" ).text() + "' >" + jQuery( item ).find( "name" ).text() + "</option>");
			});
		});
	}
}

function getSelectedValues( jQueryString )
{
	var result = new Array();
	jQuery.each( jQuery( jQueryString ).children(), function(i, item ){
		if( item.selected==true){
			result.push( JSON.parse( item.value ) );
		}
	});
	
	return result;
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
	var input = jQuery( html ).find("select, :text");

	input.each( function(i, item){		
		if( id == item.id ) result = true;		
	});

	return result;
}

function filterDataElements( filter, container, list )
{
	var filterLower = filter.toString().toLowerCase();
	
	var dataElementList = jQuery( container + " " + list );
	dataElementList.empty();
	
	jQuery( container + " " + list + "Store" ).children().each( function(i, item){
		item = jQuery( item );		
		var toMatch = item.text().toString().toLowerCase();		
        if( toMatch.indexOf(filterLower) != -1 ){
			dataElementList.append( "<option value='" + item.attr('value') + "' dename='"+item.attr('dename')+"' decode='"+item.attr('decode')+"'>" + item.text() + "</option>" );
		};
	});	
}

function insertDataElement( source, programStageUid, isLabel )
{	
	var dataElement = JSON.parse( jQuery( source + ' #dataElementIds').val() );

	if( dataElement == null )
	{
		jQuery( source + " #message_").html( "<span class='bold'>" + i18n_specify_dataelememt + "</span>" );
		return;
	} else {
		jQuery( source + " #message_").html( "" );
	}

	var dataElementUid = dataElement.uid;	
	var dataElementName = dataElement.name;	
	var dataElementValueType = dataElement.valueType;
	
	var htmlCode = "";
	var id = programStageUid + "-" + dataElementUid + "-val" ;
	
	if( isLabel )
	{
		var selectedText = jQuery("#designTextarea").ckeditor().editor.getSelection().getSelectedText();
		var element = jQuery('#dataElementSelection option:selected');
		
		if( element.length == 0 ) return;
		
		var assigned = checkLabelAssigned( id );
		if( assigned && assigned.assigned ){
			jQuery( " #message_").html( "<span class='bold'>" + i18n_label_is_assigned_to + ': ' + assigned.assignedTo + "</span>" );
			return;
		}
		else{
			var label = '<span title="' + selectedText + '" d2-input-label="' + id + '">' + selectedText + '</span>';		
			var lEditor = jQuery("#designTextarea").ckeditor().editor;
			lEditor.insertHtml( label );
		}
	}
	else
	{
		if( dataElementUid == "executionDate" )
		{
			id = dataElementUid;
		}	
		var titleValue = dataElementUid + " - " + dataElementName + " - " + dataElementValueType;
		
		if ( dataElementValueType == "BOOLEAN" )
		{
			var displayName = dataElementName;
			htmlCode = "<input title=\"" + titleValue + "\" name=\"entryselect\" id=\"" + id + "\" value=\"[" + displayName + "]\" title=\"" + displayName + "\">";
		} 
		else if ( dataElementValueType == "TRUE_ONLY" )
		{
			var displayName = dataElementName;
			htmlCode = "<input type=\"checkbox\" title=\"" + titleValue + "\" name=\"entryselect\" id=\"" + id + "\" title=\"" + displayName + "\">";
		} 
		else if ( dataElementValueType == "USERNAME" )
		{
			var displayName = dataElementName;
			htmlCode = "<input title=\"" + titleValue + "\" value=\"[" + displayName + "]\" name=\"entryfield\" id=\"" + id + "\" username=\"true\" />";
		} 
		else
		{
			var displayName = dataElementName;
			htmlCode = "<input title=\"" + titleValue + "\" value=\"[" + displayName + "]\" name=\"entryfield\" id=\"" + id + "\" />";
		}
		
		if( checkExisted( id ) )
		{		
			jQuery( " #message_").html( "<span class='bold'>" + i18n_dataelement_is_inserted + "</span>" );
			return;
		}else{
			var oEditor = jQuery("#designTextarea").ckeditor().editor;
			oEditor.insertHtml( htmlCode );
			jQuery(" #message_").html("");
		}
	}
}

function displayNameOnChange( div, displayName )
{
	// display - name
	if(displayName=='1'){
		jQuery('#' + div + ' [id=dataElementIds] option').each(function(){
			var item = jQuery(this);
			item[0].text = item.attr('dename');
			item[0].title = item[0].text;
		});
		jQuery('#' + div + ' [id=dataElementIdsStore] option').each(function(){
			var item = jQuery(this);
			item[0].text = item.attr('dename');
		});
	}
	// display - code
	else if(displayName=='2'){
		jQuery('#' + div + ' [id=dataElementIds] option').each(function(){
			var item = jQuery(this);
			item[0].text = item.attr('decode');
			item[0].title = item[0].text;
		});
		jQuery('#' + div + ' [id=dataElementIdsStore] option').each(function(){
			var item = jQuery(this);
			item[0].text = item.attr('decode');
		});
	}
	// display - code and name
	else{
		jQuery('#' + div + ' [id=dataElementIds] option').each(function(){
			var item = jQuery(this);
			item[0].text = "(" + item.attr('decode') + ") " + item.attr('dename');
			item[0].title = item[0].text;
		});
		jQuery('#' + div + ' [id=dataElementIdsStore] option').each(function(){
			var item = jQuery(this);
			item[0].text = "(" + item.attr('decode') + ") " + item.attr('dename');
		});
	}
}

function sortByOnChange( div, sortBy)
{
	if( sortBy == 1)
	{
		jQuery('#' + div + ' [id=dataElementIds]').each(function() {

			// Keep track of the selected option.
			var selectedValue = $(this).val();

			// sort it out
			$(this).html($("option", $(this)).sort(function(a, b) { 
				return $(a).attr('dename') == $(b).attr('dename') ? 0 : $(a).attr('dename') < $(b).attr('dename') ? -1 : 1 
			}));

			// Select one option.
			$(this).val(selectedValue);

		});
	}
	else
	{
		jQuery('#' + div + ' [id=dataElementIds]').each(function() {

			// Keep track of the selected option.
			var selectedValue = $(this).val();

			// sort it out
			$(this).html($("option", $(this)).sort(function(a, b) { 
				return $(a).attr('decode') == $(b).attr('decode') ? 0 : $(a).attr('decode') < $(b).attr('decode') ? -1 : 1 
			}));

			// Select one option.
			$(this).val(selectedValue);

		});
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

function setAutoSaveDataEntrySetting(_autoSave)
{
	jQuery.postJSON("setAutoSaveDataEntrySetting.action", {autoSave:_autoSave}, function(json) {
		autoSave = _autoSave;
		if (_autoSave) {
			window.setTimeout( "validateDataEntryFormTimeout( false );", 60000 );
		}
		else{
			window.clearTimeout(timeOut);
		}
	});
}

function validateDataEntryFormTimeout()
{
	validateDataEntryForm();
	timeOut = window.setTimeout( "validateDataEntryFormTimeout();", 60000 );
}

function validateDataEntryForm()
{
	var name = getFieldValue('name');
	if( name.length==0 || name.length < 4 || name.length > 150 )
	{
		setHeaderDelayMessage( i18n_enter_a_name );
		return;
	}
	
	$.post( 'validateDataEntryForm.action',
	{
		name: getFieldValue('name'),
		dataEntryFormId: getFieldValue('dataEntryFormId')
	}, 
	function( json )
	{
		if ( json.response == 'success' )
		{
			autoSaveDataEntryForm();
		}
		else if ( json.response = 'error' )
		{
			setHeaderDelayMessage( json.message );
		}
	} );
}

function autoSaveDataEntryForm()
{
	$.postUTF8( 'autoSaveDataEntryForm.action',
	{
		name: getFieldValue('name'),
		designTextarea: jQuery("#designTextarea").ckeditor().editor.getData(),
		programId: getFieldValue('programId'),
		programStageId: getFieldValue('programStageId'),
		dataEntryFormId: getFieldValue('dataEntryFormId')
	},
	function( json ) 
	{
		setHeaderDelayMessage( i18n_save_success ); 
	} );
}

