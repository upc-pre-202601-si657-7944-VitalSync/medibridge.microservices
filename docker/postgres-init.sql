SELECT 'CREATE DATABASE medibridge_iam'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'medibridge_iam')\gexec

SELECT 'CREATE DATABASE medibridge_profiles'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'medibridge_profiles')\gexec

SELECT 'CREATE DATABASE medibridge_payments'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'medibridge_payments')\gexec

SELECT 'CREATE DATABASE medibridge_appointments'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'medibridge_appointments')\gexec

SELECT 'CREATE DATABASE medibridge_healthmonitoring'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'medibridge_healthmonitoring')\gexec

SELECT 'CREATE DATABASE medibridge_medication'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'medibridge_medication')\gexec

SELECT 'CREATE DATABASE medibridge_reports'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'medibridge_reports')\gexec
