package com.example.authservice.type.handler;

import com.example.authservice.type.TeacherStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;

public class TeacherStatusTypeHandler extends BaseTypeHandler<TeacherStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, TeacherStatus status, JdbcType jdbcType) throws SQLException {
        ps.setString(i, status.name());
    }

    @Override
    public TeacherStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : TeacherStatus.valueOf(value);
    }

    @Override
    public TeacherStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : TeacherStatus.valueOf(value);
    }

    @Override
    public TeacherStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : TeacherStatus.valueOf(value);
    }
}
