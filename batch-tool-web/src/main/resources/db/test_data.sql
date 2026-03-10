-- =====================================================
-- 测试数据初始化脚本
-- 用于测试规则组执行、并发执行等功能
-- =====================================================

-- 1. 创建测试数据库
CREATE DATABASE IF NOT EXISTS `migrate_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `migrate_db`;

-- 2. 创建测试数据表

-- 表1: 用户表
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 表2: 订单表
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    `pay_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 表3: 订单明细表
DROP TABLE IF EXISTS `t_order_item`;
CREATE TABLE `t_order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '数量',
    `unit_price` DECIMAL(10,2) NOT NULL COMMENT '单价',
    `total_price` DECIMAL(12,2) NOT NULL COMMENT '总价',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 表4: 商品表
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架',
    `description` TEXT COMMENT '描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 表5: 操作日志表
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `module` VARCHAR(50) NOT NULL COMMENT '模块',
    `operation` VARCHAR(100) NOT NULL COMMENT '操作',
    `method` VARCHAR(200) DEFAULT NULL COMMENT '方法',
    `request_params` TEXT COMMENT '请求参数',
    `response_result` TEXT COMMENT '响应结果',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    `error_msg` TEXT COMMENT '错误信息',
    `duration` BIGINT DEFAULT 0 COMMENT '耗时(毫秒)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_module` (`module`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 3. 插入测试数据

-- 插入用户数据 (100条)
INSERT INTO `t_user` (`username`, `email`, `phone`, `status`) VALUES
('user001', 'user001@test.com', '13800000001', 1),
('user002', 'user002@test.com', '13800000002', 1),
('user003', 'user003@test.com', '13800000003', 1),
('user004', 'user004@test.com', '13800000004', 1),
('user005', 'user005@test.com', '13800000005', 1),
('user006', 'user006@test.com', '13800000006', 0),
('user007', 'user007@test.com', '13800000007', 1),
('user008', 'user008@test.com', '13800000008', 1),
('user009', 'user009@test.com', '13800000009', 1),
('user010', 'user010@test.com', '13800000010', 1);

-- 插入商品数据 (20条)
INSERT INTO `t_product` (`product_name`, `category_id`, `price`, `stock`, `status`, `description`) VALUES
('iPhone 15 Pro', 1, 8999.00, 100, 1, 'Apple iPhone 15 Pro 256GB'),
('MacBook Pro 14', 1, 14999.00, 50, 1, 'Apple MacBook Pro 14英寸 M3芯片'),
('iPad Pro 12.9', 1, 9299.00, 80, 1, 'Apple iPad Pro 12.9英寸'),
('AirPods Pro 2', 1, 1899.00, 200, 1, 'Apple AirPods Pro 第二代'),
('Apple Watch Ultra', 1, 6299.00, 60, 1, 'Apple Watch Ultra 2'),
('小米14 Pro', 2, 4999.00, 150, 1, '小米14 Pro 骁龙8 Gen3'),
('小米平板6 Pro', 2, 2499.00, 100, 1, '小米平板6 Pro 11英寸'),
('Redmi K70', 2, 2499.00, 200, 1, 'Redmi K70 骁龙8 Gen2'),
('华为Mate 60 Pro', 3, 6999.00, 80, 1, '华为Mate 60 Pro 麒麟9000S'),
('华为MatePad Pro', 3, 4999.00, 60, 1, '华为MatePad Pro 13.2英寸'),
('OPPO Find X7', 4, 5999.00, 70, 1, 'OPPO Find X7 Ultra'),
('vivo X100 Pro', 4, 4999.00, 90, 1, 'vivo X100 Pro 天玑9300'),
('一加12', 5, 4299.00, 120, 1, '一加12 骁龙8 Gen3'),
('三星S24 Ultra', 6, 9699.00, 40, 1, 'Samsung Galaxy S24 Ultra'),
('索尼Xperia 1 V', 6, 8499.00, 30, 1, 'Sony Xperia 1 V'),
('联想ThinkPad X1', 7, 12999.00, 25, 1, 'ThinkPad X1 Carbon Gen 11'),
('戴尔XPS 15', 7, 13999.00, 35, 1, 'Dell XPS 15 9530'),
('华硕ROG Phone 8', 8, 5999.00, 45, 1, 'ASUS ROG Phone 8 Pro'),
('机械革命钛坦', 7, 7999.00, 55, 1, '机械革命钛坦16 Pro'),
('外星人m18', 7, 24999.00, 15, 1, 'Alienware m18 R2');

-- 插入订单数据 (50条)
INSERT INTO `t_order` (`order_no`, `user_id`, `total_amount`, `pay_amount`, `status`) VALUES
('ORD202401010001', 1, 8999.00, 8999.00, 3),
('ORD202401010002', 2, 14999.00, 14499.00, 3),
('ORD202401020001', 3, 1899.00, 1899.00, 3),
('ORD202401020002', 1, 11298.00, 10798.00, 3),
('ORD202401030001', 4, 4999.00, 4999.00, 3),
('ORD202401030002', 5, 6999.00, 6999.00, 3),
('ORD202401040001', 6, 2499.00, 2499.00, 1),
('ORD202401040002', 7, 5999.00, 5999.00, 2),
('ORD202401050001', 8, 4299.00, 4299.00, 3),
('ORD202401050002', 9, 9699.00, 9699.00, 3),
('ORD202401060001', 10, 8499.00, 8499.00, 3),
('ORD202401060002', 1, 12999.00, 12999.00, 3),
('ORD202401070001', 2, 13999.00, 13999.00, 3),
('ORD202401070002', 3, 5999.00, 5999.00, 3),
('ORD202401080001', 4, 7999.00, 7999.00, 3),
('ORD202401080002', 5, 24999.00, 24499.00, 3),
('ORD202401090001', 6, 9299.00, 9299.00, 0),
('ORD202401090002', 7, 6299.00, 6299.00, 1),
('ORD202401100001', 8, 4999.00, 4999.00, 3),
('ORD202401100002', 9, 2499.00, 2499.00, 3);

-- 插入订单明细数据
INSERT INTO `t_order_item` (`order_id`, `product_id`, `product_name`, `quantity`, `unit_price`, `total_price`) VALUES
(1, 1, 'iPhone 15 Pro', 1, 8999.00, 8999.00),
(2, 2, 'MacBook Pro 14', 1, 14999.00, 14999.00),
(3, 4, 'AirPods Pro 2', 1, 1899.00, 1899.00),
(4, 1, 'iPhone 15 Pro', 1, 8999.00, 8999.00),
(4, 4, 'AirPods Pro 2', 1, 1899.00, 1899.00),
(4, 5, 'Apple Watch Ultra', 1, 399.00, 399.00),
(5, 6, '小米14 Pro', 1, 4999.00, 4999.00),
(6, 9, '华为Mate 60 Pro', 1, 6999.00, 6999.00),
(7, 8, 'Redmi K70', 1, 2499.00, 2499.00),
(8, 11, 'OPPO Find X7', 1, 5999.00, 5999.00),
(9, 13, '一加12', 1, 4299.00, 4299.00),
(10, 14, '三星S24 Ultra', 1, 9699.00, 9699.00),
(11, 15, '索尼Xperia 1 V', 1, 8499.00, 8499.00),
(12, 16, '联想ThinkPad X1', 1, 12999.00, 12999.00),
(13, 17, '戴尔XPS 15', 1, 13999.00, 13999.00),
(14, 18, '华硕ROG Phone 8', 1, 5999.00, 5999.00),
(15, 19, '机械革命钛坦', 1, 7999.00, 7999.00),
(16, 20, '外星人m18', 1, 24999.00, 24999.00),
(17, 3, 'iPad Pro 12.9', 1, 9299.00, 9299.00),
(18, 5, 'Apple Watch Ultra', 1, 6299.00, 6299.00);

-- 插入操作日志数据 (100条)
INSERT INTO `t_operation_log` (`user_id`, `module`, `operation`, `method`, `ip`, `status`, `duration`) VALUES
(1, '用户管理', '登录', 'UserController.login', '192.168.1.100', 1, 156),
(1, '订单管理', '创建订单', 'OrderController.create', '192.168.1.100', 1, 234),
(2, '用户管理', '登录', 'UserController.login', '192.168.1.101', 1, 89),
(2, '商品管理', '浏览商品', 'ProductController.list', '192.168.1.101', 1, 45),
(3, '用户管理', '登录', 'UserController.login', '192.168.1.102', 1, 112),
(3, '订单管理', '查询订单', 'OrderController.query', '192.168.1.102', 1, 67),
(4, '用户管理', '登录', 'UserController.login', '192.168.1.103', 1, 98),
(4, '商品管理', '加入购物车', 'CartController.add', '192.168.1.103', 1, 123),
(5, '用户管理', '登录', 'UserController.login', '192.168.1.104', 1, 145),
(5, '订单管理', '取消订单', 'OrderController.cancel', '192.168.1.104', 1, 178),
(1, '用户管理', '修改密码', 'UserController.updatePassword', '192.168.1.100', 1, 234),
(2, '订单管理', '创建订单', 'OrderController.create', '192.168.1.101', 1, 345),
(3, '商品管理', '收藏商品', 'ProductController.favorite', '192.168.1.102', 1, 56),
(4, '订单管理', '支付订单', 'OrderController.pay', '192.168.1.103', 1, 456),
(5, '用户管理', '退出登录', 'UserController.logout', '192.168.1.104', 1, 23),
(6, '用户管理', '登录', 'UserController.login', '192.168.1.105', 0, 89),
(7, '用户管理', '登录', 'UserController.login', '192.168.1.106', 1, 134),
(8, '商品管理', '搜索商品', 'ProductController.search', '192.168.1.107', 1, 267),
(9, '订单管理', '创建订单', 'OrderController.create', '192.168.1.108', 1, 389),
(10, '用户管理', '登录', 'UserController.login', '192.168.1.109', 1, 156);

-- =====================================================
-- 切回主数据库，插入测试规则组和规则数据
-- =====================================================

USE `polardbx_tools`;  -- 假设主数据库名为 polardbx_tools，请根据实际情况修改

-- 插入测试规则组
INSERT INTO `rule_group` (`id`, `name`, `description`, `source_host`, `source_port`, `source_user`, `source_password`, 
    `target_host`, `target_port`, `target_user`, `target_password`, `status`) VALUES
(1, 'migrate_db测试组', 'migrate_db数据库迁移测试规则组', 
    '127.0.0.1', 3306, 'root', 'encrypted_password_here',  -- 请替换为实际的加密密码
    '127.0.0.1', 3306, 'root', 'encrypted_password_here',  -- 请替换为实际的加密密码
    1);

-- 插入测试迁移规则（基于 migrate_db 的5张表）
INSERT INTO `migration_rule` (`group_id`, `rule_name`, `source_database`, `source_table`, `target_database`, `target_table`, 
    `operation_type`, `columns`, `where_condition`, `producer_count`, `consumer_count`, `batch_size`, `status`) VALUES
-- 规则1: 导出用户表
(1, '导出用户表', 'migrate_db', 't_user', NULL, NULL, 
    'EXPORT', NULL, NULL, 2, 2, 500, 1),

-- 规则2: 导出订单表
(1, '导出订单表', 'migrate_db', 't_order', NULL, NULL, 
    'EXPORT', NULL, NULL, 2, 2, 500, 1),

-- 规则3: 导出订单明细表
(1, '导出订单明细表', 'migrate_db', 't_order_item', NULL, NULL, 
    'EXPORT', NULL, NULL, 2, 2, 500, 1),

-- 规则4: 导出商品表
(1, '导出商品表', 'migrate_db', 't_product', NULL, NULL, 
    'EXPORT', NULL, NULL, 2, 2, 500, 1),

-- 规则5: 导出操作日志表
(1, '导出操作日志表', 'migrate_db', 't_operation_log', NULL, NULL, 
    'EXPORT', NULL, NULL, 2, 2, 500, 1);

-- =====================================================
-- 可选：创建目标数据库用于导入测试
-- =====================================================

CREATE DATABASE IF NOT EXISTS `migrate_db_target` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 提示信息
SELECT '==============================================' AS '';
SELECT '测试数据初始化完成!' AS '';
SELECT '==============================================' AS '';
SELECT '数据库: migrate_db' AS '';
SELECT '- t_user (用户表): 10条数据' AS '';
SELECT '- t_order (订单表): 20条数据' AS '';
SELECT '- t_order_item (订单明细表): 20条数据' AS '';
SELECT '- t_product (商品表): 20条数据' AS '';
SELECT '- t_operation_log (操作日志表): 20条数据' AS '';
SELECT '' AS '';
SELECT '规则组: migrate_db测试组 (ID=1)' AS '';
SELECT '规则: 5条导出规则' AS '';
SELECT '==============================================' AS '';
