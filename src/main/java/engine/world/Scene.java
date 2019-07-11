package engine.world;

import engine.graphics.BlockDebrisParticleEmitter;
import engine.graphics.DirectionalLight;
import engine.graphics.ParticleEmitterInterface;
import engine.world.ChunkManager;
import static engine.world.TextureManager.*;

import org.joml.Vector3f;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Scene {
    public ChunkManager chunkManager;
    public DirectionalLight light;
    public List<ParticleEmitterInterface> particleEmitters;

    public Scene(ChunkManager chunkManager, DirectionalLight light) {
        this.chunkManager = chunkManager;
        this.light = light;
        particleEmitters = new LinkedList<>();
    }

    public void init() {
        chunkManager.init();
    }

    public void clear() {
        chunkManager.clear();
    }

    public void destroyBlock(Vector3f selectedBlockPos) {
        int x = (int) selectedBlockPos.x, y = (int) selectedBlockPos.y, z = (int) selectedBlockPos.z;
        particleEmitters.add(new BlockDebrisParticleEmitter(x, y, z, chunkManager.getBlock(x, y, z).getBlockID()));
        chunkManager.updateBlock((int) selectedBlockPos.x, (int) selectedBlockPos.y, (int) selectedBlockPos.z, AIR);
    }

    public void putBlock(Vector3f selectedBlockPos, int blockID) {
        chunkManager.updateBlock((int) selectedBlockPos.x, (int) selectedBlockPos.y, (int) selectedBlockPos.z, blockID);
    }

    public void update(long elapsedTime) {

        Iterator<ParticleEmitterInterface> iter = particleEmitters.iterator();
        while (iter.hasNext()) {
            ParticleEmitterInterface emitter = iter.next();
            emitter.update(elapsedTime);

            // remove empty particle emitters
            if (emitter.getParticles().size() == 0) {
                iter.remove();
            }
        }
    }

}
