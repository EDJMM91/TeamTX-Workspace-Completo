# Optimize Gradle Build and Migrate to Version Catalog

Increase Gradle RAM to 8GB, enable advanced caching, and migrate dependencies to a Version Catalog (`libs.versions.toml`) for better maintainability and performance.

## Proposed Changes

### Performance & Cache Optimization

#### [gradle.properties](file:///D:/OsmAnd-Entorno/android/gradle.properties)

- Increase heap size to 8GB.
- Enable build caching, parallel execution, and file system watching.
- Enable configuration cache (if compatible) to skip the configuration phase for repetitive builds.

```properties
org.gradle.jvmargs=-Xmx8g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.vfs.watch=true
org.gradle.configuration-cache=true
android.enableResourceOptimizations=true
```

---

### Version Catalog Migration

#### [NEW] [libs.versions.toml](file:///D:/OsmAnd-Entorno/android/gradle/libs.versions.toml)

- Centralize all dependency versions and library definitions.

#### [build.gradle](file:///D:/OsmAnd-Entorno/android/build.gradle)

- Update buildscript to use version catalog if applicable (or at least prepare for it).

#### [OsmAnd/build.gradle](file:///D:/OsmAnd-Entorno/android/OsmAnd/build.gradle)

- Refactor to use `libs` references.

#### [OsmAnd-java/build.gradle](file:///D:/OsmAnd-Entorno/android/OsmAnd-java/build.gradle)

- Refactor to use `libs` references.

#### [OsmAnd-shared/build.gradle.kts](file:///D:/OsmAnd-Entorno/android/OsmAnd-shared/build.gradle.kts)

- Refactor to use `libs` references.

---

### Build Protocol Optimizations

- Configuration Cache will reduce "protocol" (startup time).
- VFS Watch will ensure only "active" (changed) files are compiled.

## Verification Plan

### Automated Tests
- Run `./gradlew :OsmAnd:assembleDebug` twice. The second run should be significantly faster (almost instant if nothing changed) due to configuration cache and build cache.
- Command: `gradlew :OsmAnd:assembleDebug --info`

### Manual Verification
- Check the Gradle console output for "Configuration cache entry reused" and "FROM-CACHE" or "UP-TO-DATE" labels on tasks.
