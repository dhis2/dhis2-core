isAjax = true;

function organisationUnitSelected( orgUnits, orgUnitNames )
{	
	showById('selectDiv');
	hideById('listPatientDiv');

	enable('advancedSearchBtn');
	
	setFieldValue( "orgunitName", orgUnitNames[0] );
	setFieldValue( "orgunitId", orgUnits[0] );
}

selection.setListenerFunction( organisationUnitSelected );

function enableBtn()
{
	if(registration==undefined || !registration)
	{
		var programIdAddPatient = getFieldValue('programIdAddPatient');
		if( programIdAddPatient!='' ){
			enable('listPatientBtn');
			enable('advancedSearchBtn');
			jQuery('#advanced-search :input').each( function( idx, item ){
				enable(this.id);
			});
		}
		else
		{
			disable('listPatientBtn');
			disable('advancedSearchBtn');
			jQuery('#advanced-search :input').each( function( idx, item ){
				disable(this.id);
			});
		}
	}
}

// ----------------------------------------------------------------------------
// Search patients by name
// ----------------------------------------------------------------------------

function getPatientsByName( divname )
{	
	var fullName = jQuery('#' + divname + ' [id=fullName]').val().replace(/^\s+|\s+$/g,"");
	if( fullName.length > 0) 
	{
		contentDiv = 'resultSearchDiv';
		$('#resultSearchDiv' ).load("getPatientsByName.action",
			{
				fullName: fullName
			}).dialog({
				title: i18n_search_result,
				maximize: true, 
				closable: true,
				modal:true,
				overlay:{ background:'#000000', opacity: 0.8},
				width: 800,
				height: 400
		});
	}
	else
	{
		alert( i18n_no_patients_found );
	}
}

// -----------------------------------------------------------------------------
// Advanced search
// -----------------------------------------------------------------------------

function addAttributeOption()
{
	jQuery('#advancedSearchTB [name=clearSearchBtn]').attr('disabled', false);
	var rowId = 'advSearchBox' + jQuery('#advancedSearchTB select[name=searchObjectId]').length + 1;
	var contend  = '<td>' + getInnerHTML('searchingAttributeIdTD') + '</td>';
		contend += '<td>' + searchTextBox ;
		contend += '&nbsp;<input type="button" name="clearSearchBtn" class="normal-button" value="' + i18n_clear + '" onclick="removeAttributeOption(' + "'" + rowId + "'" + ');"></td>';
		contend = '<tr id="' + rowId + '">' + contend + '</tr>';

	jQuery('#advancedSearchTB').append( contend );
	var rowspan = eval( jQuery('[name=addAndSearchBtn]').attr('rowspan') );
	jQuery('[name=addAndSearchBtn]').attr('rowspan', rowspan + 1);
}	

function removeAttributeOption( rowId )
{
	jQuery( '#' + rowId ).remove();
	if( jQuery( '#advancedSearchTB tr' ).length == 2 ){
		jQuery('#advancedSearchTB [name=clearSearchBtn]').attr('disabled', true);
	}
	var rowspan = eval( jQuery('[name=addAndSearchBtn]').attr('rowspan') );
	jQuery('[name=addAndSearchBtn]').attr('rowspan', rowspan - 1);
}	

//------------------------------------------------------------------------------
// Search patients by selected attribute
//------------------------------------------------------------------------------

function searchObjectOnChange( this_ )
{	
	var container = jQuery(this_).parent().parent().attr('id');
	var attributeId = jQuery('#' + container + ' [id=searchObjectId]').val(); 
	var element = jQuery('#' + container + ' [name=searchText]');
	var valueType = jQuery('#' + container+ ' [id=searchObjectId] option:selected').attr('valueType');
	
	jQuery('#searchText_' + container).removeAttr('readonly', false);
	jQuery('#searchText_' + container).val("");
	if( attributeId == 'fixedAttr_birthDate' )
	{
		element.replaceWith( getDateField( container ) );
		datePickerValid( 'searchText_' + container );
		return;
	}
	
	$( '#searchText_' + container ).datepicker("destroy");
	$('#' + container + ' [id=dateOperator]').replaceWith("");
	if( attributeId == 'prg' )
	{
		element.replaceWith( programComboBox );
	}
	else if ( attributeId=='fixedAttr_gender' )
	{
		element.replaceWith( getGenderSelector() );
	}
	else if ( attributeId=='fixedAttr_age' )
	{
		element.replaceWith( getAgeTextBox() );
	}
	else if ( valueType=='YES/NO' )
	{
		element.replaceWith( getTrueFalseBox() );
	}
	else
	{
		element.replaceWith( searchTextBox );
	}
}

