package be.tarsos.tarsos.dsp.android.ui;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

public class TarsosDSPActivity extends ActionBarActivity {

    private double[] scale = new double[]{261.63, 311.13, 349.23, 369.99, 392.00, 466.16};
    private double tolerance = 0.01;
    private int lastNote = -1;

    EditText editText;
    Button playButton;
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
                        TextView text = (TextView) findViewById(R.id.textView1);
                        text.setText("" + pitchInHz);

                        TextView resultView = (TextView) findViewById(R.id.resultTextView);

                        for (int i = 0; i < scale.length; ++i) {
                            if (pitchInHz < (1 + tolerance) * scale[i] &&
                                    pitchInHz > (1 - tolerance) * scale[i] &&
                                    i != lastNote) {
                                resultView.setText(resultView.getText().toString() + " " + i);
                                lastNote = i;
                                break;
                            }
                        }
                    }
                });

            }
        }));
        new Thread(dispatcher, "Audio Dispatcher").start();

    }

    @Override
    protected void onResume() {
        super.onResume();
        editText = (EditText) findViewById(R.id.editText1);
        playButton = (Button) findViewById(R.id.button1);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String text = editText.getText().toString();
                playString(text);
            }
        });
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

    String numberGlobal;

    public void playString(String number) {

        numberGlobal = convertToBase7(number);
        playSound(0);

    }

    private String convertToBase7(String number) {

        int originalNumber = 0;
        int len = number.length();
        int mult = 1, digit;
        for(int i = len - 1; i >= 0; i--) {
            digit = number.charAt(i) - '0';
            digit *= mult;
            originalNumber += digit;
            mult *= 10;
        }


        String ret = "";
        while(originalNumber > 0) {
            digit = originalNumber % 7;
            originalNumber /= 7;
            ret = digit + ret;
        }


        return ret;
    }

    int[] songs = {R.raw.sa, R.raw.re, R.raw.ga, R.raw.ma, R.raw.pa, R.raw.dha, R.raw.ni};

    MediaPlayer mp;

    public void playSound(final int index) {
        if(index >= numberGlobal.length()) return;
        Log.d("abc", "Value of numberGlobal: " + numberGlobal);
        mp = MediaPlayer.create(getApplicationContext(), songs[numberGlobal.charAt(index) - '0']);
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
