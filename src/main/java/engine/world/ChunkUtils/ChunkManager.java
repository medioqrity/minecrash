package engine.world.ChunkUtils;

import java.util.*;

import engine.world.Block;
import engine.world.gen.ChunkGenerator;
import engine.world.gen.ChunkGeneratorOverWorld;
import javafx.util.Pair;
import org.joml.Vector3f;

public class ChunkManager {
    private Map<Pair<Integer, Integer>, Chunk> chunkMap;
    private int[] dx = {1, 0, -1, 0};
    private int[] dz = {0, -1, 0, 1};
//    private ChunkGenerator chunkGenerator;
    private int viewDistanceNear = 8;
    private int viewDistanceFar = 12;
    private Vector3f generateCenter;
    private Set<Pair<Integer, Integer>> updateList;

    public ChunkManager() {
        chunkMap = new HashMap<>();
//        chunkGenerator = new ChunkGeneratorOverWorld();
        generateCenter = new Vector3f();
        updateList = new HashSet<>();
    }

    public void init() {
//        for (int i = 0; i < WORLD_MAX_WIDTH; ++i) {
//            for (int j = 0; j < WORLD_MAX_LENGTH; ++j) {
//                System.out.println("[INFO] Generating Chunk [" + i + ", " + j + "]");
//                chunkMap.put(new Pair<>(i, j), chunkGenerator.generateChunk(i, j));
//            }
//        }
//        for (Chunk chunk : chunkMap.values()) {
//            System.out.println("[INFO] Generating Chunk Mesh [" + chunk.getx() + ", " + chunk.getz() + "]");
//            chunk.generateMesh(this);
//        }
    }

    public Collection<Chunk> getChunks() {
        return chunkMap.values();
    }

    public void clear() {
        for (Chunk chunk : chunkMap.values()) {
            chunk.clear();
        }
    }

    public Block getBlock(int x, int y, int z) { // x y z are world coord
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        Chunk curChunk = chunkMap.get(new Pair<>(chunkX, chunkZ));
        if (curChunk == null) return null;
        return curChunk.getBlock(x & 15, y, z & 15);
    }

//    private boolean valid(int chunkX, int chunkZ) {
//        return 0 <= chunkX && chunkX < WORLD_MAX_WIDTH && 0 <= chunkZ && chunkZ < WORLD_MAX_LENGTH;
//    }

    public void updateBlock(int x, int y, int z, int blockID) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        Chunk curChunk = chunkMap.get(new Pair<>(chunkX, chunkZ));
        if (curChunk == null) return;
        curChunk.setBlock(blockID, x & 15, y, z & 15);
        curChunk.updateMesh(y >> 4, this);
        if ((y & 15) == 0)
            curChunk.updateMesh((y >> 4) - 1, this);
        if ((y & 15) == 15)
            curChunk.updateMesh((y >> 4) + 1, this);
        for (int d = 0; d < 4; ++d) {
            int nx = x + dx[d], nz = z + dz[d];
            int nX = nx >> 4, nZ = nz >> 4;
            curChunk = chunkMap.get(new Pair<>(nX, nZ));
            if (curChunk == null) continue;
            if (nX != chunkX) curChunk.updateMesh(y >> 4, this);
            if (nZ != chunkZ) curChunk.updateMesh(y >> 4, this);
        }
    }

    private boolean tooFar(Chunk chunk) {
        return Math.abs(((int) generateCenter.x >> 4) - chunk.getx()) > viewDistanceFar ||
               Math.abs(((int) generateCenter.z >> 4) - chunk.getz()) > viewDistanceFar;
    }

    private boolean inSight(int x, int z) {
        return Math.abs(((int) generateCenter.x >> 4) - x) <= viewDistanceNear &&
               Math.abs(((int) generateCenter.z >> 4) - z) <= viewDistanceNear;
    }

    public void update(Vector3f cameraPosition) {
        List<MultiThreadChunkGenerator> generators = new LinkedList<>();

        generateCenter.set(cameraPosition);

        chunkMap.values().removeIf(this::tooFar);

        int centerX = (int) generateCenter.x >> 4;
        int centerZ = (int) generateCenter.z >> 4;
        for (int i = centerX - viewDistanceNear; i <= centerX + viewDistanceNear; ++i) {
            for (int j = centerZ - viewDistanceNear; j <= centerZ + viewDistanceNear; ++j) {
                if (i < 0 || j < 0) continue;
                if (!chunkMap.containsKey(new Pair<>(i, j))) {
                    MultiThreadChunkGenerator generator = new MultiThreadChunkGenerator(i, j);
                    generators.add(generator);
                    generator.start();
                    updateList.add(new Pair<>(i, j));
                    for (int d = 0; d < 4; ++d) {
                        int nx = i + dx[d];
                        int ny = j + dz[d];
                        if (chunkMap.containsKey(new Pair<>(nx, ny))) {
                            updateList.add(new Pair<>(nx, ny));
                        }
                    }
                }
            }
        }

        try {
            for (MultiThreadChunkGenerator generator : generators) {
                generator.join();
                chunkMap.put(new Pair<>(generator.getX(), generator.getZ()), generator.getChunk());
            }
        } catch (InterruptedException e) {
            System.err.println("[ERROR] ChunkManager.update(): Error in generating chunks.");
            e.printStackTrace();
        }

        for (Pair<Integer, Integer> p : updateList) {
            chunkMap.get(p).generateMesh(this);
        }

        updateList.clear();
    }

}