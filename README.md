# picocsv - lightweight CSV library for Java 

[![Download](https://img.shields.io/github/release/nbbrd/picocsv.svg)](https://github.com/nbbrd/picocsv/releases/latest)

This Java library provides a reader and a writer for CSV files.

Key points:
- lightweight library with no dependency
- Java 8 minimum requirement
- designed to be embedded into other libraries as an external dependency or as source
- has a module-info that makes it compatible with [JPMS](https://www.baeldung.com/java-9-modularity) 

Features:
- reads/writes CSV from/to files and streams
- provides a simple low-level API
- does not interpret content
- does not correct invalid files
- follows the [RFC4180](https://tools.ietf.org/html/rfc4180) specification
- is very fast

Read example:

```java
Path input = ...;
try (Csv.Reader reader = Csv.Reader.of(input, StandardCharsets.UTF_8, Csv.Format.RFC4180)) {
  while (reader.readLine()) {
    while (reader.readField()) {
      CharSequence field = reader;
      ...
    }
  }
}
```

Write example:

```java
Path output = ...;
try (Csv.Writer writer = Csv.Writer.of(output, StandardCharsets.UTF_8, Csv.Format.RFC4180)) {
  writer.writeField("...");
  writer.writeEndOfLine();
}
```

## Setup

```xml
<dependencies>
  <dependency>
    <groupId>be.nbb.rd</groupId>
    <artifactId>picocsv</artifactId>
    <version>LATEST_VERSION</version>
  </dependency>
</dependencies>

<repositories>
  <repository>
    <id>oss-jfrog-artifactory-releases</id>
    <url>https://oss.jfrog.org/artifactory/oss-release-local</url>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
  </repository>
</repositories>
```
