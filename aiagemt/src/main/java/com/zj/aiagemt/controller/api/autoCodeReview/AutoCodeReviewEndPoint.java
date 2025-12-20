package com.zj.aiagemt.controller.api.autoCodeReview;



//@Slf4j
//@RestController
//@RequestMapping("/api/auto/review")
//public class AutoCodeReviewEndPoint {
//    @Resource
//    private AutoCodeReviewExecuteStrategy autoCodeReviewExecuteStrategy;
//
//    @PostMapping("/codeReview")
//    public String codeReview(@RequestBody String diff) {
//        List<String> strings = splitDiff(diff, 5000);
//        AutoCodeCommandEntity build = AutoCodeCommandEntity.builder().aiAgentId("6").diff(strings).build();
//        String execute = null;
//        try {
//            execute = autoCodeReviewExecuteStrategy.execute(build);
//        } catch (Exception e) {
//            log.error("自动审计执行失败:{}", e.getMessage());
//        }
//        log.info("自动审计执行结果: {}", execute);
//        return execute;
//    }
//
//
//    public List<String> splitDiff(String diff, int maxChunkSize) {
//
//        return List.of(diff);
//    }
//}
