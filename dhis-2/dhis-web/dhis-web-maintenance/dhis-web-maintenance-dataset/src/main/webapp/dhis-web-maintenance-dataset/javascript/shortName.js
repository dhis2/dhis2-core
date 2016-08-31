
// -----------------------------------------------------------------------------
// $Id: shortName.js 1251 2006-01-31 06:43:27Z torgeilo $
// -----------------------------------------------------------------------------

function nameChanged()
{
    var nameField = document.getElementById( 'name' );
    var shortNameField = document.getElementById( 'shortName' );

	if ( nameField.value.length >= 20 )
	{
		shortNameField.value = nameField.value;
    }
    previousName = nameField.value;
}
