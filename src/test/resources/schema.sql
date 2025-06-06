-- J:\My Drive\bookx-backend\src\test\resources\schema.sql
DROP TABLE IF EXISTS USERS;

CREATE TABLE USERS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    USERNAME VARCHAR(255) NOT NULL UNIQUE, -- Assuming this is fine as is, or map to "user_name" if entity field is userName
    EMAIL VARCHAR(255) NOT NULL UNIQUE,
    PASSWORD_HASH VARCHAR(255) NOT NULL,
    FULL_NAME VARCHAR(255),
    ENABLED BOOLEAN DEFAULT TRUE,
    REGISTERED_AT TIMESTAMP,
    PROFILE_IMAGE_URL VARCHAR(255),
    LATITUDE DOUBLE,
    LONGITUDE DOUBLE
);
