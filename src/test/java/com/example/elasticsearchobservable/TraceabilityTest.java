package com.example.elasticsearchobservable;

import com.example.elasticsearchobservable.elastic.Article;
import com.example.elasticsearchobservable.elastic.ArticleRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.observability.DefaultSignalListener;
import reactor.core.publisher.Flux;

import java.time.Duration;

@SpringBootTest
//@ExtendWith(OutputCaptureExtension.class)
public class TraceabilityTest {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveController.class);
    @Autowired
    private ArticleRepository repository;

    @Autowired
    private ObservationRegistry observationRegistry;

    @Test
    public void checkTracingInLog() {
    // Create an Observation and observe your code!
        Observation.createNotStarted("user.name", observationRegistry)
                .contextualName("getting-user-name")
                .observe(() -> {
                    logger.info("Hello");
                    fluxIteration();
                    elasticSearchRepoCall();
                });

//        MDC.put("traceId", "my trace id");
//        MDC.put("spanId", "my span id");
    }

    private static void fluxIteration() {
        Flux.just(1, 2, 3)
                .delayElements(Duration.ofMillis(1))
                        .handle((result, sink) -> logger.info("inside flux handle"))
                .contextCapture()
                .blockLast();
    }

    private void elasticSearchRepoCall() {
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
                .contextCapture()
                .block();
    }

}
