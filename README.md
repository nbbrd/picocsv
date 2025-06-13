# picocsv - unusual CSV library for Java

[![Download](https://img.shields.io/github/release/nbbrd/picocsv.svg)](https://github.com/nbbrd/picocsv/releases/latest)
[![Changes](https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fnbbrd%2Fpicocsv%2Fbadges%2Funreleased-changes.json)](https://github.com/nbbrd/picocsv/blob/develop/CHANGELOG.md)

**Picocsv is an unusual CSV library designed to be embedded in other libraries.**  
While it can be used directly, it's main purpose is to be the core foundation of those other libraries.  
For a more user-friendly CSV library, you should have a look at the fast and well-documented [FastCSV library](https://github.com/osiegmar/FastCSV/).

👍 Key points:

- lightweight library with no dependency (~25KB)
- very fast (cf. [benchmark](https://github.com/osiegmar/JavaCsvBenchmarkSuite)) and efficient (no heap memory allocation)
- designed to be embedded into other libraries
  as [an external dependency](https://search.maven.org/artifact/com.github.nbbrd.picocsv/picocsv)
  or [as a single-file source](https://github.com/nbbrd/picocsv/blob/develop/src/main/java/nbbrd/picocsv/Csv.java)
- has a module-info that makes it compatible with [JPMS](https://www.baeldung.com/java-9-modularity)
- compatible with GraalVM Native Image (genuine Java, no reflection, no bytecode manipulation)
- can be easily shaded
- Java 8 minimum requirement

🚀 Features:

- [reads/writes](#readwrite) CSV from/to character streams
- provides a minimalist [null-free](#null-free-api) low-level API
- does not interpret content
- does not correct invalid files
- follows the [RFC4180](https://tools.ietf.org/html/rfc4180) specification
- supports [custom line separator, field delimiter, quoting character and comment character](#custom-formats)
- supports custom quoting strategy
- supports unicode characters

> [!IMPORTANT]
> Note that the `Csv.Format#acceptMissingField` option must be set to `false` to closely follow the RFC4180 specification.
> The default value is currently `true` but will be reversed in the next major release.

## Features

### Read/Write

picocsv provides a low-level API to read and write CSV files from/to character streams.  
This API follows the [try-with-resources statement](https://www.baeldung.com/java-try-with-resources)
and closes the underlying character stream after use.

#### Reading character streams

The reading is done by the `Csv.Reader` class and has the following characteristics:

- it is instantiated by the `Csv.Reader.of(Csv.Format, Csv.ReaderOptions, java.io.Reader)` factory method
- its options are defined by the `Csv.ReaderOptions` class

Typical reader instantiation and usage:

```java
try (java.io.Reader chars = ...) {
  try (Csv.Reader reader = Csv.Reader.of(Csv.Format.DEFAULT, Csv.ReaderOptions.DEFAULT, chars)) {
    ...
  }
}
```

Basic reading 1️⃣ of all fields 2️⃣ skipping comments 3️⃣:

```java
while (reader.readLine()) {      // 1️⃣
  if (!reader.isComment()) {     // 3️⃣
    while (reader.readField()) { // 2️⃣
      CharSequence field = reader;
      ...
    }
  }
}
```

Configuring reading options:

```java
Csv.ReaderOptions strict = Csv.ReaderOptions.builder().lenientSeparator(false).build();
```

#### Writing character streams

The writing is done by the `Csv.Writer` class and has the following characteristics:

- it is instantiated by the `Csv.Writer.of(Csv.Format, Csv.WriterOptions, java.io.Writer)` factory method
- its options are defined by the `Csv.WriterOptions` class

Typical writer instantiation and usage:

```java
try (java.io.Writer chars = ...) {
  try (Csv.Writer writer = Csv.Writer.of(Csv.Format.DEFAULT, Csv.WriterOptions.DEFAULT, chars)) {
    ...
  }
}
```

Basic writing 1️⃣ of some fields 2️⃣ and comments 3️⃣:

```java
writer.writeComment("Some comment"); // 3️⃣
writer.writeField("Some field");     // 2️⃣
writer.writeEndOfLine();             // 1️⃣
```

Configuring writing options:

```java
Csv.WriterOptions customOptions = Csv.WriterOptions.builder().maxCharsPerField(1024).build();
```

### Null-free API

picocsv provides a null-free API that accepts null parameters and returns non-null values.

```java
writer.writeComment(null); // same as `csv.writeComment("")`
writer.writeField(null); // same as `csv.writeField("")`
```

### Custom formats

Custom formats are defined by the `Csv.Format` object:

| Option       | Description       | Default Value |
|--------------|-------------------|---------------|
| `#separator` | Line separator    | `\r\n`        |
| `#delimiter` | Field delimiter   | `,`           |
| `#quote`     | Quoting character | `"`           |
| `#comment`   | Comment character | `#`           |

```java
Csv.Format tsv = Csv.Format.builder().delimiter('\t').build();
Csv.Format embedded = Csv.Format.builder().delimiter('=').separator(",").build();
```

## Cookbook

### Readable/Appendable

picocsv only supports `java.io.Reader`/`java.io.Writer` as input/output for performance reasons.
However, it is still possible to use `Readable`/`Appendable` by wrapping them in adapters.
See [`Cookbook#asCharReader(Readable)`](https://github.com/nbbrd/picocsv/blob/develop/src/test/java/_demo/Cookbook.java) and [`Cookbook#asCharWriter(Appendable)`](https://github.com/nbbrd/picocsv/blob/develop/src/test/java/_demo/Cookbook.java).

### Disabling comments

Comments can be disabled by setting the `Csv.Format#comment` option to the null character `\0`.
```java
Csv.Format noComment = Csv.Format.builder().comment('\0').build();
```

> [!NOTE]
> Note that this might lead to problems since binary data is allowed in [RFC-4180-bis](https://fastcsv.org/architecture/interpretation/#status-of-the-rfc).
> It will be fixed in a future release.

### Skipping comments

Comments can be skipped by using the `Csv.Reader#isComment()` method.
See [`Cookbook#skipComments(Csv.Reader)`](https://github.com/nbbrd/picocsv/blob/develop/src/test/java/_demo/Cookbook.java).
```java
while (reader.readLine()) {
    if (!reader.isComment()) {
        while (reader.readField()) { ... }
    }
}
```

### Skipping empty lines

Empty lines are valid lines represented by a single empty field in RFC-4180.  
However, it is still possible to skip them by using the `Csv.Format#acceptMissingField` option.

```java
Csv.Format format = Csv.Format.builder().acceptMissingField(true).build();
try (Csv.Reader reader = ...) {
    while (reader.readLine()) {
        if (!reader.readField()) {
            continue; // 💡 line without field => empty line
        }
        do { ... } while (reader.readField());
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
