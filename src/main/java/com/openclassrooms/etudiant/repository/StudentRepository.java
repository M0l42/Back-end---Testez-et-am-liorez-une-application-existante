package com.openclassrooms.etudiant.repository;

import com.openclassrooms.etudiant.entities.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);

    @Query("SELECT s from Student s WHERE " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.lastName)  LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.email)     LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Student> search(@Param("q") String q);
}
