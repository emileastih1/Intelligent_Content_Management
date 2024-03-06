-- Schema documentcontent
-- Schéma privileges for postgres user
GRANT USAGE ON SCHEMA documentcontent TO postgres;
GRANT ALL ON ALL TABLES IN SCHEMA documentcontent TO postgres;
GRANT ALL ON ALL SEQUENCES IN SCHEMA documentcontent TO postgres;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA documentcontent TO postgres;

-- Schéma privileges for doc_management_user
GRANT USAGE ON SCHEMA documentcontent TO doc_management_user;
GRANT ALL ON ALL TABLES IN SCHEMA documentcontent TO doc_management_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA documentcontent TO doc_management_user;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA documentcontent TO doc_management_user;

-- Schema vectorcontent
-- Schema privileges for postgres user
GRANT USAGE ON SCHEMA vectorcontent TO postgres;
GRANT ALL ON ALL TABLES IN SCHEMA vectorcontent TO postgres;
GRANT ALL ON ALL SEQUENCES IN SCHEMA vectorcontent TO postgres;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA vectorcontent TO postgres;

-- Schéma privileges for doc_management_user
GRANT USAGE ON SCHEMA vectorcontent TO doc_management_user;
GRANT ALL ON ALL TABLES IN SCHEMA vectorcontent TO doc_management_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA vectorcontent TO doc_management_user;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA vectorcontent TO doc_management_user;