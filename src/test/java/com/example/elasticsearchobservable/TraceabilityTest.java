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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
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
    public void checkTracingInLogStandalone(CapturedOutput capturedOutput) {
        // Create an Observation and observe your code
        Observation.createNotStarted("user.name", observationRegistry)
                .contextualName("getting-user-name")
                .observe(() -> {
                    logger.info("Hello");
                });

        checkOutput(capturedOutput, "Hello");
    }


    @Test
    public void checkTracingInLogStandardFlux(CapturedOutput capturedOutput) {
        // Create an Observation and observe your code
        Observation.createNotStarted("user.name", observationRegistry)
                .contextualName("getting-user-name")
                .observe(() -> {
                    fluxIteration();
                });
        checkOutput(capturedOutput, "inside flux handle");
    }

    @Test
    public void checkTracingInLogElasticSearchCall(CapturedOutput capturedOutput) {
        // Create an Observation and observe your code
        Observation.createNotStarted("user.name", observationRegistry)
                .contextualName("getting-user-name")
                .observe(() -> {
                    elasticSearchRepoCall();
                });
        checkOutput(capturedOutput, "inside elastic search handle");
        checkOutput(capturedOutput, "inside elastic search doOnComplete");
    }

    private static void checkOutput(CapturedOutput capturedOutput, String match) {
        String[] lines = capturedOutput.getOut().split("\\n");
        boolean result = Arrays.stream(lines)
                .filter(s -> s.contains(match))
                .allMatch(s -> !s.contains("[server,,]"));
        assertTrue(result);
    }

    private static void fluxIteration() {
        Flux.just(1, 2, 3)
                .delayElements(Duration.ofMillis(1))
                        .handle((result, sink) -> logger.info("inside flux handle"))
//                .contextCapture()
                .blockLast();
    }

    private void elasticSearchRepoCall() {
        repository.findByName("Article 1")
                .map(Article::getName)
                .tap(() -> new DefaultSignalListener<>() {
                    @Override
                    public void doOnComplete() {
                        logger.info("inside elastic search doOnComplete");
//                        assertThat(output.getOut()).contains("my trace id");
                    }
                })
                .handle((result, sink) -> {
                    logger.info("inside elastic search handle");
                    //                  assertThat(output.getOut()).contains("my trace id");
                    sink.next(result);
                })
//                .contextCapture()
                .block();
    }

}
