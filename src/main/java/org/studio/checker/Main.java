package org.studio.checker;

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.studio.checker.processor.AudioProcessor;
import org.studio.checker.processor.ImageProcessor;
import picocli.CommandLine;
import studio.core.v1.exception.StoryTellerException;
import studio.core.v1.model.StoryPack;
import studio.core.v1.model.asset.MediaAsset;
import studio.core.v1.model.asset.MediaAssetType;
import studio.core.v1.model.metadata.StoryPackMetadata;
import studio.core.v1.service.PackFormat;
import studio.core.v1.utils.stream.StoppingConsumer;
import studio.core.v1.utils.stream.ThrowingFunction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

@CommandLine.Command(name = "studio-pack-checker", mixinStandardHelpOptions = true, version = "studio-pack-checker 0.1",
        description = "Check and compress archive packs for STUdio.", defaultValueProvider = CommandLine.PropertiesDefaultProvider.class)
@Slf4j
public class Main implements Callable<Integer> {

    private static final String CONVERTED_SPC_ZIP = "_converted_spc.zip";

    @CommandLine.Parameters(index = "0", description = "The folder where are packs or the filepath of a unique pack.")
    private Path inputPath;

    @CommandLine.Option(names = {"-o", "--outputdir"}, description = "The output folder, same as input folder if not specified")
    private Path outputPath;

    @CommandLine.Option(names = {"-r", "--repair"}, description = "Repair if needed the pack file")
    private boolean repair;

    @CommandLine.Option(names = {"-c", "--compress"}, description = "Compress (and repair) if needed the pack file")
    private boolean compress;

    @CommandLine.Option(names = {"-f", "--force"}, description = "Overwrite the pack")
    private boolean force;

    @CommandLine.Option(names = {"-p", "--parallel"}, description = "Parallel process")
    private boolean parallel;

    @CommandLine.Option(names = {"-i", "--inc"}, description = "Increase version pack number if modification")
    private boolean increaseVersion;

    private enum MediaGroup {
        AUDIO, IMAGE;
    }

    // this example implements Callable, so parsing, error handling and handling user
    // requests for usage help or version help can be done with one line of code.
    public static void main(String... args) {
        CommandLine cmd = new CommandLine(new Main());
        File defaultsFile = new File("command.properties");
        cmd.setDefaultValueProvider(new CommandLine.PropertiesDefaultProvider(defaultsFile));
        int exitCode = cmd.execute(args);

        log.info("End of studio-pack-checker");
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        log.info("Starting studio-pack-checker");

        repair = compress || repair; // If compress is activated, so repair too
        if (outputPath == null) {
            outputPath = Files.isDirectory(inputPath) ? inputPath : inputPath.getParent();
        }

        if (Files.isDirectory(inputPath)) {
            try (Stream<Path> list = Files.list(inputPath)) {
                list.filter(path -> ThrowingFunction.unchecked(internPath -> Files.isRegularFile(path) && !Files.isHidden(path)).apply(path)).forEach(this::checkPackWithResult);
            }
        } else {
            this.checkPackWithResult(inputPath);
        }
        return 0;
    }

    private void checkPackWithResult(Path inputPackPath) {
        if (!inputPackPath.toString().endsWith(CONVERTED_SPC_ZIP)) {
            log.info("Checking {} pack", inputPackPath.getFileName());
            CheckResult result = checkPack(inputPackPath);
            result.showResult();
            log.info("------------------------");
        }
    }

    public CheckResult checkPack(Path inputPackPath) {
        PackFormat format = PackFormat.fromPath(inputPackPath);
        if (!PackFormat.ARCHIVE.equals(format)) {
            return CheckResult.ko(inputPackPath, "Wrong extension, only check zip files");
        }

        try {
            StoryPackMetadata metadata = format.getReader().readMetadata(inputPackPath);

            if (metadata == null) {
                return CheckResult.ko(inputPackPath, "No story.json found");
            }

            if (metadata.getThumbnail() == null) {
                log.warn("No thumbnail found");
            }
        } catch (IOException e) {
            return CheckResult.ko(inputPackPath, e);
        }

        try {
            StoryPack pack = format.getReader().read(inputPackPath);
            Path outputPackPath;
            if (repair && !force) {
                outputPackPath = Paths.get(outputPath.toString() + "/" + inputPackPath.getFileName().toString() + CONVERTED_SPC_ZIP);
                if (Files.exists(outputPackPath)) {
                    return CheckResult.ko(inputPackPath, "Target file already exists");
                }
            } else {
                outputPackPath = inputPackPath;
            }

            try {
                if (recompress(pack)) {
                    if (increaseVersion) {
                        pack.setVersion((short) (pack.getVersion() + 1));
                    }
                    format.getWriter().write(pack, outputPackPath, true);
                    return compress ? CheckResult.compress(outputPackPath, pack) : CheckResult.repair(outputPackPath, pack);
                }
            } catch (StoryTellerException e) {
                // TODO Need to handle these errors
                return CheckResult.ko(inputPackPath, e);
            }

            return CheckResult.ok(inputPackPath, pack);
        } catch (IOException e) {
            return CheckResult.ko(inputPackPath, e);
        }
    }

    public boolean recompress(StoryPack storyPack) {
        // Image
        boolean modification = processAssets(storyPack, MediaGroup.IMAGE, MediaAssetType.BMP, ThrowingFunction.unchecked(ia -> ImageProcessor.getProcessor(compress).apply(ia)));
        // Audio
        modification |= processAssets(storyPack, MediaGroup.AUDIO, MediaAssetType.MP3, ThrowingFunction.unchecked(aa -> AudioProcessor.getProcessor(repair, compress).apply(aa)));

        return modification;
    }

    private boolean processAssets(StoryPack storyPack, MediaGroup mg, MediaAssetType targetType,
                                  Function<MediaAsset, byte[]> processor) {
        // Cache prepared assets bytes
        Map<String, byte[]> assets = new ConcurrentHashMap<>();
        List<MediaAsset> medias = storyPack.assets(mg == MediaGroup.IMAGE);
        AtomicBoolean modification = new AtomicBoolean(false);

        try (ProgressBar pb = new ProgressBarBuilder()
                .setInitialMax(medias.size())
                .setUpdateIntervalMillis(100)
                .setTaskName("Converting " + mg)
                .setMaxRenderedLength(125)
                //.setConsumer(new DelegatingProgressBarConsumer(log::info))
                .build()) {
            var mediasStream = parallel ? medias.parallelStream() : medias.stream();
            mediasStream.forEach(StoppingConsumer.stopped(a -> {
                pb.setExtraMessage(a.getName()); // Set extra message to display at the end of the bar

                String assetHash = a.findHash();
                // Update data (converted if needed)
                byte[] rawData = assets.computeIfAbsent(assetHash, s -> processor.apply(a));
                if (!Arrays.equals(rawData, a.getRawData())) {
                    a.setRawData(rawData);
                    // force type
                    a.changeType(targetType);
                    modification.set(true);
                }

                pb.step();
            }));
        }
        // Clean cache
        assets.clear();

        return modification.get();
    }

}
