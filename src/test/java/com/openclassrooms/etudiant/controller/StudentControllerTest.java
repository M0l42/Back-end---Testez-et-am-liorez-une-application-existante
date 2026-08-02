package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.StudentRequestDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.StudentRepository;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.service.JwtService;
import com.openclassrooms.etudiant.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class StudentControllerTest {

    private static final String URL = "/api/students";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String LOGIN = "librarian";
    private static final String PASSWORD = "password";

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@bibliotheque.fr";

    @Container
    static MySQLContainer mySQLContainer = new MySQLContainer("mysql:8.0");

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    private String token;

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mySQLContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> mySQLContainer.getUsername());
        registry.add("spring.datasource.password", () -> mySQLContainer.getPassword());
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @BeforeEach
    public void beforeEach() {
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);
        userService.register(user);

        token = jwtService.generateToken(user);
    }

    @AfterEach
    public void afterEach() {
        studentRepository.deleteAll();
        userRepository.deleteAll();
    }

    private StudentRequestDTO buildRequest(String email) {
        StudentRequestDTO studentRequestDTO = new StudentRequestDTO();
        studentRequestDTO.setFirstName(FIRST_NAME);
        studentRequestDTO.setLastName(LAST_NAME);
        studentRequestDTO.setEmail(email);
        return studentRequestDTO;
    }

    private Student persistStudent(String email) {
        Student student = new Student();
        student.setFirstName(FIRST_NAME);
        student.setLastName(LAST_NAME);
        student.setEmail(email);
        return studentRepository.save(student);
    }

    @Test
    public void findAllWithoutTokenIsUnauthorized() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void createWithoutTokenIsUnauthorized() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                        .content(objectMapper.writeValueAsString(buildRequest(EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void findAllWithMalformedTokenIsUnauthorized() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL)
                        .header(AUTH_HEADER, BEARER_PREFIX + "not-a-jwt")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void createStudentWithoutRequiredData() throws Exception {
        // GIVEN
        StudentRequestDTO studentRequestDTO = new StudentRequestDTO();

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .content(objectMapper.writeValueAsString(studentRequestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void createStudentWithInvalidEmail() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .content(objectMapper.writeValueAsString(buildRequest("not-an-email")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void createStudentSuccessful() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .content(objectMapper.writeValueAsString(buildRequest(EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(EMAIL));
    }

    @Test
    public void createAlreadyExistStudent() throws Exception {
        // GIVEN
        persistStudent(EMAIL);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .content(objectMapper.writeValueAsString(buildRequest(EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void findAllStudents() throws Exception {
        // GIVEN
        persistStudent(EMAIL);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL)
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].email").value(EMAIL));
    }

    @Test
    public void findStudentById() throws Exception {
        // GIVEN
        Student student = persistStudent(EMAIL);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL + "/" + student.getId())
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(student.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(FIRST_NAME));
    }

    @Test
    public void findUnknownStudentByIdIsNotFound() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL + "/99999")
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void updateStudentSuccessful() throws Exception {
        // GIVEN
        Student student = persistStudent(EMAIL);
        StudentRequestDTO studentRequestDTO = buildRequest("jane.roe@bibliotheque.fr");
        studentRequestDTO.setFirstName("Jane");

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/" + student.getId())
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .content(objectMapper.writeValueAsString(studentRequestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(student.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("Jane"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("jane.roe@bibliotheque.fr"));
    }

    @Test
    public void updateUnknownStudentIsNotFound() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/99999")
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .content(objectMapper.writeValueAsString(buildRequest(EMAIL)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void deleteStudentSuccessful() throws Exception {
        // GIVEN
        Student student = persistStudent(EMAIL);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.delete(URL + "/" + student.getId())
                        .header(AUTH_HEADER, BEARER_PREFIX + token))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        // THEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL + "/" + student.getId())
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void deleteUnknownStudentIsNotFound() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.delete(URL + "/99999")
                        .header(AUTH_HEADER, BEARER_PREFIX + token))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void searchStudentsMatchesCaseInsensitivePartial() throws Exception {
        // GIVEN
        persistStudent(EMAIL);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL + "/search")
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .param("q", "jo")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                // THEN
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].email").value(EMAIL));
    }

    @Test
    public void searchStudentsWithNoMatchReturnsEmptyArray() throws Exception {
        // GIVEN
        persistStudent(EMAIL);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(URL + "/search")
                        .header(AUTH_HEADER, BEARER_PREFIX + token)
                        .param("q", "zzzz")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                // THEN
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
    }
}
