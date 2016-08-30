
function filterValues( filter )
{
    var list = document.getElementById( 'list' );
    
    var rows = list.getElementsByTagName( 'tr' );
    
    for ( var i = 0; i < rows.length; ++i )
    {
        var cell = rows[i].getElementsByTagName( 'td' )[0];
        
        var value = cell.firstChild.nodeValue;

        if ( filter.length <= value.length
            && value.substring( 0, filter.length ).toLowerCase() == filter.toLowerCase() )
        {
            rows[i].style.display = 'table-row';
        }
        else
        {
            rows[i].style.display = 'none';
        }
    }
}
