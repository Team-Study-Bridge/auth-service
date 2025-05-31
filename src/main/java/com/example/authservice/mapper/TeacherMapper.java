package com.example.authservice.mapper;

import com.example.authservice.dto.InstructorProfileResponseDTO;
import com.example.authservice.dto.TeacherSummaryDTO;
import com.example.authservice.model.Teacher;
import com.example.authservice.type.TeacherStatus;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface TeacherMapper {

    void insertTeacher(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("bio") String bio,
            @Param("category") String category,
            @Param("profileImage") String profileImage,
            @Param("resumeFile") String resumeFile
    );

    boolean existsById(@Param("id") Long id);

    void updateTeacherStatus(@Param("id") Long id, @Param("teacherStatus") TeacherStatus teacherStatus);

    List<TeacherSummaryDTO> findTeachersWithPaging(@Param("offset") int offset,
                                                   @Param("size") int size,
                                                   @Param("teacherStatus") TeacherStatus teacherStatus);

    int countTeachers(@Param("teacherStatus") TeacherStatus teacherStatus);

    Teacher findById(@Param("id") Long userId);

    String findTeacherByName(@Param("id") Long id);

    TeacherStatus findTeacherStatus(@Param("id") Long id);

    InstructorProfileResponseDTO findInstructorProfileById(@Param("id") Long id);
}