function getTrueFalseBox()
{
	var trueFalseBox  = '<select id="searchText" name="searchText">';
	trueFalseBox += '<option value="true">' + i18n_yes + '</option>';
	trueFalseBox += '<option value="false">' + i18n_no + '</option>';
	trueFalseBox += '</select>';
	return trueFalseBox;
}
	
function getGenderSelector()
{
	var genderSelector = '<select id="searchText" name="searchText" style="width:200px;">';
		genderSelector += '<option value="M">' + i18n_male + '</option>';
		genderSelector += '<option value="F">' + i18n_female + '</option>';
		genderSelector += '<option value="T">' + i18n_transgender + '</option>';
		genderSelector += '</select>';
	return genderSelector;
}

function getAgeTextBox( container )
{
	var ageField = '<select id="dateOperator" name="dateOperator" style="width:40px"><option value=">"> > </option><option value=">="> >= </option><option value="="> = </option><option value="<"> < </option><option value="<="> <= </option></select>';
	ageField += '<input type="text" id="searchText_' + container + '" name="searchText" style="width:160px;">';
	return ageField;
}

function getDateField( container )
{
	var dateField = '<select id="dateOperator_' + container + '" name="dateOperator" style="width:40px"><option value=">"> > </option><option value=">="> >= </option><option value="="> = </option><option value="<"> < </option><option value="<="> <= </option></select>';
	dateField += '<input type="text" id="searchText_' + container + '" name="searchText" style="width:160px;" onkeyup="searchPatientsOnKeyUp( event );">';
	return dateField;
}

//-----------------------------------------------------------------------------
// Search Patient
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// Search Patient
//-----------------------------------------------------------------------------

function searchPatientsOnKeyUp( event )
{
	var key = getKeyCode( event );
	
	if ( key==13 )// Enter
	{
		validateAdvancedSearch();
	}
}

function getKeyCode(e)
{
	 if (window.event)
		return window.event.keyCode;
	 return (e)? e.which : null;
}

function validateAdvancedSearch()
{
	hideById( 'listPatientDiv' );
	var flag = true;
	var dateOperator = '';

	if (jQuery( '#advancedSearchTB tr' ).length > 1 ){
		jQuery("#advancedSearchTB :input").each( function( i, item )
		{
			var elementName = $(this).attr('name');
			if( elementName=='searchText' && jQuery( item ).val() == '')
			{
				setHeaderDelayMessage( i18n_specify_search_criteria );
				flag = false;
			}
		});
	}
		
	if(flag){
		contentDiv = 'listPatientDiv';
		jQuery( "#loaderDiv" ).show();
		advancedSearch( getSearchParams() );
	}
}

function getSearchParams()
{
	var params = "";
	var programIds = "";
	
	var flag = false;
	jQuery( '#advancedSearchTB tr' ).each( function( i, row ){
		var dateOperator = "";
		var p = "";
		jQuery( this ).find(':input').each( function( idx, item ){
			if(item.type!="button"){
				if( idx == 0){
					p = "&searchTexts=" + item.value;
					if(item.value=='prg'){
						programIds += '&programIds=';
						flag = true;
					}
				}
				else if( item.name == 'dateOperator'){
					dateOperator = item.value;
				}
				else if( item.name == 'searchText'){
					if( item.value!='')
					{
						p += "_";
						if ( dateOperator.length >0 ) {
							p += dateOperator + "'" +  item.value.toLowerCase() + "'";
						}
						else{
							p += htmlEncode( item.value.toLowerCase().replace(/^\s*/, "").replace(/\s*$/, "") );
						}
						
						if( flag ){
							programIds += item.value;
							flag = false;
						}
					}
					else {
						p = "";
					}
				}
			}
		});
		
		params += p;
	});
		
	var searchInAllFacility = byId('searchInAllFacility').checked;		
	params += '&searchBySelectedOrgunit=' + !searchInAllFacility;
	params += programIds;
	
	return params;
}

