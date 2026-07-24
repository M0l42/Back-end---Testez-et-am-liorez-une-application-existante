package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class StudentServiceTest {
    private static final Long ID = 1L;
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@bibliotheque.fr";

    @Mock
    private StudentRepository studentRepository;
    @InjectMocks
    private StudentService studentService;

    private Student buildStudent(Long id) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName(FIRST_NAME);
        student.setLastName(LAST_NAME);
        student.setEmail(EMAIL);
        return student;
    }

    @Test
    public void test_create_null_student_throws_IllegalArgumentException() {
        // GIVEN

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> studentService.create(null));
    }

    @Test
    public void test_create_already_exist_email_throws_IllegalArgumentException() {
        // GIVEN
        Student student = buildStudent(null);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buildStudent(ID)));

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> studentService.create(student));
        verify(studentRepository, never()).save(any());
    }

    @Test
    public void test_create_student() {
        // GIVEN
        Student student = buildStudent(null);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(studentRepository.save(any())).thenReturn(buildStudent(ID));

        // WHEN
        studentService.create(student);

        // THEN
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        assertThat(studentCaptor.getValue()).isEqualTo(student);
    }

    @Test
    public void test_findAll_students() {
        // GIVEN
        when(studentRepository.findAll()).thenReturn(List.of(buildStudent(ID)));

        // WHEN
        List<Student> students = studentService.findAll();

        // THEN
        assertThat(students).hasSize(1);
        assertThat(students.get(0).getEmail()).isEqualTo(EMAIL);
    }

    @Test
    public void test_findById_unknown_student_throws_EntityNotFoundException() {
        // GIVEN
        when(studentRepository.findById(ID)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(EntityNotFoundException.class,
                () -> studentService.findById(ID));
    }

    @Test
    public void test_findById_student() {
        // GIVEN
        when(studentRepository.findById(ID)).thenReturn(Optional.of(buildStudent(ID)));

        // WHEN
        Student student = studentService.findById(ID);

        // THEN
        assertThat(student.getId()).isEqualTo(ID);
    }

    @Test
    public void test_update_student() {
        // GIVEN
        Student updated = new Student();
        updated.setFirstName("Jane");
        updated.setLastName("Roe");
        updated.setEmail("jane.roe@bibliotheque.fr");

        when(studentRepository.findById(ID)).thenReturn(Optional.of(buildStudent(ID)));
        when(studentRepository.findByEmail("jane.roe@bibliotheque.fr")).thenReturn(Optional.empty());
        when(studentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        studentService.update(ID, updated);

        // THEN
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        assertThat(studentCaptor.getValue().getId()).isEqualTo(ID);
        assertThat(studentCaptor.getValue().getFirstName()).isEqualTo("Jane");
        assertThat(studentCaptor.getValue().getEmail()).isEqualTo("jane.roe@bibliotheque.fr");
    }

    @Test
    public void test_update_student_keeping_its_own_email() {
        // GIVEN the email is unchanged, the student's own row must not count as a duplicate
        Student updated = buildStudent(null);
        updated.setFirstName("Jane");

        when(studentRepository.findById(ID)).thenReturn(Optional.of(buildStudent(ID)));
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buildStudent(ID)));
        when(studentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        studentService.update(ID, updated);

        // THEN
        verify(studentRepository).save(any());
    }

    @Test
    public void test_update_student_with_email_of_another_student_throws_IllegalArgumentException() {
        // GIVEN
        Student updated = buildStudent(null);
        when(studentRepository.findById(ID)).thenReturn(Optional.of(buildStudent(ID)));
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buildStudent(2L)));

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> studentService.update(ID, updated));
        verify(studentRepository, never()).save(any());
    }

    @Test
    public void test_delete_unknown_student_throws_EntityNotFoundException() {
        // GIVEN
        when(studentRepository.findById(ID)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(EntityNotFoundException.class,
                () -> studentService.delete(ID));
        verify(studentRepository, never()).delete(any());
    }

    @Test
    public void test_delete_student() {
        // GIVEN
        Student student = buildStudent(ID);
        when(studentRepository.findById(ID)).thenReturn(Optional.of(student));

        // WHEN
        studentService.delete(ID);

        // THEN
        verify(studentRepository).delete(student);
    }
}
