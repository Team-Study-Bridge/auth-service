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
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("bio") String bio,
            @Param("category") String category,
            @Param("profileImage") String profileImage,
            @Param("resumeFile") String resumeFile
    );

    boolean existsByUserId(@Param("userId") Long userId);

    void updateTeacherStatus(@Param("userId") Long userId, @Param("teacherStatus") TeacherStatus teacherStatus);

    List<TeacherSummaryDTO> findTeachersWithPaging(@Param("offset") int offset,
                                                   @Param("size") int size,
                                                   @Param("teacherStatus") TeacherStatus teacherStatus);

    int countTeachers(@Param("teacherStatus") TeacherStatus teacherStatus);

    Teacher findByUserId(@Param("userId") Long userId);

    String findTeacherByName(@Param("userId") Long userId);

    TeacherStatus findTeacherStatus(@Param("userId") Long userId);

    InstructorProfileResponseDTO findInstructorProfileByUserId(@Param("userId") Long userId);
}
