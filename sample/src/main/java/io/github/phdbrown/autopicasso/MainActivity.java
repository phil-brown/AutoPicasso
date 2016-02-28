package io.github.phdbrown.autopicasso;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import io.github.phdbrown.autopicasso.annotations.Picasso;

public class MainActivity extends AppCompatActivity {

    @Picasso(value = R.id.image, url = "http://square.github.io/picasso/static/sample.png")
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AutoPicasso.load(this);
    }
}
