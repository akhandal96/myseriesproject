/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.download.subtitles.tvsubtitles;

import database.SeriesRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import tools.MySeriesLogger;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import myComponents.MyMessages;
import myComponents.MyUsefulFunctions;
import tools.download.subtitles.Subtitle;
import tools.download.subtitles.SubtitleCode;
import tools.download.subtitles.SubtitleConstants;

/**
 * Gets the tvSubtitles code for a series
 * @author lordovol
 */
public class GetTvSubtitlesCode implements SubtitleConstants{

  public String tSubCode;
  private SeriesRecord series;
  private ArrayList<TCode> sLinks;

  /**
   * Gets the code for a seires
   * @param series The series
   */
  public GetTvSubtitlesCode(SeriesRecord series) {
    try {
      this.series = series;
      getCode();
    } catch (IOException ex) {
      MySeriesLogger.logger.log(Level.SEVERE, null, ex);
      MyMessages.error("I/O Error", "could not read from input stream");
    }
  }

  private void getCode() throws IOException {
    if (MyUsefulFunctions.hasInternetConnection(TV_SUBTITLES_URL)) {
      URL subsUrl = new URL(TV_SUBTITLES_URL + "search.php?q=" + URLEncoder.encode(series.getTitle(), "UTF-8"));
      BufferedReader in = new BufferedReader(new InputStreamReader(subsUrl.openStream()));
      parseSearchResult(in);
      in.close();
      if (  sLinks.isEmpty()) {
        MyMessages.message("Series not found", "The series " + series.getFullTitle() + " is not found in SubtitleOnline");
      } else if (sLinks.size() == 1) {
        this.tSubCode = sLinks.get(0).getCode();
      } else {
         TCode tl = (TCode) MyMessages.ask("Choose the right series", "Multiple series found", null, sLinks.toArray(), null);
            //TCode tl = (TCode) JOptionPane.showInputDialog(null, "Multiple series found", "Choose the right series", JOptionPane.QUESTION_MESSAGE, null, sLinks.toArray(), 0);
        if (tl != null) {
          this.tSubCode = tl.getCode();
        }
      }
    } else {
      MyMessages.internetError();
    }
  }

  private void parseSearchResult(BufferedReader in) throws IOException {
    sLinks = new ArrayList<TCode>();
    String line = "";
    boolean inResults = false;
    String curTitle = "";
    String curSOnlineCode = "";
    while ((line = in.readLine()) != null) {
      if (line.indexOf("<p class=\"description\">Search results") > -1) {
        String[] results = line.split("(<a href=\"/)|(</a>)", -1);
        for (int i = 0; i < results.length; i++) {
          String r = results[i];
          if (r.startsWith("tvshow")) {
            String code = r.replaceAll("(tvshow-)|(\\.html.++)", "");
            String title = r.replaceAll("(tvshow-\\d*\\.html\">)", "");
            TCode link = new TCode();
            link.setTitle(title);
            link.setCode(code);
            sLinks.add(link);
          }
        }
      }
    }

  }

  class TCode extends SubtitleCode {

    @Override
    public void setCode(String code) {
      this.code = code.trim() + "-" + series.getSeason();
    }
  }
}
