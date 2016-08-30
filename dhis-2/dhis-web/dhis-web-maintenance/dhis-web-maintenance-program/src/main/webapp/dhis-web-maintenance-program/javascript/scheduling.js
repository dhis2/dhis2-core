
// -----------------------------------------------------------------------
// Schedule Messages
// -----------------------------------------------------------------------

function scheduleTasks()
{
	$.post( 'scheduleTasks.action',{
		execute:false,
		timeSendingMessage: getFieldValue("timeSendingMessage")
	}, function( json ){
		var status = json.scheduleTasks.status;
		if( status=='not_started' ){
			status = i18n_not_started;
		}
		setInnerHTML('info', i18n_scheduling_is + " " + status);
		if( json.scheduleTasks.running=="true" ){
			setFieldValue('scheduledBtn', i18n_stop);
			enable('executeButton');
		}
		else{
			setFieldValue('scheduledBtn', i18n_start);
			disable('executeButton');
		}
	});
}

function executeTasks()
{
	var ok = confirm( i18n_execute_tasks_confirmation );
	if ( ok )
	{		
		$.post( 'scheduleTasks.action',{
			execute:true,
			timeSendingMessage: getFieldValue("timeSendingMessage")
		}, function( json ){
			pingMessageNotificationsTimeout();
		});
	}
}

function pingMessageNotificationsTimeout()
{
	pingNotifications( 'SENDING_REMINDER_MESSAGE', 'notificationTable' );
	setTimeout( "pingMessageNotificationsTimeout()", 200 );
}

// -----------------------------------------------------------------------
// Schedule Automated Aggregate
// -----------------------------------------------------------------------

function schedulingAggCondTasks()
{
	$.post( 'scheduleCaseAggTasks.action',{
		taskStrategy:getFieldValue('taskStrategy'),
		execute:false
	}, function( json ){
		var status = json.scheduleTasks.status;
		if( status=='not_started' ){
			status = i18n_not_started;
		}
		setInnerHTML('info', i18n_scheduling_is + " " + status);
		if( json.scheduleTasks.running=="true" ){
			setFieldValue('scheduledBtn', i18n_stop);
			enable('executeButton');
		}
		else{
			setFieldValue('scheduledBtn', i18n_start);
			disable('executeButton');
		}
	});
}

function executeAggCondTasks()
{
	var ok = confirm( i18n_execute_tasks_confirmation );
	if ( ok )
	{
		$.post( 'scheduleCaseAggTasks.action',{
			taskStrategy:getFieldValue('taskStrategy'),
			execute:true
		},function( json ){
			pingNotificationsTimeout();
		});
	}
}

function pingNotificationsTimeout()
{
	pingNotifications( 'AGGREGATE_QUERY_BUILDER', 'notificationTable' );
	setTimeout( "pingNotificationsTimeout()", 200 );
}
