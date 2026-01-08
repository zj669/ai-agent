package com.zj.aiagent.config;

//@Component
//public class EmbedingModelConfig {
//    @Value("${spring.ai.openai.embedding.model}")
//    private String embeddingModel;
//    @Value("${spring.ai.openai.embedding.base-url}")
//    private String baseUrl;
//    @Value("${spring.ai.openai.embedding.endpoint}")
//    private String endpoint;
//
//    @Bean
//    public PgVectorStore pgVectorStore(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate ,
//                                       @Qualifier("openAiEmbeddingModel") OpenAiEmbeddingModel openAiEmbeddingModel) {
//        return PgVectorStore.builder(jdbcTemplate, openAiEmbeddingModel).build();
//    }
//    @Bean
//    public TokenTextSplitter tokenTextSplitter() {
//        return new TokenTextSplitter();
//    }
//
//    @Bean
//    public OpenAiEmbeddingModel openAiEmbeddingModel(WebClient.Builder client) {
//        OpenAiApi openAiApi = OpenAiApi.builder()
//                .baseUrl(baseUrl)
//                .apiKey("sk-950fbd6e45624ec48668a854932f4427")
//                .embeddingsPath( endpoint)
//                .completionsPath( endpoint)
//                .webClientBuilder(client).build();
//        MetadataMode embed = MetadataMode.EMBED;
//        OpenAiEmbeddingOptions build = OpenAiEmbeddingOptions.builder().model(embeddingModel).dimensions(768).build();
//        RetryTemplate retryTemplate = RetryTemplate.defaultInstance();
//
//
//        return new OpenAiEmbeddingModel(openAiApi,embed, build, retryTemplate);
//    }
//}
