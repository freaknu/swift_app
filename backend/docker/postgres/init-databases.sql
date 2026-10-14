-- psql script used both on a fresh PostgreSQL volume and on later starts.
-- CREATE DATABASE uses the configured POSTGRES_USER as owner.
SELECT format('CREATE DATABASE %I', required.database_name)
FROM (VALUES
    ('authservice_db'),
    ('userservice_db'),
    ('locationservice_db'),
    ('riderservice_db'),
    ('paymentservice_db'),
    ('driver_db'),
    ('ratingservice_db'),
    ('notificationservice_db'),
    ('analyticalservice_db'),
    ('matchingservice_db'),
    ('chatservice_db')
) AS required(database_name)
WHERE NOT EXISTS (
    SELECT 1 FROM pg_database WHERE datname = required.database_name
)
\gexec
