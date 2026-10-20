GRADLE      := ./gradlew
JAR         := lwjgl3/build/libs/2DGame-1.0.0.jar
SERVER_MAIN := io.github.game2d.server.ServerLauncher
HOST        ?= localhost

# On macOS, without this option, StartupHelper restarts the game in a new process and loses the server address.
ifeq ($(shell uname),Darwin)
CLIENT_FLAGS := -XstartOnFirstThread
endif

# The game needs Java 21: use the first Java >= 21 found (JAVA_HOME, PATH, then the ones installed by Gradle).
JAVA ?= $(shell for j in "$$JAVA_HOME/bin/java" java $$HOME/.gradle/jdks/*/Contents/Home/bin/java $$HOME/.gradle/jdks/*/*/Contents/Home/bin/java $$HOME/.gradle/jdks/*/bin/java $$HOME/.gradle/jdks/*/*/bin/java; do \
	v=$$("$$j" -version 2>&1 | head -n 1 | sed -E 's/[^"]*"([0-9]+).*/\1/'); \
	if [ "$$v" -ge 21 ] 2>/dev/null; then echo "$$j"; break; fi; done)

.PHONY: help solo server client duo build test doc clean check-java

help:
	@echo "Available commands:"
	@echo "  make solo                  solo game (against a still target)"
	@echo "  make server                multiplayer server (ports 54555 TCP / 54777 UDP)"
	@echo "  make client [HOST=address] client connected to the server (localhost by default)"
	@echo "  make duo                   two players on this computer: server + 2 windows"
	@echo "  make test                  runs the tests"
	@echo "  make doc                   generates the Javadoc (core/build/docs/javadoc/index.html)"
	@echo "  make build                 builds the game ($(JAR))"
	@echo "  make clean                 deletes the build files"

solo:
	$(GRADLE) lwjgl3:run

doc:
	$(GRADLE) core:javadoc

build:
	$(GRADLE) lwjgl3:jar

check-java:
	@if [ -z "$(JAVA)" ]; then echo "Java 21 not found: install it, or run 'make solo' first so that Gradle downloads it."; exit 1; fi

server: build check-java
	$(JAVA) -cp $(JAR) $(SERVER_MAIN)

client: build check-java
	$(JAVA) $(CLIENT_FLAGS) -jar $(JAR) $(HOST)

duo: build check-java
	@$(JAVA) -cp $(JAR) $(SERVER_MAIN) & server=$$!; \
	trap 'kill $$server 2>/dev/null' EXIT INT TERM; \
	sleep 2; \
	$(JAVA) $(CLIENT_FLAGS) -jar $(JAR) localhost & first=$$!; \
	$(JAVA) $(CLIENT_FLAGS) -jar $(JAR) localhost & second=$$!; \
	wait $$first $$second

test:
	$(GRADLE) core:test

clean:
	$(GRADLE) clean
