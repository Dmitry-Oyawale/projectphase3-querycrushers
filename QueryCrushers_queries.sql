--QueryCrushers: Kevin Lai, Ziyue Chen, Chenxi Zeng

--1
SELECT category_name
FROM Category
JOIN Business ON Category.business_ID = Business.business_ID
WHERE Business.city = 'Scottsdale' AND Business.state = 'AZ'
GROUP BY category_name;

SELECT attribute_name
FROM Attribute
JOIN Business ON Attribute.business_ID = Business.business_ID
WHERE Business.city = 'Scottsdale' AND Business.state = 'AZ'
GROUP BY attribute_name;

--2
SELECT business_ID, name, street_address, num_tips
FROM Business
WHERE EXISTS (
    SELECT category_name
    FROM Category
    WHERE category_name = 'Restaurants' AND Category.business_ID = Business.business_ID
) AND EXISTS (
    SELECT category_name
    FROM Category
    WHERE category_name = 'Breakfast & Brunch' AND Category.business_ID = Business.business_ID
) AND EXISTS (
    SELECT category_name
    FROM Category
    WHERE category_name = 'Bakeries' AND Category.business_ID = Business.business_ID
) AND Business.city = 'Scottsdale' AND Business.state = 'AZ'
ORDER BY name;

--3
SELECT B.business_ID, B.name, B.street_address, B.num_tips
FROM Business B
WHERE B.city = 'Scottsdale' AND B.state = 'AZ'
  AND EXISTS (
      SELECT 1 FROM Attribute A
      WHERE A.business_ID = B.business_ID
        AND A.attribute_name = 'BusinessAcceptsCreditCards'
        AND A.value = 'True'
  )
  AND EXISTS (
      SELECT 1 FROM Attribute A
      WHERE A.business_ID = B.business_ID
        AND A.attribute_name = 'ByAppointmentOnly'
        AND A.value = 'True'
  )
  AND EXISTS (
      SELECT 1 FROM Attribute A
      WHERE A.business_ID = B.business_ID
        AND A.attribute_name = 'WiFi'
        AND A.value = 'free'
  )
ORDER BY B.name;

--4
SELECT B.business_ID, B.name, B.street_address, B.num_tips
FROM Business B
JOIN Business_Hour H ON B.business_ID = H.business_ID
WHERE B.city = 'Scottsdale' AND B.state = 'AZ'
  AND EXISTS (
      SELECT 1 FROM Attribute A
      WHERE A.business_ID = B.business_ID
        AND A.attribute_name = 'BusinessAcceptsCreditCards' AND A.value = 'True'
  )
  AND EXISTS (
      SELECT 1 FROM Attribute A
      WHERE A.business_ID = B.business_ID
        AND A.attribute_name = 'RestaurantsPriceRange2' AND A.value = '2'
  )
  AND EXISTS (
      SELECT 1 FROM Attribute A
      WHERE A.business_ID = B.business_ID
        AND A.attribute_name = 'WiFi' AND A.value = 'free'
  ) AND EXISTS (
    SELECT 1
    FROM Category
    WHERE category_name = 'Restaurants' AND Category.business_ID = B.business_ID
) AND EXISTS (
    SELECT 1
    FROM Category
    WHERE category_name = 'Breakfast & Brunch' AND Category.business_ID = B.business_ID
) AND EXISTS (
    SELECT 1
    FROM Category
    WHERE category_name = 'Bakeries' AND Category.business_ID = B.business_ID
) AND H.day_of_the_week = 'Monday'
  AND H.opening_time::time <= TIME '10:30'
  AND H.closing_time::time >= TIME '13:30'
ORDER BY B.name;

--5
CREATE OR REPLACE FUNCTION count_categories(bid1 CHAR(22), bid2 CHAR(22))
RETURNS INTEGER AS $$
DECLARE
    common_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO common_count
    FROM (
        SELECT category_name
        FROM Category
        WHERE business_ID = bid1
        INTERSECT
        SELECT category_name
        FROM Category
        WHERE business_ID = bid2
    ) AS common_categories;

    RETURN common_count;
END;
$$ LANGUAGE plpgsql;

--Test
SELECT count_categories('iPPzDL_oY8SJCjmycuXcVg', 'ncXQtqJT5Gk1QztwTrBrgw');

--6
CREATE OR REPLACE FUNCTION geodistance(
    lat1 DOUBLE PRECISION, lon1 DOUBLE PRECISION,
    lat2 DOUBLE PRECISION, lon2 DOUBLE PRECISION
)
RETURNS DOUBLE PRECISION AS $$
DECLARE
    radlat1 DOUBLE PRECISION := RADIANS(lat1);
    radlat2 DOUBLE PRECISION := RADIANS(lat2);
    dlat DOUBLE PRECISION := RADIANS(lat2 - lat1);
    dlon DOUBLE PRECISION := RADIANS(lon2 - lon1);
    a DOUBLE PRECISION;
    c DOUBLE PRECISION;
    r DOUBLE PRECISION := 3958.8; 
BEGIN
    a := SIN(dlat/2)^2 + COS(radlat1) * COS(radlat2) * SIN(dlon/2)^2;
    c := 2 * ATAN2(SQRT(a), SQRT(1-a));
    RETURN r * c;
