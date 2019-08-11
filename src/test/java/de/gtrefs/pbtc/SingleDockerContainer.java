package de.gtrefs.pbtc;

import net.jqwik.api.*;
import net.jqwik.api.stateful.Action;
import net.jqwik.api.stateful.ActionSequence;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleDockerContainer {

    @Property(tries = 10)
    public void starting_a_container_should_be_idempotent(@ForAll("starts") ActionSequence<GenericContainer> starts){
        try(GenericContainer alpine = new GenericContainer()) {
            starts.run(alpine);
            assertThat(alpine.isRunning()).isEqualTo(true);
        }
    }

    @Property(tries = 60)
    public void a_container_should_always_be_in_the_last_action_state(@ForAll("startAndStops") ActionSequence<GenericContainer> startsAndStops){
        try(GenericContainer alpine = new GenericContainer()) {
            startsAndStops.run(alpine);
            List<Action<GenericContainer>> actions = startsAndStops.runActions();
            if(actions.get(actions.size() -1) instanceof StopContainer){
                Statistics.collect("Container should be stopped");
                assertThat(alpine.isRunning()).isEqualTo(false);
            }
            if(actions.get(actions.size() -1) instanceof StartContainer){
                Statistics.collect("Container should be started");
                assertThat(alpine.isRunning()).isEqualTo(true);
            }
        }
    }

    @Provide
    Arbitrary<ActionSequence<GenericContainer>> startAndStops(){
        return Arbitraries.sequences(Arbitraries.of(new StartContainer(), new StopContainer()))
                .ofMinSize(3)
                .ofMaxSize(10);
    }

    @Provide
    Arbitrary<ActionSequence<GenericContainer>> starts(){
        return Arbitraries.sequences(Arbitraries.of(new StartContainer()))
                .ofMinSize(3)
                .ofMaxSize(10);
    }

    static class StopContainer implements Action<GenericContainer> {

        @Override
        public GenericContainer run(GenericContainer model) {
            model.stop();
            return model;
        }
    }

    static class StartContainer implements Action<GenericContainer> {

        @Override
        public GenericContainer run(GenericContainer model) {
            model.start();
            return model;
        }
    }
}