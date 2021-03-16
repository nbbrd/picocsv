# picocsv - lightweight CSV library for Java 

[![Download](https://img.shields.io/github/release/nbbrd/picocsv.svg)](https://github.com/nbbrd/picocsv/releases/latest)

This Java library handles CSV content.  
While directly usable, it is designed to be the core foundation of other libraries.

Key points:
- lightweight library with no dependency
- designed to be embedded into other libraries as an external dependency or [as source](https://github.com/nbbrd/picocsv/blob/develop/src/main/java/nbbrd/picocsv/Csv.java)
- has a module-info that makes it compatible with [JPMS](https://www.baeldung.com/java-9-modularity) 
- Java 7 minimum requirement

Features:
- reads/writes CSV from/to character streams
- provides a minimalist low-level API
- does not interpret content
- does not correct invalid files
- follows the [RFC4180](https://tools.ietf.org/html/rfc4180) specification
- supports custom line separator

Read example:

```java
StringReader input = new StringReader("...");
try (Csv.Reader reader = Csv.Reader.of(Csv.Format.DEFAULT, Csv.ReaderOptions.DEFAULT, input, Csv.DEFAULT_CHAR_BUFFER_SIZE)) {
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
StringWriter output = new StringWriter();
try (Csv.Writer writer = Csv.Writer.of(Csv.Format.DEFAULT, Csv.WriterOptions.DEFAULT, output, Csv.DEFAULT_CHAR_BUFFER_SIZE)) {
  writer.writeField("...");
  writer.writeEndOfLine();
}
```

Maven setup:

```xml
<dependency>
  <groupId>com.github.nbbrd.picocsv</groupId>
  <artifactId>picocsv</artifactId>
  <version>LATEST_VERSION</version>
</dependency>
```
