## v3.4-rc1 (2015-03-02)

### Added
* Added a new implementation for the position sensor which uses the FusedLocationProvider of Google Play Services via SensePrefs.Location.FUSED_PROVIDER. The goal accuracy/battery consumption can be set via SensePrefs.Location.FUSED_PROVIDER_PRIORITY.

### Deprecated
* The position sensor using the Android framework location API's via SensePrefs.Location.GPS|WIFI|AUTO_GPS

### Dependency
* Google Play services client library 

## v3.3.2 (2015-03-02)

### Changed
* Updated the NoiseSensor, removed automatic gain control by selecting the VOICE_RECOGNITION audio input source

<!---
## Templates

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security
-->
