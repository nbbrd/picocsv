# picocsv - lightweight CSV library for Java

[![Download](https://img.shields.io/github/release/nbbrd/picocsv.svg)](https://github.com/nbbrd/picocsv/releases/latest)

This Java library handles CSV content.  
While directly usable, it is designed to be the core foundation of other libraries.

Key points:

- lightweight library with no dependency
- fast and efficient (no heap memory allocation)
- designed to be embedded into other libraries
  as [an external dependency](https://search.maven.org/artifact/com.github.nbbrd.picocsv/picocsv)
  or [as a single-file source](https://github.com/nbbrd/picocsv/blob/develop/src/main/java/nbbrd/picocsv/Csv.java)
- has a module-info that makes it compatible with [JPMS](https://www.baeldung.com/java-9-modularity)
- Java 8 minimum requirement

Features:

- reads/writes CSV from/to character streams
- provides a minimalist low-level API
- does not interpret content
- does not correct invalid files
- follows the [RFC4180](https://tools.ietf.org/html/rfc4180) specification
- supports custom line separator
- supports comment character

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
