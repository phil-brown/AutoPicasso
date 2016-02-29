package io.github.phdbrown.autopicasso;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import io.github.phdbrown.autopicasso.annotations.Picasso;

public class MainActivity extends AppCompatActivity implements com.squareup.picasso.Picasso.Listener {

    @Picasso
    (
            value = R.id.image,
            url = "http://square.github.io/picasso/static/sample.png",
            placeholder = R.mipmap.ic_launcher,
            log = true
    )
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AutoPicasso.load(this);
    }

    @Override
    public void onImageLoadFailed(com.squareup.picasso.Picasso picasso, Uri uri, Exception exception) {
        exception.printStackTrace();
    }
}
