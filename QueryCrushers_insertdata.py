# QueryCrushers: Kevin Lai, Ziyue Chen, Chenxi Zeng

# CS 3431
# For additional examples:
# https://www.psycopg.org/docs/usage.html#query-parameters

#  If psycopg2 is not installed, install it using pip installer :  
#     pip install psycopg2 (Windows)  
#  or pip3 install psycopg2 (Mac)
# 
#  If the installation of psycopg2 fails, try installing the binary. The binary is  a stand-alone package, not requiring a compiler or external libraries:
#     $ pip install psycopg2-binary  (Windows)
# or  $ pip3 install psycopg2-binary  (Mac)
#    
import json
import psycopg2
from contextlib import contextmanager
from dotenv import load_dotenv
from datetime import datetime
import os

# Load environment variables from .env file (if used)
# load_dotenv() is a function from the python-dotenv package that loads environment variables from a .env file into the Python environment. 
load_dotenv()

# Database connection parameters
#TODO: update  username and password
psql_params = {
    "DB_NAME" : "tempyelp",    # Make sure that "tempdb" database is already created in your PostgreSQL server.
    "DB_USER" : os.getenv("POSTGRES_USER"),  # get from environment variables
    "DB_PASSWORD" : os.getenv("POSTGRES_PASSWORD"), # get from environment variables
    "DB_HOST" : "localhost",  # or the IP address of your database server
    "DB_PORT" : "5432"  # Default PostgreSQL port
}
@contextmanager
def connect_psql(db_params):
    conn = None
    #connect to tempdb database on postgres server using psycopg2
    try:
        conn = psycopg2.connect(dbname=db_params["DB_NAME"],
                                user=db_params["DB_USER"],
                                password=db_params["DB_PASSWORD"],
                                host=db_params["DB_HOST"],
                                port=db_params["DB_PORT"])
        # Create a cursor object to execute SQL queries
        cursor = conn.cursor()
        print("Connected to PostgreSQL")

        # Yield cursor (execution pauses here, allowing the caller to use it)
        yield cursor

        # Commit transactions (if needed)
        conn.commit()

    except Exception as e:
        print(f"Database error: {e}")
        if conn:
            conn.rollback()  # Rollback in case of an error
    finally:
        if conn:
            cursor.close()
            conn.close()
            print("Connection closed")

"""cleanStr4SQL function removes the "single quote" or "back quote" characters from strings. """
def cleanStr4SQL(s):
    return s.replace("'","`").replace("\n"," ")

# Insert business data
def insert_business():
    with connect_psql(psql_params) as cursor:
        #reading the JSON file
        with open('.//yelp_business.JSON','r') as f:    #TODO: update path for the input file
            line = f.readline()
            count_line = 0
            while line:
                data = json.loads(line)
                # Generate the INSERT statement for the current business
                # TODO: The below INSERT statement is based on a simple (and incomplete) businesstable schema. Update the statement based on your own table schema and
                # include values for all businessTable attributes
                try:
                    cursor.execute("""INSERT INTO Business (business_ID, name, street_address, state, city,
                                            zip_code, latitude, longitude, stars, is_open, num_tips)
                                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);""", 
                                        (cleanStr4SQL(data["business_id"]), cleanStr4SQL(data["name"]), cleanStr4SQL(data["address"]), cleanStr4SQL(data["state"]), cleanStr4SQL(data["city"]),
                                        cleanStr4SQL(data["postal_code"]), data["latitude"], data["longitude"], data["stars"], data["is_open"], 0))              
                except Exception as e:
                    print("Insert to business table failed!",e)
                
                line = f.readline()
                count_line +=1
        print(count_line)
        f.close()

insert_business()


# The above code assumes the following BusinessTable schema 
"""
CREATE TABLE Business (
    name VARCHAR,
    business_ID CHAR(22),
    street_address VARCHAR,
    city VARCHAR,
    state VARCHAR,
    zip_code CHAR(5),
    latitude REAL,
    longitude REAL,
    stars REAL,
    is_open INTEGER,
    num_tips INTEGER DEFAULT 0,
    PRIMARY KEY(business_ID)
);

SELECT COUNT(*) FROM business;

"""


