#!/bin/bash
set -euo pipefail

DATA_URL="https://raw.githubusercontent.com/datasciencedojo/datasets/master/titanic.csv"
CSV_PATH="/var/lib/mysql-files/titanic.csv"

echo "Downloading Titanic dataset..."
curl -sSL "$DATA_URL" -o "$CSV_PATH"

chmod 644 "$CSV_PATH"

echo "Loading Titanic dataset into MySQL..."
mysql --local-infile=1 -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
USE analytics;
LOAD DATA INFILE '$CSV_PATH'
INTO TABLE passengers
FIELDS TERMINATED BY ','
    ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(
  PassengerId,
  Survived,
  Pclass,
  Name,
  Sex,
  @AgeStr,        -- read Age into a variable
  SibSp,
  Parch,
  Ticket,
  Fare,
  Cabin,
  Embarked
)
SET Age = NULLIF(@AgeStr, '');
SQL

echo "Passenger data import completed."