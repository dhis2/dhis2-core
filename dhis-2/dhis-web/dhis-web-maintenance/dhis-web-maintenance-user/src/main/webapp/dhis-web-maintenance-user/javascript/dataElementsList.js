var selectedList;
var availableList;

function move( listId ) {

  var fromList = document.getElementById(listId);

  if ( fromList.selectedIndex == -1 ) { return; }

  if ( ! availableList ) {
    availableList = document.getElementById('availableList');
  }

  if ( ! selectedList ) {
    selectedList = document.getElementById('selectedList');
  }

  var toList = ( fromList == availableList ? selectedList : availableList );

  while ( fromList.selectedIndex > -1 ) {
    option = fromList.options.item(fromList.selectedIndex);
    fromList.remove(fromList.selectedIndex);
    toList.add(option, null);
  }

}

var selectedListAuthority;
var availableListAuthority;

function moveAuthority( listId ) {

  var fromList = document.getElementById(listId);

  if ( fromList.selectedIndex == -1 ) { return; }

  if ( ! availableListAuthority ) {
    availableListAuthority = document.getElementById('availableListAuthority');
  }

  if ( ! selectedListAuthority ) {
    selectedListAuthority = document.getElementById('selectedListAuthority');
  }

  var toList = ( fromList == availableListAuthority ? selectedListAuthority : availableListAuthority );

  while ( fromList.selectedIndex > -1 ) {
    option = fromList.options.item(fromList.selectedIndex);
    fromList.remove(fromList.selectedIndex);
    toList.add(option, null);
  }

}

function submitForm() {

  if ( ! availableList ) {
    availableList = document.getElementById('availableList');
  }

  if ( ! selectedList ) {
    selectedList = document.getElementById('selectedList');
  }

  // selectAll(availableList);
  selectAll(selectedList);

  return false;

}

function selectAll(list) {

  for ( var i = 0, option; option = list.options.item(i); i++ ) {
    option.selected = true;
  }

}