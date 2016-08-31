$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

function showUpdateValidationCriteriaForm( context ) {
  location.href = 'showUpdateValidationCriteriaForm.action?id=' + context.id + '&programId=' + getFieldValue('programId');
}

// -----------------------------------------------------------------------------
// Remove Criteria
// -----------------------------------------------------------------------------

function removeCriteria( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeValidationCriteria.action');
}

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showValidationCriteriaDetails( context ) {
  jQuery.getJSON('getValidationCriteria.action', { id: context.id, programId: getFieldValue('programId')  }, function( json ) {
    setInnerHTML('nameField', json.validationCriteria.name);
    setInnerHTML('descriptionField', json.validationCriteria.description);
	setInnerHTML('idField', json.validationCriteria.uid);

    var property = json.validationCriteria.property;
    var operator = json.validationCriteria.operator;
    var value = json.validationCriteria.value;

    // get operator
    if( operator == 0 ) {
      operator = '=';
    } else if( operator == -1 ) {
      operator = '<';
    } else {
      operator = '>';
    }

    setInnerHTML('criteriaField', property + " " + operator + " " + value);
    showDetails();
  });
}

// ----------------------------------------------------------------------------------------
// Show div to Add or Update Validation-Criteria
// ----------------------------------------------------------------------------------------

function showDivValue() {
	var value = getFieldValue('value');
	var property = jQuery('#property option:selected');
	var type = property.attr('valueType');
	var propertyName = property.val();	
	enable('operator');
	
	var valueField = "";
	 if(type=='bool') {
		valueField = "<select id='value' name='value' class=\"{validate:{required:true}}\" style=\"width:140px;\">";
		valueField += "<option value='true' >" + i18n_yes + "</option>";
		valueField += "<option value='false' >" + i18n_no + "</option>";
		valueField += "</select>";
	}
	else if(type == "trueOnly" ){
		valueField = "<select id='value' name='value' class=\"{validate:{required:true}}\" style=\"width:140px;\">";
		valueField += "<option value='true' >" + i18n_yes + "</option>";
		valueField += "</select>";
	}
	else if(type=='date') {
		valueField = "<input id='value' name='value' class=\"{validate:{required:true}}\" style=\"width:140px;\"/>";
	}
	else if(type=='optionSet') {
		var opts = property.attr('opt').split(";");
		valueField = "<select id='value' name='value' class=\"{validate:{required:true}}\" style=\"width:140px;\">";
		for(var i=1;i<opts.length;i++){
			var opt = opts[i].split(":");
			valueField += "<option value=\"" + opt[0] + "\" >" + opt[1] + "</option>";
		}
		valueField += "</select>";
		
		setFieldValue('operator','0');
		disable('operator');
	}
	else if( type == "phoneNumber" ){
		valueField = "<input id='value' name='value' class=\"{validate:{phone:true,required:true}}\" style=\"width:140px;\" />";
	}
	else if( type == "age" || type == "number" ){
		valueField = "<input id='value' name='value' class=\"{validate:{number:true,required:true}}\" style=\"width:140px;\" />";
	}
	else if( type == "letter" ){
		valueField = "<input id='value' name='value' class=\"{validate:{letterswithbasicpunc:true,required:true}}\" style=\"width:140px;\" />";
	}
	else{
		valueField = "<input id='value' name='value' class=\"{validate:{required:true}}\" style=\"width:140px;\"/>";
	}
	
	setInnerHTML('valueTD', valueField);
	setFieldValue('value', value);
	if(type=='date') {
		datePickerValid( 'value', false, false );
	}
}

function fillValue( value ) {
  byId('value').value = value;
}
