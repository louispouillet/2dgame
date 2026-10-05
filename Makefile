GRADLE      := ./gradlew
JAR         := lwjgl3/build/libs/2DGame-1.0.0.jar
SERVER_MAIN := io.github.game2d.server.ServerLauncher
HOST        ?= localhost

# Sur macOS, sans cette option, StartupHelper relance le jeu dans un nouveau processus et perd l'adresse du serveur.
ifeq ($(shell uname),Darwin)
CLIENT_FLAGS := -XstartOnFirstThread
endif

# Le jeu demande Java 21 : on prend le premier Java >= 21 trouve (JAVA_HOME, PATH, puis ceux installes par Gradle).
JAVA ?= $(shell for j in "$$JAVA_HOME/bin/java" java $$HOME/.gradle/jdks/*/Contents/Home/bin/java $$HOME/.gradle/jdks/*/*/Contents/Home/bin/java $$HOME/.gradle/jdks/*/bin/java $$HOME/.gradle/jdks/*/*/bin/java; do \
	v=$$("$$j" -version 2>&1 | head -n 1 | sed -E 's/[^"]*"([0-9]+).*/\1/'); \
	if [ "$$v" -ge 21 ] 2>/dev/null; then echo "$$j"; break; fi; done)

.PHONY: help solo server client duo build test clean check-java

help:
	@echo "Commandes disponibles :"
	@echo "  make solo                  partie solo (un adversaire immobile)"
	@echo "  make server                serveur multijoueur (ports 54555 TCP / 54777 UDP)"
	@echo "  make client [HOST=adresse] client connecte au serveur (localhost par defaut)"
	@echo "  make duo                   test a deux sur cet ordinateur : serveur + 2 fenetres"
	@echo "  make test                  lance les tests"
	@echo "  make build                 construit le jeu ($(JAR))"
	@echo "  make clean                 supprime les fichiers de compilation"

solo:
	$(GRADLE) lwjgl3:run

build:
	$(GRADLE) lwjgl3:jar

check-java:
	@if [ -z "$(JAVA)" ]; then echo "Java 21 introuvable : installe-le ou lance d'abord 'make solo' pour que Gradle le telecharge."; exit 1; fi

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
