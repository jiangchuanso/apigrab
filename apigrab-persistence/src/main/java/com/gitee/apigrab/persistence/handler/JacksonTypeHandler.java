package com.gitee.apigrab.persistence.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.gitee.apigrab.common.util.JsonUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Base MyBatis type handler that serializes/deserializes a value to/from a JSON text column
 * (delegates to {@link JsonUtils} for a consistent JSON configuration).
 */
public abstract class JacksonTypeHandler<T> extends BaseTypeHandler<T> {

    protected abstract TypeReference<T> type();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, JsonUtils.toJson(parameter));
        } catch (Exception e) {
            throw new SQLException("Failed to serialize JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return read(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return read(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return read(cs.getString(columnIndex));
    }

    private T read(String json) throws SQLException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JsonUtils.parse(json, type());
        } catch (Exception e) {
            throw new SQLException("Failed to deserialize JSON: " + e.getMessage(), e);
        }
    }
}
