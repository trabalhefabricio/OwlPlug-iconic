# Building OwlPlug With VST2 Support

## Important: VST2 SDK Headers Required ⚠️

**VST2 support is enabled** - you need VST2 SDK headers to build the native components.

**What you need:**
1. Get `aeffect.h` and `aeffectx.h` files (see below for where to find them)
2. Place them in `owlplug-host/external/vst2_sdk/pluginterfaces/vst2.x/`
3. Then build the project

**Where to get VST2 SDK headers:**
- Old VST3 SDK (from before June 2018), for example: vstsdk3610_11_06_2018_build_37
- Old JUCE version 5.3.2
- Your own backup if you downloaded VST2 SDK before

**Plugin support:**
- ✅ VST2 - enabled (needs SDK headers)
- ✅ VST3 - works
- ✅ AU - works  
- ✅ LV2 - works

**Don't have VST2 SDK?** You can disable VST2 support (see bottom of this guide) to build without it.

## Building the Native Host Component

The native host component is a library file that helps the app scan and load plugins. You need to build this separately from the main Java app.

### Prerequisites

1. **VST2 SDK Headers** (required):
   - Get `aeffect.h` and `aeffectx.h`
   - Place in `owlplug-host/external/vst2_sdk/pluginterfaces/vst2.x/`
   - See section above for where to find them

2. **JUCE and Projucer**: Download Projucer first
   ```bash
   ./build/download-projucer.sh
   ```

3. **Platform-specific build tools**:
   - **Linux**: GCC/G++, make, and development libraries
   - **macOS**: Xcode and Command Line Tools
   - **Windows**: Visual Studio 2019 or later with C++ tools

4. **Java Development Kit**: JDK 25 or later
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
3. Verify that VST2, VST3, AU, and LV2 plugins are detected
4. **Note**: All plugin formats should work with this build (if you provided VST2 SDK headers)

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
- Make sure you have the VST2 SDK headers in `owlplug-host/external/vst2_sdk/pluginterfaces/vst2.x/`
- Required files: `aeffect.h` and `aeffectx.h`
- If you don't have them, see the top of this guide for where to get them

## Disabling VST2 Support (Optional)

**Note:** VST2 is currently enabled. If you can't get the SDK headers, you can disable it.

To build without VST2 support:

1. **Edit configuration file** `owlplug-host/src/main/juce/OwlPlugHost.jucer`:
   - Change `JUCE_PLUGINHOST_VST="1"` to `JUCE_PLUGINHOST_VST="0"`
   - Remove VST2 SDK paths from all platform configurations

2. **Edit another file** `owlplug-host/src/main/juce/JuceLibraryCode/AppConfig.h`:

2. **Edit another file** `owlplug-host/src/main/juce/JuceLibraryCode/AppConfig.h`:
   - Change `#define JUCE_PLUGINHOST_VST 1` to `#define JUCE_PLUGINHOST_VST 0`

3. **Rebuild everything**:
   - Run the build scripts again (see above)

After disabling VST2, the app will work with VST3, AU, and LV2 plugins, but not VST2 plugins.
