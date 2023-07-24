package aflak.me.tensorflowlitexor;

import android.Manifest;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private TensorFlowInferenceInterface inferenceInterface;
    private static final String TAG = "MainActivity";

    private static final int INPUT_SIZE = 200;
    private static final String INPUT_NAME = "batch_normalization_1_input";
    private static final String OUTPUT_NAME = "dense_1/Softmax";

    //private static final String MODEL_FILE = "file:///android_asset/modello_ottimizzato_con_script.pb";
    private static final String MODEL_FILE = "file:///android_asset/my_model_77.68%.h5.pb";
    private static final String LABEL_FILE = "file:///android_asset/labels.txt";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    Button btnpic;
    Button btntest;
    Button fromGallery;
    ImageView imgTakenPic;
    ProgressBar progressBar;
    TextView textView;
    private static final int CAM_REQUEST=1313;
    private static final int GALLERY_REQUEST=1414;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // keep the screen on

        initTensor();

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAM_REQUEST);

        btnpic = (Button) findViewById(R.id.button);
        fromGallery = (Button) findViewById(R.id.buttonGallery);
        btntest = (Button) findViewById(R.id.buttonTest);
        imgTakenPic = (ImageView)findViewById(R.id.imageView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textView = (TextView) findViewById(R.id.textView);

        btnpic.setOnClickListener(new btnTakePhotoClicker());
        fromGallery.setOnClickListener(new btnPickFromGallery());
        btntest.setOnClickListener(new btnTestAccuracy());
    }


    public void initTensor(){
        int IMAGE_MEAN = 128;       
        int IMAGE_STD = 128;        
        classifier = TensorFlowImageClassifier.create(
                getAssets(),
                MODEL_FILE,
                LABEL_FILE,
                INPUT_SIZE,
                IMAGE_MEAN,
                IMAGE_STD,
                INPUT_NAME,
                OUTPUT_NAME
        );
    }


    private String dogNames[] = {"Chihuahua", "Maltese_dog", "Blenheim_spaniel", "toy_terrier", "basset", "beagle", "bloodhound", "Walker_hound", "otterhound", "Weimaraner",
            "Irish_terrier",
            "Boston_bull",
            "silky_terrier",
            "flat-coated_retriever",
            "golden_retriever",
            "Labrador_retriever",
            "Gordon_setter",
            "cocker_spaniel",
            "Old_English_sheepdog",
            "Shetland_sheepdog",
            "Border_collie",
            "Rottweiler",
            "German_shepherd",
            "Doberman",
            "Greater_Swiss_Mountain_dog",
            "boxer",
            "French_bulldog",
            "Saint_Bernard",
            "Siberian_husky",
            "Pomeranian"};

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAM_REQUEST){
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, false);
            imgTakenPic.setImageBitmap(resizedBitmap);

            textView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            final List<Classifier.Recognition> results = classifier.recognizeImage(resizedBitmap);
            String t = "Dog Breed:\n";
            for(int i=0; i<results.size(); i++) {
                t += dogNames[Integer.parseInt(results.get(i).getTitle())] + ": " + new DecimalFormat("##.##").format(results.get(i).getConfidence()*100) + "%\n";
            }

            textView.setText(t);
            textView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }

        if(requestCode == GALLERY_REQUEST){
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(
                        this.getContentResolver(), data.getData());

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (bitmap != null) {
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, false);
                imgTakenPic.setImageBitmap(resizedBitmap);

                textView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);

                final List<Classifier.Recognition> results = classifier.recognizeImage(resizedBitmap);
                String t = "Dog Breed:\n";
                for(int i=0; i<results.size(); i++) {
                    t += dogNames[Integer.parseInt(results.get(i).getTitle())] + ": " + new DecimalFormat("##.##").format(results.get(i).getConfidence()*100) + "%\n";
                }

                textView.setText(t);
                textView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }
    }


    class btnTakePhotoClicker implements  Button.OnClickListener{
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent,CAM_REQUEST);
        }
    }

    class btnPickFromGallery implements  Button.OnClickListener{
        @Override
        public void onClick(View view) {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, GALLERY_REQUEST);
        }
    }

    class btnTestAccuracy implements  Button.OnClickListener{
        @Override
        public void onClick(View view) {
            textView.setText("");
            textView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            final AssetManager am = getAssets();

            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        String[] l = am.list("testimages/Test");
                        String folder;
                        String path;
                        Bitmap b;
                        float counter = 0;
                        int trues = 0;
                        int falses = 0;
                        InputStream is = null;
                        Classifier.Recognition res = null;
                        //Log.d("Testing", "folders: " + l.length);
                        for(int i=0; i<l.length; i++) {
                            folder = l[i];
                            //Log.d("Testing", i + "/" + l.length + " folder: " + folder);
                            String[] l2 = am.list("testimages/Test/" + folder);
                            //Log.d("Testing", l[i] + " images: " + l2.length);
                            for(int j=0; j<l2.length; j++) {
                                counter++;
                                aggiornaTextView(counter);
                                path = "testimages/Test/" + folder + "/" + l2[j];
                                //Log.d("Testing", j + "/" + l2.length + " " + path);
                                is = am.open(path);
                                b = BitmapFactory.decodeStream(is);
                                final List<Classifier.Recognition> results = classifier.recognizeImage(b);
                                res = results.get(0);
                                if(dogNames[Integer.parseInt(res.getTitle())].equals(folder))
                                    trues += 1;
                                else
                                    falses += 1;
                                //Log.d("Testing", "      previsione: " + dogNames[Integer.parseInt(res.getTitle())] + " (" + res.getConfidence()*100 + "%), vero: " + folder + ", trues: " + trues + ", falses: " + falses);
                            }
                            //Log.d("Testing", "--------------------------------");
                        }
                        final float f = ((float) trues / 654) * 100;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText("Test Accuracy: " + new DecimalFormat("##.##").format(f) + "%");
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            thread.start();
        }
    }


    // Update the text view
    public void aggiornaTextView(final float counter) {
        runOnUiThread(new Runnable() {
            public void run() {
                textView.setText("Test accuracy computation... " + (int) ((counter / 654)*100) + "%");
            }
        });
    }
}
