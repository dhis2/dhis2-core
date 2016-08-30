currentType = '';
function changeParserType( value )
{
	hideAll();
    if ( value == 'KEY_VALUE_PARSER' || value == 'J2ME_PARSER') {
        showById( "dataSetParser" );
		enable("selectedDataSetID");
    } else if ( value == 'ALERT_PARSER' || value == 'UNREGISTERED_PARSER' ) {
    	showById( "alertParser" );
		enable("userGroupID");
    } 
   else if (value == 'EVENT_REGISTRATION_PARSER') {
    	showById( "eventRegistrationParser" );
		enable("selectedProgramIdWithoutRegistration");
    }
    else if (value == 'TRACKED_ENTITY_REGISTRATION_PARSER') {
    	showById( "registrationParser" );
		enable("selectedProgramId");
    }
	currentType = value;
}

function hideAll() 
{
	hideById( "dataSetParser" ); 
	disable( "selectedDataSetID" ); 
	
	hideById( "eventRegistrationParser" ); 
	disable( "selectedProgramIdWithoutRegistration" ); 

	hideById( "alertParser" );
	disable( "userGroupID" );

	hideById( "registrationParser" );
	disable( "selectedProgramId" );
}

function generateSpecialCharactersForm()
{
	var rowId = jQuery('.trSpecialCharacter').length + 1;

	var contend = '<tr id="trSpecialCharacter' + rowId + '" name="trSpecialCharacter' + rowId + '" class="trSpecialCharacter">'
				+	'<td><input id="name' + rowId + '" name="name' + rowId + '" type="text" class="name {validate:{required:true}}" onblur="checkDuplicatedSpeCharName(this.value,' + rowId + ')"  placeholder="' + i18_special_characters + '" )/></td>'
				+	'<td><input id="value' + rowId + '" name="value' + rowId + '" type="text" class="value {validate:{required:true}}" onblur="checkDuplicatedSpeCharValue(this.value, ' + rowId + ')" placeholder="' + i18_value + '"/>'
				+   	'<input type="button" value="remove" onclick="removeSpecialCharactersForm(' + rowId + ')"/></td>'
				+ '</tr>';
	jQuery('#specialCharacters').append( contend );

}

function removeSpecialCharactersForm( rowId )
{
	jQuery("[name=trSpecialCharacter" + rowId + "]").remove();
}

function openFormulaForm(displayName, index)
{
	$("#displayName").html(displayName);
	$("#index").val(index);
	
	$("#removeButton").hide();
	$('#targetDataElement option').prop('selected', false);
	$('#operator option').prop('selected', false);
	$("#selectedTargetDataElement").html($("#targetDataElement option:selected").text());
	
	var formulaText = $("#" + "formula" + index).val();

	if (formulaText != "") {
		var operator = formulaText.substring(0,1);
		var dataElementId = formulaText.substring(1,formulaText.length);
		
		$("#removeButton").show();
		$('#targetDataElement  option[value="' + dataElementId + '"]').prop("selected", true);
		$('#operator  option[value="' + operator + '"]').prop("selected", true);
		$("#selectedTargetDataElement").html($("#targetDataElement option:selected").text());
	}
	
	dialog.dialog("option", "title", "Formula Form");
	dialog.dialog("open");
}
	
function collectFormula() {
	var operator = $("#operator option:selected").text();
	var deId = $("#targetDataElement option:selected").val();
	var index = $("#index").val();
	$("#addFormula" + index).val("Edit Formula");
	$("#" + "formula" + index).val(operator + "" + deId);
	dialog.dialog( "close" );
}

function closeFormulaForm() {
	dialog.dialog( "close" );
}

function closeAndFormulaForm() {
	var index = $("#index").val();
	$("#" + "formula" + index).val("");
	$("#addFormula" + index).val("Add Formula");
	dialog.dialog( "close" );
}
