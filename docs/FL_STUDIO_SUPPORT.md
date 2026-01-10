# FL Studio Project Support

This document describes the FL Studio (.flp) project file support added to OwlPlug.

## Overview

OwlPlug now supports FL Studio project files with full feature parity with Ableton and Reaper support. This includes:

- **Project Discovery**: Automatic detection and scanning of .flp files
- **Metadata Extraction**: Project name, version, creation/modification dates
- **Plugin Analysis**: VST2, VST3, and AU plugin references
- **Plugin Lookup**: Matching project plugins against installed plugins
- **UI Integration**: Full display with FL Studio logo and project details
- **Version Support**: FL Studio 12.x through 24.x and future versions (25+, 26+, etc.)

## Implementation

### Core Components

1. **DawApplication Enum** (`com.owlplug.project.model.DawApplication`)
   - Added `FL_STUDIO` entry

2. **FLStudioParser** (`com.owlplug.project.tasks.discovery.flstudio.FLStudioParser`)
   - Binary parser for .flp format
   - Reads FLhd/FLdt chunks
   - Extracts plugin information from event stream
   - Supports multiple FL Studio versions

3. **FLStudioProjectExplorer** (`com.owlplug.project.tasks.discovery.flstudio.FLStudioProjectExplorer`)
   - Implements ProjectExplorer interface
   - Converts FL plugins to DawPlugin objects
   - Formats version numbers for display

4. **ProjectSyncTask Integration**
   - Automatically scans .flp files alongside .als and .rpp files

5. **UI Components**
   - FL Studio logo icon
   - ApplicationDefaults integration
   - Project info display

### File Format Details

FL Studio uses a binary format with the following structure:

```
FLhd (Header)
- Chunk size (4 bytes, little-endian)
- Format version (2 bytes, little-endian)

FLdt (Data)
- Event stream with plugin information
```

#### Key Event IDs

- `0xC0/0xC9` (192/201): Plugin names
- `0xCA` (202): Plugin paths
- `0xCB` (203): Plugin vendors
- `0x58` (88): Plugin filenames

#### Version Formatting

Version numbers are stored as integers and formatted as:
- `2400` → "24.0" (FL Studio 24.0)
- `2411` → "24.1.1"
- `2100` → "21.0"
- `2008` → "20.8"
- `1234` → "12.3.4"

The version formatting algorithm is future-proof and automatically supports
all current and future FL Studio versions (25+, 26+, etc.).

## Building

### Requirements

- Java 25 (JDK 25)
- Maven 3.6+
- Native build tools for owlplug-host (platform-specific)

### Build Commands

```bash
# Full build
mvn clean install

# Build client only (requires pre-built modules)
cd owlplug-client
mvn clean install spring-boot:repackage

# Run tests
mvn test
```

### CI/CD

The project includes GitHub Actions workflows:

- **main.yml**: Full build pipeline for master/dev branches
- **dev.yml**: Quick build and test for all branches
- **pr-build.yml**: PR validation with test reports

All workflows are configured with Java 25 and will automatically run when changes are pushed.

## Testing

### Unit Tests

Located in `owlplug-client/src/test/java/com/owlplug/project/tasks/discovery/FLStudioProjectExplorerTest.java`

Tests cover:
- File format detection (.flp)
- Project metadata extraction
- Plugin parsing (VST2/VST3)
- Version formatting

### Test Resources

Sample FL Studio project: `owlplug-client/src/test/resources/projects/flstudio/test_project.flp`

Contains:
- FL Studio 20.8 format
- Sample VST2 plugin (TestSynth)
- Sample VST3 plugin (SuperDelay)

## Usage

### User Perspective

1. **Configure Project Directory**
   - Go to Settings → Projects
   - Add directory containing FL Studio projects

2. **Sync Projects**
   - Click "Sync Projects" button
   - FL Studio projects will be discovered alongside Ableton/Reaper

3. **View Project Details**
   - Select project from tree view
   - See all metadata and plugin list
   - Check plugin status (Found/Missing)

4. **Open Projects**
   - Click "Open Project" to launch in FL Studio
   - Click folder icon to open project directory

### Developer Perspective

To extend or modify FL Studio support:

1. **Parser** (`FLStudioParser.java`)
   - Add new event IDs for additional data
   - Modify plugin extraction logic
   - Update version detection

2. **Explorer** (`FLStudioProjectExplorer.java`)
   - Modify metadata extraction
   - Adjust plugin format detection
   - Update version formatting

3. **Constants**
   - Plugin name filters: `EXCLUDED_PLUGIN_NAMES`
   - File extensions: `EXT_*` constants

## Compatibility

### Supported FL Studio Versions

- FL Studio 12.x (legacy)
- FL Studio 20.x 
- FL Studio 21.x
- FL Studio 24.x (latest as of 2024-2025)
- FL Studio 25.x and beyond (future-proof)

**Note:** The binary format parser is designed to handle any FL Studio version
from 12.x onwards, as well as future versions (25+, 26+, etc.), as long as 
Image-Line maintains backward compatibility in the .flp format structure.

### Plugin Formats

- VST2 (.dll, .vst, .so)
- VST3 (.vst3)
- AU (.component) - macOS only

### Limitations

- Binary format parsing may vary with major FL Studio updates
- Native FL Studio plugins are not tracked (only VST/AU)
- Some project metadata may not be available in older formats

## Contributing

When contributing to FL Studio support:

1. Follow existing code patterns from Ableton/Reaper explorers
2. Add tests for new functionality
3. Update this documentation
4. Ensure backward compatibility with older FL Studio versions
5. Run full test suite before submitting PR

## Troubleshooting

### Common Issues

**Problem: FL Studio projects not detected**
- Solution: Ensure .flp file extension (case-insensitive)
- Check project directory is configured in Settings → Projects
- Run "Sync Projects" to scan for new files

**Problem: No plugins found in project**
- Solution: This is normal for projects without VST/VST3/AU plugins
- Native FL Studio plugins are not tracked
- Check if plugins are actually VST-based, not FL native

**Problem: Parse error on specific project**
- Solution: File may be corrupted or use unsupported format
- Try opening in FL Studio first to verify it's valid
- Check OwlPlug logs for detailed error messages
- Report issue with FL Studio version number

**Problem: Wrong plugin count**
- Solution: Parser may filter out duplicates or non-plugin entries
- Some mixer/pattern entries are excluded intentionally
- Check logs for "duplicate plugins removed" message

**Problem: Missing plugin paths**
- Solution: Some FL Studio projects don't store full paths
- Plugin names are still extracted and searchable
- Try resaving project in FL Studio to update metadata

### Performance

**Large Projects**:
- Files up to 500MB are supported
- Parsing time scales with project complexity
- Check logs for parsing statistics

**Slow Scanning**:
- Initial scan indexes all projects
- Subsequent scans are incremental
- Consider excluding temp/backup directories

### Reporting Issues

When reporting FL Studio support issues, include:

1. FL Studio version number
2. OwlPlug version
3. Project file size
4. Error message from logs
5. Sample .flp file if possible

## References

- [FL Studio File Format Research](https://github.com/demberto/PyFLP) (Python implementation)
- [OwlPlug Documentation](https://github.com/DropSnorz/OwlPlug/wiki)
- [Project Structure Documentation](https://github.com/DropSnorz/OwlPlug/wiki/Projects-and-DAW-Support)
