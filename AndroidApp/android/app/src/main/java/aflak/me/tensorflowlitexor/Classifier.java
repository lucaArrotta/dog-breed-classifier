package aflak.me.tensorflowlitexor;

/**
 * Created by Luca on 18/08/2018.
 */

import android.graphics.Bitmap;
import android.graphics.RectF;

import java.util.List;

/**
 * Generic interface for interacting with different recognition engines.
 */
public interface Classifier {

    public class Recognition{
        private final String id;
        private final String title;
        private final Float confidence;
        private RectF location;

        public Recognition(
                final String id, final String title, final Float confidence, final RectF location){
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getId(){return id;}
        public String getTitle(){return title;}
        public Float getConfidence(){return confidence;}
        public RectF getLocation(){return location;}
        public void setLocation(RectF location){this.location = location;}

        public String toString(){
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }

    List<Recognition> recognizeImage(Bitmap bitmap);
    void enableStatLogging(final boolean debug);
    String getStatString();
    void close();
}
