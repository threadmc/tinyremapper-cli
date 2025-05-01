package com.threadmc.trc;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.IOException;
import java.nio.file.Path;

public class RemapService {

    private final Path inputJar;
    private final Path mappingsFile;
    private final Path outputJar;

    public RemapService(Path inputJar, Path mappingsFile, Path outputJar) {
        this.inputJar = inputJar;
        this.mappingsFile = mappingsFile;
        this.outputJar = outputJar;
    }

    public void run() throws IOException {
        System.out.println("Loading mappings...");
        MemoryMappingTree tree = new MemoryMappingTree();
        MappingReader.read(mappingsFile, tree);

        String fromNamespace = "official";
        String toNamespace = "named";

        System.out.println("Setting up TinyRemapper...");

        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(TinyUtils.createTinyMappingProvider(mappingsFile, fromNamespace, toNamespace))
                .renameInvalidLocals(true)
                .rebuildSourceFilenames(true)
                .fixPackageAccess(true)
                .build();

        System.out.println("Remapping jar...");
        try ( @SuppressWarnings("deprecation") OutputConsumerPath outputConsumer = new OutputConsumerPath(outputJar) ) {
            outputConsumer.addNonClassFiles(inputJar);
            remapper.readInputs(inputJar);
            remapper.apply(outputConsumer);
        }

        remapper.finish();
        System.out.println("Remapped jar created at: " + outputJar.toAbsolutePath());
    }
}