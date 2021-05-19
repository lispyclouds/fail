## Fail

Ideas to fail better with Clojure

### Requirements
- Java 11+
- Clojure [CLI](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools)
- [Babashka](https://babashka.org/)
- [PostgreSQL](https://www.postgresql.org/download/) 12+

### Running tests
- Install a recent version of docker
- `bb test` to execute all the tests

### Building the jar
- `bb compile` to have the `fail.jar` which can be executed directly

### Running
- `clojure -M -m fail.main` or `java -jar fail.jar` to start the app on `7777`
- Make sure a PostgreSQL instance is running on `localhost:5432`
- API reference:
  - `GET /health`: runs the health checks
  - `GET /contacts`: lists all known contacts
  - `POST /contacts`: adds a new contact with the `name` and `phone` as POST body
  - `GET /search?name=someone`: searches with typo tolerance names in the contacts
