# haruhibot-server

A Java 21 / Spring Boot QQ bot using a NapCat reverse WebSocket connection, with an optional Vue 2 WebUI.

## Build

Use JDK 21 and Maven 3.8+. Install frontend dependencies with npm ci in webui/.

- Windows: build.bat
- Linux/macOS: sh build.sh
- Backend only: build-back.bat or sh build-back.sh
- Tests: mvn clean test

Build scripts skip tests. Backend-only packaging uses existing webui/dist, so run a full build after frontend changes. Extract target/haruhibotServer.zip and run start.bat or sh start.sh. Configure NapCat to connect to ws://{ip}:{port}/api/ws. WebUI credentials live in config/webui.properties.

## Layout

The backend is a single Maven module with feature packages under com.haruhi.botserver.features. Build scripts live in scripts/, runtime scripts in scripts/runtime/, and the distribution descriptor in src/assembly/package.xml.

See [architecture and migration notes](docs/architecture.md) and [configuration documentation](docs/config-redesign.md). Run a clean build after upgrading the package structure. The legacy logging.level.com.haruhi.botServer setting remains readable; new configurations use logging.level.com.haruhi.botserver.
