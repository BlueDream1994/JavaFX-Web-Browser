/*
 *  This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

   Also(warning!):
 
  1)You are not allowed to sell this product to third party.
  2)You can't change license and made it like you are the owner,author etc.
  3)All redistributions of source code files must contain all copyright
     notices that are currently in this file, and this list of conditions without
     modification.
 */


package application;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.validator.routines.UrlValidator;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;
import javafx.scene.web.WebView;
import marquee.Marquee;

/**
 * @author GOXR3PLUS
 *
 */
public class WebBrowserTabController extends StackPane {

    /** The logger. */
    private final Logger logger = Logger.getLogger(getClass().getName());

    //------------------------------------------------------------

    @FXML
    private BorderPane borderPane;

    @FXML
    private JFXButton backwardButton;

    @FXML
    private JFXButton forwardButton;

    @FXML
    private JFXTextField searchBar;

    @FXML
    private ComboBox<String> searchEngineComboBox;

    @FXML
    private JFXButton reloadButton;

    @FXML
    private WebView webView;

    // -------------------------------------------------------------

    /** The engine. */
    WebEngine webEngine;

    /** The web history */
    WebHistory history;
    ObservableList<WebHistory.Entry> historyEntryList;

    Tab tab;
    String firstWebSite;

    /**
     * Constructor
     * 
     * @param tab
     * @param firstWebSite
     */
    public WebBrowserTabController(Tab tab, String firstWebSite) {
	this.tab = tab;
	this.firstWebSite = firstWebSite;
	this.tab.setContent(this);

	// ------------------------------------FXMLLOADER ----------------------------------------
	FXMLLoader loader = new FXMLLoader(getClass().getResource("WebBrowserTabController.fxml"));
	loader.setController(this);
	loader.setRoot(this);

	try {
	    loader.load();
	} catch (IOException ex) {
	    logger.log(Level.SEVERE, "", ex);
	}
    }

    /**
     * Called as soon as .fxml is initialized [[SuppressWarningsSpartan]]
     */
    @FXML
    private void initialize() {

	//-------------------WebView------------------------	
	// hide webview scrollbars whenever they appear.
	webView.getChildrenUnmodifiable().addListener((Change<? extends Node> change) -> {
	    Set<Node> deadSeaScrolls = webView.lookupAll(".scroll-bar");
	    for (Node scroll : deadSeaScrolls) {
		scroll.setVisible(false);
		scroll.setManaged(false);
	    }
	});

	//-------------------WebEngine------------------------
	webEngine = webView.getEngine();
	webEngine.getLoadWorker().exceptionProperty().addListener(error -> {

	    //----------If you want the below to work you have to add ControlsFX Library in the class path---------

	    //	    ActionTool.showNotification("Error Occured",
	    //		    "Trying to connect to a website error occured:\n\t["
	    //			    + webEngine.getLoadWorker().getException().getMessage()
	    //			    + "]\nMaybe you don't have internet connection.",
	    //		    Duration.seconds(2), NotificationType.ERROR);

	    System.out.println("An error happening trying to load the website");
	});

	history = webEngine.getHistory();
	historyEntryList = history.getEntries();
	SimpleListProperty<Entry> list = new SimpleListProperty<>(historyEntryList);

	//-------------------TAB------------------------
	tab.setTooltip(new Tooltip(""));
	tab.getTooltip().textProperty().bind(webEngine.titleProperty());
	//tab.textProperty().bind(webEngine.titleProperty())

	// Graphic
	StackPane stack = new StackPane();

	// indicator
	ProgressBar indicator = new ProgressBar();
	indicator.progressProperty().bind(webEngine.getLoadWorker().progressProperty());
	indicator.visibleProperty().bind(webEngine.getLoadWorker().runningProperty());
	indicator.setMaxSize(30, 11);

	// text
	Text text = new Text();
	text.setStyle("-fx-font-size:70%;");
	text.textProperty().bind(Bindings.max(0, indicator.progressProperty()).multiply(100.00).asString("%.02f %%"));
	// text.visibleProperty().bind(library.getSmartController().inputService.runningProperty())

	Marquee marquee = new Marquee();
	marquee.textProperty().bind(tab.getTooltip().textProperty());
	marquee.setStyle(
		"-fx-background-radius:0 0 0 0; -fx-background-color:rgb(255,255,255,0.5); -fx-border-color:transparent;");

	stack.getChildren().addAll(indicator, text);
	stack.setManaged(false);
	stack.setVisible(false);

	// stack
	indicator.visibleProperty().addListener(l -> {
	    if (indicator.isVisible()) {
		stack.setManaged(true);
		stack.setVisible(true);
	    } else {
		stack.setManaged(false);
		stack.setVisible(false);
	    }
	});

	// HBOX
	HBox hBox = new HBox();
	hBox.getChildren().addAll(stack, marquee);
	tab.setGraphic(hBox);

	//-------------------Items------------------------
	//searchBar
	searchBar.focusedProperty().addListener((observable, oldValue, newValue) -> {
	    if (newValue) // if focused
		searchBar.textProperty().unbind();
	    else
		searchBar.textProperty().bind(webEngine.locationProperty());
	});
	searchBar.setOnAction(a -> loadWebSite(searchBar.getText()));

	//reloadButton
	reloadButton.setOnAction(a -> {
	    if (history.getEntries().isEmpty())
		webEngine.load("about:home");
	    else
		webEngine.reload();
	});

	//backwardButton
	backwardButton.setOnAction(a -> goBack());
	backwardButton.disableProperty().bind(history.currentIndexProperty().isEqualTo(0));

	//forwardButton
	forwardButton.setOnAction(a -> goForward());
	forwardButton.disableProperty()
		.bind(history.currentIndexProperty().greaterThanOrEqualTo(list.sizeProperty().subtract(1)));

	//searchEngineComboBox
	searchEngineComboBox.getItems().addAll("Google", "DuckDuckGo", "Bing", "Yahoo");
	searchEngineComboBox.getSelectionModel().select(1);

	//System.out.println(history.getCurrentIndex() + "," + historyEntryList.size())

	//Load the website
	loadWebSite(firstWebSite);
    }

