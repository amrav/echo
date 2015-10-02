package be.tarsos.tarsos.dsp.android.ui;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

public class SendContactActivity extends ActionBarActivity {

    EditText editText;
    Button playButton;
    String numberGlobal;
    int prevGlobal;
    int[] songs = {R.raw.sa, R.raw.re2, R.raw.ga, R.raw.ma, R.raw.pa, R.raw.dha, R.raw.ni, R.raw.sa2};
    MediaPlayer mp;
    private LinearLayout backgroundLayout;
    private Drawable[] backgrounds;
    private int currentBackground = 0;
    private double[] scale = new double[]{126, 146, 151, 174, 188, 214, 237, 255};
    private double tolerance = 0.01;
    private int lastNote = 0, currCount = 0, curr = 0;

    private void ripple() {
        final RippleDrawable rippleDrawable = (RippleDrawable) backgroundLayout.getBackground();
        Point size = new Point();
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(size);
        rippleDrawable.setHotspot(size.x / 2, size.y / 2);
        rippleDrawable.setState(new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled});

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                backgroundLayout.setBackground(backgrounds[(++currentBackground) % 2]);
                rippleDrawable.setState(new int[]{});
            }
        }, 500);
        //backgroundLayout.setBackground(getDrawable(R.drawable.ripple2));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tarsos_dsp);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);

        dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, new PitchDetectionHandler() {

            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult,
                                    AudioEvent audioEvent) {
                final float pitchInHz = pitchDetectionResult.getPitch();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        TextView resultView = (TextView) findViewById(R.id.resultTextView);

                        for (int i = 0; i < scale.length; ++i) {
                            if (pitchInHz < (1 + tolerance) * scale[i] &&
                                    pitchInHz > (1 - tolerance) * scale[i] &&
                                    i != lastNote) {
                                if (i == curr) {
                                    currCount++;
                                    Log.d("curr", "incremented a " + i);
                                } else {
                                    Log.d("curr", "found a " + i);
                                    curr = i;
                                    currCount = 1;
                                }
                                if (currCount > 2) {
                                    if (lastNote < i) i--;
                                    String currentString = resultView.getText().toString() + i;
                                    resultView.setText(currentString);
                                    ripple();
                                    if (isDone(currentString) == true) {
                                        resultView.setText(convertToBase10(currentString));
                                        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
                                        // Sets the MIME type to match the Contacts Provider
                                        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
                                        intent.putExtra(ContactsContract.Intents.Insert.PHONE, currentString)
                                                .putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                                                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
                                        startActivity(intent);
                                        //stop listening, go to contact page
                                    }
                                    if (lastNote <= i) lastNote = i + 1;
                                    else lastNote = i;
                                }
                                break;
                            }
                        }
                    }
                });

            }
        }));
        new Thread(dispatcher, "Audio Dispatcher").start();

    }

    boolean isDone(String currentString) {
        String newString = convertToBase10(currentString);
        return newString.length() == 10 && newString.charAt(0) - '0' >= 7;
    }

    @Override
    protected void onResume() {
        super.onResume();
        backgroundLayout = (LinearLayout) findViewById(R.id.backgroundLayout);
        backgrounds = new Drawable[]{getDrawable(R.drawable.ripple), getDrawable(R.drawable.ripple2)};
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tarsos_ds, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void playString(String number) {

        numberGlobal = convertToBase7(number);
        prevGlobal = 0;
        playSound(0);

    }

    private String convertToBase10(String number) {
        long originalNumber = 0;
        int len = number.length();
        long mult = 1, digit;
        for (int i = len - 1; i >= 0; i--) {
            digit = number.charAt(i) - '0';
            digit *= mult;
            originalNumber += digit;
            mult *= 7;
        }

        Log.d("abc", String.valueOf(originalNumber));

        String ret = "";
        while (originalNumber > 0) {
            digit = originalNumber % 10;
            originalNumber /= 10;
            ret = digit + ret;
        }


        return ret;
    }

    private String convertToBase7(String number) {

        long originalNumber = 0;
        int len = number.length();
        long mult = 1, digit;
        for (int i = len - 1; i >= 0; i--) {
            digit = number.charAt(i) - '0';
            digit *= mult;
            originalNumber += digit;
            mult *= 10;
        }


        String ret = "";
        while (originalNumber > 0) {
            digit = originalNumber % 7;
            originalNumber /= 7;
            ret = digit + ret;
        }


        return ret;
    }

    public void playSound(final int index) {
        if (index >= numberGlobal.length()) return;
        Log.d("abc", "Value of numberGlobal: " + numberGlobal);
        int songIndex = numberGlobal.charAt(index) - '0';
        if (prevGlobal <= songIndex) songIndex++;
        prevGlobal = songIndex;
        mp = MediaPlayer.create(getApplicationContext(), songs[songIndex]);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
                playSound(index + 1);
            }
        });
        mp.start();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tarsos_ds,
                    container, false);

            return rootView;
        }
    }
}
