# picocsv - unusual CSV library for Java

[![Download](https://img.shields.io/github/release/nbbrd/picocsv.svg)](https://github.com/nbbrd/picocsv/releases/latest)
[![Changes](https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fnbbrd%2Fpicocsv%2Fbadges%2Funreleased-changes.json)](https://github.com/nbbrd/picocsv/blob/develop/CHANGELOG.md)

Picocsv is an unusual CSV library designed to be embedded in other libraries.  
While it can be used directly, it's main purpose is to be the core foundation of those other libraries.  
For a more user-friendly CSV library, you should have a look at the fast and well-documented [FastCSV library](https://github.com/osiegmar/FastCSV/).

Key points:

- lightweight library with no dependency (less than 25KB)
- very fast (cf. [benchmark](https://github.com/osiegmar/JavaCsvBenchmarkSuite)) and efficient (no heap memory allocation)
- designed to be embedded into other libraries
  as [an external dependency](https://search.maven.org/artifact/com.github.nbbrd.picocsv/picocsv)
  or [as a single-file source](https://github.com/nbbrd/picocsv/blob/develop/src/main/java/nbbrd/picocsv/Csv.java)
- has a module-info that makes it compatible with [JPMS](https://www.baeldung.com/java-9-modularity)
- compatible with GraalVM Native Image (genuine Java, no reflection, no bytecode manipulation)
- can be easily shaded
- Java 8 minimum requirement

Features:

- reads/writes CSV from/to character streams
- provides a minimalist null-free low-level API
- does not interpret content
- does not correct invalid files
- follows the [RFC4180](https://tools.ietf.org/html/rfc4180) specification
- supports custom line separator, field delimiter, quoting character and comment character
- supports custom quoting strategy
- supports unicode characters

⚠️ _Note that the `Csv.Format#acceptMissingField` option must be set to `false` to closely follow the RFC4180 specification.
The default value is currently `true` but will be reversed in the next major release._

## Examples

### Read examples

Basic reading of all fields skipping comments:

```java
try (java.io.Reader chars = ...; 
        Csv.Reader csv = Csv.Reader.of(Csv.Format.DEFAULT, Csv.ReaderOptions.DEFAULT, chars)) {
  while (csv.readLine()) {
    if (!csv.isComment()) {
      while (csv.readField()) {
        CharSequence field = csv;
        ...
      }
    }
  }
}
```

Configuring reading options:

```java
Csv.ReaderOptions strict = Csv.ReaderOptions.builder().lenientSeparator(false).build();
```

### Write examples

Basic writing of some fields and comments:

```java
try (java.io.Writer chars = ...;
        Csv.Writer csv = Csv.Writer.of(Csv.Format.DEFAULT, Csv.WriterOptions.DEFAULT, chars)) {
  csv.writeComment("Some comment");
  csv.writeField("Some field");
  csv.writeEndOfLine();
}
```

### Custom format

```java
Csv.Format tsv = Csv.Format.builder().delimiter('\t').build();
Csv.Format embedded = Csv.Format.builder().delimiter('=').separator(",").build();
```

### Readable/Appendable

picocsv only supports `java.io.Reader`/`java.io.Writer` as input/output for performance reasons.
However, it is still possible to use `Readable`/`Appendable` by wrapping them in adapters.
See [`Cookbook#asCharReader(Readable)`](https://github.com/nbbrd/picocsv/blob/develop/src/test/java/_demo/Cookbook.java) and [`Cookbook#asCharWriter(Appendable)`](https://github.com/nbbrd/picocsv/blob/develop/src/test/java/_demo/Cookbook.java).

### Disabling comments

Comments can be disabled by setting the `Csv.Format#comment` option to the null character `\0`.
```java
Csv.Format noComment = Csv.Format.builder().comment('\0').build();
```
⚠️ _Note that this might lead to problems since binary data is allowed in RFC-4180-bis.
It will be fixed in a future release._

### Skipping comments

Comments can be skipped by using the `Csv.Reader#isComment()` method.
```java
while (csv.readLine()) {
    if (!csv.isComment()) {
        while (csv.readField()) { ... }
    }
}
```

### Skipping empty lines

Empty lines are valid lines represented by a single empty field in RFC-4180.  
However, it is still possible to skip them by using the `Csv.Format#acceptMissingField` option.
```java
Csv.Format format = Csv.Format.builder().acceptMissingField(true).build();
try (Csv.Reader csv = ...) {
    while (csv.readLine()) {
        if (!csv.readField()) {
            continue; // line without field => empty line
        }
        do { ... } while (csv.readField());
    }
}
```

## Setup

Maven setup:

```xml
<dependency>
    <groupId>com.github.nbbrd.picocsv</groupId>
    <artifactId>picocsv</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

## Developing

This project is written in Java and uses [Apache Maven](https://maven.apache.org/) as a build tool.  
It requires [Java 8 as minimum version](https://whichjdk.com/) and all its dependencies are hosted on [Maven Central](https://search.maven.org/).

The code can be build using any IDE or by just type-in the following commands in a terminal:

```shell
git clone https://github.com/nbbrd/picocsv.git
cd picocsv
mvn clean install
```

## Contributing

Any contribution is welcome and should be done through pull requests and/or issues.

## Licensing

The code of this project is licensed under the [European Union Public Licence (EUPL)](https://joinup.ec.europa.eu/page/eupl-text-11-12).
