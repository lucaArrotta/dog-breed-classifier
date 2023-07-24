package aflak.me.tensorflowlitexor;

/**
 * Created by Luca on 18/08/2018.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.os.TraceCompat;
import android.util.Log;

import static android.os.Trace.beginSection;
import static android.os.Trace.endSection;

/**
 * A classifier specialized to label images using TensorFlow.
 */
public class TensorFlowImageClassifier implements Classifier {
    private static final String TAG = "TensorFlowClassifier";

    private static final int MAX_RESULTS = 5;
    private static final float THRESHOLD = 0.1f;

    private String inputName;
    private String outputName;
    private int inputSize;
    private int imageMean;
    private float imageStd;

    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private float[] floatValues;
    private float[] outputs;
    private String[] outputNames;

    private boolean logStats = false;
    private TensorFlowInferenceInterface inferenceInterface;
    private TensorFlowImageClassifier() {}



    public static Classifier create(
            AssetManager assetManager, String modelFilename, String labelFilename, int inputSize, int imageMean, float imageStd, String inputName, String outputName){
        TensorFlowImageClassifier c = new TensorFlowImageClassifier();
        c.inputName = inputName;
        c.outputName = outputName;

        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        Log.d(TAG, "reading labels from : " + actualFilename);
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(assetManager.open(actualFilename)));
            String line;
            while((line = br.readLine()) != null){
                c.labels.add(line);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("failed reading labels" , e);
        }

        c.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);

        final Operation operation = c.inferenceInterface.graphOperation(outputName);
        final int numClasses = (int)operation.output(0).shape().size(1);
        Log.d(TAG, "reading " + c.labels.size() + " labels, size of output layers : " + numClasses);

        c.inputSize = inputSize;
        c.imageMean = imageMean;
        c.imageStd = imageStd;

        c.outputNames = new String[]{outputName};
        c.intValues = new int[inputSize * inputSize];
        c.floatValues = new float[inputSize * inputSize * 3];
        c.outputs = new float[numClasses];

        return c;
    }

    public List<Recognition> recognizeImage(final Bitmap bitmap){
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for(int i = 0; i < intValues.length; i++){
            final int val = intValues[i];

            // reverse the color orderings.
            floatValues[i*3] = (float) (Color.blue(val) / 255.0);
            floatValues[i*3 + 1] = (float) (Color.green(val) / 255.0);
            floatValues[i*3 + 2] = (float) (Color.red(val) / 255.0);
        }

        inferenceInterface.feed(inputName, floatValues, 1, inputSize, inputSize, 3);

        inferenceInterface.run(outputNames, logStats);

        inferenceInterface.fetch(outputName, outputs);

        PriorityQueue<Recognition> pq = new PriorityQueue<Recognition>(
                3,
                new Comparator<Recognition>(){
                    public int compare(Recognition lhs, Recognition rhs){
                        return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                    }
                }
        );

        for(int i = 0; i < outputs.length; ++i){
            if(outputs[i] > THRESHOLD){
                pq.add(
                        new Recognition("" + i, labels.size() > i ? labels.get(i) : "unknown", outputs[i], null));
            }
        }

        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        int recognitionSize = Math.min(pq.size(), MAX_RESULTS);
        for(int i = 0; i < recognitionSize; ++i){
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

    public void enableStatLogging(boolean logStats){this.logStats = logStats;}
    public String getStatString(){return inferenceInterface.getStatString();}
    public void close(){inferenceInterface.close();}
}