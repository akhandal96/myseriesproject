/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.download.torrents;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import tools.MySeriesLogger;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JProgressBar;
import myComponents.MyMessages;
import myComponents.MyUsefulFunctions;
import tools.download.torrents.uriSchemeHandler.CouldNotOpenUriSchemeHandler;
import tools.download.torrents.uriSchemeHandler.URISchemeHandler;
import tools.options.MySeriesOptions;
import tools.options.Paths;

/**
 *
 * @author lordovol
 */
public abstract class AbstractTorrentDownload implements TorrentConstants {

  protected AbstractTorrentForm form;
  protected URI uri;
  protected JProgressBar progress;

  protected void getTorrent(AbstractTorrent torrent) throws IOException {
    URI u = torrent.getUri();
    if (u != null) {
      if (torrent.isIsMagnetLink()) {
        URISchemeHandler uriSchemeHandler = new URISchemeHandler();
        try {
          uriSchemeHandler.open(u);
        } catch (CouldNotOpenUriSchemeHandler ex) {
          MyMessages.warning("Torrent Error", "Could not handle magnet link", true);
          MySeriesLogger.logger.log(Level.SEVERE, "Could not open magnet link uri", ex);
        }
      } else {
        if (isTorrent(torrent)) {
          progress.setString("Downloading the torrent");
          progress.setIndeterminate(true);
          MySeriesLogger.logger.log(Level.INFO, "Getting torrent {0}", torrent.getTitle());
          downloadTorrent(torrent);
        } else {
          MyMessages.warning("No Torrent", "No valid torrent is found", true);
        }
      }
    } else {
      MyMessages.warning("No Torrent", "No valid torrent is found", true);
    }
    form.dispose();
    myseriesproject.MySeries.glassPane.deactivate();
  }

  protected void getStream() {
    InputStream in = null;
    try {
      MySeriesLogger.logger.log(Level.INFO, "Getting torrent stream");
      URL rss = uri.toURL();
      in = rss.openStream();
      ArrayList<AbstractTorrent> torrents = readStream(in);
      progress.setString(torrents.size() + " torrents found");
      progress.setIndeterminate(false);
      if (torrents.isEmpty()) {
        MySeriesLogger.logger.log(Level.INFO, "No torrent found");
        MyMessages.warning("No Torrents", "No torrent was found", true);
      } else if (torrents.size() == 1) {
        MySeriesLogger.logger.log(Level.INFO, "Torrent found {0}", torrents.get(0).getTitle());
        getTorrent(torrents.get(0));
      } else {
        MySeriesLogger.logger.log(Level.INFO, "{0} torrents found", torrents.size());
        AbstractTorrent tor = getSelectedTorrent(torrents);
        if (tor != null) {
          getTorrent(tor);
        }
      }
    } catch (IOException ex) {
      MySeriesLogger.logger.log(Level.SEVERE, "Could not read torrent stream", ex);
    } finally {
      //myseries.MySeries.glassPane.deactivate();
    }
  }

  protected abstract AbstractTorrent getSelectedTorrent(ArrayList<AbstractTorrent> torrents);

  protected abstract ArrayList<AbstractTorrent> readStream(InputStream in);

  private void downloadTorrent(AbstractTorrent torrent) {
    try {
      if (!isTorrent(torrent)) {
        return;
      }
      MySeriesLogger.logger.log(Level.INFO, "Downloading the torrent");
      InputStream is = null;
      BufferedOutputStream outStream = null;
      byte[] buf;
      URLConnection uCon = torrent.getUri().toURL().openConnection();
      is = uCon.getInputStream();
      buf = new byte[1024];
      int ByteRead;
      int ByteWritten = 0;
      String torrentName;
      String[] t = torrent.getLink().split("/", -1);
      torrentName = t[t.length - 1];
      String filename = Paths.TORRENTS_PATH + torrentName;
      outStream = new BufferedOutputStream(new FileOutputStream(MySeriesOptions._USER_DIR_ + filename));
      while ((ByteRead = is.read(buf)) != -1) {
        outStream.write(buf, 0, ByteRead);
        ByteWritten += ByteRead;
      }
      is.close();
      outStream.close();
      MySeriesLogger.logger.log(Level.FINE, "Torrent downloaded");
      MySeriesLogger.logger.log(Level.INFO, "Opening torrent with default application");
      Desktop.getDesktop().open(new File(filename));
      form.dispose();
    } catch (IOException ex) {
      MySeriesLogger.logger.log(Level.SEVERE, "Could not read torrent stream", ex);
    } finally {
      myseriesproject.MySeries.glassPane.deactivate();
    }
  }

  protected abstract boolean isTorrent(AbstractTorrent torrent) throws MalformedURLException, IOException;
}
