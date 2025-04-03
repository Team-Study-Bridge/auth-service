package com.example.authservice.type.handler;

import com.example.authservice.type.Provider;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;

public class ProviderTypeHandler extends BaseTypeHandler<Provider> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Provider parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public Provider getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : Provider.valueOf(value);
    }

    @Override
    public Provider getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : Provider.valueOf(value);
    }

    @Override
    public Provider getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : Provider.valueOf(value);
    }
}
