package com.google.android.apps.mytracks.io;

import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.mymaps.MapsFacade;
import com.google.android.apps.mytracks.io.mymaps.MapsService;
import com.google.android.apps.mytracks.io.mymaps.MapsStringsProvider;
import com.google.android.apps.mytracks.io.mymaps.MapsFacade.AuthenticationRefresher;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.Context;

/**
 * Factory to easily instantiate a {@link MapsFacade}.
 *
 * @author Rodrigo Damazio
 */
public class MyMapsFactory {

  /**
   * Simple provider for reading strings from resources.
   */
  private static class MyMapsStringProvider implements MapsStringsProvider {
    private final Context context;

    public MyMapsStringProvider(Context context) {
      this.context = context;
    }

    @Override
    public String getNewMapDescription() {
      return context.getString(R.string.new_map_description);
    }

    @Override
    public String getStart() {
      return context.getString(R.string.start);
    }

    @Override
    public String getEnd() {
      return context.getString(R.string.end);
    }
  }

  /**
   * Creates a new Maps façade interface instance.
   *
   * @param context current context
   * @param auth a valid authentication manager
   * @return the façade
   */
  public static MapsFacade newMapsClient(Activity context, final AuthManager auth) {
    MapsFacade client = MapsService.newClient(context,
        GDataClientFactory.getGDataClient(context),
        new MyMapsStringProvider(context),
        auth.getAuthToken());
    client.setAuthenticationRefresher(new AuthenticationRefresher() {
      @Override
      public void invalidateAndRefresh(Runnable done) {
        auth.invalidateAndRefresh(done);
      }
    });
    return client;
  }

  private MyMapsFactory() {}
}
