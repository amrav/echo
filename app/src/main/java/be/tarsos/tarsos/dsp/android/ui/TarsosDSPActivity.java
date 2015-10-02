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

    //private double[] scale = new double[]{126, 147, 151.5, 174, 188, 214, 235, 255.5};
    private double[] scale = new double[] {800, 900, 1000, 1101, 1200, 1302, 1401, 1502};
    private double tolerance = 0.02;
    private int lastNote = 0, currCount = 0, curr = 0;

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
                                if(i == curr) {
                                    currCount++;
                                    Log.d("curr", "incremented a " + i);
                                }
                                else {
                                    Log.d("curr", "found a " + i);
                                    curr = i;
                                    currCount = 1;
                                }
                                if(currCount > 2) {
                                    if (lastNote < i) i--;
                                    String currentString = resultView.getText().toString() + i;
                                    resultView.setText(currentString);
                                    if (isDone(currentString) == true) {
                                        resultView.setText(convertToBase10(currentString));
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
        if(newString.length() == 10 && newString.charAt(0) - '0' >= 7) return true;
        else return false;
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
    int prevGlobal;

    public void playString(String number) {

        numberGlobal = convertToBase7(number);
        prevGlobal = 0;
        playSound(0);

    }

    private String convertToBase10(String number) {
        long  originalNumber = 0;
        int len = number.length();
        long mult = 1, digit;
        for(int i = len - 1; i >= 0; i--) {
            digit = number.charAt(i) - '0';
            digit *= mult;
            originalNumber += digit;
            mult *= 7;
        }

        Log.d("abc", String.valueOf(originalNumber));

        String ret = "";
        while(originalNumber > 0) {
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

    ///int[] songs = {R.raw.sa, R.raw.re2, R.raw.ga, R.raw.ma, R.raw.pa, R.raw.dha, R.raw.ni, R.raw.sa2};
    int[] songs = {R.raw.s800hz, R.raw.s900hz, R.raw.s1000hz, R.raw.s1100hz, R.raw.s1200hz, R.raw.s1300hz, R.raw.s1400hz, R.raw.s1500hz};
    MediaPlayer mp;

    public void playSound(final int index) {
        if(index >= numberGlobal.length()) {mp = null; return;}
        Log.d("abc", "Value of numberGlobal: " + numberGlobal);
        int songIndex = numberGlobal.charAt(index) - '0';
        if(prevGlobal <= songIndex) songIndex++;
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
