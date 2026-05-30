CREATE DATABASE IF NOT EXISTS drivepilot_car_rental DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE drivepilot_car_rental;

CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(100),
  real_name VARCHAR(50),
  id_card VARCHAR(30),
  driver_license_no VARCHAR(50),
  status VARCHAR(20) NOT NULL,
  role VARCHAR(20) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  INDEX idx_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS car_category (
  category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_name VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS store (
  store_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_name VARCHAR(100) NOT NULL,
  city VARCHAR(50) NOT NULL,
  address VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  business_hours VARCHAR(100),
  status VARCHAR(20) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS car (
  car_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  car_name VARCHAR(100) NOT NULL,
  brand VARCHAR(50) NOT NULL,
  model VARCHAR(50) NOT NULL,
  category_id BIGINT,
  plate_number VARCHAR(30) NOT NULL,
  store_id BIGINT,
  price_per_day DECIMAL(10,2) NOT NULL,
  deposit DECIMAL(10,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  mileage INT,
  description TEXT,
  version BIGINT,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  INDEX idx_car_brand (brand),
  INDEX idx_car_status (status),
  CONSTRAINT fk_car_category FOREIGN KEY (category_id) REFERENCES car_category(category_id),
  CONSTRAINT fk_car_store FOREIGN KEY (store_id) REFERENCES store(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS car_image (
  image_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  car_id BIGINT NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  is_main BIT NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  CONSTRAINT fk_car_image_car FOREIGN KEY (car_id) REFERENCES car(car_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rental_order (
  order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  car_id BIGINT NOT NULL,
  pickup_store_id BIGINT NOT NULL,
  return_store_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  rental_days INT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  deposit_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(30) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  INDEX idx_order_status (status),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_order_car FOREIGN KEY (car_id) REFERENCES car(car_id),
  CONSTRAINT fk_order_pickup_store FOREIGN KEY (pickup_store_id) REFERENCES store(store_id),
  CONSTRAINT fk_order_return_store FOREIGN KEY (return_store_id) REFERENCES store(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_order (
  payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  payment_no VARCHAR(50) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  pay_amount DECIMAL(10,2) NOT NULL,
  pay_type VARCHAR(20) NOT NULL,
  pay_status VARCHAR(20) NOT NULL,
  transaction_no VARCHAR(100),
  pay_time DATETIME,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES rental_order(order_id),
  CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contract (
  contract_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_no VARCHAR(50) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  contract_url VARCHAR(255) NOT NULL,
  sign_status VARCHAR(20) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  CONSTRAINT fk_contract_order FOREIGN KEY (order_id) REFERENCES rental_order(order_id),
  CONSTRAINT fk_contract_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS comment_record (
  comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  car_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  score INT NOT NULL,
  content TEXT,
  status VARCHAR(20) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(user_id),
  CONSTRAINT fk_comment_car FOREIGN KEY (car_id) REFERENCES car(car_id),
  CONSTRAINT fk_comment_order FOREIGN KEY (order_id) REFERENCES rental_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS maintenance_record (
  record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  car_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  description TEXT,
  cost DECIMAL(10,2),
  record_time DATETIME NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  CONSTRAINT fk_maintenance_car FOREIGN KEY (car_id) REFERENCES car(car_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
