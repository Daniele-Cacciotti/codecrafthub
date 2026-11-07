package com.codecrafthub.controller;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.codecrafthub.model.Course;
import com.codecrafthub.model.Course.Status;
import com.codecrafthub.service.CourseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://127.0.0.1:3000")
public class CourseController {
    private static final Logger LOG = LoggerFactory.getLogger(CourseController.class);
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public Map<String, Object> getAllCourses() throws IOException {
        List<Course> courses = courseService.getAllCourses();
        return Map.of("success", true, "count", courses.size(), "courses", courses);
    }

    @GetMapping("/stats")
    public Map<String, Object> getCourseStats() throws IOException {
        List<Course> courses = courseService.getAllCourses();
        Map<String, Integer> byStatus = new LinkedHashMap<>();

        // Start at zero so every status appears even when there are no matching courses.
        for (Status status : Status.values()) {
            byStatus.put(status.label(), 0);
        }
        for (Course course : courses) {
            byStatus.merge(course.status().label(), 1, Integer::sum);
        }

        return Map.of("success", true, "total", courses.size(), "by_status", byStatus);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getCourse(@PathVariable long id) throws IOException {
        return courseResponse(courseService.getCourse(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addCourse(@RequestBody ObjectNode body) throws IOException {
        CourseInput input = readInput(body, true);
        Course course = courseService.addCourse(input.name(), input.description(), input.targetDate(), input.status());
        return ResponseEntity.created(URI.create("/api/courses/" + course.id())).body(courseResponse(course));
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateCourse(@PathVariable long id, @RequestBody ObjectNode body) throws IOException {
        CourseInput input = readInput(body, false);
        Course course = courseService.updateCourse(id, input.name(), input.description(), input.targetDate(), input.status());
        return courseResponse(course);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable long id) throws IOException {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // A JSON tree lets us distinguish omitted fields from fields explicitly set to null.
    private CourseInput readInput(ObjectNode body, boolean required) {
        String name = readText(body, "name", required);
        String description = readText(body, "description", required);
        String targetDate = readText(body, "target_date", required);
        String status = readText(body, "status", required);
        return new CourseInput(name, description, parseDate(targetDate),
                status == null ? null : Status.fromLabel(status));
    }

    private String readText(ObjectNode body, String field, boolean required) {
        JsonNode value = body.get(field);
        if (value == null && !required) {
            return null;
        }
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException(field + " must be a non-blank string");
        }
        return value.textValue();
    }

    private LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        if (value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException exception) {
                // The format is correct, but the date itself does not exist.
            }
        }
        throw new IllegalArgumentException("target_date must be a valid date in YYYY-MM-DD format");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> invalidInput(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> invalidJson() {
        return error(HttpStatus.BAD_REQUEST, "Request body must be a valid JSON object");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> courseNotFound(NoSuchElementException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> storageError(IOException exception) {
        LOG.error("Could not read or write courses.json", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read or write courses.json");
    }

    private Map<String, Object> courseResponse(Course course) {
        return Map.of("success", true, "course", course);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "error", message));
    }

    private record CourseInput(String name, String description, LocalDate targetDate, Status status) {}
}