    /**
     * Return the Search Url for the Search Provider For example for `Google` returns `https://www.google.com/search?q=`
     * 
     * @param searchProvider
     * @return The Search Engine Url
     */
    public String getSearchEngineSearchUrl(String searchProvider) {
	//Find
	switch (searchProvider.toLowerCase()) {
	case "bing":
	    return "http://www.bing.com/search?q=";
	case "duckduckgo":
	    return "https://duckduckgo.com/?q=";
	case "yahoo":
	    return "https://search.yahoo.com/search?p=";
	default: //then google
	    return "https://www.google.com/search?q=";
	}
    }

    /**
     * Return the Search Url for the Search Provider For example for `Google` returns `https://www.google.com/search?q=`
     * 
     * @param searchProvider
     * @return The Search Engine Url
     */
    public String getSearchEngineHomeUrl(String searchProvider) {
	//Find
	switch (searchProvider.toLowerCase()) {
	case "bing":
	    return "http://www.bing.com";
	case "duckduckgo":
	    return "https://duckduckgo.com";
	case "yahoo":
	    return "https://search.yahoo.com";
	default: //then google
	    return "https://www.google.com";
	}
    }

    /**
     * Loads the given website , either directly if the url is a valid WebSite Url or using a SearchEngine like Google
     * 
     * @param webSite
     */
    public void loadWebSite(String webSite) {
	//Search if it is a valid WebSite url
	String load = !new UrlValidator().isValid(webSite) ? null : webSite;

	//Load
	try {
	    webEngine.load(load != null ? load
		    : getSearchEngineSearchUrl(searchEngineComboBox.getSelectionModel().getSelectedItem())
			    + URLEncoder.encode(searchBar.getText(), "UTF-8"));
	} catch (UnsupportedEncodingException ex) {
	    ex.printStackTrace();
	}

    }

    /**
     * Loads the default website
     */
    public void loadDefaultWebSite() {
	webEngine.load(getSearchEngineHomeUrl(searchEngineComboBox.getSelectionModel().getSelectedItem()));
    }

    /**
     * Goes Backward one Page
     * 
     */
    public void goBack() {
	history.go(historyEntryList.size() > 1 && history.getCurrentIndex() > 0 ? -1 : 0);
	//System.out.println(history.getCurrentIndex() + "," + historyEntryList.size())
    }

    /**
     * Goes Forward one Page
     * 
     */
    public void goForward() {
	history.go(historyEntryList.size() > 1 && history.getCurrentIndex() < historyEntryList.size() - 1 ? 1 : 0);
	//System.out.println(history.getCurrentIndex() + "," + historyEntryList.size())
    }

}
