package com.threadmc.trc;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.io.InputStream;
import java.nio.file.Files;

public class RemapService {

    private final Path inputJar;
    private final Path mappingsFile;
    private final Path outputJar;
    private final String fromNamespace;
    private final String toNamespace;

    public RemapService(Path inputJar, Path mappingsFile, Path outputJar, String fromNamespace, String toNamespace) {
        this.inputJar = inputJar;
        this.mappingsFile = mappingsFile;
        this.outputJar = outputJar;
        this.fromNamespace = fromNamespace;
        this.toNamespace = toNamespace;
    }

    public void run() throws IOException {
        System.out.println("Loading mappings...");
        MemoryMappingTree tree = new MemoryMappingTree();
        MappingReader.read(mappingsFile, tree);

        System.out.println("Setting up TinyRemapper...");

        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(TinyUtils.createTinyMappingProvider(mappingsFile, fromNamespace, toNamespace))
                .renameInvalidLocals(true)
                .rebuildSourceFilenames(true)
                .fixPackageAccess(true)
                .build();

        System.out.println("Remapping jar...");
        try (@SuppressWarnings("deprecation") OutputConsumerPath outputConsumer = new OutputConsumerPath(outputJar)) {
            outputConsumer.addNonClassFiles(inputJar);
            remapper.readInputs(inputJar);
            remapper.apply(outputConsumer);
        }

        if (!Files.exists(outputJar)) {
            throw new IOException("Remapped output jar was not created: " + outputJar);
        }

        Path tempJar = Files.createTempFile("remapped-filtered", ".jar");
        try (
            JarFile jarFile = new JarFile(outputJar.toFile());
            JarOutputStream jos = new JarOutputStream(Files.newOutputStream(tempJar))
        ) {
            for (JarEntry entry : java.util.Collections.list(jarFile.entries())) {
                String n = entry.getName().replace('\\', '/');
                if (n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".DSA") || n.endsWith(".RSA"))) {
                    continue;
                }
                JarEntry newEntry = new JarEntry(entry.getName());
                if (entry.getMethod() == JarEntry.STORED) {
                    newEntry.setMethod(JarEntry.STORED);
                    newEntry.setSize(entry.getSize());
                    newEntry.setCompressedSize(entry.getCompressedSize());
                    newEntry.setCrc(entry.getCrc());
                }
                jos.putNextEntry(newEntry);
                try (InputStream is = jarFile.getInputStream(entry)) {
                    is.transferTo(jos);
                }
                jos.closeEntry();
            }
        }
        
        Files.move(tempJar, outputJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        remapper.finish();
        System.out.println("Remapped jar created at: " + outputJar.toAbsolutePath());
    }
}