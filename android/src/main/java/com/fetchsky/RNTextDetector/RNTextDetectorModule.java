
package com.fetchsky.RNTextDetector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Base64;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;

public class RNTextDetectorModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private FirebaseVisionTextRecognizer detector;
    private FirebaseVisionImage image;

    public RNTextDetectorModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        try {
            detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void detect(String base64, final Promise promise) {
        try {
            Bitmap img = getBitmap(base64);
            image = FirebaseVisionImage.fromBitmap(img);
            Task<FirebaseVisionText> result =
                    detector.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                    promise.resolve(getDataAsArray(firebaseVisionText));
                                }
                            })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(Exception e) {
                                            e.printStackTrace();
                                            promise.reject(e);
                                        }
                                    });
            ;
        } catch (Exception e) {
            promise.reject(e);
            e.printStackTrace();
        }
    }

    private Bitmap getBitmap(String base64) {
        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    /**
     * Converts firebaseVisionText into a map
     *
     * @param firebaseVisionText
     * @return
     */
    private WritableArray getDataAsArray(FirebaseVisionText firebaseVisionText) {
        WritableArray data = Arguments.createArray();
        WritableMap info = Arguments.createMap();
        WritableMap coordinates = Arguments.createMap();

        for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
            info = Arguments.createMap();
            coordinates = Arguments.createMap();

            Rect boundingBox = block.getBoundingBox();

            coordinates.putInt("top", boundingBox.top);
            coordinates.putInt("left", boundingBox.left);
            coordinates.putInt("width", boundingBox.width());
            coordinates.putInt("height", boundingBox.height());

            info.putMap("bounding", coordinates);
            info.putString("text", block.getText());
            info.putDouble("confidence", block.getConfidence());
            data.pushMap(info);
        }

        return data;
    }


    @Override
    public String getName() {
        return "RNTextDetector";
    }
}