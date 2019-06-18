alter table fileresource
add column if not exists hasmultiplestoragefiles boolean;

update fileresource set hasmultiplestoragefiles = false;