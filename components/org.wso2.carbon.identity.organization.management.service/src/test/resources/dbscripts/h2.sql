CREATE TABLE IF NOT EXISTS UM_ORG (
    UM_ID VARCHAR(255) NOT NULL,
    UM_ORG_NAME VARCHAR(255) NOT NULL,
    UM_ORG_DESCRIPTION VARCHAR(1024),
    UM_CREATED_TIME TIMESTAMP NOT NULL,
    UM_LAST_MODIFIED TIMESTAMP  NOT NULL,
    UM_STATUS VARCHAR(255) DEFAULT 'ACTIVE' NOT NULL,
    UM_PARENT_ID VARCHAR(255),
    UM_ORG_TYPE VARCHAR(100) NOT NULL,
    PRIMARY KEY (UM_ID),
    FOREIGN KEY (UM_PARENT_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS UM_ORG_ATTRIBUTE (
    UM_ID INTEGER NOT NULL AUTO_INCREMENT,
    UM_ORG_ID VARCHAR(255) NOT NULL,
    UM_ATTRIBUTE_KEY VARCHAR(255) NOT NULL,
    UM_ATTRIBUTE_VALUE VARCHAR(512),
    PRIMARY KEY (UM_ID),
    UNIQUE (UM_ORG_ID, UM_ATTRIBUTE_KEY),
    FOREIGN KEY (UM_ORG_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE
);

CREATE TABLE  IF NOT EXISTS UM_ORG_HIERARCHY (
    UM_PARENT_ID VARCHAR(36) NOT NULL,
    UM_ID VARCHAR(36) NOT NULL,
    DEPTH INTEGER,
    PRIMARY KEY (UM_PARENT_ID, UM_ID),
    FOREIGN KEY (UM_PARENT_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE,
    FOREIGN KEY (UM_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE
);

INSERT INTO UM_ORG(UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, UM_LAST_MODIFIED, UM_STATUS, UM_ORG_TYPE)
SELECT UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, UM_LAST_MODIFIED, UM_STATUS, UM_ORG_TYPE FROM (
	SELECT
	    '10084a8d-113f-4211-a0d5-efe36b082211' AS UM_ID,
	    'ROOT' AS UM_ORG_NAME,
	    'This is the root organization.' AS UM_ORG_DESCRIPTION,
	    CURRENT_TIMESTAMP AS UM_CREATED_TIME,
	    CURRENT_TIMESTAMP AS UM_LAST_MODIFIED,
	    'ACTIVE' AS UM_STATUS,
	    'TENANT' AS UM_ORG_TYPE
) temp
WHERE NOT EXISTS (SELECT * FROM UM_ORG org WHERE org.UM_ID = temp.UM_ID);

