package com.example.elasticsearchobservable;

import com.example.elasticsearchobservable.elastic.Article;
import com.example.elasticsearchobservable.elastic.ArticleRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import reactor.core.observability.DefaultSignalListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;

import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureObservability
public class TraceabilityTest {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveController.class);
    @Autowired
    private ArticleRepository repository;

    @Autowired
    private ObservationRegistry observationRegistry;

    @BeforeAll
    public static void setup() {
        Hooks.enableAutomaticContextPropagation();
    }

    @Test
    public void checkTracingInLogStandalone() {
        // Create an Observation and observe your code
        Observation.createNotStarted("user.name", observationRegistry)
                .contextualName("getting-user-name")
                .observe(() -> {
                    logger.info("Hello");
                    assertNotNull(MDC.get("traceId"));
                    assertNotNull(MDC.get("spanId"));
                });
    }


    @Test
    public void checkTracingInLogStandardFlux() {
        // Create an Observation and observe your code
        Observation.createNotStarted("user.name", observationRegistry)
                .contextualName("getting-user-name")
                .observe(() -> {
                    fluxIteration();
                });
    }

    @Test
    public void checkTracingInLogElasticSearchCall() {
        // Create an Observation and observe your code
        Observation.createNotStarted("user.name", observationRegistry)
                .contextualName("getting-user-name")
                .observe(() -> {
                    elasticSearchRepoCall();
                });
    }

    private  void fluxIteration() {
        Flux.just(1, 2, 3)
                .delayElements(Duration.ofMillis(1))
                        .handle((result, sink) -> {
                            logger.info("inside flux handle");
                            assertNotNull(MDC.get("traceId"));
                            assertNotNull(MDC.get("spanId"));
                        })
//                .contextCapture()
                .blockLast();
    }

    private void elasticSearchRepoCall() {
        repository.findByName("Article 1")
                .map(Article::getName)
                .doOnNext(el -> {
                    logger.info("inside elastic search doOnNext");
                    assertNotNull(MDC.get("traceId"));
                    assertNotNull(MDC.get("spanId"));
                })
                .tap(() -> new DefaultSignalListener<>() {
                    @Override
                    public void doOnComplete() {
                        logger.info("inside elastic search doOnComplete");
                        assertNotNull(MDC.get("traceId"));
                        assertNotNull(MDC.get("spanId"));
                    }
                })
                .handle((result, sink) -> {
                    logger.info("inside elastic search handle");
                    assertNotNull(MDC.get("traceId"));
                    assertNotNull(MDC.get("spanId"));
                    sink.next(result);
                })
//                .contextCapture()
                .block();
    }

}
