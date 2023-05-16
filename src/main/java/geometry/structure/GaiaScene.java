package geometry.structure;

import geometry.exchangable.GaiaBufferDataSet;
import geometry.exchangable.GaiaSet;
import geometry.types.TextureType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Matrix4d;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaScene {
    private List<GaiaNode> nodes = new ArrayList<>();
    private List<GaiaMaterial> materials = new ArrayList<>();
    private Path originalPath;

    public GaiaScene(GaiaSet gaiaSet) {
        List<GaiaBufferDataSet> bufferDataSets = gaiaSet.getBufferDatas();
        List<GaiaMaterial> materials = gaiaSet.getMaterials();

        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();
        transformMatrix.rotateX(Math.toRadians(-90)); // y and z axis swap

        GaiaNode rootNode = new GaiaNode();
        rootNode.setName("BatchedRootNode");
        rootNode.setTransformMatrix(transformMatrix);
        this.materials = materials;
        this.nodes.add(rootNode);

        bufferDataSets.stream().forEach((bufferDataSet) -> {
            GaiaNode node = new GaiaNode(bufferDataSet);
            rootNode.getChildren().add(node);
        });
    }

    public void renderScene(int program) {
        for (GaiaNode node : nodes) {
            node.renderNode(program);
        }
    }

    // getTotalIndicesCount
    public int getTotalIndicesCount() {
        return GaiaNode.getTotalIndicesCount(0, nodes);
    }
    // getTotalIndices
    public List<Short> getTotalIndices() {
        return GaiaNode.getTotalIndices(new ArrayList<Short>(), nodes);
    }

    // getTotalVerticesCount
    public int getTotalVerticesCount() {
        return GaiaNode.getTotalVerticesCount(0, nodes);
    }
    // getTotalVertices
    public List<Float> getTotalVertices() {
        return GaiaNode.getTotalVertices(new ArrayList<>(), nodes);
    }

    //getTotalNormalsCount
    public int getTotalNormalsCount() {
        return GaiaNode.getTotalNormalsCount(0, nodes);
    }
    //getTotalNormals
    public List<Float> getTotalNormals() {
        return GaiaNode.getTotalNormals(new ArrayList<Float>(), nodes);
    }

    //getTotalTexCoordsCount
    public int getTotalTexcoordsCount() {
        return GaiaNode.getTotalTexcoordsCount(0, nodes);
    }
    //getTotalTexCoords
    public List<Float> getTotalTexcoords() {
        return GaiaNode.getTotalTexcoords(new ArrayList<Float>(), nodes);
    }

    //getTotalColorsCount
    public int getTotalColorsCount() {
        return GaiaNode.getTotalColorsCount(0, nodes);
    }
    //getTotalColors
    public List<Float> getTotalColors() {
        return GaiaNode.getTotalColors(new ArrayList<Float>(), nodes);
    }

    public int getTotalTextureSize() {
        //return GaiaMaterial.getTotalTextureSize(0, materials);
        return 0;
    }

    // getAllTexturePaths
    public List<String> getAllTexturePaths() {
        List<String> texturePaths = new ArrayList<>();
        for (GaiaMaterial material : materials) {
            LinkedHashMap<TextureType, List<GaiaTexture>> textures = material.getTextures();
            textures.forEach((textureType, gaiaTextures) -> {
                for (GaiaTexture gaiaTexture : gaiaTextures) {
                    String texturePath = gaiaTexture.getPath();
                    if (texturePath != null) {
                        //gaiaTexture.setTexturePath(FileUtils.getFileName(texturePath));
                        texturePaths.add(texturePath);
                    }
                }
            });

        }
        return texturePaths;
    }
}
