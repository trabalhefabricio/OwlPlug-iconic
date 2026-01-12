# Building OwlPlug (Simple Guide)

## Quick Start - Builds Without Special Files! ✅

**Good news:** This project is set up to build successfully **without needing VST2 SDK headers**.

**What this means:**
- ✅ **You can build right away** - no extra files needed
- ✅ VST3, AU, and LV2 plugins will work
- ❌ VST2 plugins won't work (but you can enable them later if needed)

**If you need VST2 support:** Skip to the "Enabling VST2 Support" section at the bottom.

## Building the Native Host Component

The native host component is a library file that helps the app scan and load plugins. You need to build this separately from the main Java app.

### Prerequisites

1. **JUCE and Projucer**: Download Projucer first
   ```bash
   ./build/download-projucer.sh
   ```

2. **Platform-specific build tools**:
   - **Linux**: GCC/G++, make, and development libraries
   - **macOS**: Xcode and Command Line Tools
   - **Windows**: Visual Studio 2019 or later with C++ tools

3. **Java Development Kit**: JDK 25 or later
   - Set `JAVA_HOME` environment variable to your JDK installation

### Build Commands

#### Linux
```bash
./build/build-host-linux.sh
```

The build output will be located at:
```
owlplug-host/src/main/juce/Builds/LinuxMakefile/build/libowlplug-host.so
```

#### macOS
```bash
./build/build-host-osx.sh
```

The build output will be located at:
```
owlplug-host/src/main/juce/Builds/MacOSX/build/Release/libowlplug-host-1.5.0.dylib
```

#### Windows
```bash
./build/build-host-win.sh
```

The build output will be located at:
```
owlplug-host/src/main/juce/Builds/VisualStudio2019/x64/Release/owlplug-host-1.5.0.dll
```

## Replacing Files in Existing Installation

After building the native component, you need to replace the library file in your existing OwlPlug installation.

### Finding the Installation Directory

**Windows:**
- Default location: `C:\Program Files\OwlPlug\`
- The native library should be in the `app` subdirectory

**macOS:**
- Default location: `/Applications/OwlPlug.app/`
- Right-click the app and select "Show Package Contents"
- Navigate to `Contents/app/`

**Linux:**
- Installed via `.deb`: `/opt/owlplug/` or `/usr/lib/owlplug/`
- Check: `dpkg -L owlplug` to see all installed files

### Locating the Native Library in Installation

The native library file will be named:
- **Linux**: `libowlplug-host.so` or `libowlplug-host-1.5.0.so`
- **macOS**: `libowlplug-host-1.5.0.dylib` or `libowlplug-host.dylib`
- **Windows**: `owlplug-host-1.5.0.dll` or `owlplug-host.dll`

Look for it in:
```
<installation-directory>/app/
```

### Backup and Replace

1. **Backup the original library** (recommended):
   ```bash
   # Example for Linux
   cp /opt/owlplug/app/libowlplug-host.so /opt/owlplug/app/libowlplug-host.so.backup
   ```

2. **Copy the newly built library**:
   ```bash
   # Example for Linux
   sudo cp owlplug-host/src/main/juce/Builds/LinuxMakefile/build/libowlplug-host.so /opt/owlplug/app/
   ```

3. **Set proper permissions** (Linux/macOS):
   ```bash
   # Example for Linux
   sudo chmod 755 /opt/owlplug/app/libowlplug-host.so
   ```

4. **Restart OwlPlug** and verify it works

### Verification

After replacing the library:
1. Launch OwlPlug
2. Try scanning a plugin directory
3. Verify that VST3, AU, and LV2 plugins are detected
4. **Note**: VST2 plugins won't be detected unless you enabled VST2 support (see below)

## Building the Complete Application

If you want to build the entire OwlPlug application (not just the native component):

```bash
# Build native components first (see above)
./build/build-host-linux.sh    # or build-host-osx.sh or build-host-win.sh

# Build Java application
mvn clean install

# Build runnable JAR
cd owlplug-client
mvn clean install spring-boot:repackage

# Run OwlPlug
mvn spring-boot:run
```

## Troubleshooting

### "Library not found" or "JNI error"
- Ensure the library file is in the correct location
- Check that file permissions are correct (755 for executable)
- Verify the library was built for the correct architecture (x64)

### Build fails with VST2 SDK errors
- This shouldn't happen with the current configuration (VST2 is disabled)
- If you see VST2 errors, the configuration may have been changed
- Make sure `JUCE_PLUGINHOST_VST="0"` in the `.jucer` file

## Enabling VST2 Support (For Advanced Users)

**Note:** VST2 is currently disabled, so you don't need this to build.

If you want to enable VST2 plugin support:

1. **Get the special files** (`aeffect.h` and `aeffectx.h`):
   - Download old VST3 SDK (from before June 2018) OR
   - Download old JUCE version 5.3.2
   - Extract and find these two files

2. **Put them in the right place**:
   - Copy the files to: `owlplug-host/external/vst2_sdk/pluginterfaces/vst2.x/`

3. **Change the settings**:
   - Edit `owlplug-host/src/main/juce/OwlPlugHost.jucer`:
     - Change `JUCE_PLUGINHOST_VST="0"` to `JUCE_PLUGINHOST_VST="1"`
     - Add VST2 SDK path back to headerPath for all platforms

4. **Edit another file**:
   - Edit `owlplug-host/src/main/juce/JuceLibraryCode/AppConfig.h`:
     - Change `#define JUCE_PLUGINHOST_VST 0` to `#define JUCE_PLUGINHOST_VST 1`

5. **Rebuild everything**:
   - Run the build scripts again (see above)

For more detailed technical instructions, see `owlplug-host/external/vst2_sdk/README.md`.
