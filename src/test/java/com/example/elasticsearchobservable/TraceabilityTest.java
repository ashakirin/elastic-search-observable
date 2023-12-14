package com.example.elasticsearchobservable;

import com.example.elasticsearchobservable.elastic.Article;
import com.example.elasticsearchobservable.elastic.ArticleRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.observability.DefaultSignalListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureObservability
public class TraceabilityTest {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveController.class);
//    @Autowired
//    private ArticleRepository repository;

    @MockBean
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
                    assertThat(MDC.get("traceId")).isNotNull();
                    assertThat(MDC.get("spanId")).isNotNull();
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
        when(repository.findByName(any())).thenReturn(Mono.just(Article.builder().name("Test").build()));
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
                            assertThat(MDC.get("traceId")).isNotNull();
                            assertThat(MDC.get("spanId")).isNotNull();
                        })
//                .contextCapture()
                .blockLast();
    }

    private void elasticSearchRepoCall() {
        repository.findByName("Article 1")
                .map(Article::getName)
                .doOnNext(el -> {
                    logger.info("inside elastic search doOnNext");
                    assertThat(MDC.get("traceId")).isNotNull();
                    assertThat(MDC.get("spanId")).isNotNull();
                })
                .tap(() -> new DefaultSignalListener<>() {
                    @Override
                    public void doOnComplete() {
                        logger.info("inside elastic search doOnComplete");
                        assertThat(MDC.get("traceId")).isNotNull();
                        assertThat(MDC.get("spanId")).isNotNull();
                    }
                })
                .handle((result, sink) -> {
                    logger.info("inside elastic search handle");
                    assertThat(MDC.get("traceId")).isNotNull();
                    assertThat(MDC.get("spanId")).isNotNull();
                    sink.next(result);
                })
//                .contextCapture()
                .block();
    }

}
