package test.org.springdoc.api.v30.app242;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class RootModel {

    private Integer rootProperty;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonUnwrapped
    private UnwrappedModelOne unwrappedModelOne;

    private UnwrappedModelTwo unwrappedModelTwo;

    public Integer getRootProperty() {
        return rootProperty;
    }

    public void setRootProperty(Integer rootProperty) {
        this.rootProperty = rootProperty;
    }

    public UnwrappedModelOne getUnwrappedModelOne() {
        return unwrappedModelOne;
    }

    public void setUnwrappedModelOne(UnwrappedModelOne unwrappedModelOne) {
        this.unwrappedModelOne = unwrappedModelOne;
    }

    public UnwrappedModelTwo getUnwrappedModelTwo() {
        return unwrappedModelTwo;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonUnwrapped
    public void setUnwrappedModelTwo(UnwrappedModelTwo unwrappedModelTwo) {
        this.unwrappedModelTwo = unwrappedModelTwo;
    }
}
