package com.google.android.apps.mytracks.io;

import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.mymaps.MapsFacade;
import com.google.android.apps.mytracks.io.mymaps.MapsFacade.AuthenticationRefresher;
import com.google.android.apps.mytracks.io.mymaps.MapsService;

import android.app.Activity;

/**
 * Factory to easily instantiate a {@link MapsFacade}.
 *
 * @author Rodrigo Damazio
 */
public class MyMapsFactory {

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
