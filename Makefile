# Short names for the documented build, demo, PDF, and packaging commands.
.PHONY: help test verify run compose-up compose-down docs package

help:
	@echo "Targets: test, verify, run, compose-up, compose-down, docs, package"

test:
	mvn --batch-mode --no-transfer-progress test

verify:
	mvn --batch-mode --no-transfer-progress verify

run:
	mvn spring-boot:run

compose-up:
	docker compose up --build

compose-down:
	docker compose down

docs:
	./scripts/build-docs.sh

package:
	./scripts/package-submission.sh

