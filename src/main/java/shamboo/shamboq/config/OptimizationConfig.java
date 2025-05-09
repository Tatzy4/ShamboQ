package shamboo.shamboq.config;

/**
 * Configuration for optimization settings using builder pattern
 */
public class OptimizationConfig {
    // Optimization settings
    private final boolean optimizeChunks;
    private final boolean disableMobs;
    private final boolean reduceViewDistance;
    private final boolean setupNoTickZone;
    private final boolean disableTerrainGeneration;
    private final boolean disablePlayerTicks;
    private final boolean spectatorMode;
    private final boolean dedicatedThreadPool;
    private final boolean aggressiveChunkManagement;
    private final int threadPoolSize;
    private final int maxLoadedChunks;
    private final int queueViewDistance;

    private OptimizationConfig(Builder builder) {
        this.optimizeChunks = builder.optimizeChunks;
        this.disableMobs = builder.disableMobs;
        this.reduceViewDistance = builder.reduceViewDistance;
        this.setupNoTickZone = builder.setupNoTickZone;
        this.disableTerrainGeneration = builder.disableTerrainGeneration;
        this.disablePlayerTicks = builder.disablePlayerTicks;
        this.spectatorMode = builder.spectatorMode;
        this.dedicatedThreadPool = builder.dedicatedThreadPool;
        this.aggressiveChunkManagement = builder.aggressiveChunkManagement;
        this.threadPoolSize = builder.threadPoolSize;
        this.maxLoadedChunks = builder.maxLoadedChunks;
        this.queueViewDistance = builder.queueViewDistance;
    }

    // Getters
    public boolean isOptimizeChunks() {
        return optimizeChunks;
    }

    public boolean isDisableMobs() {
        return disableMobs;
    }

    public boolean isReduceViewDistance() {
        return reduceViewDistance;
    }

    public boolean isSetupNoTickZone() {
        return setupNoTickZone;
    }

    public boolean isDisableTerrainGeneration() {
        return disableTerrainGeneration;
    }

    public boolean isDisablePlayerTicks() {
        return disablePlayerTicks;
    }

    public boolean isSpectatorMode() {
        return spectatorMode;
    }

    public boolean isDedicatedThreadPool() {
        return dedicatedThreadPool;
    }

    public boolean isAggressiveChunkManagement() {
        return aggressiveChunkManagement;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public int getMaxLoadedChunks() {
        return maxLoadedChunks;
    }

    public int getQueueViewDistance() {
        return queueViewDistance;
    }

    /**
     * Builder for OptimizationConfig
     */
    public static class Builder {
        private boolean optimizeChunks = true;
        private boolean disableMobs = true;
        private boolean reduceViewDistance = true;
        private boolean setupNoTickZone = true;
        private boolean disableTerrainGeneration = true;
        private boolean disablePlayerTicks = true;
        private boolean spectatorMode = true;
        private boolean dedicatedThreadPool = true;
        private boolean aggressiveChunkManagement = true;
        private int threadPoolSize = 1;
        private int maxLoadedChunks = 9;
        private int queueViewDistance = 2;

        public Builder optimizeChunks(boolean value) {
            this.optimizeChunks = value;
            return this;
        }

        public Builder disableMobs(boolean value) {
            this.disableMobs = value;
            return this;
        }

        public Builder reduceViewDistance(boolean value) {
            this.reduceViewDistance = value;
            return this;
        }

        public Builder setupNoTickZone(boolean value) {
            this.setupNoTickZone = value;
            return this;
        }

        public Builder disableTerrainGeneration(boolean value) {
            this.disableTerrainGeneration = value;
            return this;
        }

        public Builder disablePlayerTicks(boolean value) {
            this.disablePlayerTicks = value;
            return this;
        }

        public Builder spectatorMode(boolean value) {
            this.spectatorMode = value;
            return this;
        }

        public Builder dedicatedThreadPool(boolean value) {
            this.dedicatedThreadPool = value;
            return this;
        }

        public Builder aggressiveChunkManagement(boolean value) {
            this.aggressiveChunkManagement = value;
            return this;
        }

        public Builder threadPoolSize(int value) {
            this.threadPoolSize = Math.max(1, value);
            return this;
        }

        public Builder maxLoadedChunks(int value) {
            this.maxLoadedChunks = Math.max(1, value);
            return this;
        }

        public Builder queueViewDistance(int value) {
            this.queueViewDistance = Math.max(1, Math.min(8, value));
            return this;
        }

        public OptimizationConfig build() {
            return new OptimizationConfig(this);
        }
    }
}