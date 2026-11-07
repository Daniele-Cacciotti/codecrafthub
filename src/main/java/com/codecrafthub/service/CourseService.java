package com.codecrafthub.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.codecrafthub.model.Course;
import com.codecrafthub.model.Course.Status;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class CourseService {
    private final Path dataFile = Path.of("courses.json").toAbsolutePath();
    private final ObjectMapper mapper;

    public CourseService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    void initializeDataFile() throws IOException {
        if (Files.notExists(dataFile)) {
            writeCourses(List.of());
        }
    }

    public synchronized List<Course> getAllCourses() throws IOException {
        return readCourses();
    }

    public synchronized Course getCourse(long id) throws IOException {
        return findCourse(readCourses(), id);
    }

    // Keep each read, change, and write together so requests cannot overwrite each other.
    public synchronized Course addCourse(String name, String description, LocalDate targetDate, Status status)
            throws IOException {
        List<Course> courses = readCourses();
        long id = courses.stream().mapToLong(Course::id).max().orElse(0) + 1;
        Course course = new Course(id, name, description, targetDate, status, Instant.now());
        courses.add(course);
        writeCourses(courses);
        return course;
    }

    public synchronized Course updateCourse(long id, String name, String description,
            LocalDate targetDate, Status status) throws IOException {
        List<Course> courses = readCourses();
        Course existing = findCourse(courses, id);

        // The lab allows updating a single field. Keep values that were omitted.
        Course updated = new Course(existing.id(),
                Objects.requireNonNullElse(name, existing.name()),
                Objects.requireNonNullElse(description, existing.description()),
                Objects.requireNonNullElse(targetDate, existing.targetDate()),
                Objects.requireNonNullElse(status, existing.status()),
                existing.createdAt());
        courses.set(courses.indexOf(existing), updated);
        writeCourses(courses);
        return updated;
    }

    public synchronized void deleteCourse(long id) throws IOException {
        List<Course> courses = readCourses();
        courses.remove(findCourse(courses, id));
        writeCourses(courses);
    }

    private Course findCourse(List<Course> courses, long id) {
        return courses.stream()
                .filter(course -> course.id() == id)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Course with ID " + id + " was not found"));
    }

    private List<Course> readCourses() throws IOException {
        // Let read errors reach the controller so bad data is never treated as an empty list.
        List<Course> courses = mapper.readerFor(new TypeReference<List<Course>>() {})
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readValue(dataFile.toFile());
        if (courses == null || courses.stream().anyMatch(Objects::isNull)) {
            throw new IOException("courses.json must contain an array of courses");
        }
        return courses;
    }

    private void writeCourses(List<Course> courses) throws IOException {
        // Write a complete temporary file first, then replace the saved file.
        Path temporaryFile = Files.createTempFile(dataFile.getParent(), "courses-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporaryFile.toFile(), courses);
            try {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
