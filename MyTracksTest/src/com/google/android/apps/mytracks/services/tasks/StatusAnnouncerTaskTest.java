/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.apps.mytracks.services.tasks;

import static com.google.android.testing.mocking.AndroidMock.capture;
import static com.google.android.testing.mocking.AndroidMock.eq;
import static com.google.android.testing.mocking.AndroidMock.expect;
import static com.google.android.testing.mocking.AndroidMock.same;

import com.google.android.apps.mytracks.services.tasks.StatusAnnouncerTask;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.easymock.Capture;

/**
 * Tests for {@link StatusAnnouncerTask}.
 * WARNING: I'm not responsible if your eyes start bleeding while reading this
 *          code. You have been warned. It's still better than no test, though.
 *
 * @author Rodrigo Damazio
 */
public class StatusAnnouncerTaskTest extends AndroidTestCase {

  // Use something other than our hardcoded value
  private static final Locale DEFAULT_LOCALE = Locale.KOREAN;
  private static final String ANNOUNCEMENT = "I can haz cheeseburger?";
  private Locale oldDefaultLocale;

  private StatusAnnouncerTask task;
  private StringUtils stringUtils;
  private StatusAnnouncerTask mockTask;
  private Capture<OnInitListener> initListenerCapture;
  private Capture<PhoneStateListener> phoneListenerCapture;
  private TextToSpeechDelegate ttsDelegate;
  private TextToSpeechInterface tts;

  /**
   * Mockable interface that we delegate TTS calls to.
   */
  interface TextToSpeechInterface {
    int addEarcon(String earcon, String packagename, int resourceId);
    int addEarcon(String earcon, String filename);
    int addSpeech(String text, String packagename, int resourceId);
    int addSpeech(String text, String filename);
    boolean areDefaultsEnforced();
    String getDefaultEngine();
    Locale getLanguage();
    int isLanguageAvailable(Locale loc);
    boolean isSpeaking();
    int playEarcon(String earcon, int queueMode,
        HashMap<String, String> params);
    int playSilence(long durationInMs, int queueMode,
        HashMap<String, String> params);
    int setEngineByPackageName(String enginePackageName);
    int setLanguage(Locale loc);
    int setOnUtteranceCompletedListener(OnUtteranceCompletedListener listener);
    int setPitch(float pitch);
    int setSpeechRate(float speechRate);
    void shutdown();
    int speak(String text, int queueMode, HashMap<String, String> params);
    int stop();
    int synthesizeToFile(String text, HashMap<String, String> params,
        String filename);
  }

  /**
   * Subclass of {@link TextToSpeech} which delegates calls to the interface
   * above.
   * The logic here is stupid and the author is ashamed of having to write it
   * like this, but basically the issue is that TextToSpeech cannot be mocked
   * without running its constructor, its constructor runs async operations
   * which call other methods (and then if the methods are part of a mock we'd
   * have to set a behavior, but we can't 'cause the object hasn't been fully
   * built yet).
   * The logic is that calls made during the constructor (when tts is not yet
   * set) will go up to the original class, but after tts is set we'll forward
   * them all to the mock.
   */
  private class TextToSpeechDelegate
      extends TextToSpeech implements TextToSpeechInterface {
    public TextToSpeechDelegate(Context context, OnInitListener listener) {
      super(context, listener);
    }

    @Override
    public int addEarcon(String earcon, String packagename, int resourceId) {
      if (tts == null) {
        return super.addEarcon(earcon, packagename, resourceId);
      }
      return tts.addEarcon(earcon, packagename, resourceId);
    }

    @Override
    public int addEarcon(String earcon, String filename) {
      if (tts == null) {
        return super.addEarcon(earcon, filename);
      }
      return tts.addEarcon(earcon, filename);
    }

    @Override
    public int addSpeech(String text, String packagename, int resourceId) {
      if (tts == null) {
        return super.addSpeech(text, packagename, resourceId);
      }
      return tts.addSpeech(text, packagename, resourceId);
    }

    @Override
    public int addSpeech(String text, String filename) {
      if (tts == null) {
        return super.addSpeech(text, filename);
      }
      return tts.addSpeech(text, filename);
    }

    @Override
    public Locale getLanguage() {
      if (tts == null) {
        return super.getLanguage();
      }
      return tts.getLanguage();
    }

    @Override
    public int isLanguageAvailable(Locale loc) {
      if (tts == null) {
        return super.isLanguageAvailable(loc);
      }
      return tts.isLanguageAvailable(loc);
    }

    @Override
    public boolean isSpeaking() {
      if (tts == null) {
        return super.isSpeaking();
      }
      return tts.isSpeaking();
    }

    @Override
    public int playEarcon(String earcon, int queueMode,
        HashMap<String, String> params) {
      if (tts == null) {
        return super.playEarcon(earcon, queueMode, params);
      }
      return tts.playEarcon(earcon, queueMode, params);
    }

    @Override
    public int playSilence(long durationInMs, int queueMode,
        HashMap<String, String> params) {
      if (tts == null) {
        return super.playSilence(durationInMs, queueMode, params);
      }
      return tts.playSilence(durationInMs, queueMode, params);
    }

    @Override
    public int setLanguage(Locale loc) {
      if (tts == null) {
        return super.setLanguage(loc);
      }
      return tts.setLanguage(loc);
    }

    @Override
    public int setOnUtteranceCompletedListener(
        OnUtteranceCompletedListener listener) {
      if (tts == null) {
        return super.setOnUtteranceCompletedListener(listener);
      }
      return tts.setOnUtteranceCompletedListener(listener);
    }

    @Override
    public int setPitch(float pitch) {
      if (tts == null) {
        return super.setPitch(pitch);
      }
      return tts.setPitch(pitch);
    }

    @Override
    public int setSpeechRate(float speechRate) {
      if (tts == null) {
        return super.setSpeechRate(speechRate);
      }
      return tts.setSpeechRate(speechRate);
    }

    @Override
    public void shutdown() {
      if (tts == null) {
        super.shutdown();
        return;
      }
      tts.shutdown();
    }

    @Override
    public int speak(
        String text, int queueMode, HashMap<String, String> params) {
      if (tts == null) {
        return super.speak(text, queueMode, params);
      }
      return tts.speak(text, queueMode, params);
    }

    @Override
    public int stop() {
      if (tts == null) {
        return super.stop();
      }
      return tts.stop();
    }

    @Override
    public int synthesizeToFile(String text, HashMap<String, String> params,
        String filename) {
      if (tts == null) {
        return super.synthesizeToFile(text, params, filename);
      }
      return tts.synthesizeToFile(text, params, filename);
    }
  }

