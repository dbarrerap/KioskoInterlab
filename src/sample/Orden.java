package sample;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.json.JSONObject;

/**
 * Stores user data to perform queries and handling data.
 */
public class Orden {

    /**
     * orden, clave: Data given by the user
     * exist: response from WebService, 1 = exists, 0 = does not exist
     * content: full response from WebService
     */
    private StringProperty orden, clave;
    private JSONObject content;

    public Orden(String orden, String clave) {
        this.orden = new SimpleStringProperty(orden);
        this.clave = new SimpleStringProperty(clave);
    }

    String getOrden() {
        return orden.get();
    }

    String getClave() {
        return clave.get();
    }

    public JSONObject getContent() {
        return content;
    }

    public void setContent(JSONObject content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Orden: " + orden.get() + " Clave: " + clave.get();
    }
}
