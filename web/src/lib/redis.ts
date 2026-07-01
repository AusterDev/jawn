import Redis from "ioredis";
import "dotenv/config";

console.log(process.env.REDIS_PASSWORD)
const RedisClient = new Redis(
    {
        host: process.env.REDIS_HOST,
        port: parseInt(process.env.REDIS_PORT || "6379", 10),
        password: process.env.REDIS_PASSWORD,
        maxRetriesPerRequest: 3,
        connectTimeout: 5000,
        lazyConnect: true,
        retryStrategy(times) {
            if (times > 5) {
                console.error("Redis connection failed. Max retries limit reached");
                return null;
            }
            return Math.min(times * 200, 2000);
        }
    }
);

RedisClient.on("error", (error) => {
    console.error("Redis initialization failed with error: ", error);
});

RedisClient.on("connect", () => {
    console.log("Redis connection created");
});

RedisClient.connect().catch((error) => {
    console.error("Initial redis connection failed", error);
});

export default RedisClient;