  @UsesMocks({
    StatusAnnouncerTask.class,
    StringUtils.class,
  })
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    oldDefaultLocale = Locale.getDefault();
    Locale.setDefault(DEFAULT_LOCALE);
    stringUtils = AndroidMock.createMock(StringUtils.class, getContext());

    // Eww, the effort required just to mock TextToSpeech is insane
    final AtomicBoolean listenerCalled = new AtomicBoolean();
    OnInitListener blockingListener = new OnInitListener() {
      @Override
      public void onInit(int status) {
        synchronized (this) {
          listenerCalled.set(true);
          notify();
        }
      }
    };

    ttsDelegate = new TextToSpeechDelegate(getContext(), blockingListener);

    // Wait for all async operations done in the constructor to finish.
    synchronized (blockingListener) {
      while (!listenerCalled.get()) {
        // Releases the synchronized lock until we're woken up.
        blockingListener.wait();
      }
    }

    // Phew, done, now we can start forwarding calls
    tts = AndroidMock.createMock(TextToSpeechInterface.class);

    initListenerCapture = new Capture<OnInitListener>();
    phoneListenerCapture = new Capture<PhoneStateListener>();

    // Create a partial forwarding mock
    mockTask = AndroidMock.createMock(StatusAnnouncerTask.class,
        getContext(), stringUtils);
    task = new StatusAnnouncerTask(getContext(), stringUtils) {
      @Override
      protected TextToSpeech newTextToSpeech(Context ctx,
          OnInitListener onInitListener) {
        return mockTask.newTextToSpeech(ctx, onInitListener);
      }

      @Override
      protected String getAnnouncement(TripStatistics stats) {
        return mockTask.getAnnouncement(stats);
      }

      @Override
      protected void listenToPhoneState(
          PhoneStateListener listener, int events) {
        mockTask.listenToPhoneState(listener, events);
      }
    };
  }

  @Override
  protected void tearDown() {
    Locale.setDefault(oldDefaultLocale);
  }

  public void testStart() {
    doStart();
    OnInitListener ttsInitListener = initListenerCapture.getValue();
    assertNotNull(ttsInitListener);

    expect(tts.isLanguageAvailable(DEFAULT_LOCALE))
        .andStubReturn(TextToSpeech.LANG_AVAILABLE);
    expect(tts.setLanguage(DEFAULT_LOCALE))
        .andReturn(TextToSpeech.LANG_AVAILABLE);
    expect(tts.setSpeechRate(StatusAnnouncerTask.TTS_SPEECH_RATE))
        .andReturn(TextToSpeech.SUCCESS);

    AndroidMock.replay(tts, stringUtils);

    ttsInitListener.onInit(TextToSpeech.SUCCESS);

    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  public void testStart_languageNotSupported() {
    doStart();
    OnInitListener ttsInitListener = initListenerCapture.getValue();
    assertNotNull(ttsInitListener);

    expect(tts.isLanguageAvailable(DEFAULT_LOCALE))
        .andStubReturn(TextToSpeech.LANG_NOT_SUPPORTED);
    expect(tts.setLanguage(Locale.ENGLISH))
        .andReturn(TextToSpeech.LANG_AVAILABLE);
    expect(tts.setSpeechRate(StatusAnnouncerTask.TTS_SPEECH_RATE))
        .andReturn(TextToSpeech.SUCCESS);

    AndroidMock.replay(tts, stringUtils);

    ttsInitListener.onInit(TextToSpeech.SUCCESS);

    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  public void testStart_notReady() {
    doStart();
    OnInitListener ttsInitListener = initListenerCapture.getValue();
    assertNotNull(ttsInitListener);

    AndroidMock.replay(tts, stringUtils);

    ttsInitListener.onInit(TextToSpeech.ERROR);

    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  public void testShutdown() {
    // First, start
    doStart();
    AndroidMock.verify(mockTask);
    AndroidMock.reset(mockTask);

    // Then, shut down
    PhoneStateListener phoneListener = phoneListenerCapture.getValue();
    mockTask.listenToPhoneState(
        same(phoneListener), eq(PhoneStateListener.LISTEN_NONE));
    tts.shutdown();
    AndroidMock.replay(mockTask, tts, stringUtils);
    task.shutdown();
    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  public void testRun() throws Exception {
    // Expect service data calls
    TripStatistics stats = new TripStatistics();

    // Expect announcement building call
    expect(mockTask.getAnnouncement(same(stats))).andStubReturn(ANNOUNCEMENT);

    // Put task in "ready" state
    startTask(TextToSpeech.SUCCESS);

    // Expect actual announcement call
    expect(tts.speak(
        eq(ANNOUNCEMENT), eq(TextToSpeech.QUEUE_FLUSH),
        AndroidMock.<HashMap<String, String>>isNull()))
            .andReturn(TextToSpeech.SUCCESS);

    // Run the announcement
    AndroidMock.replay(tts, stringUtils);
    task.runWithStatistics(stats);
    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  public void testRun_notReady() throws Exception {
    // Put task in "not ready" state
    startTask(TextToSpeech.ERROR);

    // Run the announcement
    AndroidMock.replay(tts, stringUtils);
    task.runWithStatistics(null);
    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  public void testRun_duringCall() throws Exception {
    startTask(TextToSpeech.SUCCESS);

    expect(tts.isSpeaking()).andStubReturn(false);

    // Run the announcement
    AndroidMock.replay(tts, stringUtils);
    PhoneStateListener phoneListener = phoneListenerCapture.getValue();
    phoneListener.onCallStateChanged(TelephonyManager.CALL_STATE_OFFHOOK, null);
    task.runWithStatistics(null);
    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  public void testRun_ringWhileSpeaking() throws Exception {
    startTask(TextToSpeech.SUCCESS);

    expect(tts.isSpeaking()).andStubReturn(true);
    expect(tts.stop()).andReturn(TextToSpeech.SUCCESS);

    AndroidMock.replay(tts, stringUtils);

    // Update the state to ringing - this should stop the current announcement.
    PhoneStateListener phoneListener = phoneListenerCapture.getValue();
    phoneListener.onCallStateChanged(TelephonyManager.CALL_STATE_RINGING, null);

    // Run the announcement - this should do nothing.
    task.runWithStatistics(null);

    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  public void testRun_whileRinging() throws Exception {
    startTask(TextToSpeech.SUCCESS);

    expect(tts.isSpeaking()).andStubReturn(false);

    // Run the announcement
    AndroidMock.replay(tts, stringUtils);
    PhoneStateListener phoneListener = phoneListenerCapture.getValue();
    phoneListener.onCallStateChanged(TelephonyManager.CALL_STATE_RINGING, null);
    task.runWithStatistics(null);
    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  public void testRun_noService() throws Exception {
    startTask(TextToSpeech.SUCCESS);

    // Run the announcement
    AndroidMock.replay(tts, stringUtils);
    task.run(null);
    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  public void testRun_noStats() throws Exception {
    // Expect service data calls

    startTask(TextToSpeech.SUCCESS);

    // Run the announcement
    AndroidMock.replay(tts, stringUtils);
    task.runWithStatistics(null);
    AndroidMock.verify(mockTask, tts, stringUtils);
  }

  private void startTask(int state) {
    AndroidMock.resetToNice(tts, stringUtils);
    AndroidMock.replay(tts, stringUtils);
    doStart();
    OnInitListener ttsInitListener = initListenerCapture.getValue();
    ttsInitListener.onInit(state);
    AndroidMock.resetToDefault(tts, stringUtils);
  }

  private void doStart() {
    mockTask.listenToPhoneState(capture(phoneListenerCapture),
        eq(PhoneStateListener.LISTEN_CALL_STATE));
    expect(mockTask.newTextToSpeech(
        same(getContext()), capture(initListenerCapture)))
        .andStubReturn(ttsDelegate);
    AndroidMock.replay(mockTask);
    task.start();
  }
}
