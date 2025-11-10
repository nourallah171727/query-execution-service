## Asynchronous Query Execution API with Caching
A RESTful Spring Boot application that executes analytical SQL queries asynchronously.
Queries are persisted in MySQL and cached in an in-memory `ConcurrentHashMap` for faster retrieval.
## ✨ Features
- Asynchronous query execution for long-running jobs
- REST API endpoints to submit, list, and check job status
- Lightweight in-memory caching
- Persistent MySQL storage
- restoring interrupted queries due to app crashes for example
## ⚙️ Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 9.5.0+

### Database setup

1. Start MySQL.
2. Create a database called analytics where we will create 3 tables :
   ```sql
   CREATE DATABASE analytics;
   USE analytics;
   ```
   ```sql
   CREATE TABLE `query` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `text` text,
    PRIMARY KEY (`id`)
    )
   ```
  then 
  ```sql
  CREATE TABLE `query_job` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `query_id` bigint NOT NULL,
  `status` enum('QUEUED','RUNNING','SUCCEEDED','FAILED') NOT NULL,
  `error` text,
  PRIMARY KEY (`id`),
  KEY `query_id` (`query_id`),
  CONSTRAINT `query_job_ibfk_1` FOREIGN KEY (`query_id`) REFERENCES `query` (`id`)
  )
  ```
  then 
  ```sql
  CREATE TABLE `passengers` (
  `PassengerId` int NOT NULL,
  `Survived` tinyint DEFAULT NULL,
  `Pclass` int DEFAULT NULL,
  `Name` varchar(100) DEFAULT NULL,
  `Sex` varchar(10) DEFAULT NULL,
  `Age` float DEFAULT NULL,
  `SibSp` int DEFAULT NULL,
  `Parch` int DEFAULT NULL,
  `Ticket` varchar(50) DEFAULT NULL,
  `Fare` float DEFAULT NULL,
  `Cabin` varchar(20) DEFAULT NULL,
  `Embarked` varchar(5) DEFAULT NULL,
  PRIMARY KEY (`PassengerId`)
  )
  ```
3. (Optional: if you want to run the tests and see results) import the csv file of the titanic dataset into the passenger's table from here : https://github.com/datasciencedojo/datasets/blob/master/titanic.csv
4. make sure to create a mySQL with username = "user" and password="analytic":
   ```sql
    CREATE USER 'user'@'localhost' IDENTIFIED BY 'analytic';
   ```
5. make sure to grant this account full privilege on query table and query_job table , but only read right on the Passengers table :
   ```sql
   -- Full privileges on query and query_job tables
    GRANT ALL PRIVILEGES ON analytics.query TO 'user'@'localhost';
    GRANT ALL PRIVILEGES ON analytics.query_job TO 'user'@'localhost';

    -- Read-only access to passengers table    
    GRANT SELECT ON analytics.passengers TO 'user'@'localhost';

    FLUSH PRIVILEGES;
   ```
6. either run the app through terminal using
      ```bash
      ./mvnw spring-boot:run
      ```
      for bash-based shells or just use your IDE
