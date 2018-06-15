package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

public class Resultado implements Initializable {
    @FXML private WebView webView;

    private Orden orden;
    private static final int BUFFER_SIZE = 8192;
    private String filename = "resultados.pdf";

    /**
     * Receives data from Query Scene
     * @param orden Orden object with data to retrieve file.
     */
    void initData(Orden orden)
    {
        this.orden = orden;
    }

    /**
     * Prints PDF file and returns to Query Scene
     * @param ae Used to get Stage
     */
    public void imprimir(ActionEvent ae) {
        try {
            PDDocument document = PDDocument.load(new File(filename));
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(document));
            job.print();
            document.close();
        } catch (IOException | PrinterException e) {
            e.printStackTrace();
        }

        try {
            Stage window = (Stage)((Node)ae.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
            Scene scene = new Scene(root);
            window.setScene(scene);
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MyAsyncTask task = new MyAsyncTask();
        task.setDaemon(false);
        task.execute();
    }

    /**
     * Asynchronous Task.
     * Handles networking in the background so UI thread is not compromised.
     */
    private class MyAsyncTask extends AsyncTask<String, Integer, Boolean> {
        WebEngine engine = webView.getEngine();

        @Override
        public void onPreExecute() {

        }

        /**
         * Retrieve file from Orden object's URL
         * @param params not used.
         * @return Boolean if file has been successfully retrieved.
         */
        @Override
        public Boolean doInBackground(String... params) {
            try {
                URL url = new URL(orden.getContent().getString("url"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    InputStream in = url.openStream();
                    /*OutputStream out = new FileOutputStream(filename);
                    int length;
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while ((length = in.read(buffer)) != -1) {
                        out.write(buffer, 0, length);
                        progressCallback(length);
                    }*/
                    Files.copy(in, new File(filename).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } else {
                    return false;
                }

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * Shows file in PDF.js viewer.
         * @param success Response from doInBackground.
         */
        @Override
        public void onPostExecute(Boolean success) {
            if (success) {
                System.out.println("Cargando visor...");
                engine.setJavaScriptEnabled(true);
                File f = new File("viewer.html");
                engine.load(f.toURI().toString());
                System.out.println("Visor cargado.");
            } else {
                Alert alert = new Alert(AlertType.WARNING);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setTitle("Error con archivo");
                alert.setHeaderText("Error cargando el archivo...");
                alert.setContentText("Ha habido un error cargando el archivo.\nConsultar en ventanilla.");
                alert.showAndWait();
            }

        }

        @Override
        public void progressCallback(Integer... params) {
            System.out.println("Progress: " + params[0]);
        }
    }
}
