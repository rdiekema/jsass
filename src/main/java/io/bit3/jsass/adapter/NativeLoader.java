package io.bit3.jsass.adapter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;

/**
 * This loader handle the extraction and loading of the shared library files from the jar.
 */
final class NativeLoader {
  private static final Logger LOG = LoggerFactory.getLogger(NativeLoader.class);

  private NativeLoader() {
  }

  /**
   * Load the shared libraries.
   */
  static void loadLibrary() {
    try {
      File dir = Files.createTempDirectory("libjsass-").toFile();
      dir.deleteOnExit();

      if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
        System.load(saveLibrary(dir, "sass"));
      }

      System.load(saveLibrary(dir, "jsass"));
    } catch (Exception exception) {
      LOG.warn(exception.getMessage(), exception);
      throw new LoaderException(exception);
    }
  }

  /**
   * Find the right shared library, depending on the operating system and architecture.
   *
   * @throws UnsupportedOperationException Throw an exception if no native library for this platform
   *                                       was found.
   */
  private static URL findLibraryResource(final String libraryFileName) {
    String osName = System.getProperty("os.name").toLowerCase();
    String osArch = System.getProperty("os.arch").toLowerCase();
    String resourceName = null;

    if (osName.startsWith("win")) {
      resourceName = determineWindowsLibrary(libraryFileName, osName, osArch);
    } else if (osName.startsWith("linux")) {
      resourceName = determineLinuxLibrary(libraryFileName, osName, osArch);
    } else if (osName.startsWith("freebsd")) {
      resourceName = determineFreebsdLibrary(libraryFileName, osName, osArch);
    } else if (osName.startsWith("mac")) {
      resourceName = determineMacLibrary(libraryFileName);
    } else {
      unsupportedPlatform(osName, osArch);
    }

    URL resource = NativeLoader.class.getResource(resourceName);

    if (null == resource) {
      unsupportedPlatform(osName, osArch);
    }

    return resource;
  }

  /**
   * Determine the right windows library depending on the architecture.
   *
   * @param library The library name.
   * @param osName  The operating system name.
   * @param osArch  The system architecture.
   * @return The library resource.
   * @throws UnsupportedOperationException Throw an exception if no native library for this platform
   *                                       was found.
   */
  private static String determineWindowsLibrary(
      final String library,
      final String osName,
      final String osArch
  ) {
    String resourceName;
    String platform;
    String fileExtension = "dll";

    switch (osArch) {
      case "i386":
      case "x86":
        platform = "windows-x32";
        break;

      case "amd64":
      case "x86_64":
        platform = "windows-x64";
        break;

      default:
        throw new UnsupportedOperationException(
            "Platform " + osName + ":" + osArch + " not supported"
        );
    }

    resourceName = "/" + platform + "/" + library + "." + fileExtension;
    return resourceName;
  }

  /**
   * Determine the right linux library depending on the architecture.
   *
   * @param library The library name.
   * @param osName  The operating system name.
   * @param osArch  The system architecture.
   * @return The library resource.
   * @throws UnsupportedOperationException Throw an exception if no native library for this platform
   *                                       was found.
   */
  private static String determineLinuxLibrary(
      final String library,
      final String osName,
      final String osArch
  ) {
    String resourceName;
    String platform = null;
    String fileExtension = "so";

    switch (osArch) {
      case "amd64":
      case "x86_64":
        platform = "linux-x64";
        break;

      default:
        unsupportedPlatform(osName, osArch);
    }

    resourceName = "/" + platform + "/" + library + "." + fileExtension;
    return resourceName;
  }

  /**
   * Determine the right FreeBSD library depending on the architecture.
   *
   * @param library The library name.
   * @param osName  The operating system name.
   * @param osArch  The system architecture.
   * @return The library resource.
   * @throws UnsupportedOperationException Throw an exception if no native library for this platform
   *                                       was found.
   */
  private static String determineFreebsdLibrary(
      final String library,
      final String osName,
      final String osArch
  ) {
    String resourceName;
    String platform = null;
    String fileExtension = "so";

    switch (osArch) {
      case "amd64":
      case "x86_64":
        platform = "freebsd-x64";
        break;

      default:
        unsupportedPlatform(osName, osArch);
    }

    resourceName = "/" + platform + "/" + library + "." + fileExtension;
    return resourceName;
  }

  /**
   * Determine the right mac library depending on the architecture.
   *
   * @param library The library name.
   * @return The library resource.
   */
  private static String determineMacLibrary(final String library) {
    String resourceName;
    String platform = "darwin";
    String fileExtension = "dylib";
    resourceName = "/" + platform + "/" + library + "." + fileExtension;
    return resourceName;
  }

  /**
   * Save the shared library in the given temporary directory.
   */
  static String saveLibrary(final File dir, final String libraryName) throws IOException {
    String libraryFileName = "lib" + libraryName;

    URL libraryResource = findLibraryResource(libraryFileName);

    String basename = FilenameUtils.getName(libraryResource.getPath());
    File file = new File(dir, basename);
    file.deleteOnExit();

    try (
        InputStream in = libraryResource.openStream();
        OutputStream out = new FileOutputStream(file)
    ) {
      IOUtils.copy(in, out);
    }

    return file.getAbsolutePath();
  }

  private static void unsupportedPlatform(final String osName, final String osArch) {
    throw new UnsupportedOperationException(
        "Platform " + osName + ":" + osArch + " not supported"
    );
  }
}
