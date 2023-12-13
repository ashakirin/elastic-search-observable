package com.example.elasticsearchobservable;

import com.example.elasticsearchobservable.elastic.Article;
import com.example.elasticsearchobservable.elastic.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.observability.DefaultSignalListener;

@SpringBootTest
//@ExtendWith(OutputCaptureExtension.class)
public class TraceabilityTest {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveController.class);
    @Autowired
    private ArticleRepository repository;

    @Test
    public void checkTracingInLog() {
        MDC.put("traceId", "my trace id");
        MDC.put("spanId", "my span id");
        logger.info("before");
        repository.findByName("Article 1")
                .map(Article::getName)
                .tap(() -> new DefaultSignalListener<>() {
                    @Override
                    public void doOnComplete() {
                        logger.info("inside doOnComplete");
//                        assertThat(output.getOut()).contains("my trace id");
                    }
                })
                .handle((result, sink) -> {
                    logger.info("inside handle");
  //                  assertThat(output.getOut()).contains("my trace id");
                    sink.next(result);
                })
                .block();
    }
}
