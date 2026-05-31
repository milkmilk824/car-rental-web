-- =============================================================================
-- DrivePilot Car Rental — MySQL 8 Schema
-- =============================================================================
-- Usage:
--   mysql -u root -p < schema.sql
-- Or run statements block by block in your SQL client.
-- All tables use InnoDB + utf8mb4, matching JPA entity definitions exactly.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS drivepilot_car_rental
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE drivepilot_car_rental;

-- -------------------------------------------------------------------
-- users — 用户表
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  user_id          BIGINT       NOT NULL AUTO_INCREMENT,
  username         VARCHAR(50)  NOT NULL,
  password         VARCHAR(255) NOT NULL,
  phone            VARCHAR(20)  DEFAULT NULL,
  email            VARCHAR(100) DEFAULT NULL,
  real_name        VARCHAR(50)  DEFAULT NULL,
  id_card          VARCHAR(30)  DEFAULT NULL,
  driver_license_no VARCHAR(50) DEFAULT NULL,
  status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  role             VARCHAR(20)  NOT NULL DEFAULT 'USER',
  create_time      DATETIME     NOT NULL,
  update_time      DATETIME     NOT NULL,
  PRIMARY KEY (user_id),
  UNIQUE INDEX idx_user_username (username),
  INDEX idx_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- car_category — 车辆分类
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS car_category (
  category_id   BIGINT       NOT NULL AUTO_INCREMENT,
  category_name VARCHAR(50)  NOT NULL,
  description   VARCHAR(255) DEFAULT NULL,
  create_time   DATETIME     NOT NULL,
  update_time   DATETIME     NOT NULL,
  PRIMARY KEY (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- store — 门店
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS store (
  store_id       BIGINT       NOT NULL AUTO_INCREMENT,
  store_name     VARCHAR(100) NOT NULL,
  city           VARCHAR(50)  NOT NULL,
  address        VARCHAR(255) NOT NULL,
  phone          VARCHAR(20)  DEFAULT NULL,
  business_hours VARCHAR(100) DEFAULT NULL,
  status         VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
  create_time    DATETIME     NOT NULL,
  update_time    DATETIME     NOT NULL,
  PRIMARY KEY (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- store_staff — 门店员工绑定
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS store_staff (
  store_staff_id BIGINT   NOT NULL AUTO_INCREMENT,
  user_id        BIGINT   NOT NULL,
  store_id       BIGINT   NOT NULL,
  create_time    DATETIME NOT NULL,
  update_time    DATETIME NOT NULL,
  PRIMARY KEY (store_staff_id),
  UNIQUE INDEX uk_store_staff_user_store (user_id, store_id),
  INDEX idx_store_staff_user (user_id),
  INDEX idx_store_staff_store (store_id),
  CONSTRAINT fk_store_staff_user  FOREIGN KEY (user_id)  REFERENCES users(user_id),
  CONSTRAINT fk_store_staff_store FOREIGN KEY (store_id) REFERENCES store(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- car — 车辆
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS car (
  car_id        BIGINT        NOT NULL AUTO_INCREMENT,
  car_name      VARCHAR(100)  NOT NULL,
  brand         VARCHAR(50)   NOT NULL,
  model         VARCHAR(50)   NOT NULL,
  category_id   BIGINT        DEFAULT NULL,
  plate_number  VARCHAR(30)   NOT NULL,
  store_id      BIGINT        DEFAULT NULL,
  price_per_day DECIMAL(10,2) NOT NULL,
  deposit       DECIMAL(10,2) NOT NULL,
  status        VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',
  mileage       INT           DEFAULT 0,
  description   TEXT,
  version       BIGINT        DEFAULT NULL,
  create_time   DATETIME      NOT NULL,
  update_time   DATETIME      NOT NULL,
  PRIMARY KEY (car_id),
  INDEX idx_car_brand (brand),
  INDEX idx_car_status (status),
  CONSTRAINT fk_car_category FOREIGN KEY (category_id) REFERENCES car_category(category_id),
  CONSTRAINT fk_car_store    FOREIGN KEY (store_id)    REFERENCES store(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- car_image — 车辆图片
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS car_image (
  image_id    BIGINT       NOT NULL AUTO_INCREMENT,
  car_id      BIGINT       NOT NULL,
  image_url   VARCHAR(255) NOT NULL,
  is_main     BIT          NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL,
  update_time DATETIME     NOT NULL,
  PRIMARY KEY (image_id),
  CONSTRAINT fk_car_image_car FOREIGN KEY (car_id) REFERENCES car(car_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- rental_order — 租赁订单
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rental_order (
  order_id         BIGINT        NOT NULL AUTO_INCREMENT,
  order_no         VARCHAR(50)   NOT NULL,
  user_id          BIGINT        NOT NULL,
  car_id           BIGINT        NOT NULL,
  pickup_store_id  BIGINT        NOT NULL,
  return_store_id  BIGINT        NOT NULL,
  start_time       DATETIME      NOT NULL,
  end_time         DATETIME      NOT NULL,
  rental_days      INT           NOT NULL,
  total_amount     DECIMAL(10,2) NOT NULL,
  deposit_amount   DECIMAL(10,2) NOT NULL,
  status           VARCHAR(30)   NOT NULL DEFAULT 'PENDING_PAYMENT',
  create_time      DATETIME      NOT NULL,
  update_time      DATETIME      NOT NULL,
  PRIMARY KEY (order_id),
  UNIQUE INDEX idx_order_no (order_no),
  INDEX idx_order_status (status),
  CONSTRAINT fk_order_user         FOREIGN KEY (user_id)          REFERENCES users(user_id),
  CONSTRAINT fk_order_car          FOREIGN KEY (car_id)           REFERENCES car(car_id),
  CONSTRAINT fk_order_pickup_store FOREIGN KEY (pickup_store_id)  REFERENCES store(store_id),
  CONSTRAINT fk_order_return_store FOREIGN KEY (return_store_id)  REFERENCES store(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- payment_order — 支付单
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_order (
  payment_id     BIGINT        NOT NULL AUTO_INCREMENT,
  payment_no     VARCHAR(50)   NOT NULL,
  order_id       BIGINT        NOT NULL,
  user_id        BIGINT        NOT NULL,
  pay_amount     DECIMAL(10,2) NOT NULL,
  pay_type       VARCHAR(20)   NOT NULL DEFAULT 'MOCK',
  pay_status     VARCHAR(20)   NOT NULL DEFAULT 'WAITING',
  transaction_no VARCHAR(100)  DEFAULT NULL,
  pay_time       DATETIME      DEFAULT NULL,
  create_time    DATETIME      NOT NULL,
  update_time    DATETIME      NOT NULL,
  PRIMARY KEY (payment_id),
  UNIQUE INDEX idx_payment_no (payment_no),
  CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES rental_order(order_id),
  CONSTRAINT fk_payment_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- contract — 合同
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS contract (
  contract_id  BIGINT       NOT NULL AUTO_INCREMENT,
  contract_no  VARCHAR(50)  NOT NULL,
  order_id     BIGINT       NOT NULL,
  user_id      BIGINT       NOT NULL,
  contract_url VARCHAR(255) NOT NULL,
  sign_status  VARCHAR(20)  NOT NULL DEFAULT 'UNSIGNED',
  create_time  DATETIME     NOT NULL,
  update_time  DATETIME     NOT NULL,
  PRIMARY KEY (contract_id),
  UNIQUE INDEX idx_contract_no (contract_no),
  CONSTRAINT fk_contract_order FOREIGN KEY (order_id) REFERENCES rental_order(order_id),
  CONSTRAINT fk_contract_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- comment_record — 评价记录
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comment_record (
  comment_id  BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL,
  car_id      BIGINT      NOT NULL,
  order_id    BIGINT      NOT NULL,
  score       INT         NOT NULL,
  content     TEXT        DEFAULT NULL,
  status      VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  create_time DATETIME    NOT NULL,
  update_time DATETIME    NOT NULL,
  PRIMARY KEY (comment_id),
  CONSTRAINT fk_comment_user  FOREIGN KEY (user_id)  REFERENCES users(user_id),
  CONSTRAINT fk_comment_car   FOREIGN KEY (car_id)   REFERENCES car(car_id),
  CONSTRAINT fk_comment_order FOREIGN KEY (order_id) REFERENCES rental_order(order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- maintenance_record — 维修保养记录
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS maintenance_record (
  record_id   BIGINT        NOT NULL AUTO_INCREMENT,
  car_id      BIGINT        NOT NULL,
  type        VARCHAR(20)   NOT NULL,
  description TEXT          DEFAULT NULL,
  cost        DECIMAL(10,2) DEFAULT 0.00,
  record_time DATETIME      NOT NULL,
  create_time DATETIME      NOT NULL,
  update_time DATETIME      NOT NULL,
  PRIMARY KEY (record_id),
  CONSTRAINT fk_maintenance_car FOREIGN KEY (car_id) REFERENCES car(car_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
