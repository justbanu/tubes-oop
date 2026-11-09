JAVA ?= java
JAVAC ?= javac
SOURCES := $(shell find src/main/java -name '*.java')
TESTS := $(shell find src/test/java -name '*.java')

.PHONY: all build run test integration smoke docs clean
all: build
build: build/nangor-blast.jar

build/nangor-blast.jar: $(SOURCES) config/green-garden.map config/green-garden.properties
	mkdir -p build/classes
	$(JAVAC) --release 17 -Xlint:all -Werror -d build/classes $(SOURCES)
	jar --create --file $@ --main-class nangor.Main -C build/classes . -C . config

run: build
	$(JAVA) -jar build/nangor-blast.jar

build/test-classes/.stamp: build/nangor-blast.jar $(TESTS)
	mkdir -p build/test-classes
	$(JAVAC) --release 17 -Xlint:all -Werror -cp build/classes -d build/test-classes $(TESTS)
	touch $@

test: build/test-classes/.stamp
	$(JAVA) -ea -cp build/classes:build/test-classes nangor.core.RulesTest
	$(JAVA) -ea -cp build/classes:build/test-classes nangor.core.IntegrationTest
	$(JAVA) -ea -cp build/classes:build/test-classes nangor.core.CampaignTest

integration: build/test-classes/.stamp
	$(JAVA) -ea -cp build/classes:build/test-classes nangor.core.IntegrationTest
	$(JAVA) -ea -cp build/classes:build/test-classes nangor.core.CampaignTest

smoke: build
	python3 scripts/terminal_smoke.py

docs:
	python3 scripts/build_docs.py

clean:
	rm -rf build target