END;
$$ LANGUAGE plpgsql;

--Test
SELECT geodistance(33.6399735577, -112.1334044052, 33.5796797, -111.9275444);

--7
SELECT Bus1.business_ID, Bus1.name, Bus1.city, Bus1.zip_code, Bus2.business_ID, Bus2.name, count_categories(Bus1.business_ID, Bus2.business_ID) AS common_categories
FROM Business Bus1, Business Bus2
WHERE Bus1.business_ID = 'iPPzDL_oY8SJCjmycuXcVg'
AND Bus2.Business_ID != Bus1.business_ID
AND Bus2.zip_code = Bus1.zip_code
AND geodistance(Bus1.latitude, Bus1.longitude, Bus2.latitude, Bus2.longitude) <= 20
AND EXISTS(
	SELECT 1 FROM Category Cat1
	JOIN Category Cat2 ON Cat1.category_name = Cat2.category_name
	WHERE Cat1.business_ID = Bus1.business_ID AND Cat2.business_ID = Bus2.business_ID)
ORDER BY common_categories DESC
LIMIT 15;

--8
SELECT Business.business_ID, Business.name, Business.street_address, Business.num_tips
FROM Business
JOIN Category ON Business.business_ID = Category.business_ID
WHERE Business.zip_code = '85251'
	AND Category.category_name = 'Restaurants'
	AND Business.num_tips = (
		SELECT MAX(Bus2.num_tips)
		FROM Business Bus2
		JOIN Category Cat2 ON Bus2.business_ID = Cat2.business_ID
		WHERE Bus2.zip_code = '85251'
			AND Cat2.category_name = 'Restaurants');



--9 
SELECT Users.user_name, Tip.time, Tip.text
FROM Friendship
JOIN Users ON Friendship.friend_ID = Users.user_ID
JOIN Tip ON Users.user_ID = Tip.user_ID
WHERE Friendship.user_ID = 'TiWF94rl8Q6jqQf2YZSFPA'
ORDER BY Tip.time DESC
LIMIT 1;

--10
SELECT Users.user_ID, Users.user_name, Tip.time, Tip.text
FROM Users
JOIN (
	SELECT user_ID, MAX(time) AS max_time
	FROM Tip
	GROUP BY user_ID) AS Latest ON Users.user_ID = Latest.user_ID
JOIN Tip ON Tip.user_ID = Latest.user_ID AND Tip.time = Latest.max_time
WHERE Users.user_ID IN (
	SELECT friend_ID
	FROM Friendship
	WHERE user_ID = 'TiWF94rl8Q6jqQf2YZSFPA' )
ORDER BY Tip.time DESC;
	

--11
CREATE OR REPLACE FUNCTION updateTip() RETURNS TRIGGER AS '
DECLARE
    user_tips integer;
    business_tips integer;
BEGIN
    SELECT COUNT(*) INTO user_tips
    FROM Tip
    WHERE NEW.user_ID = Tip.user_ID;

    SELECT COUNT(*) INTO business_tips
    FROM Tip
    WHERE NEW.business_ID = Tip.business_ID;

    UPDATE Users
    SET tip_count = user_tips
    WHERE Users.user_ID = NEW.user_ID;

    UPDATE Business
    SET num_tips = business_tips
    WHERE Business.business_ID = NEW.business_ID;
    RETURN NEW;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER insertTip
AFTER INSERT ON Tip
For Each Row
EXECUTE PROCEDURE updateTip();

--Test
INSERT INTO Tip (business_ID, time, number_of_likes, text, user_ID)
VALUES ('FDEm-c3NAXnTVtl-hgzAhA', '2025-04-21 09:30:00', '90', 'Test statement', 'Ps_zkoSnuv2Gy-QIt0jEJg');

SELECT tip_count FROM Users WHERE user_ID = 'Ps_zkoSnuv2Gy-QIt0jEJg';
SELECT num_tips FROM Business WHERE business_ID = 'FDEm-c3NAXnTVtl-hgzAhA';

--12
CREATE OR REPLACE FUNCTION hourCheck() RETURNS TRIGGER AS '
DECLARE
    open_time TIME;
    close_time TIME;
    day_week TEXT;
BEGIN
    day_week := INITCAP(TRIM(TO_CHAR(NEW.checkin_time, ''FMDay'')));

    SELECT opening_time::TIME, closing_time::TIME
    INTO open_time, close_time
    FROM Business_hour
    WHERE business_ID = NEW.business_ID AND day_of_the_week = day_week;


    IF NOT FOUND THEN
        RAISE EXCEPTION ''Cannot check in on this day'';
    END IF;

    IF (NEW.checkin_time::TIME < open_time OR NEW.checkin_time::TIME > close_time) THEN
        RAISE EXCEPTION ''Cannot check in at this time'';
    END IF;
    RETURN NEW;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER businessCheck
BEFORE INSERT ON Checkin
For Each Row
EXECUTE PROCEDURE hourCheck();

--Test
INSERT INTO Checkin (checkin_time, business_ID)
VALUES ('2025-04-21 07:00:00', 'r8764MtYyt8JhxMvrfM_xQ');
