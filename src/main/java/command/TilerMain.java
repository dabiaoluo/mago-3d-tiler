package command;

import geometry.batch.Batched3DModel;
import geometry.batch.Batcher;
import geometry.batch.GaiaBatcher;
import geometry.batch.GaiaTransfomer;
import geometry.exchangable.GaiaSet;
import geometry.structure.GaiaMaterial;
import geometry.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileExistsException;
import org.apache.logging.log4j.Level;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import tiler.*;
import tiler.tileset.Tileset;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;


@Slf4j
public class TilerMain {
    public static Options createOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "Print help");
        options.addOption("v", "version", false, "Print version");
        options.addOption("q", "quiet", false, "Quiet mode");

        options.addOption("i", "input", true, "Input file path");
        options.addOption("o", "output", true, "Output file path");
        options.addOption("it", "inputType", true, "Input file type");
        options.addOption("ot", "outputType", true, "Output file type");

        options.addOption("k", "kml", false, "Use KML file.");
        options.addOption("c", "crs", true, "Coordinate Reference Systems EPSG code");
        options.addOption("r", "recursive", false, "Recursive search directory");
        options.addOption("sc", "scale", true, "Scale factor");
        options.addOption("st", "strict", true, "Strict mode");

        options.addOption("gn", "genNormals", false, "generate normals");
        options.addOption("nt", "ignoreTextures", false, "ignore textures");
        options.addOption("yz", "swapYZ", false, "swap YZ");

        options.addOption("d", "debug", false, "Debug mode");
        options.addOption("gf", "gltf", false, "Create gltf file");
        options.addOption("gb", "glb", false, "Create glb file");

        options.addOption("mc", "maxCount", true, "Max count of nodes (Default: 256)");
        return options;
    }

    public static void main(String[] args) {
        Configurator.initLogger();
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("quiet")) {
                Configurator.setLevel(Level.OFF);
            }
            start();
            if (cmd.hasOption("debug")) {
                log.info("Starting Gaia3D Tiler in debug mode.");
            }
            if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Gaia3D Tiler", options);
                return;
            }
            if (cmd.hasOption("version")) {
                log.info("Gaia3D Tiler version 1.0.0");
                return;
            }
            if (!cmd.hasOption("input")) {
                log.error("input file path is not specified.");
                return;
            }
            if (!cmd.hasOption("output")) {
                log.error("output file path is not specified.");
                return;
            }
            /*if (!cmd.hasOption("crs")) {
                log.error("crs is not specified.");
                return;
            }*/
            File inputFile = new File(cmd.getOptionValue("input"));
            File outputFile = new File(cmd.getOptionValue("output"));
            execute(cmd, inputFile, outputFile);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
            log.error("Failed to parse command line properties", e);
        }
        underline();
    }

    private static void execute(CommandLine command, File inputFile, File outputFile) throws IOException {
        long start = System.currentTimeMillis();

        String crs = command.getOptionValue("crs");
        String inputExtension = command.getOptionValue("inputType");
        Path inputPath = inputFile.toPath();
        Path outputPath = outputFile.toPath();
        inputFile.mkdir();
        outputFile.mkdir();
        if (!validate(outputPath)) {
            return;
        }
        FormatType formatType = FormatType.fromExtension(inputExtension);
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem source = (crs != null) ? factory.createFromName("EPSG:" + crs) : null;


        FileLoader fileLoader = new FileLoader(command);
        List<TileInfo> tileInfos = fileLoader.loadTileInfos(formatType, inputPath, command.hasOption("recursive"));
        tileInfos.forEach((tileInfo) -> {
            GaiaTransfomer.translate(source, tileInfo);
        });

        TilerOptions tilerOptions = TilerOptions.builder()
                .inputPath(inputPath)
                .outputPath(outputPath)
                .inputFormatType(formatType)
                .source(source)
                .build();
        Tiler tiler = new Gaia3DTiler(tilerOptions, command);
        Tileset tileset = tiler.tile(tileInfos);
        tiler.writeTileset(outputPath, tileset);

        List<ContentInfo> contentInfos = tileset.findAllBatchInfo();
        contentInfos.forEach(contentInfo -> {
            GaiaTransfomer.relocation(contentInfo);
            try {
                Batcher batcher = new GaiaBatcher(contentInfo, command);
                GaiaSet batchedSet = batcher.batch();
                if (batchedSet.getMaterials().size() < 1 || batchedSet.getBufferDatas().size() < 1) {
                    throw new RuntimeException("No materials or buffers");
                }

                Batched3DModel batched3DModel = new Batched3DModel(command);
                batched3DModel.write(batchedSet, contentInfo);
                batched3DModel = null;
                contentInfo.getUniverse().getScenes().forEach(gaiaScene -> {
                    gaiaScene.getMaterials().forEach(GaiaMaterial::deleteTextures);
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        long end = System.currentTimeMillis();
        log.info("Tiling finished in {} seconds.", (end - start) / 1000);
    }

    private static boolean validate(Path outputPath) throws IOException {
        File output = outputPath.toFile();
        if (!output.exists()) {
            throw new FileExistsException("output path is not exist.");
        } else if (!output.isDirectory()) {
            throw new NotDirectoryException("output path is not directory.");
        } else if (!output.canWrite()) {
            throw new IOException("output path is not writable.");
        }
        return true;
    }

    private static void start() {
        log.info(
            " _______  ___      _______  _______  __   __  _______ \n" +
            "|       ||   |    |   _   ||       ||  |_|  ||   _   |\n" +
            "|    _  ||   |    |  |_|  ||  _____||       ||  |_|  |\n" +
            "|   |_| ||   |    |       || |_____ |       ||       |\n" +
            "|    ___||   |___ |       ||_____  ||       ||       |\n" +
            "|   |    |       ||   _   | _____| || ||_|| ||   _   |\n" +
            "|___|    |_______||__| |__||_______||_|   |_||__| |__|");
        underline();
    }

    private static void underline() {
        log.info("======================================================");
    }
}