# Insert business categories
def insert_business_categories():
    with connect_psql(psql_params) as cursor:
        #reading the JSON file
        with open('.//yelp_business.JSON','r') as f:    #TODO: update path for the input file
            line = f.readline()
            count_category = 0
            while line:
                data = json.loads(line)
                # Generate the INSERT statement for the current business
                # TODO: The below INSERT statement is based on a simple (and incomplete) businesstable schema. Update the statement based on your own table schema and
                # include values for all businessTable attributes
                business_id = cleanStr4SQL(data["business_id"])
                categories = data.get("categories", "")
                if categories:
                    for category in categories.split(','):
                        category_single = cleanStr4SQL(category.strip())
                        count_category += 1
                        try:
                            cursor.execute("""INSERT INTO Category (business_ID, category_name)
                                    VALUES (%s, %s); """, 
                                    (cleanStr4SQL(data['business_id']),category_single) )       
                        except Exception as e:
                            print("Insert to business categories table failed!",e)
                line = f.readline()
        print(count_category)
        f.close()

insert_business_categories()


# Insert business attributes
def insert_business_attributes():
    with connect_psql(psql_params) as cursor:
        #reading the JSON file
        with open('.//yelp_business.JSON','r') as f:    #TODO: update path for the input file
            line = f.readline()
            count_attribute = 0
            while line:
                data = json.loads(line)
                # Generate the INSERT statement for the current business
                # TODO: The below INSERT statement is based on a simple (and incomplete) businesstable schema. Update the statement based on your own table schema and
                # include values for all businessTable attributes
                attributes = data.get("attributes", {})
                def insert_attr(attri_name, attri_value):
                    nonlocal count_attribute
                    try:
                        cursor.execute("""INSERT INTO Attribute (business_ID, attribute_name, value)
                            VALUES (%s, %s, %s); """, 
                            (cleanStr4SQL(data['business_id']), cleanStr4SQL(attri_name), cleanStr4SQL(attri_value)))
                        count_attribute += 1       
                    except Exception as e:
                        print("Insert to business categories table failed!",e)

                if isinstance(attributes, dict):
                    for key, value in attributes.items():
                        if isinstance(value, dict):
                            for sub_key, sub_val in value.items():
                                insert_attr(str(sub_key), str(sub_val))
                        else:
                            insert_attr(key, str(value))
                line = f.readline()
        print(count_attribute)
        f.close()

insert_business_attributes()

# Insert business hours
def insert_business_hours():
    with connect_psql(psql_params) as cursor:
        #reading the JSON file
        with open('.//yelp_business.JSON','r') as f:    #TODO: update path for the input file
            line = f.readline()
            count_hours = 0
            while line:
                data = json.loads(line)
                # Generate the INSERT statement for the current business
                # TODO: The below INSERT statement is based on a simple (and incomplete) businesstable schema. Update the statement based on your own table schema and
                # include values for all businessTable attributes
                business_id = cleanStr4SQL(data["business_id"])
                hours = data.get("hours", {})
                for day, time_range in hours.items():
                    try:
                        opentime, closetime = [t.strip() for t in time_range.split('-')]
                        cursor.execute("""INSERT INTO Business_hour (day_of_the_week, opening_time, closing_time, business_ID)
                                          VALUES (%s, %s, %s, %s);""", 
                                    (day, opentime, closetime, business_id) )  
                        count_hours += 1            
                    except Exception as e:
                        print("Insert to business hours table failed!",e)
                
                line = f.readline()
        print(count_hours)
        f.close()

insert_business_hours()

