package com.gaia3d.process.tileprocess.tile;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.basic.structure.GaiaScene;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4d;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Getter
@Setter
@Builder
@Slf4j
public class TileInfo {
    private int serial = -1;

    private KmlInfo kmlInfo;
    private GaiaScene scene;
    private GaiaSet set;
    private GaiaPointCloud pointCloud;
    private String name;

    private Matrix4d transformMatrix;
    private GaiaBoundingBox boundingBox;
    private Path scenePath;
    private Path outputPath;
    private Path tempPath;

    private void init() {
        GaiaNode rootNode = this.scene.getNodes().get(0);
        this.transformMatrix = rootNode.getTransformMatrix();
        this.name = rootNode.getName();

        this.boundingBox = this.scene.getGaiaBoundingBox();
        this.scenePath = this.scene.getOriginalPath();

        this.tempPath = this.outputPath.resolve("temp");
        File tempFile = this.tempPath.toFile();
        if (!tempFile.exists() && tempFile.mkdir()) {
            log.info("[Pre] Created temp directory in {}", this.tempPath);
        }
    }

    public void minimize(int serial) {
        if (this.scene != null && !this.scene.getNodes().isEmpty()) {
            GaiaSet tempSet = new GaiaSet(this.scene);
            this.tempPath = tempSet.writeFile(this.tempPath, serial);
            tempSet.clear();
            tempSet = null;
            this.scene.clear();
            this.scene = null;
        }/* else {
            log.warn("Can't minimize tile info because scene is null.");
        }*/
    }

    public void maximize() {
        if (this.tempPath == null) {
            //log.warn("Can't maximize tile info because temp path is null.");
            return;
        }
        File tempFile = this.tempPath.toFile();
        if (!tempFile.isFile()) {
            //log.warn("Can't maximize tile info because temp path is null.");
            return;
        }
        if (this.set != null) {
            this.set.deleteTextures();
            this.set = null;
        }
        this.set = new GaiaSet(this.tempPath);
    }

    public void clear() {
        this.scene = null;
        this.set = null;
    }

    public void deleteTemp() throws IOException {
        if (this.tempPath != null) {
            File file = this.tempPath.toFile();
            File parent = file.getParentFile();
            if (file.isFile()) {
                if (parent.isDirectory()) {
                    log.info("[Delete][temp] {}", parent);
                    FileUtils.deleteDirectory(parent);
                    return;
                }
            } else if (file.isDirectory()) {
                log.info("[Delete][temp] {}", file);
                FileUtils.deleteDirectory(file);
                return;
            }
            log.warn("Can not delete temp files: {}", file);
        }
    }
}
