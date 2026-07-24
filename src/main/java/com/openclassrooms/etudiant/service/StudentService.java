package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;

    public Student create(Student student) {
        Assert.notNull(student, "Student must not be null");
        log.info("Creating new student");

        assertEmailIsAvailable(student.getEmail(), null);
        return studentRepository.save(student);
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Student findById(Long id) {
        Assert.notNull(id, "Id must not be null");
        return studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id " + id));
    }

    public Student update(Long id, Student student) {
        Assert.notNull(student, "Student must not be null");
        log.info("Updating student {}", id);

        Student existingStudent = findById(id);
        assertEmailIsAvailable(student.getEmail(), id);

        existingStudent.setFirstName(student.getFirstName());
        existingStudent.setLastName(student.getLastName());
        existingStudent.setEmail(student.getEmail());
        return studentRepository.save(existingStudent);
    }

    public void delete(Long id) {
        log.info("Deleting student {}", id);
        studentRepository.delete(findById(id));
    }

    /**
     * The email column is unique: reject a duplicate with a 400 rather than letting the
     * database constraint surface as a 500. On update, the student's own row is not a clash.
     */
    private void assertEmailIsAvailable(String email, Long currentStudentId) {
        studentRepository.findByEmail(email)
                .filter(existingStudent -> !existingStudent.getId().equals(currentStudentId))
                .ifPresent(existingStudent -> {
                    throw new IllegalArgumentException("Student with email " + email + " already exists");
                });
    }

}
