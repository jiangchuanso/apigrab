package com.gitee.apigrab.persistence.entity.enums;

/**
 * Target column types.
 *
 * <p>A deliberately small set.
 *
 * <p>{@code VARCHAR} is a bounded string needing a length; {@code STRING} is <strong>long text</strong>
 * and takes no length — MySQL {@code LONGTEXT}, PostgreSQL {@code TEXT}, Oracle {@code CLOB},
 * SQL Server {@code NVARCHAR(MAX)}. {@code STRING} therefore <strong>cannot be a primary key</strong>
 * (MySQL rejects a TEXT/BLOB key without a prefix length).
 *
 * <p>Removed in 2026-10-14: {@code INTEGER} / {@code INT} (superseded by {@code BIGINT}),
 * {@code NUMBER} (superseded by {@code DECIMAL}), {@code DATETIME} (superseded by
 * {@code TIMESTAMP}), {@code TEXT} (superseded by {@code STRING}, which is also long text).
 *
 * <p><strong>Migration note:</strong> {@code APIGRAB_FIELD_MAPPING.target_type} has no type handler, so
 * MyBatis resolves it by enum name. A database migrated before this change may still hold the old
 * names and would fail to load; run once:
 * <pre>
 * UPDATE APIGRAB_FIELD_MAPPING SET target_type='BIGINT'    WHERE target_type IN ('INTEGER','INT');
 * UPDATE APIGRAB_FIELD_MAPPING SET target_type='DECIMAL'   WHERE target_type='NUMBER';
 * UPDATE APIGRAB_FIELD_MAPPING SET target_type='TIMESTAMP' WHERE target_type='DATETIME';
 * UPDATE APIGRAB_FIELD_MAPPING SET target_type='STRING'    WHERE target_type='TEXT';
 * </pre>
 */
public enum TargetType {
    VARCHAR, STRING, BIGINT, DECIMAL, DATE, TIMESTAMP, BOOLEAN
}
