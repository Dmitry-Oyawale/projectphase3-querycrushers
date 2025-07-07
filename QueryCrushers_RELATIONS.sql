--QueryCrushers: Kevin Lai, Ziyue Chen, Chenxi Zeng
DROP TABLE IF EXISTS Business_hour;
DROP TABLE IF EXISTS Checkin;
DROP TABLE IF EXISTS Attribute;
DROP TABLE IF EXISTS Category;
DROP TABLE IF EXISTS Tip;
DROP TABLE IF EXISTS Business;
DROP TABLE IF EXISTS Friendship;
DROP TABLE IF EXISTS Users;

CREATE TABLE Users(
    user_ID CHAR(22),
    user_name VARCHAR,
    date VARCHAR,
    tip_count INTEGER DEFAULT 0,
    number_of_fans INTEGER,
    average_stars REAL,
    funny VARCHAR,
    useful VARCHAR,
    cool VARCHAR,
    PRIMARY KEY (user_ID)
);

--Assumed “funny , useful, and cool” scores are separate
CREATE TABLE Friendship (
    user_id VARCHAR(22),
    friend_id VARCHAR(22),
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES Users(user_ID) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES Users(user_ID) ON DELETE CASCADE
);


CREATE TABLE Business (
    business_ID CHAR(22),
    name VARCHAR,
    street_address VARCHAR,
    state VARCHAR,
    city VARCHAR,
    zip_code CHAR(5),
    latitude REAL,
    longitude REAL,
    stars REAL,
    is_open INTEGER,
    num_tips INTEGER DEFAULT 0,
    PRIMARY KEY(business_ID)
);

CREATE TABLE Tip(
    business_ID CHAR(22),
    time VARCHAR,
    number_of_likes INTEGER,
    text VARCHAR,
    user_ID CHAR(22),
    PRIMARY KEY (time, user_ID, business_ID),
    FOREIGN KEY (user_ID) REFERENCES Users(user_ID),
    FOREIGN KEY (business_ID) REFERENCES Business(business_ID)
);


CREATE TABLE Category(
    business_ID CHAR(22),
    category_name VARCHAR,
    PRIMARY KEY(business_ID, category_name),
    FOREIGN KEY (business_ID) REFERENCES Business(business_ID)
);

CREATE TABLE Attribute(
    business_ID CHAR(22),
    attribute_name VARCHAR,
    value VARCHAR,
    PRIMARY KEY(business_ID, attribute_name),
    FOREIGN KEY (business_ID) REFERENCES Business(business_ID)
);

CREATE TABLE Checkin(
    checkin_time  TIMESTAMP,
    business_ID CHAR(22),
    PRIMARY KEY(checkin_time, business_ID),
    FOREIGN KEY (business_ID) REFERENCES Business(business_ID)
);

CREATE TABLE Business_hour(
    day_of_the_week VARCHAR,
    opening_time VARCHAR,
    closing_time VARCHAR,
    business_ID CHAR(22),
    PRIMARY KEY(day_of_the_week, business_ID),
    FOREIGN KEY (business_ID) REFERENCES Business(business_ID)
);
