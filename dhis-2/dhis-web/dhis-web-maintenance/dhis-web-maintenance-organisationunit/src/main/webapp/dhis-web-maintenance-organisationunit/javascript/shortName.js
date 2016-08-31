
// -----------------------------------------------------------------------------
// $Id: shortName.js 1229 2006-01-29 17:59:00Z torgeilo $
// -----------------------------------------------------------------------------

function nameChanged()
{
	/* fail quietly if previousName is not available */
	if(previousName === undefined) {
		return;
	}
	
    var nameField = document.getElementById( 'name' );
    var shortNameField = document.getElementById( 'shortName' );
    var maxLength = parseInt( shortNameField.maxLength );
    
    if ( previousName != nameField.value
        && nameField.value.length <= maxLength
        && ( shortNameField.value == previousName
          || shortNameField.value.length == 0 ))
    {
            shortNameField.value = nameField.value;
    }
    
    previousName = nameField.value;
}
