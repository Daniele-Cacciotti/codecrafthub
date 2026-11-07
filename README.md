# CodeCraftHub

CodeCraftHub is a local learning tracker for developers. It provides a Spring Boot REST API for creating, viewing, updating, and deleting courses. Each course has a name, description, target completion date, and progress status.

- Course data persists in `courses.json` between runs.
- Jackson handles JSON requests, responses, and file storage.
- The API validates inputs and returns JSON error messages for invalid data, missing courses, and storage errors.

## Installation

1. Install a JDK (Java 17 or later) and Maven. Install `curl` to run the API examples.
2. Open the `codecrafthub` folder in VS Code. Run the commands below from its integrated terminal, in the directory containing `pom.xml`.
3. Check your tools and build the application. Maven downloads the dependencies on the first build.

```bash
java -version
mvn -version
mvn clean package
```

## Run the application

```bash
mvn spring-boot:run
```

You can also start the packaged application after building it.

```bash
java -jar target/codecrafthub-1.0.0.jar
```

The application listens on `127.0.0.1:8080` by default. It creates `courses.json` in the working directory on first launch. Leave the server running while you try the examples in a second terminal.

## API endpoints

| Method | Endpoint | Operation | Success response |
| --- | --- | --- | --- |
| `POST` | `/api/courses` | Create a course | `201 Created` with `success` and `course` |
| `GET` | `/api/courses` | List courses | `200 OK` with `success`, `count`, and `courses` |
| `GET` | `/api/courses/{id}` | Get a course by ID | `200 OK` with `success` and `course` |
| `PUT` | `/api/courses/{id}` | Update the supplied fields | `200 OK` with `success` and `course` |
| `DELETE` | `/api/courses/{id}` | Delete a course | `204 No Content` |

Create requests require `name`, `description`, `target_date`, and `status`. Use a valid calendar date in `YYYY-MM-DD` format and one of the exact status values `Not Started`, `In Progress`, or `Completed`. The server assigns `id` and `created_at`.

### Create a course

```bash
API_URL='http://127.0.0.1:8080/api/courses'

curl -i -X POST "$API_URL" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Spring Boot Basics",
    "description": "Build a simple REST API",
    "target_date": "2027-01-15",
    "status": "Not Started"
  }'
```

The response includes a `Location` header with the course URL. The response body looks like this.

```json
{
  "success": true,
  "course": {
    "id": 1,
    "name": "Spring Boot Basics",
    "description": "Build a simple REST API",
    "target_date": "2027-01-15",
    "status": "Not Started",
    "created_at": "2026-11-07T14:00:00Z"
  }
}
```

### List courses or get one course

Set `COURSE_ID` to the `id` from the create response. The first course in a fresh catalog gets ID `1`.

```bash
COURSE_ID=1
curl -i "$API_URL"
curl -i "$API_URL/$COURSE_ID"
```

### Update a course

A `PUT` can update a single field. Omitted fields keep their current values.

```bash
curl -i -X PUT "$API_URL/$COURSE_ID" \
  -H 'Content-Type: application/json' \
  -d '{"status": "In Progress"}'
```

### Delete the example course

```bash
curl -i -X DELETE "$API_URL/$COURSE_ID"
```

A successful delete returns an empty body. A subsequent `GET` for that ID returns `404 Not Found`.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `mvn` is missing or uses an older Java version | Run `mvn -version` and check the Java version it reports. Put the Maven executable on `PATH` and set `JAVA_HOME` to your JDK directory if needed. |
| Maven cannot find `pom.xml` | Open a terminal in the `codecrafthub` project folder. |
| Port `8080` is in use | Stop the other application or start CodeCraftHub with `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`. Update `API_URL` to use `8081` as well. |
| `GET` returns `"count": 0` | This is expected for an empty catalog. If you already added courses, check that you started the app from the same directory as your `courses.json`. |
| `400 Bad Request` | Check the `error` field. Send a JSON object with `Content-Type: application/json`, use `target_date` in `YYYY-MM-DD` format, and match the capitalization and spacing of the status values. |
| `404 Not Found` | List the courses and check the ID in the URL. |
| `500 Internal Server Error` mentioning `courses.json` | Check the server log. The file must contain a valid JSON array, and the application needs permission to read it and write to its directory. |

