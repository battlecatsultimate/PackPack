package mandarin.packpack.supporter.bc;

import java.math.BigDecimal;

public class DPSNode {
    public final BigDecimal xCoordinate;
    public final BigDecimal slopeChange;
    public final BigDecimal valueChange;
    public boolean ignoreValue;

    public DPSNode(BigDecimal xCoordinate, BigDecimal slopeChange, BigDecimal valueChange) {
        this.xCoordinate = xCoordinate;
        this.slopeChange = slopeChange;
        this.valueChange = valueChange;
        this.ignoreValue = false;
    }

    public DPSNode(BigDecimal xCoordinate, BigDecimal slopeChange, BigDecimal valueChange, boolean ignoreValue) {
        this.xCoordinate = xCoordinate;
        this.slopeChange = slopeChange;
        this.valueChange = valueChange;
        this.ignoreValue = ignoreValue;
    }
}
