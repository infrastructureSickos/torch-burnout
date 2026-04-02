package com.infrastructuresickos.torch_burnout;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

/**
 * Handles random-tick burnout for torch blocks.
 * Registered manually on the Forge bus — no @Mod.EventBusSubscriber.
 *
 * Uses BlockEvent.NeighborNotifyEvent as a periodic hook isn't available without
 * custom block registration. Instead, we hook into the server tick via a random
 * block tick surrogate: we subscribe to neighbor-notify on torch positions and
 * apply our own random check.
 *
 * Actually, the correct approach for random-tick behavior without custom blocks is
 * to use the TickEvent.LevelTickEvent and iterate over loaded chunks. However,
 * Forge provides BlockEvent.NeighborNotifyEvent which fires too infrequently.
 *
 * The cleanest approach: subscribe to TickEvent.LevelTickEvent (server-side),
 * and on each tick, pick a small number of random loaded chunk positions to
 * check for torches, mimicking the vanilla random-tick mechanism.
 */

import net.minecraftforge.event.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;

public class TorchBurnoutEventHandler {

    private static final Random RANDOM = new Random();

    // Number of random block positions to check per chunk per tick
    // Vanilla uses 3 random ticks per chunk section per tick; we use 1 check
    // across all loaded chunks sampled from a subset to keep it lightweight.
    private static final int CHECKS_PER_TICK = 20;

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;

        for (int i = 0; i < CHECKS_PER_TICK; i++) {
            // Pick a random loaded chunk
            Iterable<ChunkHolder> chunks = level.getChunkSource().chunkMap.getChunks();
            // Collect one chunk by skipping a random offset — lightweight approximation
            int skip = RANDOM.nextInt(Math.max(1, countApprox(level)));
            LevelChunk chunk = null;
            int s = 0;
            for (ChunkHolder h : chunks) {
                LevelChunk c = h.getTickingChunk();
                if (c == null) continue;
                if (s++ >= skip) { chunk = c; break; }
            }
            if (chunk == null) continue;

            // Pick a random position within the chunk
            int x = chunk.getPos().getMinBlockX() + RANDOM.nextInt(16);
            int z = chunk.getPos().getMinBlockZ() + RANDOM.nextInt(16);
            int y = level.getMinBuildHeight() + RANDOM.nextInt(level.getHeight());
            BlockPos pos = new BlockPos(x, y, z);

            BlockState state = level.getBlockState(pos);
            if (!isTorch(state)) continue;

            tryBurnOut(level, pos);
        }
    }

    private void tryBurnOut(ServerLevel level, BlockPos pos) {
        double chance = TBConfig.INSTANCE.baseChance.get();

        // Rain + sky access multiplier
        if (level.isRaining() && level.canSeeSky(pos)) {
            chance *= TBConfig.INSTANCE.rainMultiplier.get();
        }

        // Enderman proximity multiplier
        int radius = TBConfig.INSTANCE.endermanRadius.get();
        AABB searchBox = new AABB(pos).inflate(radius);
        List<EnderMan> endermen = level.getEntitiesOfClass(EnderMan.class, searchBox);
        if (!endermen.isEmpty()) {
            chance *= TBConfig.INSTANCE.endermanMultiplier.get() * endermen.size();
        }

        if (RANDOM.nextDouble() >= chance) return;

        // Burn out: remove torch, drop a stick
        level.removeBlock(pos, false);
        ItemEntity stick = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(Items.STICK));
        level.addFreshEntity(stick);
    }

    private boolean isTorch(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH);
    }

    /** Rough estimate of loaded chunk count to avoid materializing the full list. */
    private int countApprox(ServerLevel level) {
        int count = 0;
        for (ChunkHolder ignored : level.getChunkSource().chunkMap.getChunks()) {
            count++;
            if (count >= 500) break; // cap so we don't iterate thousands
        }
        return count;
    }
}
