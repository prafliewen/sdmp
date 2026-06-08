CREATE TABLE IF NOT EXISTS `dict_item` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT          COMMENT 'primary key',
    `type`       VARCHAR(64)  NOT NULL                          COMMENT 'dict type',
    `key`        VARCHAR(64)  NOT NULL                          COMMENT 'dict key',
    `label`      VARCHAR(128) NOT NULL                          COMMENT 'display label',
    `value`      VARCHAR(255) NULL                              COMMENT 'value',
    `sort`       INT          NOT NULL DEFAULT 0                COMMENT 'sort',
    `enabled`    TINYINT(1)   NOT NULL DEFAULT 1                COMMENT 'enabled',
    `deleted`    TINYINT(1)   NOT NULL DEFAULT 0                COMMENT 'logic delete',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_type_key` (`type`, `key`, `deleted`),
    KEY `idx_dict_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
