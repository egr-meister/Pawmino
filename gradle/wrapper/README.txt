Gradle Wrapper
==============

This project uses the Gradle Wrapper. The binary `gradle-wrapper.jar` is NOT
committed here. Generate it once with an installed Gradle, or let Android Studio
create it automatically when you open the project.

To generate the wrapper jar and scripts manually:

    gradle wrapper --gradle-version 8.9

The provided `gradle-wrapper.properties`, `gradlew`, and `gradlew.bat` are ready
to use once `gradle-wrapper.jar` exists in this folder.

The GitHub Actions workflow generates the wrapper automatically before building.
