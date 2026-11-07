package team.projectpulse.student;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer>, JpaSpecificationExecutor<Student> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select student from Student student where student.id = :studentId")
    Optional<Student> findForUpdate(@Param("studentId") Integer studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select student from Student student
            where student.id = :studentId and exists (
                select section.sectionId from Section section
                where section = student.section and (
                    section.course.courseAdmin.id = :callerId or exists (
                        select instructor.id from Section assignedSection join assignedSection.instructors instructor
                        where assignedSection = section and instructor.id = :callerId
                    )
                )
            )
            """)
    Optional<Student> findManagedForUpdate(@Param("studentId") Integer studentId, @Param("callerId") Integer callerId);

    // Find all students in a section
    List<Student> findBySectionSectionId(Integer sectionId);

    List<Student> findByTeamTeamId(Integer teamId);
}
