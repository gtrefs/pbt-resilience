package de.gtrefs.pbtc;

import net.jqwik.api.*;
import net.jqwik.api.stateful.Action;
import net.jqwik.api.stateful.ActionSequence;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleDockerContainer {
    final static Path RESOURCE_PATH = Paths.get("src/test/resources/docker/running-alpine");
    ImageFromDockerfile RUNNING_ALPINE_IMAGE = new ImageFromDockerfile().withFileFromPath(".", RESOURCE_PATH);

    @Property(tries = 10)
    public void starting_a_container_should_be_idempotent(@ForAll("starts") ActionSequence<GenericContainer> starts){
        try(GenericContainer alpine = new GenericContainer(RUNNING_ALPINE_IMAGE)) {
            starts.run(alpine);
            assertThat(alpine.isRunning()).isEqualTo(true);
        }
    }

    @Property(tries = 10)
    public void a_container_should_always_be_in_the_last_action_state(@ForAll("startAndStops") ActionSequence<GenericContainer> startsAndStops){
        try(GenericContainer alpine = new GenericContainer(RUNNING_ALPINE_IMAGE)) {
            startsAndStops.run(alpine);
            List<Action<GenericContainer>> actions = startsAndStops.runActions();
            if(actions.get(actions.size() -1).equals(DockerAction.stop())){
                Statistics.collect("Container should be stopped");
                assertThat(alpine.isRunning()).isEqualTo(false);
            }
            if(actions.get(actions.size() -1).equals(DockerAction.start())){
                Statistics.collect("Container should be started");
                assertThat(alpine.isRunning()).isEqualTo(true);
            }
        }
    }

    @Provide
    Arbitrary<ActionSequence<GenericContainer>> startAndStops(){
        return Arbitraries.sequences(Arbitraries.of(DockerAction.start(), DockerAction.stop()))
                .ofMinSize(3)
                .ofMaxSize(10);
    }

    @Provide
    Arbitrary<ActionSequence<GenericContainer>> starts(){
        return Arbitraries.sequences(Arbitraries.of(DockerAction.start()))
                .ofMinSize(3)
                .ofMaxSize(10);
    }

    public interface DockerAction extends Action<GenericContainer> {
        static DockerAction start() {
            return container -> {
                container.start();
                return container;
            };
        }

        static DockerAction stop() {
            return container -> {
                container.stop();
                return container;
            };
        }
    }
}