# Quiz Application

REST API for quizzes. Users take timed quizzes and get a score, admins manage quizzes, questions and categories. Java 21, Spring Boot, Spring Data JPA, MySQL, Gradle. A simple web UI is in `src/main/resources/static`.

## Run

1. Create the database:
   ```sql
   CREATE DATABASE Quiz CHARACTER SET utf8mb4;
   ```
2. Set the MySQL password with the `DB_PASSWORD` environment variable, or create `src/main/resources/application-local.yml` (not committed):
   ```yaml
   spring:
     datasource:
       password: your_password
   ```
3. Start the app:
   ```
   gradlew.bat bootRun --args="--spring.profiles.active=local"
   ```
   On Linux/macOS use `./gradlew bootRun`.

The app runs on http://localhost:6769. On the first start it creates sample quizzes and a default admin account.

## Main endpoints

- `POST /api/auth/register`, `/login`, `/logout`; `GET /api/auth/me`
- `GET /api/quizzes?title=&categoryId=&difficulty=`
- `POST /api/users/{userId}/quizzes/{quizId}/attempts`: start a quiz
- `PUT /api/users/{userId}/attempts/{id}/questions/{questionId}/answer`: answer
- `PATCH /api/users/{userId}/attempts/{id}/finish`: finish and get the score
- `GET /api/users/{userId}/history`, `/statistics`, `/favorites`
- `GET /api/leaderboard`
- `/api/admin/**`: statistics, all results, popular quizzes

## Known issues

Work in progress: management endpoints are not yet protected by an admin check, and the default admin password is hard-coded.