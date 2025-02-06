package software.openex.oms.binary.event;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import software.openex.oms.event.OMSEvent;

/**
 * @author Alireza Pourtaghi
 */
@Description("Event that is used to measure encoding duration of a model into native memory.")
@Label("Encoding Duration")
@Name("software.openex.oms.binary.event.EncodeEvent")
public final class EncodeEvent extends OMSEvent {
    @Label("Model ID")
    private final int id;

    public EncodeEvent(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
