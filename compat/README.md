# Compatibility Harness

This directory contains the Java compatibility smoke runner used by the manual GitHub Actions workflow.

## Repository Secrets

The workflow expects these repository-level GitHub Actions secrets:

- `BAMBOO_COMPAT_PRODUCT_LICENSE`
  Atlassian Bamboo Data Center host timebomb license from the official Atlassian page.
- `BAMBOO_COMPAT_APP_LICENSE`
  Optional. Reserved for future licensed-app smoke tests. The current plugin build does not require it.

## Local Dry Run

You can run the harness locally once a plugin JAR exists in `target/`:

```bash
./mvnw clean package
./mvnw -q -f compat/bamboo-specs/pom.xml \
  -Dcompat.rootDir="$(pwd)" \
  -Dcompat.version=10.2.5 \
  -Dcompat.productLicense='...' \
  exec:java
```
