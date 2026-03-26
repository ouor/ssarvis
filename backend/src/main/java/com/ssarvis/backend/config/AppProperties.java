package com.ssarvis.backend.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Openai openai = new Openai();
    private final Cors cors = new Cors();
    private final Dashscope dashscope = new Dashscope();
    private final Media media = new Media();
    private final Storage storage = new Storage();
    private final Auth auth = new Auth();
    private final Bootstrap bootstrap = new Bootstrap();

    public Openai getOpenai() {
        return openai;
    }

    public Cors getCors() {
        return cors;
    }

    public Dashscope getDashscope() {
        return dashscope;
    }

    public Media getMedia() {
        return media;
    }

    public Storage getStorage() {
        return storage;
    }

    public Auth getAuth() {
        return auth;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public static class Openai {
        private String apiKey;
        private String model = "gpt-5";
        private String baseUrl = "https://api.openai.com/v1";
        private int chatHistoryTurns = 10;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getChatHistoryTurns() {
            return chatHistoryTurns;
        }

        public void setChatHistoryTurns(int chatHistoryTurns) {
            this.chatHistoryTurns = chatHistoryTurns;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Dashscope {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String realtimeUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";
        private String ttsModel = "qwen3-tts-vc-realtime-2026-01-15";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getRealtimeUrl() {
            return realtimeUrl;
        }

        public void setRealtimeUrl(String realtimeUrl) {
            this.realtimeUrl = realtimeUrl;
        }

        public String getTtsModel() {
            return ttsModel;
        }

        public void setTtsModel(String ttsModel) {
            this.ttsModel = ttsModel;
        }
    }

    public static class Media {
        private String ffmpegPath = "ffmpeg";

        public String getFfmpegPath() {
            return ffmpegPath;
        }

        public void setFfmpegPath(String ffmpegPath) {
            this.ffmpegPath = ffmpegPath;
        }
    }

    public static class Storage {
        private final S3 s3 = new S3();

        public S3 getS3() {
            return s3;
        }
    }

    public static class Auth {
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }
    }

    public static class Bootstrap {
        private final DefaultAccount defaultAccount = new DefaultAccount();

        public DefaultAccount getDefaultAccount() {
            return defaultAccount;
        }
    }

    public static class DefaultAccount {
        private boolean enabled;
        private String username;
        private String password;
        private String displayName = "기본 계정";
        private List<String> voiceSamplePaths = new ArrayList<>();
        private List<String> voiceAliases = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public List<String> getVoiceSamplePaths() {
            return voiceSamplePaths;
        }

        public void setVoiceSamplePaths(List<String> voiceSamplePaths) {
            this.voiceSamplePaths = voiceSamplePaths;
        }

        public List<String> getVoiceAliases() {
            return voiceAliases;
        }

        public void setVoiceAliases(List<String> voiceAliases) {
            this.voiceAliases = voiceAliases;
        }
    }

    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMinutes = 120;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenExpirationMinutes() {
            return accessTokenExpirationMinutes;
        }

        public void setAccessTokenExpirationMinutes(long accessTokenExpirationMinutes) {
            this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        }
    }

    public static class S3 {
        private boolean enabled;
        private String bucket;
        private String region = "ap-northeast-2";
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String keyPrefix = "ssarvis/tts";
        private boolean pathStyleAccess;
        private String publicBaseUrl;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }
}
