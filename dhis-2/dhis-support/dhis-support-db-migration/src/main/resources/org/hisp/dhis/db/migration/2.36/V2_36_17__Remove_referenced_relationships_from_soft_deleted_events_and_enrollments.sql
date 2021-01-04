update relationship
	set from_relationshipitemid = null, 
		to_relationshipitemid = null
	where from_relationshipitemid in (select relationshipitemid from relationshipitem ri 
										left join programstageinstance psi on ri.programstageinstanceid = psi.programstageinstanceid 
										where psi.deleted=true  )
		or to_relationshipitemid in (select relationshipitemid from relationshipitem ri 
										left join programstageinstance psi on ri.programstageinstanceid = psi.programstageinstanceid 
										where psi.deleted=true  );

update relationship 
	set from_relationshipitemid = null,
		to_relationshipitemid = null 
	where from_relationshipitemid in (select relationshipitemid from relationshipitem ri 
										left join programinstance pi on ri.programinstanceid = pi.programinstanceid 
										where pi.deleted=true  )
		or to_relationshipitemid in (select relationshipitemid from relationshipitem ri 
										left join programinstance pi on ri.programinstanceid = pi.programinstanceid 
										where pi.deleted=true );

delete from relationshipitem where relationshipid in (select relationshipid from relationship where from_relationshipitemid is null or to_relationshipitemid is null );

delete from relationship where from_relationshipitemid is null or to_relationshipitemid is null;
