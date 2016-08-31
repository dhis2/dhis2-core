var COLOR_GREEN = '#b9ffb9';
var COLOR_WHITE = '#ffffff'

jQuery(document).ready(function() {
 
    validation2( 'programValidationForm', function( form )
	{
		form.submit();
	},{
		'rules' : getValidationRules( "programValidation" )
	});

	dhis2.contextmenu.makeContextMenu({
		menuId: 'contextMenu',
		menuItemActiveClass: 'contextMenuItemActive'
    });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateSingleProgramValidationForm( context ) {
  location.href = 'showUpdateSingleProgramValidationForm.action?validationId=' + context.id;
}

function showProgramValidationDetails( context ) {
  jQuery.getJSON('getProgramValidation.action', { validationId: context.id }, function( json ) {
    setInnerHTML('descriptionField', json.validation.description);
    setInnerHTML('idField', json.validation.uid);

    var operator = json.validation.operator;
    setInnerHTML('operatorField', i18nalizeOperator(operator));

    setInnerHTML('leftSideDescriptionField', json.validation.leftSideDescription);
    setInnerHTML('leftSideExpressionField', json.validation.leftSideExpression);
    setInnerHTML('rightSideDescriptionField', json.validation.rightSideDescription);
    setInnerHTML('rightSideExpressionField', json.validation.rightSideExpression);

    showDetails();
  });
}

function i18nalizeOperator( operator ) {
  if( operator == "equal_to" ) {
    return i18n_equal_to;
  }
  else if( operator == "not_equal_to" ) {
    return i18n_not_equal_to;
  }
  else if( operator == "greater_than" ) {
    return i18n_greater_than;
  }
  else if( operator == "greater_than_or_equal_to" ) {
    return i18n_greater_than_or_equal_to;
  }
  else if( operator == "less_than" ) {
    return i18n_less_than;
  }
  else if( operator == "less_than_or_equal_to" ) {
    return i18n_less_than_or_equal_to;
  }

  return null;
}

// -----------------------------------------------------------------------------
// Remove ProgramValidation
// -----------------------------------------------------------------------------

function removeProgramValidation( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeProgramValidation.action');
}

//------------------------------------------------------------------------------
// Load data-elements of each program-stage
//------------------------------------------------------------------------------

function getLeftPrgramStageDataElements() {
  clearListById('dataElementId');

  var programStage = document.getElementById('leftStage');
  var programStageId = programStage.options[ programStage.selectedIndex ].id;
  var programStageUid = programStage.options[ programStage.selectedIndex ].value;
  if( programStageId == '' ) return;

  jQuery.getJSON("getTrackedEntityDataElements.action", {
    programStageId: programStageId
  }, function( json ) {
    jQuery('#dataElementId').append('<option value="[PS:' + programStageUid + '.DUE_DATE]">' + i18n_due_date + '</option>');
    jQuery('#dataElementId').append('<option value="[PS:' + programStageUid + '.REPORT_DATE]">' + i18n_report_date + '</option>');
    for( i in json.dataElements ) {
      var id = '[DE:' + programStageUid + '.' + json.dataElements[i].id + ']';
      jQuery('#dataElementId').append('<option value="' + id + '">' + json.dataElements[i].name + '</option>');
    }
  });
}

//------------------------------------------------------------------------------
// Show Left side form for designing
//------------------------------------------------------------------------------

function editLeftExpression() {
  left = true;

  $('#expression').val($('#leftSideExpression').val());
  $('#expression-container [id=description]').val($('#leftSideDescription').val());
  $('#formulaText').text($('#leftSideTextualExpression').val());
  $('#nullIfBlank').attr('checked', ( $('#leftSideNullIfBlank').val() == 'true' || $('#leftSideNullIfBlank').val() == '' ));
  setInnerHTML("exp-descriptionInfo", "");
  setInnerHTML("exp-expressionInfo", "");
  $("#expression-container [id=description]").css("background-color", "#ffffff");
  $("#expression-container [id=expression]").css("background-color", "#ffffff");

  dialog.dialog("open");
}

function editRightExpression() {
  left = false;

  $('#expression').val($('#rightSideExpression').val());
  $('#expression-container [id=description]').val($('#rightSideDescription').val());
  $('#formulaText').text($('#rightSideTextualExpression').val());
  $('#nullIfBlank').attr('checked', ( $('#rightSideNullIfBlank').val() == 'true' || $('#rightSideNullIfBlank').val() == '' ));

  dialog.dialog("open");
}

//------------------------------------------------------------------------------
// Insert formulas
//------------------------------------------------------------------------------

function insertText( inputAreaName, inputText ) {
  insertTextCommon(inputAreaName, inputText);
  getExpressionText();
}


function getExpressionText() {
	$.ajax({
		url: "getProgramExpressionDescription.action",
		type: "POST",
		data:{ programExpression: $('#expression').val() },
		dataType: "json",
		success: function( json ){
			setInnerHTML("formulaText", json.message);
			  if( json.response == "error" ){
				$("#formulaText").css("color","red");
				$("#formulaText").addClass("validateError");
			  }
			  else{
				$("#formulaText").css("color","black");
				$("#formulaText").removeClass("error");
			  }
		}
	});


}

var left = true;
function insertExpression() {
  var expression = $('#expression').val();
  var description = $('#expression-container [id=description]').val();

  if( left ) {
    $('#leftSideExpression').val(expression);
    $('#leftSideDescription').val(description);
    $('#leftSideTextualExpression').val($('#formulaText').text());
    $('#leftSideNullIfBlank').val($('#nullIfBlank').is(':checked'));
  }
  else {
    $('#rightSideExpression').val(expression);
    $('#rightSideDescription').val(description);
    $('#rightSideTextualExpression').val($('#formulaText').text());
    $('#rightSideNullIfBlank').val($('#nullIfBlank').is(':checked'));
  }

  dialog.dialog("close");
}

function validateExpression() {	
  getExpressionText();
  if( checkValidationRule(jQuery("#expression-container [id=description]"), i18n_description_not_null) == false )
    return;
  if( checkValidationRule(jQuery("#expression-container [id=expression]"), i18n_expression_not_null) == false )
    return;
  insertExpression();
}

function checkValidationRule( field, message ) {
  if( field.val().length == 0 ) {
    setInnerHTML("exp-" + field.attr("name") + "Info", message);
    $('#expression-container [id=' + field.attr("name") + "]").css("background-color", "#ffc5c5");
    return false;
  } 
  else if( $("#formulaText").attr("class") == "validateError" ){
	return false;
  }
  else {
    setInnerHTML("exp-" + field.attr("name") + "Info", '');
    $('#expression-container [id=' + field.attr("name") + "]").css("background-color", "#ffffff");
  }

  return true;
}

function clearSearchText() {
  setFieldValue('filter', '');
  filterList('', 'dataElementId')
}