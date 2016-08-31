function removeTable( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, "removeTable.action");
}

function runReportTable( context ) {
  location.href = 'getReportParams.action?uid=' + context.uid + '&mode=table';
}
