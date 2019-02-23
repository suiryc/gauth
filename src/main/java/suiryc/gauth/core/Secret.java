package suiryc.gauth.core;

public class Secret {

    private String label;
    private String value;

    public Secret(String label, String value) {
        this.label = label;
        this.value = value;
    }

    /** Gets secret label. */
    public String getLabel() {
        return label;
    }

    /** Gets secret value. */
    public String getValue() {
        return value;
    }

}
