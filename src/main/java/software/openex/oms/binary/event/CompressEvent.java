package software.openex.oms.binary.event;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import software.openex.oms.event.OMSEvent;

/**
 * @author Alireza Pourtaghi
 */
@Description("Event that is used to measure LZ4 compression duration of a binary representation.")
@Label("Compressing Duration")
@Name("software.openex.oms.binary.event.CompressEvent")
public final class CompressEvent extends OMSEvent {
    @Label("Model ID")
    private final int id;

    public CompressEvent(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
