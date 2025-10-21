package com.lumen.build;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;

public abstract class SanitizePromptHeaderTransform implements TransformAction<TransformParameters.None> {
    private static final String INVALID_TOKEN = "\"{str}\"";

    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        File input = getInputArtifact().get().getAsFile();
        String outputName;
        if (input.getName().endsWith(".aar")) {
            outputName = input.getName().substring(0, input.getName().length() - 4) + "-sanitized.aar";
        } else {
            outputName = input.getName() + "-sanitized";
        }
        File output = outputs.file(outputName);
        File tempDir;
        try {
            tempDir = Files.createTempDirectory("prompt-header-" + input.getName()).toFile();
        } catch (IOException e) {
            throw new GradleException("Unable to create temp directory for " + input.getAbsolutePath(), e);
        }

        try {
            unzipTo(input, tempDir);
            boolean patched = replaceInvalidPromptHeaderStrings(tempDir);
            if (patched) {
                zipFrom(tempDir, output);
            } else {
                copyFile(input, output);
            }
        } catch (IOException ex) {
            throw new GradleException("Failed to sanitize " + input.getAbsolutePath(), ex);
        } finally {
            try {
                deleteRecursively(tempDir.toPath());
            } catch (IOException ignored) {
                // best effort cleanup
            }
        }
    }

    private static void copyFile(File input, File output) throws IOException {
        output.getParentFile().mkdirs();
        Files.copy(input.toPath(), output.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean replaceInvalidPromptHeaderStrings(File root) throws IOException {
        if (root == null || !root.exists()) {
            return false;
        }
        boolean patchedAny = false;
        List<File> valuesFiles = findValuesXmlFiles(root);
        for (File file : valuesFiles) {
            String original = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (original.contains(INVALID_TOKEN)) {
                String patched = original.replace(INVALID_TOKEN, "\"%1$s\"");
                if (!patched.equals(original)) {
                    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                        writer.write(patched);
                    }
                    patchedAny = true;
                }
            }
        }
        return patchedAny;
    }

    private static List<File> findValuesXmlFiles(File root) {
        List<File> files = new ArrayList<>();
        Deque<File> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            File current = stack.pop();
            File[] children = current.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (child.isDirectory()) {
                    stack.push(child);
                } else if ("values.xml".equals(child.getName())) {
                    files.add(child);
                }
            }
        }
        return files;
    }

    private static void unzipTo(File input, File destination) throws IOException {
        destination.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(input)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File entryFile = new File(destination, entry.getName());
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    File parent = entryFile.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }
                    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(entryFile))) {
                        zis.transferTo(output);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void zipFrom(File source, File output) throws IOException {
        output.getParentFile().mkdirs();
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
            Path root = source.toPath();
            Files.walk(root).forEach(path -> {
                Path relative = root.relativize(path);
                if (relative.getNameCount() == 0) {
                    return;
                }
                String normalized = relative.toString().replace(File.separatorChar, '/');
                try {
                    if (Files.isDirectory(path)) {
                        if (!normalized.endsWith("/")) {
                            normalized = normalized + "/";
                        }
                        zos.putNextEntry(new ZipEntry(normalized));
                        zos.closeEntry();
                    } else {
                        zos.putNextEntry(new ZipEntry(normalized));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walk(root)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            });
    }
}
