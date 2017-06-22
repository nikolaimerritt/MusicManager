package sample;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.stage.WindowEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javax.xml.soap.Text;
import java.awt.*;


public class Main
{
    public static void initialiseGUI()
    {
        // making Stage container
        Stage stage = new Stage();
        stage.setTitle("Hello world!");
        stage.setResizable(true);

        // making GridPane container
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(5);
        grid.setHgap(5);

        // defining name text field
        final TextField name = new TextField();
        name.setPromptText("Enter your first name");
        name.setPrefColumnCount(10);
        name.getText();
        GridPane.setConstraints(name, 0, 0);
        grid.getChildren().add(name);

        // defining last name text field
        final TextField lastName = new TextField();
        lastName.setPromptText("Enter your last name");
        lastName.setPrefColumnCount(10);
        lastName.getText();
        GridPane.setConstraints(lastName, 0, 1);
        grid.getChildren().add(lastName);

        // defining comment text field
        final TextField comment = new TextField();
        comment.setPromptText("Enter your comments");
        comment.setPrefColumnCount(10);
        comment.getText();
        GridPane.setConstraints(comment, 0, 2);
        grid.getChildren().add(comment);

        // defining submit button
        Button submit = new Button("Submit");
        TextField[] allTextFields = new TextField[]{name, lastName, comment};
        submit.setOnAction((ActionEvent action) -> {
            System.out.println(textFromFields(allTextFields));
        });
        GridPane.setConstraints(submit, 1, 0);
        grid.getChildren().add(submit);

        // defining the clear button
        Button clear = new Button("Clear");
        GridPane.setConstraints(clear, 1, 1);
        grid.getChildren().add(clear);

        // adding an image as the background
        /*Image image = new Image("red.jpg");
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setLayoutX(300);
        imageView.setLayoutY(300);
        imageView.setFitWidth(200);
        imageView.setPreserveRatio(true);
        grid.getChildren().add(imageView);*/

        // finally setting up stage
        stage.setScene(new Scene(grid, 900, 900));
        stage.setWidth(400);
        stage.setHeight(400);
        stage.setOnCloseRequest((WindowEvent we) -> terminate());
        stage.show();
    }

    public static String textFromFields(TextField[] textFields)
    {
        String allText = "";
        for (TextField textField : textFields)
        {
            allText += textField.getText() + "\t";
        }

        return allText;
    }

    public static void terminate()
    {
        System.out.println("BAI BAI");
        System.exit(0);
    }

    public static void launchFX()
    {
        new JFXPanel();
        Platform.runLater(() -> initialiseGUI());
    }

    public static void main(String[] args)
    {
        launchFX();
    }


}
