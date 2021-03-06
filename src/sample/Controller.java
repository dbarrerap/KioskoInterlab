package sample;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    /**
     * Data provided by Interlab to access WebService
     */
    private String myURL = "http://181.39.15.91:8080/WsInterlab/rest/seguridad/obtenerResultado?";
    private final String credentials = "userinterlab:inter$18l@b";

    @FXML private TextField txtOrden;
    @FXML private PasswordField txtClave;
    @FXML private VBox kbfororden, kbforclave;
    @FXML Button btnConsultar;

    private String err_msg = "";

    /**
     * Check if all TextFields are not empty.
     * @return boolean.
     */
    private boolean valid_data() {
        String orden = txtOrden.getText();
        String clave = txtClave.getText();
        orden = orden.trim();
        clave = clave.trim();

        if (orden.isEmpty())
            err_msg += "- Numero de Orden vacio\n";
        if (clave.isEmpty())
            err_msg += "- Clave vacio\n";

        if (!err_msg.equals(""))
        {
            err_msg += "La consulta ha fallado. Por favor revisar los campos mencionados.";
            return false;
        }
        return true;
    }

    /**
     * Clear fields for new query.
     */
    public void cancelar(ActionEvent event)
    {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("first.fxml"));
            Stage window = (Stage) ((Node)event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            window.setScene(scene);
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void limpiar()
    {
        txtOrden.clear();
        txtClave.clear();
        txtOrden.requestFocus();
    }

    /**
     * Query for Interlab report against WebService
     * @param ae Node (Button) that executes the query.
     */
    public void consultar(ActionEvent ae) {

        if (valid_data())
        {
            MyAsyncTask task = new MyAsyncTask(ae);
            task.setDaemon(false);
            task.execute(myURL);

        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Alerta");
            alert.setHeaderText("Datos Invalidos");
            alert.setContentText(err_msg);
            alert.showAndWait();
            err_msg = "";
            limpiar();
        }
    }

    public void kbForOrden(ActionEvent e) {
        String c = ((Button) e.getSource()).getText();
        txtOrden.setText(txtOrden.getText() + c);
    }

    public void kbForClave(ActionEvent e) {
        String c = ((Button) e.getSource()).getText();
        txtClave.setText(txtClave.getText() + c);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtOrden.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                kbforclave.setVisible(false);
                kbfororden.setVisible(true);
            }
        });
        txtClave.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                kbforclave.setVisible(true);
                kbfororden.setVisible(false);
            }
        });
    }

    /**
     * Asynchronous Task.
     * Handles networking in the background so UI thread is not compromised.
     */
    private class MyAsyncTask extends AsyncTask<String, Integer, Boolean> {
        private Orden orden;
        private ActionEvent e;
        private HttpURLConnection conn;
        private StringBuilder sb;

        MyAsyncTask(ActionEvent e) {
            this.e = e;
        }

        /**
         * Creates a new Orden object. Disables query button.
         */
        @Override
        public void onPreExecute() {
            orden = new Orden(txtOrden.getText(), txtClave.getText());
            btnConsultar.setDisable(true);
            btnConsultar.setText("Consultando...");
        }

        /**
         * Query the WebService for report
         * @param params not used.
         * @return Boolean if reports exists or not.
         */
        @Override
        public Boolean doInBackground(String... params) {
            try {
                URL url = new URL(params[0] + "arg0=" + orden.getOrden() + "&arg1=" + orden.getClave() + "&arg2=1");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String auth = Base64.getEncoder().encodeToString(credentials.getBytes());
                conn.setRequestProperty("Authorization", "Basic " + auth);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    if (Integer.parseInt(conn.getHeaderField("Content-Length")) > 0)
                    {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        sb = new StringBuilder();
                        String output;
                        while ((output = br.readLine()) != null)
                            sb.append(output);

                        JSONObject json = new JSONObject(sb.toString());
                        orden.setContent(json);
                        return orden.getContent().getInt("respuesta")==1;
                    } else {
                        return false;
                    }
                }
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * If report exists, store data into Orden object and call new Scene
         * @param success Response from doInBackground.
         */
        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                try {
                    FXMLLoader loader = new FXMLLoader();
                    loader.setLocation(getClass().getResource("resultado.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root);
                    Resultado controller = loader.getController();

                    controller.initData(orden);

                    Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
                    stage.setScene(scene);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    btnConsultar.setDisable(false);
                    btnConsultar.setText("Consultar");
                } finally {
                    conn.disconnect();
                }
            } else {
                Alert alert = new Alert(AlertType.WARNING);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setTitle("Alerta");
                alert.setHeaderText("Orden no encontrada");
                alert.setContentText("Su examen no est?? listo o tiene saldo pendiente.\nPor favor, acercarse a la ventanilla.");
                alert.showAndWait();
                btnConsultar.setDisable(false);
                btnConsultar.setText("Consultar");
            }
        }

        @Override
        public void progressCallback(Integer... params) {

        }
    }
}
