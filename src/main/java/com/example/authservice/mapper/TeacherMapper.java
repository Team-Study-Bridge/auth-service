package com.example.authservice.mapper;

import com.example.authservice.dto.TeacherSummaryDTO;
import com.example.authservice.model.Teacher;
import com.example.authservice.type.TeacherStatus;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface TeacherMapper {

    void insertTeacher(
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("bio") String bio,
            @Param("category") String category,
            @Param("profileImageUrl") String profileImageUrl,
            @Param("resumeFileUrl") String resumeFileUrl
    );

    boolean existsByUserId(@Param("userId") Long userId);

    void updateTeacherStatus(@Param("id") Long id, @Param("teacherStatus") TeacherStatus teacherStatus);

    List<TeacherSummaryDTO> findTeachersWithPaging(@Param("offset") int offset,
                                                   @Param("size") int size,
                                                   @Param("status") TeacherStatus teacherStatus);

    int countTeachers(@Param("status") TeacherStatus teacherStatus);

    Teacher findByUserId(@Param("userId") Long userId);
}
