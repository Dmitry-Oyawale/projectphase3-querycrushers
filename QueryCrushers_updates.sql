--QueryCrushers: Kevin Lai, Ziyue Chen, Chenxi Zeng

--a
With a AS (
SELECT Tip.business_ID, COUNT(business_ID) AS num_tips
FROM Tip
GROUP BY Tip.business_ID)
UPDATE Business
SET num_tips = a.num_tips
FROM a
WHERE Business.business_ID = a.business_ID;

With b AS (
SELECT Tip.user_ID, COUNT(user_ID) AS tip_count
FROM Tip
GROUP BY Tip.user_ID)
UPDATE Users
SET tip_count = b.tip_count
FROM b
WHERE Users.user_ID = b.user_ID;

--b
With c AS (
SELECT Tip.business_ID, COUNT(business_ID) AS num_tips
FROM Tip
GROUP BY Tip.business_ID),
random_business AS (
    SELECT business_ID
    FROM Tip
    ORDER BY RANDOM()
    LIMIT 10
)
SELECT Business.business_ID
FROM Business
JOIN random_business rb ON rb.business_ID = Business.business_ID
JOIN c on c.business_ID = Business.business_ID
WHERE Business.num_tips = c.num_tips;

With d AS (
SELECT Tip.user_ID, COUNT(user_ID) AS tip_count
FROM Tip
GROUP BY Tip.user_ID),
random_user AS (
    SELECT user_ID
    FROM Tip
    ORDER BY RANDOM()
    LIMIT 10
)
SELECT Users.user_ID
FROM Users
JOIN random_user ru ON ru.user_ID = Users.user_ID
JOIN d on d.user_ID = Users.user_ID
WHERE Users.tip_count = d.tip_count;
