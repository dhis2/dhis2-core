function changeRuleType() {
  var op = document.getElementById('operator');
  if( 'compulsory_pair' == op.value || 'exclusive_pair' == op.value ) {
    showById('select_operator');
    op.selectedIndex = 0;
  }
}
