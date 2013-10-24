package co.paralleluniverse.comsat.webactors;

import co.paralleluniverse.strands.channels.SendPort;

public interface WebMessage {
    public SendPort<? extends WebResponse> sender();
}
