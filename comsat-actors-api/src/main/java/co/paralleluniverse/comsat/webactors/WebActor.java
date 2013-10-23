package co.paralleluniverse.comsat.webactors;

import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.fibers.SuspendExecution;

public abstract class WebActor extends BasicActor<Object, Void> {
    public static final String ACTOR_KEY = "co.paralleluniverse.actor";
}