function advancedSearch( params )
{
	$.ajax({
		url: 'searchRegistrationPatient.action',
		type:"POST",
		data: params,
		success: function( html ){
				setTableStyles();
				statusSearching = 1;
				setInnerHTML( 'listPatientDiv', html );
				showById('listPatientDiv');
				setFieldValue('listAll',false);
				jQuery( "#loaderDiv" ).hide();
			}
		});
}

// ----------------------------------------------------------------
// Get Params form Div
// ----------------------------------------------------------------

function getParamsForDiv( patientDiv)
{
	var params = '';
	var dateOperator = '';
	jQuery("#" + patientDiv + " :input").each(function()
		{
			var elementId = $(this).attr('id');
			
			if( $(this).attr('type') == 'checkbox' )
			{
				var checked = jQuery(this).attr('checked') ? true : false;
				params += elementId + "=" + checked + "&";
			}
			else if( elementId =='dateOperator' )
			{
				dateOperator = jQuery(this).val();
			}
			else if( $(this).attr('type') != 'button' )
			{
				var value = "";
				if( jQuery(this).val()!= null && jQuery(this).val() != '' )
				{
					value = htmlEncode(jQuery(this).val());
				}
				if( dateOperator != '' )
				{
					value = dateOperator + "'" + value + "'";
					dateOperator = "";
				}
				params += elementId + "="+ value + "&";
			}
		});
		
	return params;
}

// -----------------------------------------------------------------------------
// Load all patients
// -----------------------------------------------------------------------------


function listAllPatient()
{
	hideById('listPatientDiv');
	hideById('editPatientDiv');
	hideById('migrationPatientDiv');
	hideById('advanced-search');
	
	jQuery('#loaderDiv').show();
	contentDiv = 'listPatientDiv';
	if( getFieldValue('programIdAddPatient')=='')
	{
		jQuery('#listPatientDiv').load('searchRegistrationPatient.action',{
				listAll:true
			},
			function(){
				setTableStyles();
				statusSearching = 0;
				showById('listPatientDiv');
				jQuery('#loaderDiv').hide();
			});
	}
	else 
	{
		jQuery('#listPatientDiv').load('searchRegistrationPatient.action',{
				listAll:false,
				searchBySelectedOrgunit: true,
				programIds: getFieldValue('programIdAddPatient'),
				searchTexts: 'prg_' + getFieldValue('programIdAddPatient')
			},
			function(){
				setTableStyles();
				statusSearching = 0;
				showById('listPatientDiv');
				jQuery('#loaderDiv').hide();
			});
	}
	hideLoader();
}

function searchPatient()
{
	var params = '';
	jQuery( '#advancedSearchTB tbody tr' ).each( function( i, row ){
		jQuery( this ).find(':input').each( function( idx, item ){
			if( idx == 0){
				params += "searchTexts=" + item.value;
			}
			else if( idx == 1){
				params += "_" + htmlEncode( item.value.toLowerCase() );
			}
		})
	});
	params += '&listAll=false';
	params += '&searchBySelectedOrgunit=' + byId('searchBySelectedOrgunit').checked;
		
	$.ajax({
		url: 'searchRegistrationPatient.action',
		type:"POST",
		data: params,
		success: function( html ){
				statusSearching = 1;
				setInnerHTML( 'listPatientDiv', html );
				showById('listPatientDiv');
				jQuery( "#loaderDiv" ).hide();
			}
		});
}

function addPhoneToList( elementList, _id, _patientName, _phoneNo )
{
	var list = jQuery( "#" + elementList );
	
	if ( list.find( "option[value='" + _id + "']").val() == undefined )
	{
		list.append( "<option title='" + i18n_dblick_to_unselect + "' value='" + _id + "'>\"" + _patientName + " <" + _phoneNo + ">" + "\"</option>" );
	}

	jQuery( "tr#tr" + _id ).hide();
}

function removePhoneFromList( elementList, _id )
{
	var list = jQuery( "#" + elementList + " option[value='" + _id + "']" ).remove();
	
	jQuery( "tr#tr" + _id ).show();
}
