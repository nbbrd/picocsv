# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Fixed

## [2.1.0] - 2021-10-11

This is a feature release of **picocsv**.   
picocsv follows [semantic versioning](http://semver.org/).

### Added
- Add native support of comment character [#58](https://github.com/nbbrd/picocsv/issues/58)

## [2.0.0] - 2021-03-16

This is a major release of **picocsv**.   
picocsv follows [semantic versioning](http://semver.org/).

### Added
- Add support of custom line separator
- Enforce use of charBufferSize
- Improve read performance by 20%

### Changed
- Simplify API (not compatible with v1.x)
- Modify compilation to target JDK7
- Modify Maven groupId
- Deploy to Maven-Central instead of jfrog

### Fixed
- Fix missing empty-last-field

## [1.3.0] - 2020-12-01

This is a feature release of **picocsv**.   
picocsv follows [semantic versioning](http://semver.org/).

### Added
- Add BlockSize API to deal with system-specific block sizes
- Add Parsing#maxCharsPerField option to prevent out-of-memory crash
- Add some examples

### Changed
- Modify source code to be compatible with JDK7

## [1.2.0] - 2020-04-15

This is a feature release of **picocsv**.   
picocsv follows [semantic versioning](http://semver.org/).

### Added
- Add lenient parsing of line separator

## [1.1.0] - 2019-11-25

This is a feature release of **picocsv**.   
picocsv follows [semantic versioning](http://semver.org/).

### Added
- Add support of null field as empty string
- Add support of quoted single empty field

### Fixed
- Fix implementation of CharSequence in Csv.Reader

## [1.0.1] - 2019-11-19

This is a bugfix release of **picocsv**.   
picocsv follows [semantic versioning](http://semver.org/).

### Fixed
- Fix parsing of first empty field

## [1.0.0] - 2019-11-18

This is the initial release of **picocsv**.   
picocsv follows [semantic versioning](http://semver.org/).

### Added
- Initial release

[Unreleased]: https://github.com/nbbrd/picocsv/compare/v2.1.0...HEAD
[2.1.0]: https://github.com/nbbrd/picocsv/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/nbbrd/picocsv/compare/v1.3.0...v2.0.0
[1.3.0]: https://github.com/nbbrd/picocsv/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/nbbrd/picocsv/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/nbbrd/picocsv/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/nbbrd/picocsv/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/nbbrd/picocsv/releases/tag/v1.0.0