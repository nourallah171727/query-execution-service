#!/bin/bash
set -euo pipefail

DATA_URL="https://raw.githubusercontent.com/datasciencedojo/datasets/master/titanic.csv"
CSV_PATH="/docker-entrypoint-initdb.d/titanic.csv"

echo "Downloading Titanic dataset..."
if command -v curl >/dev/null 2>&1; then
  curl -sSL "$DATA_URL" -o "$CSV_PATH"
elif command -v wget >/dev/null 2>&1; then
  wget -qO "$CSV_PATH" "$DATA_URL"
elif command -v apt-get >/dev/null 2>&1; then
  echo "Installing curl to download dataset..."
  apt-get update -y >/dev/null && apt-get install -y curl >/dev/null
  rm -rf /var/lib/apt/lists/*
  curl -sSL "$DATA_URL" -o "$CSV_PATH"
else
  echo "Neither curl nor wget is available to download the Titanic dataset." >&2
  exit 1
fi

# Import data into passengers table
echo "Loading Titanic dataset into MySQL..."
mysql --local-infile=1 -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-$MYSQL_ROOT_PASSWORD}" <<SQL
USE analytics;
LOAD DATA LOCAL INFILE '$CSV_PATH'
INTO TABLE passengers
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(PassengerId, Survived, Pclass, Name, Sex, Age, SibSp, Parch, Ticket, Fare, Cabin, Embarked);
SQL

echo "Passenger data import completed."
