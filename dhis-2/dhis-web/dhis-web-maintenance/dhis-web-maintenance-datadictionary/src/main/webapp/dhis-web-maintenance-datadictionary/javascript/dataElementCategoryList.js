var selectedList;
var availableList;

function move( listId )
{
    var fromList = document.getElementById( listId );

    if ( fromList.selectedIndex == -1 )
    {
        return;
    }

    if ( !availableList )
    {
        availableList = document.getElementById( 'availableList' );
    }

    if ( !selectedList )
    {
        selectedList = document.getElementById( 'selectedList' );
    }

    var toList = ( fromList == availableList ? selectedList : availableList );

    while ( fromList.selectedIndex > -1 )
    {
        option = fromList.options.item( fromList.selectedIndex );
        fromList.remove( fromList.selectedIndex );
        toList.add( option, null );
    }
}

function moveUp( listId )
{

    var withInList = document.getElementById( listId );

    var index = withInList.selectedIndex;

    if ( index == -1 )
    {
        return;
    }

    if ( index - 1 < 0 )
    {
        return;
    }// window.alert( 'Item cant be moved up');

    var option = new Option( withInList.options[index].text, withInList.options[index].value );
    var temp = new Option( withInList.options[index - 1].text, withInList.options[index - 1].value );

    withInList.options[index - 1] = option;
    withInList.options[index - 1].selected = true;
    withInList.options[index] = temp;

}

function moveDown( listId )
{

    var withInList = document.getElementById( listId );

    var index = withInList.selectedIndex;

    if ( index == -1 )
    {
        return;
    }

    if ( index + 1 == withInList.options.length )
    {
        return;
    }// window.alert( 'Item cant be moved down');

    var option = new Option( withInList.options[index].text, withInList.options[index].value );
    var temp = new Option( withInList.options[index + 1].text, withInList.options[index + 1].value );

    withInList.options[index + 1] = option;
    withInList.options[index + 1].selected = true;
    withInList.options[index] = temp;

}

function submitForm()
{
    if ( !availableList )
    {
        availableList = document.getElementById( 'availableList' );
    }

    if ( !selectedList )
    {
        selectedList = document.getElementById( 'selectedList' );
    }

    // selectAll(availableList);
    selectAll( selectedList );

    return false;

}

function selectAll( list )
{
    for ( var i = 0, option; option = list.options.item( i ); i++ )
    {
        option.selected = true;
    }
}
