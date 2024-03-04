ALTER ROLE postgres SET search_path TO documentcontent, vectorcontent, public;
ALTER ROLE doc_management_user SET search_path TO documentcontent, vectorcontent, public;