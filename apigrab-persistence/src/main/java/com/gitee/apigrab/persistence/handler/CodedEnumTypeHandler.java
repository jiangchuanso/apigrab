package com.gitee.apigrab.persistence.handler;

import com.gitee.apigrab.persistence.entity.enums.CodedEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * MyBatis handler mapping a {@link CodedEnum} to/from its integer code (TINYINT column).
 * MyBatis instantiates it via the {@code (Class)} constructor with the property type.
 */
public class CodedEnumTypeHandler<E extends Enum<E> & CodedEnum> extends BaseTypeHandler<E> {

    private final Map<Integer, E> codeMap = new HashMap<>();

    public CodedEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        for (E value : type.getEnumConstants()) {
            codeMap.put(value.getCode(), value);
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : codeMap.get(code);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : codeMap.get(code);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : codeMap.get(code);
    }
}
