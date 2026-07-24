package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.StudentRequestDTO;
import com.openclassrooms.etudiant.dto.StudentResponseDTO;
import com.openclassrooms.etudiant.mapper.StudentDtoMapper;
import com.openclassrooms.etudiant.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentDtoMapper studentDtoMapper;

    @PostMapping
    public ResponseEntity<StudentResponseDTO> create(@Valid @RequestBody StudentRequestDTO studentRequestDTO) {
        return new ResponseEntity<>(
                studentDtoMapper.toDto(studentService.create(studentDtoMapper.toEntity(studentRequestDTO))),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> findAll() {
        return ResponseEntity.ok(studentDtoMapper.toDtoList(studentService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(studentDtoMapper.toDto(studentService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> update(@PathVariable Long id,
                                                     @Valid @RequestBody StudentRequestDTO studentRequestDTO) {
        return ResponseEntity.ok(
                studentDtoMapper.toDto(studentService.update(id, studentDtoMapper.toEntity(studentRequestDTO))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
