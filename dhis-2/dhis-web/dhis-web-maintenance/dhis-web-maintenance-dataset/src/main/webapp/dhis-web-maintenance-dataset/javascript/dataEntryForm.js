
// -----------------------------------------------------------------------------
// Delete DataEntryForm
// -----------------------------------------------------------------------------

function removeDataEntryForm( dataSetIdField, dataEntryFormId, dataEntryFormName )
{
	var result = window.confirm( i18n_confirm_delete + '\n\n' + dataEntryFormName );

	if ( result )
	{
		window.location.href = 'delDataEntryForm.action?dataSetId=' + dataSetIdField + "&dataEntryFormId=" + dataEntryFormId;
	}
}

// ----------------------------------------------------------------------
// Validation
// ----------------------------------------------------------------------

function validateDataEntryForm( exit )
{
	$.post( 'validateDataEntryForm.action',
	{
		name: $( '#nameField' ).val(),
		dataSetId: $( '#dataSetIdField' ).val(),
		dataEntryFormId: dataEntryFormId
	}, 
	function( json )
	{
		if ( json.response == 'success' )
		{
			if ( exit )
			{
				$( '#saveDataEntryForm' ).submit();
			}			
			else
			{
				saveDataEntryForm();
			}
		}
		else if ( json.response = 'input' )
		{
			setHeaderDelayMessage( json.message );
		}
	} );
}

function saveDataEntryForm() 
{
	var style = $("#style").val();
	var designTextarea = $("#designTextarea").ckeditor().editor.getData();
	
	$.postUTF8( 'autoSaveDataEntryForm.action',
	{
		nameField: getFieldValue('nameField'),
		style: style,
		designTextarea: designTextarea,
		dataSetIdField: getFieldValue('dataSetIdField'),
		dataEntryFormId: dataEntryFormId
	},
	function( json ) 
	{
		setHeaderDelayMessage( i18n_save_success ); 
		stat = "EDIT";
		dataEntryFormId = json.message;
		enable('delete');
	} );
}

function validateDataEntryFormTimeout()
{
	validateDataEntryForm( false );
	timeOut = window.setTimeout( "validateDataEntryFormTimeout();", 60000 );
}
