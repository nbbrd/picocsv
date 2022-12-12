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
- Java 7 minimum requirement

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
Reader reader=...
        try(Csv.Reader csv=Csv.Reader.of(Csv.Format.DEFAULT,Csv.ReaderOptions.DEFAULT,reader,Csv.DEFAULT_CHAR_BUFFER_SIZE)){
        while(csv.readLine()){
        if(!csv.isComment()){
        while(csv.readField()){
        CharSequence field=csv;
        ...
        }
        }
        }
        }
```

Configuring reading options:

```java
Csv.ReaderOptions strict=Csv.ReaderOptions.builder().lenientSeparator(false).build();
```

### Write examples

Basic writing of some fields and comments:

```java
Writer writer=...
        try(Csv.Writer csv=Csv.Writer.of(Csv.Format.DEFAULT,Csv.WriterOptions.DEFAULT,writer,Csv.DEFAULT_CHAR_BUFFER_SIZE)){
        csv.writeComment("Some comment");
        csv.writeField("Some field");
        csv.writeEndOfLine();
        }
```

### Custom format

```java
Csv.Format tsv=Csv.Format.builder().delimiter('\t').build();
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
