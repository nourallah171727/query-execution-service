-- Create database and tables required for the application
CREATE DATABASE IF NOT EXISTS analytics;
USE analytics;

CREATE TABLE IF NOT EXISTS `passengers` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `query` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `text` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `query_job` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `query_id` bigint NOT NULL,
  `status` enum('RUNNING','SUCCEEDED','FAILED') NOT NULL,
  `error` text,
  PRIMARY KEY (`id`),
  KEY `query_id` (`query_id`),
  CONSTRAINT `query_job_ibfk_1` FOREIGN KEY (`query_id`) REFERENCES `query` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `users` (username, password_hash, role) VALUES
  ('user', '$2a$10$wvZkc.SOWEdOvBrTLO0i4.VRwNyMsvr1D91k2nAQQogpv4sLRa.e.', 'user'),
  ('admin', '$2a$10$2PqWB9FOGsQ2Pfh0tt8DruaNuR1X4l8u8egsNltZYxHYhH4KZXbca', 'admin')
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- Create application user and grant privileges
CREATE USER IF NOT EXISTS 'admin'@'%' IDENTIFIED BY 'analytics';
GRANT ALL PRIVILEGES ON analytics.* TO 'admin'@'%';
FLUSH PRIVILEGES;
