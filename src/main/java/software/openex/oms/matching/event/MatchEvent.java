package software.openex.oms.matching.event;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import software.openex.oms.event.OMSEvent;

/**
 * @author Alireza Pourtaghi
 */
@Description("Event that is used to measure matching of orders.")
@Label("Order Matching Duration")
@Name("software.openex.oms.matching.engine.MatchEvent")
public final class MatchEvent extends OMSEvent {
    private final String symbol;

    public MatchEvent(final String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
