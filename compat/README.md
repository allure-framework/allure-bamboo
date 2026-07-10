# Compatibility Harness

This directory contains the Java compatibility smoke runner used by the manual GitHub Actions workflow.

## Repository Secrets

The workflow expects this repository-level GitHub Actions secret:

- `BAMBOO_COMPAT_PRODUCT_LICENSE`
  Atlassian Bamboo Data Center host timebomb license from the official Atlassian page.

## Local Dry Run

You can run the harness locally once a plugin JAR exists in `target/`:

```bash
./mvnw clean package
./mvnw -q -f compat/bamboo-specs/pom.xml \
  -Dcompat.rootDir="$(pwd)" \
  -Dcompat.version=12.1.9 \
  -Dcompat.productLicense='...' \
  exec:java
```
