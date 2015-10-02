package be.tarsos.tarsos.dsp.android.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.Telephone;

public class SendContactActivity extends ActionBarActivity {

    EditText editText;
    Button playButton;
    String numberGlobal;
    int prevGlobal;
    MediaPlayer mp;
    int[] songs = {R.raw.s800hz5, R.raw.s900hz5, R.raw.s1000hz5, R.raw.s1100hz5, R.raw.s1200hz5, R.raw.s1300hz5, R.raw.s1400hz5, R.raw.s1500hz5};
    private LinearLayout backgroundLayout;
    private Drawable[] backgrounds;
    private int currentBackground = 0;
    private double[] scale = new double[] {800, 900, 1000, 1101, 1200, 1302, 1401, 1502};
    private double tolerance = 0.02;
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
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Log.d("intent type", type);

            Uri uri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);

            Log.d("intent uri", uri.toString());

            ContentResolver cr = getContentResolver();
            InputStream stream = null;
            try {
                stream = cr.openInputStream(uri);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            StringBuffer fileContent = new StringBuffer("");
            int ch;
            try {
                while ((ch = stream.read()) != -1) {
                    fileContent.append((char) ch);
                    //Log.d("intnet vcard char", "appending char");
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String data = new String(fileContent);

            Log.d("VCARD INTENT", "data: " + data);

            VCard vcard = Ezvcard.parse(data).first();
            Telephone t = vcard.getTelephoneNumbers().get(0);
            Log.d("phone number!", t.getText());

            String ph = t.getText();

            ph = ph.replaceAll("\\D", "");

            if (ph.length() > 10) ph = ph.substring(ph.length() - 10);

            Log.d("stripped ph", ph);

            playString(ph);

            //TextView textView = (TextView) findViewById(R.id.resultTextView);
            //textView.setText(type);
        }
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