# Insert user data
def insert_users():
    with connect_psql(psql_params) as cursor:
        #reading the JSON file
        with open('.//yelp_user.JSON','r') as f:    #TODO: update path for the input file
            line = f.readline()
            count_line = 0
            while line:
                data = json.loads(line)
                # Generate the INSERT statement for the current business
                # TODO: The below INSERT statement is based on a simple (and incomplete) businesstable schema. Update the statement based on your own table schema and
                # include values for all businessTable attributes
                try:
                    cursor.execute("""INSERT INTO Users (user_ID, user_name, date, tip_count, number_of_fans, average_stars, funny, useful, cool)
                                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s); """, 
                                    (cleanStr4SQL(data['user_id']),cleanStr4SQL(data["name"]), cleanStr4SQL(data["yelping_since"]), 0, data["fans"], data["average_stars"], data["funny"], 
                                    data["useful"], data["cool"]) )              
                except Exception as e:
                    print("Insert to user table failed!",e)
                
                line = f.readline()
                count_line +=1
        print(count_line)
        f.close()

insert_users()

# Insert friendship
def insert_friend():
    with connect_psql(psql_params) as cursor:
        #reading the JSON file
        with open('.//yelp_user.JSON','r') as f:    #TODO: update path for the input file
            line = f.readline()
            count_friend = 0
            while line:
                data = json.loads(line)
                # Generate the INSERT statement for the current business
                # TODO: The below INSERT statement is based on a simple (and incomplete) businesstable schema. Update the statement based on your own table schema and
                # include values for all businessTable attributes
                user_id = cleanStr4SQL(data["user_id"])
                friends = data.get("friends", [])
                if isinstance(friends, str):
                    friends = friends.split(', ')
                for friend_id in friends:
                    try:
                        count_friend += 1
                        cursor.execute("""INSERT INTO Friendship (user_id, friend_id)
                                VALUES (%s, %s); """, 
                                (user_id, cleanStr4SQL(friend_id)) )              
                    except Exception as e:
                        print("Insert to tip table failed!",e)
                
                line = f.readline()
        print(count_friend)
        f.close()

insert_friend()

# Insert tips
def insert_tip():
    with connect_psql(psql_params) as cursor:
        #reading the JSON file
        with open('.//yelp_tip.JSON','r') as f:    #TODO: update path for the input file
            line = f.readline()
            count_line = 0
            while line:
                data = json.loads(line)
                # Generate the INSERT statement for the current business
                # TODO: The below INSERT statement is based on a simple (and incomplete) businesstable schema. Update the statement based on your own table schema and
                # include values for all businessTable attributes
                try:
                    cursor.execute("""INSERT INTO Tip (business_ID, time, number_of_likes, text, user_ID)
                                    VALUES (%s, %s, %s, %s, %s); """, 
                                    (cleanStr4SQL(data['business_id']),cleanStr4SQL(data["date"]), data["likes"], cleanStr4SQL(data["text"]), cleanStr4SQL(data["user_id"])) )              
                except Exception as e:
                    print("Insert to tip table failed!",e)
                
                line = f.readline()
                count_line +=1
        print(count_line)
        f.close()

insert_tip()

# Insert checkins

def time_helper(s):
    return s.zfill(2)

def fix_date_helper(date_str):
    try:
        date_part, time_part = date_str.strip().split(" ")
        time_parts = time_part.split(":")
        while len(time_parts) < 3:
            time_parts.append("00")
        time_fixed = ":".join([time_helper(t) for t in time_parts])
        return f"{date_part} {time_fixed}"
    except:
        return date_str  

def insert_checkins():
    with connect_psql(psql_params) as cursor:
        with open('./yelp_checkin.JSON', 'r') as f:
            line = f.readline()
            count_checkin = 0
            while line:
                try:
                    checkin_data = json.loads(line)
                    business_id = cleanStr4SQL(checkin_data["business_id"])
                    date_times = checkin_data.get("date", "")
                    
                    if date_times:
                        for datetimes in date_times.split(","):
                            fixed_date = fix_date_helper(datetimes.strip())
                            cursor.execute("""INSERT INTO Checkin (checkin_time, business_ID)
                                              VALUES (%s, %s);""", 
                                           (fixed_date, business_id))
                            count_checkin += 1
                except Exception as e:
                    print("Insert checkin table failed: ", e)
                
                line = f.readline()
            print(count_checkin)

insert_checkins()

