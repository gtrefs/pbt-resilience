package de.gtrefs.pbtc;

import net.jqwik.api.*;
import net.jqwik.api.stateful.Action;
import net.jqwik.api.stateful.ActionSequence;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

    @Property(tries = 10)
    public void an_action_with_precondition_false_should_not_be_executed(@ForAll("precondtionAlwaysFalse")ActionSequence<GenericContainer> actions){
        assertThatCode(() -> {
            GenericContainer noContainer = null;
            GenericContainer after = actions.run(noContainer);
        }).isInstanceOf(JqwikException.class).hasMessage("Could not generated a single action. At least 1 is required.");
        assertThat(actions.runActions()).hasSize(0);
    }

    @Property(tries = 10)
    public void a_start_action_should_not_be_run_if_the_container_is_running(@ForAll("noStartsWhenContainerIsRunning") ActionSequence<GenericContainer> actions){
        try(GenericContainer alpine = new GenericContainer(RUNNING_ALPINE_IMAGE)) {
            actions.run(alpine);
            assertThat(alpine.isRunning()).isEqualTo(true);
            assertThat(actions.runActions()).hasSize(1);
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

    @Provide
    Arbitrary<ActionSequence<GenericContainer>> precondtionAlwaysFalse(){
        return Arbitraries.sequences(Arbitraries.of(DockerAction.start().withPrecondition(container -> false)))
                .ofMinSize(3)
                .ofMaxSize(10);
    }

    @Provide
    Arbitrary<ActionSequence<GenericContainer>> noStartsWhenContainerIsRunning(){
        return Arbitraries.sequences(Arbitraries.of(DockerAction.start().withPrecondition(container -> !container.isRunning())))
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

        default DockerAction withPrecondition(Predicate<GenericContainer> precondition){
            var delegate = this;
            return new DockerAction() {
                @Override
                public boolean precondition(GenericContainer model) {
                    return precondition.test(model);
                }

                @Override
                public GenericContainer run(GenericContainer model) {
                    return delegate.run(model);
                }
            };
        }
    }
}